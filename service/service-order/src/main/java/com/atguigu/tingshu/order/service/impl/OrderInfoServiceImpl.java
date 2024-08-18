package com.atguigu.tingshu.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.account.client.UserAccountFeignClient;
import com.atguigu.tingshu.album.client.TrackInfoFeignClient;
import com.atguigu.tingshu.common.constant.RabbitMqConstant;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.common.service.RabbitService;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.order.OrderDerate;
import com.atguigu.tingshu.model.order.OrderDetail;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.model.user.UserPaidTrack;
import com.atguigu.tingshu.order.helper.SignHelper;
import com.atguigu.tingshu.order.mapper.OrderDerateMapper;
import com.atguigu.tingshu.order.mapper.OrderDetailMapper;
import com.atguigu.tingshu.order.mapper.OrderInfoMapper;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.atguigu.tingshu.order.template.ConfirmTradeClient;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.vo.account.AccountLockResultVo;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import com.atguigu.tingshu.vo.order.OrderDerateVo;
import com.atguigu.tingshu.vo.order.OrderDetailVo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Resource
    private OrderInfoMapper orderInfoMapper;

    @Resource
    private OrderDetailMapper orderDetailMapper;

    @Resource
    private OrderDerateMapper orderDerateMapper;

    @Resource
    private UserInfoFeignClient userInfoFeignClient;

    @Resource
    private TrackInfoFeignClient trackInfoFeignClient;

    @Resource
    private UserAccountFeignClient userAccountFeignClient;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private RabbitService rabbitService;

    @Resource
    private ConfirmTradeClient confirmTradeClient;

    @Override
    public OrderInfoVo trade(TradeVo tradeVo) {
        return confirmTradeClient.execute(tradeVo);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void submitOrder(OrderInfoVo orderInfoVo) {
        // 校验签名: 确保参数没有变化
        HashMap<String, Object> map = JSON.parseObject(JSON.toJSONString(orderInfoVo), HashMap.class);
        // 为了保证验签通过，这里需要和购买时一致。（提交订单时，选择的支付方式可能和购买时不一致）
        //map.put("payWay", SystemConstant.ORDER_PAY_WAY_WEIXIN);
        map.put("payWay", SystemConstant.ORDER_PAY_WAY_ACCOUNT);
        SignHelper.checkSign(map);

        // 验证订单号是否为空
        String tradeNo = orderInfoVo.getTradeNo();
        if (!StringUtils.hasText(tradeNo)) {
            throw new GuiguException(ResultCodeEnum.ILLEGAL_REQUEST);
        }
        // 如果详情列表为空：则直接抛出异常
        if (CollectionUtils.isEmpty(orderInfoVo.getOrderDetailVoList())) {
            throw new GuiguException(ResultCodeEnum.ILLEGAL_REQUEST);
        }

        // 防重校验(保证幂等性)：根据订单交易号查询缓存中是否有数据，如果有则表明该订单未被处理提交
        String script = """
                if redis.call('get', KEYS[1]) == ARGV[1]
                then
                    return redis.call('del', KEYS[1])
                else
                    return 0
                end
                """;
        DefaultRedisScript<Boolean> redisScript = new DefaultRedisScript<>(script, Boolean.class);
        Boolean flag = (Boolean) redisTemplate.execute(redisScript,
                Arrays.asList(RedisConstant.USER_TRADE_PREFIX + tradeNo), tradeNo);
        if (!flag) {
            // 重复提交：抛出异常
            throw new GuiguException(ResultCodeEnum.ORDER_SUBMIT_REPEAT);
        }

        // 判断是否重复购买 UserPaidAlbum UserPaidTrack
        if (SystemConstant.ORDER_ITEM_TYPE_ALBUM.equals(orderInfoVo.getItemType())) {
            // 判断是否重复购买专辑
            for (OrderDetailVo orderDetailVo : orderInfoVo.getOrderDetailVoList()) {
                // 根据专辑id查询该用户是否订购过当前专辑，如果订购过则抛出异常
                Result<Boolean> paidAlbumStatRes = this.userInfoFeignClient.getPaidAlbumStat(orderDetailVo.getItemId());
                Assert.notNull(paidAlbumStatRes, "根据专辑id查询当前用户是否订购过该专辑信息失败！");
                Boolean isPaidAlbum = paidAlbumStatRes.getData();
                Assert.notNull(isPaidAlbum, "未获取到专辑购买信息！");
                if (isPaidAlbum) {
                    // 如果购买过则抛出异常：不可重复购买
                    throw new GuiguException(ResultCodeEnum.REPEAT_BUY_ERROR);
                }
            }
        } else if (SystemConstant.ORDER_ITEM_TYPE_TRACK.equals(orderInfoVo.getItemType())) {
            // 判断是否重复购买声音
            // 根据声音id获取声音
            List<OrderDetailVo> orderDetailVoList = orderInfoVo.getOrderDetailVoList();
            Result<TrackInfo> trackInfoRes = this.trackInfoFeignClient.getTrackInfo(orderDetailVoList.get(0).getItemId());
            Assert.notNull(trackInfoRes, "远程调用声音信息失败！");
            TrackInfo trackInfo = trackInfoRes.getData();
            Assert.notNull(trackInfo, "声音信息为空！");
            // 获取当前用户已经购买的声音所在专辑下的所有声音
            Result<List<UserPaidTrack>> userPaidTracksRes = this.userInfoFeignClient.getPaidTracksByAlbumIdAndUserId(trackInfo.getAlbumId());
            Assert.notNull(userPaidTracksRes, "远程调用当前用户购买的订单信息列表失败！");
            if (!CollectionUtils.isEmpty(userPaidTracksRes.getData())) {
                Set<Long> paidTrackIds = userPaidTracksRes.getData().stream().map(UserPaidTrack::getTrackId).collect(Collectors.toSet());
                if (orderDetailVoList.stream().anyMatch(orderDetailVo -> paidTrackIds.contains(orderDetailVo.getItemId()))) {
                    // 如果购买过则抛出异常：不可重复购买
                    throw new GuiguException(ResultCodeEnum.REPEAT_BUY_ERROR);
                }
            }
        }

        // 判断支付类型
        if (!SystemConstant.ORDER_PAY_WAY_ACCOUNT.equals(orderInfoVo.getPayWay())) {
            // 非余额支付：目前是默认微信支付
            this.saveOrder(orderInfoVo, SystemConstant.ORDER_STATUS_UNPAID);
            // 定时取消订单
            this.rabbitService.sendMessage(RabbitMqConstant.EXCHANGE_ORDER, RabbitMqConstant.ROUTING_ORDER_DEAD,
                    orderInfoVo.getTradeNo());
        } else {
            List<OrderDetailVo> orderDetailVoList = orderInfoVo.getOrderDetailVoList();
            Long userId = AuthContextHolder.getUserId();
            // 余额支付
            // 需要先验证余额并锁定余额
            AccountLockVo accountLockVo = new AccountLockVo();
            accountLockVo.setOrderNo(tradeNo);
            accountLockVo.setUserId(userId);
            accountLockVo.setAmount(orderInfoVo.getOrderAmount());
            accountLockVo.setContent(orderDetailVoList.get(0).getItemName());
            // 验余额锁余额
            Result<AccountLockResultVo> accountLockResultVoRes = this.userAccountFeignClient.checkAndLock(accountLockVo);
            Assert.notNull(accountLockResultVoRes, "远程调用验余额锁余额接口失败");
            if (!Objects.equals(accountLockResultVoRes.getCode(), ResultCodeEnum.SUCCESS.getCode())) {
                // 如果响应结果不是200，则说明验余额锁余额失败
                throw new GuiguException(accountLockResultVoRes.getCode(), accountLockResultVoRes.getMessage());
            }
            try {
                // 新增订单
                this.saveOrder(orderInfoVo, SystemConstant.ORDER_STATUS_UNPAID);
                // 新增订单成功：发送消息扣键余额
                this.rabbitService.sendMessage(RabbitMqConstant.EXCHANGE_ACCOUNT_MINUS,
                        RabbitMqConstant.ROUTING_ACCOUNT_MINUS, tradeNo);
            } catch (Exception e) {
                // 新增订单失败：发送消息解锁余额
                this.rabbitService.sendMessage(RabbitMqConstant.EXCHANGE_ACCOUNT_UNLOCK,
                        RabbitMqConstant.ROUTING_ACCOUNT_UNLOCK, tradeNo);
                e.printStackTrace();
                throw new GuiguException(ResultCodeEnum.DATA_ERROR);
            }
        }
    }


    private void saveOrder(OrderInfoVo orderInfoVo, String orderStatus) {
        // 保存订单
        OrderInfo orderInfo = new OrderInfo();
        BeanUtils.copyProperties(orderInfoVo, orderInfo);
        orderInfo.setUserId(AuthContextHolder.getUserId());
        orderInfo.setOrderNo(orderInfoVo.getTradeNo());
        orderInfo.setOrderStatus(orderStatus);
        orderInfo.setOrderTitle(orderInfoVo.getOrderDetailVoList().get(0).getItemName());
        this.orderInfoMapper.insert(orderInfo);

        Long orderId = orderInfo.getId();
        // 保存订单的详情信息
        for (OrderDetailVo orderDetailVo : orderInfoVo.getOrderDetailVoList()) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(orderDetailVo, orderDetail);
            orderDetail.setOrderId(orderId);
            this.orderDetailMapper.insert(orderDetail);
        }

        // 保存订单减免明细信息
        if (!CollectionUtils.isEmpty(orderInfoVo.getOrderDerateVoList())) {
            for (OrderDerateVo orderDerateVo : orderInfoVo.getOrderDerateVoList()) {
                OrderDerate orderDerate = new OrderDerate();
                BeanUtils.copyProperties(orderDerateVo, orderDerate);
                orderDerate.setOrderId(orderId);
                this.orderDerateMapper.insert(orderDerate);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateOrderStatus(String orderNo) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderStatus(SystemConstant.ORDER_STATUS_PAID);
        if (this.update(orderInfo, Wrappers.lambdaUpdate(OrderInfo.class).eq(OrderInfo::getOrderNo, orderNo))) {
            // 如果更新订单状态成功，发送消息更新用户购买信息
            orderInfo = this.getOne(Wrappers.lambdaQuery(OrderInfo.class).eq(OrderInfo::getOrderNo, orderNo));
            // 组装消息内容
            UserPaidRecordVo userPaidRecordVo = new UserPaidRecordVo();
            BeanUtils.copyProperties(orderInfo, userPaidRecordVo);
            // 查询订单详情
            List<OrderDetail> orderDetailList = this.orderDetailMapper.selectList(Wrappers.lambdaQuery(OrderDetail.class)
                    .eq(OrderDetail::getOrderId, orderInfo.getId())
                    .select(OrderDetail::getItemId));
            userPaidRecordVo.setItemIdList(orderDetailList.stream().map(OrderDetail::getItemId).toList());
            // 发送消息给MQ
            this.rabbitService.sendMessage(RabbitMqConstant.EXCHANGE_USER_PAY_RECORD,
                    RabbitMqConstant.ROUTING_USER_PAY_RECORD, JSON.toJSONString(userPaidRecordVo));
        } else {
            // TODO:如果更新订单状态失败，则发送消息把余额还原
        }
    }

    @Override
    public OrderInfo getOrderInfoByOrderNo(String orderNo) {
        OrderInfo orderInfo = this.orderInfoMapper.selectOne(Wrappers.lambdaQuery(OrderInfo.class)
                .eq(OrderInfo::getOrderNo, orderNo)
                .eq(OrderInfo::getUserId, AuthContextHolder.getUserId())
                .last("limit 1"));
        if (orderInfo == null) return null;
        List<OrderDerate> orderDerateList = this.orderDerateMapper.selectList(Wrappers.lambdaQuery(OrderDerate.class)
                .eq(OrderDerate::getOrderId, orderInfo.getId()));
        orderInfo.setOrderDerateList(orderDerateList);
        List<OrderDetail> orderDetailList = this.orderDetailMapper.selectList(Wrappers.lambdaQuery(OrderDetail.class)
                .eq(OrderDetail::getOrderId, orderInfo.getId()));
        orderInfo.setOrderDetailList(orderDetailList);

        switch (orderInfo.getOrderStatus()) {
            case SystemConstant.ORDER_STATUS_UNPAID -> orderInfo.setOrderStatusName("未支付");
            case SystemConstant.ORDER_STATUS_PAID -> orderInfo.setOrderStatusName("已支付");
            case SystemConstant.ORDER_STATUS_CANCEL -> orderInfo.setOrderStatusName("已取消");
        }

        switch (orderInfo.getPayWay()) {
            case SystemConstant.ORDER_PAY_WAY_ACCOUNT -> orderInfo.setPayWayName("余额支付");
            case SystemConstant.ORDER_PAY_WAY_WEIXIN -> orderInfo.setPayWayName("微信支付");
            case SystemConstant.ORDER_PAY_WAY_ALIPAY -> orderInfo.setPayWayName("阿里支付");
        }
        return orderInfo;
    }

    @Override
    public IPage<OrderInfo> findUserPage(Integer pageNum, Integer pageSize) {
        // 分页查询订单 不要使用关联查询，否则显示会有问题
        Page<OrderInfo> orderInfoPage = this.page(new Page<>(pageNum, pageSize), Wrappers.lambdaQuery(OrderInfo.class)
                .eq(OrderInfo::getUserId, AuthContextHolder.getUserId())
                .orderByDesc(OrderInfo::getId));
        // 遍历当前页数据，查询每个订单的订单详情
        List<OrderInfo> orderInfoList = orderInfoPage.getRecords();
        if (CollectionUtils.isEmpty(orderInfoList)) return orderInfoPage;

        for (OrderInfo orderInfo : orderInfoList) {
            List<OrderDetail> orderDetailList = this.orderDetailMapper
                    .selectList(Wrappers.lambdaQuery(OrderDetail.class)
                            .eq(OrderDetail::getOrderId, orderInfo.getId()));
            orderInfo.setOrderDetailList(orderDetailList);

            switch (orderInfo.getOrderStatus()) {
                case SystemConstant.ORDER_STATUS_UNPAID -> orderInfo.setOrderStatusName("未支付");
                case SystemConstant.ORDER_STATUS_PAID -> orderInfo.setOrderStatusName("已支付");
                case SystemConstant.ORDER_STATUS_CANCEL -> orderInfo.setOrderStatusName("已取消");
            }

            switch (orderInfo.getPayWay()) {
                case SystemConstant.ORDER_PAY_WAY_ACCOUNT -> orderInfo.setPayWayName("余额支付");
                case SystemConstant.ORDER_PAY_WAY_WEIXIN -> orderInfo.setPayWayName("微信支付");
                case SystemConstant.ORDER_PAY_WAY_ALIPAY -> orderInfo.setPayWayName("阿里支付");
            }
        }
        return orderInfoPage;
    }

    @Override
    public void cancelOrder(String orderNo) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderStatus(SystemConstant.ORDER_STATUS_CANCEL);
        this.update(orderInfo, Wrappers.lambdaQuery(OrderInfo.class)
                .eq(OrderInfo::getOrderNo, orderNo)
                .ne(OrderInfo::getOrderStatus, SystemConstant.ORDER_STATUS_PAID));
    }
}
