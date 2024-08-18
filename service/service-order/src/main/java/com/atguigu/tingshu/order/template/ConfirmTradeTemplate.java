package com.atguigu.tingshu.order.template;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.order.helper.SignHelper;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author xk
 * @since 2024-08-18 15:38
 */
public abstract class ConfirmTradeTemplate {

    @Resource
    private RedisTemplate redisTemplate;
    
    public OrderInfoVo confirmTrade(TradeVo tradeVo) {
        // 创建一个订单确认页所需的vo模型
        OrderInfoVo orderInfoVo = new OrderInfoVo();
        // 调用 确认订单模版 方法
        trade(tradeVo, orderInfoVo);
        // 生成交易号(订单编号)，防重的唯一标识(幂等性)
        orderInfoVo.setTradeNo(IdWorker.getIdStr());
        // 设置默认的支付方式(微信支付)
        //orderInfoVo.setPayWay(SystemConstant.ORDER_PAY_WAY_WEIXIN);
        orderInfoVo.setPayWay(SystemConstant.ORDER_PAY_WAY_ACCOUNT);
        // 设置订单的交易类型
        orderInfoVo.setItemType(tradeVo.getItemType());
        // 为了防重：在redis中保存一份
        this.redisTemplate.opsForValue().set(RedisConstant.USER_TRADE_PREFIX + orderInfoVo.getTradeNo(), orderInfoVo.getTradeNo(),
                RedisConstant.USER_TRADE_TIMEOUT, TimeUnit.SECONDS);

        // 设置时间戳和签名
        orderInfoVo.setTimestamp(SignHelper.getTimestamp());
        orderInfoVo.setSign(SignHelper.getSign(JSON.parseObject(JSON.toJSONString(orderInfoVo), HashMap.class)));
        // 返回vo模型
        return orderInfoVo;
    }

    protected abstract void trade(TradeVo tradeVo, OrderInfoVo orderInfoVo);
}
