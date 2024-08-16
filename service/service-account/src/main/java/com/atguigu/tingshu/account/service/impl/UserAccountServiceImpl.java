package com.atguigu.tingshu.account.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.account.mapper.UserAccountDetailMapper;
import com.atguigu.tingshu.account.mapper.UserAccountMapper;
import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.constant.RabbitMqConstant;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.common.service.RabbitService;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.atguigu.tingshu.vo.account.AccountLockResultVo;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.seata.common.util.StringUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserAccountServiceImpl extends ServiceImpl<UserAccountMapper, UserAccount> implements UserAccountService {

    @Resource
    private UserAccountMapper userAccountMapper;

    @Resource
    private UserAccountDetailMapper userAccountDetailMapper;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private RabbitService rabbitService;

    @Transactional
    @Override
    public void saveUserAccount(Long userId) {
        UserAccount userAccount = new UserAccount();
        userAccount.setUserId(userId);
        this.save(userAccount);
    }

    @Override
    public BigDecimal getAvailableAmount() {
        Long userId = AuthContextHolder.getUserId();
        if (userId == null) {
            return BigDecimal.ZERO;
        }
        UserAccount userAccount = this.userAccountMapper.selectOne(Wrappers.lambdaQuery(UserAccount.class)
                .eq(UserAccount::getUserId, userId)
                .select(UserAccount::getAvailableAmount)
                .last("limit 1"));
        return userAccount == null ? BigDecimal.ZERO : userAccount.getAvailableAmount();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Result<AccountLockResultVo> checkAndLock(AccountLockVo accountLockVo) {
        String orderNo = accountLockVo.getOrderNo();
        String dataKey = RedisConstant.ACCOUNT_CHECK_INFO_PREFIX + orderNo;
        // 1.判断同一个订单是否重复验余额并锁账户
        String checkKey = RedisConstant.ACCOUNT_CHECK_LOCK_PREFIX + orderNo;
        Boolean result = this.redisTemplate.opsForValue().setIfAbsent(checkKey, "", 1, TimeUnit.HOURS);
        // 重复锁定的情况下，直接返回锁定结果
        if (!result) {
            // 2.从缓存中获取订单的锁定结果(缓存锁定结果的目的是为了方便将来减余额)
            String jsonStr = (String) this.redisTemplate.opsForValue().get(dataKey);
            if (StringUtils.isBlank(jsonStr)) {
                // 锁定中
                return Result.build(null, ResultCodeEnum.ACCOUNT_LOCK_REPEAT);
            }
            // 返回锁定结果
            AccountLockResultVo accountLockResultVo = JSON.parseObject(jsonStr, AccountLockResultVo.class);
            return Result.ok(accountLockResultVo);
        }

        // 3.没有锁定过的情况下，查询账户余额并锁账户
        UserAccount userAccount = this.userAccountMapper.check(accountLockVo.getUserId(), accountLockVo.getAmount());
        if (userAccount == null) {
            // 余额不足：先释放checkKey，再响应余额不足锁定失败
            this.redisTemplate.delete(checkKey);
            return Result.build(null, ResultCodeEnum.ACCOUNT_LESS);
        }

        // 4.锁住账户对应余额
        if (this.userAccountMapper.lock(accountLockVo.getUserId(), accountLockVo.getAmount()) == 0) {
            // 锁住账户余额失败：先释放checkKey，再响应锁住账户余额失败
            this.redisTemplate.delete(checkKey);
            return Result.build(null, ResultCodeEnum.ACCOUNT_LOCK_ERROR);
        }
        // 5.记录账户明细
        this.saveAccountDetail(accountLockVo, SystemConstant.ACCOUNT_TRADE_TYPE_LOCK, "账户锁定余额：");

        // 6.缓存锁定信息到redis
        AccountLockResultVo accountLockResultVo = new AccountLockResultVo();
        BeanUtils.copyProperties(accountLockVo, accountLockResultVo);
        this.redisTemplate.opsForValue().set(dataKey, JSON.toJSONString(accountLockResultVo));

        return Result.ok(accountLockResultVo);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void minus(String orderNo) {
        String dataKey = RedisConstant.ACCOUNT_CHECK_INFO_PREFIX + orderNo;
        // 1.判断同一个订单是否重复扣减余额(业务去重)
        String minusKey = RedisConstant.ACCOUNT_CHECK_MINUS_PREFIX + orderNo;
        Boolean flag = this.redisTemplate.opsForValue().setIfAbsent(minusKey, "", 1, TimeUnit.HOURS);
        if (!flag) return;

        // 2.获取锁定余额的缓存信息
        String jsonData = (String) this.redisTemplate.opsForValue().get(dataKey);
        if (StringUtils.isBlank(jsonData)) return;

        // 3.扣减账户余额
        AccountLockResultVo accountLockResultVo = JSON.parseObject(jsonData, AccountLockResultVo.class);
        int count = this.userAccountMapper.minus(accountLockResultVo.getUserId(), accountLockResultVo.getAmount());
        if (count == 0) {
            // 扣减账户余额失败：先释放minusKey，再响应扣减账户余额失败
            this.redisTemplate.delete(minusKey);
            throw new GuiguException(ResultCodeEnum.ACCOUNT_MINUSLOCK_ERROR);
        }
        // 记录日志
        AccountLockVo accountLockVo = new AccountLockVo();
        BeanUtils.copyProperties(accountLockResultVo, accountLockVo);
        accountLockVo.setOrderNo(orderNo);
        this.saveAccountDetail(accountLockVo, SystemConstant.ACCOUNT_TRADE_TYPE_MINUS, "账户扣减余额：");
        // 发送消息给order更新订单状态
        this.rabbitService.sendMessage(RabbitMqConstant.EXCHANGE_ORDER_PAY_SUCCESS,
                RabbitMqConstant.ROUTING_ORDER_PAY_SUCCESS, orderNo);
        // 扣减账户金额之后，删除锁定缓存。以防止重复扣减
        this.redisTemplate.delete(dataKey);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void unlock(String orderNo) {
        String dataKey = RedisConstant.ACCOUNT_CHECK_INFO_PREFIX + orderNo;
        // 1.判断同一个订单是否重复解锁余额(业务去重)
        String unlockKey = RedisConstant.ACCOUNT_CHECK_UNLOCK_PREFIX + orderNo;
        Boolean flag = this.redisTemplate.opsForValue().setIfAbsent(unlockKey, "", 1, TimeUnit.HOURS);
        if (!flag) return;

        // 2.获取锁定余额的缓存信息
        String jsonDataStr = (String) this.redisTemplate.opsForValue().get(dataKey);
        if (StringUtils.isBlank(jsonDataStr)) return;

        // 3.解锁余额
        AccountLockResultVo accountLockResultVo = JSON.parseObject(jsonDataStr, AccountLockResultVo.class);
        int count = this.userAccountMapper.unlock(accountLockResultVo.getUserId(), accountLockResultVo.getAmount());
        if (count == 0) {
            // 解锁账户余额失败：先释放unlockKey，再响应锁住账户余额失败
            this.redisTemplate.delete(unlockKey);
            throw new GuiguException(ResultCodeEnum.ACCOUNT_UNLOCK_ERROR);
        }
        // 记录日志
        AccountLockVo accountLockVo = new AccountLockVo();
        BeanUtils.copyProperties(accountLockResultVo, accountLockVo);
        accountLockVo.setOrderNo(orderNo);
        this.saveAccountDetail(accountLockVo, SystemConstant.ACCOUNT_TRADE_TYPE_UNLOCK, "账户解锁余额：");
        // 解锁账户金额之后，删除锁定缓存。以防止重复解锁
        this.redisTemplate.delete(dataKey);
    }

    private void saveAccountDetail(AccountLockVo accountLockVo, String tradeType, String prefix) {
        UserAccountDetail userAccountDetail = new UserAccountDetail();
        BeanUtils.copyProperties(accountLockVo, userAccountDetail);
        userAccountDetail.setTitle(prefix + accountLockVo.getContent());
        userAccountDetail.setTradeType(tradeType);
        this.userAccountDetailMapper.insert(userAccountDetail);
    }
}
