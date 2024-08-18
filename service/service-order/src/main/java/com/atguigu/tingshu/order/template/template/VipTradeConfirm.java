package com.atguigu.tingshu.order.template.template;

import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.order.template.ConfirmTradeBean;
import com.atguigu.tingshu.order.template.ConfirmTradeTemplate;
import com.atguigu.tingshu.user.client.VipServiceConfigFeignClient;
import com.atguigu.tingshu.vo.order.OrderDerateVo;
import com.atguigu.tingshu.vo.order.OrderDetailVo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import jakarta.annotation.Resource;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.Arrays;

/**
 * @author xk
 * @since 2024-08-18 15:54
 */
@ConfirmTradeBean(value = SystemConstant.ORDER_ITEM_TYPE_VIP)
public class VipTradeConfirm extends ConfirmTradeTemplate {
    @Resource
    private VipServiceConfigFeignClient vipServiceConfigFeignClient;

    @Override
    protected void trade(TradeVo tradeVo, OrderInfoVo orderInfoVo) {
        // 根据vip套餐的id查询vip套餐
        Result<VipServiceConfig> vipServiceConfigRes = this.vipServiceConfigFeignClient.getVipServiceConfig(tradeVo.getItemId());
        Assert.notNull(vipServiceConfigRes, "远程调用vip套餐信息失败");
        VipServiceConfig vipServiceConfig = vipServiceConfigRes.getData();
        Assert.notNull(vipServiceConfig, "所需vip套餐信息为空");
        // 计算出价格参数
        orderInfoVo.setOriginalAmount(vipServiceConfig.getPrice());
        orderInfoVo.setOrderAmount(vipServiceConfig.getDiscountPrice());
        BigDecimal derateAmount = vipServiceConfig.getPrice().subtract(vipServiceConfig.getDiscountPrice());
        orderInfoVo.setDerateAmount(derateAmount);
        // 组装出订单详情
        orderInfoVo.setOrderDetailVoList(Arrays.asList(
                OrderDetailVo.builder()
                        .itemId(tradeVo.getItemId())
                        .itemPrice(orderInfoVo.getOrderAmount())
                        .itemUrl(vipServiceConfig.getImageUrl())
                        .itemName("VIP会员：" + vipServiceConfig.getName()).build()
        ));
        // 组装减免信息
        if (derateAmount.compareTo(BigDecimal.valueOf(0.0)) != 0) {
            orderInfoVo.setOrderDerateVoList(Arrays.asList(
                    OrderDerateVo.builder()
                            .derateType(SystemConstant.ORDER_DERATE_VIP_SERVICE_DISCOUNT)
                            .derateAmount(orderInfoVo.getDerateAmount()).build()
            ));
        }
    }
}
