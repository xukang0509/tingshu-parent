package com.atguigu.tingshu.order.api;

import com.atguigu.tingshu.common.login.AuthLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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

    // http://127.0.0.1:8500/api/order/orderInfo/getOrderInfo/1824413268735791105
    @AuthLogin
    @Operation(summary = "根据订单号查询订单")
    @GetMapping("getOrderInfo/{orderNo}")
    public Result<OrderInfo> getOrderInfoByOrderNo(@PathVariable("orderNo") String orderNo) {
        return Result.ok(this.orderInfoService.getOrderInfoByOrderNo(orderNo));
    }

    // http://127.0.0.1:8500/api/order/orderInfo/findUserPage/1/10
    @AuthLogin
    @Operation(summary = "查看我的订单")
    @GetMapping("findUserPage/{pageNum}/{pageSize}")
    public Result<IPage<OrderInfo>> findUserPage(
            @Parameter(name = "pageNum", description = "页码", required = true)
            @PathVariable("pageNum") Integer pageNum,
            @Parameter(name = "pageSize", description = "每页大小", required = true)
            @PathVariable("pageSize") Integer pageSize) {
        return Result.ok(this.orderInfoService.findUserPage(pageNum, pageSize));
    }
}

