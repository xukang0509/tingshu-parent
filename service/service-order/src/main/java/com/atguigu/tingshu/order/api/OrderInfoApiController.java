package com.atguigu.tingshu.order.api;

import com.atguigu.tingshu.common.login.AuthLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "订单管理")
@RestController
@RequestMapping("api/order/orderInfo")
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderInfoApiController {

    @Autowired
    private OrderInfoService orderInfoService;

    // http://127.0.0.1:8500/api/order/orderInfo/trade
    @AuthLogin
    @Operation(summary = "订单交易")
    @PostMapping("trade")
    public Result<OrderInfoVo> trade(@RequestBody @Validated TradeVo tradeVo) {
        return Result.ok(this.orderInfoService.trade(tradeVo));
    }

    // http://127.0.0.1:8500/api/order/orderInfo/submitOrder
    @AuthLogin
    @Operation(summary = "提交订单")
    @PostMapping("submitOrder")
    public Result<Map<String, Object>> submitOrder(@RequestBody @Validated OrderInfoVo orderInfoVo) {
        this.orderInfoService.submitOrder(orderInfoVo);
        String tradeNo = orderInfoVo.getTradeNo();
        Map<String, Object> map = new HashMap<>();
        map.put("orderNo", tradeNo);
        return Result.ok(map);
    }
}

