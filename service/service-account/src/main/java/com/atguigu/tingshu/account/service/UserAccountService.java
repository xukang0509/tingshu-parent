package com.atguigu.tingshu.account.service;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.vo.account.AccountLockResultVo;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;

public interface UserAccountService extends IService<UserAccount> {


    void saveUserAccount(Long userId);

    BigDecimal getAvailableAmount();

    /**
     * 验余额并锁账户
     *
     * @param accountLockVo 锁定金额对象
     * @return 结果
     */
    Result<AccountLockResultVo> checkAndLock(AccountLockVo accountLockVo);

    /**
     * 扣减余额
     *
     * @param orderNo 订单号
     */
    void minus(String orderNo);

    /**
     * 解锁余额
     *
     * @param orderNo 订单号
     */
    void unlock(String orderNo);
}
