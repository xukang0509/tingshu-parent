package com.atguigu.tingshu.order.service;

import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
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

    /**
     * 更新订单状态
     *
     * @param orderNo 订单号
     */
    void updateOrderStatus(String orderNo);

    /**
     * 根据订单号查询订单
     *
     * @param orderNo 订单号
     * @return 订单
     */
    OrderInfo getOrderInfoByOrderNo(String orderNo);

    /**
     * 查看我的订单
     *
     * @param pageNum  页码
     * @param pageSize 每页显示个数
     * @return 订单列表
     */
    IPage<OrderInfo> findUserPage(Integer pageNum, Integer pageSize);

    /**
     * 取消订单
     *
     * @param orderNo 订单号
     */
    void cancelOrder(String orderNo);

}
