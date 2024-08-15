package com.atguigu.tingshu.order.service;

import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.baomidou.mybatisplus.extension.service.IService;

public interface OrderInfoService extends IService<OrderInfo> {
    /**
     * 订单交易
     *
     * @param tradeVo 订单确认对象
     * @return 订单对象
     */
    OrderInfoVo trade(TradeVo tradeVo);

    /**
     * 提交订单
     *
     * @param orderInfoVo 订单对象
     */
    void submitOrder(OrderInfoVo orderInfoVo);
}
