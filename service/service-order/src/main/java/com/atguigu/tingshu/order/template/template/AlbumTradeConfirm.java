package com.atguigu.tingshu.order.template.template;

import com.atguigu.tingshu.album.client.AlbumInfoFeignClient;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.order.template.ConfirmTradeBean;
import com.atguigu.tingshu.order.template.ConfirmTradeTemplate;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.vo.order.OrderDerateVo;
import com.atguigu.tingshu.vo.order.OrderDetailVo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import jakarta.annotation.Resource;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

/**
 * @author xk
 * @since 2024-08-18 15:56
 */
@ConfirmTradeBean(value = SystemConstant.ORDER_ITEM_TYPE_ALBUM)
public class AlbumTradeConfirm extends ConfirmTradeTemplate {
    @Resource
    private UserInfoFeignClient userInfoFeignClient;

    @Resource
    private AlbumInfoFeignClient albumInfoFeignClient;

    @Override
    protected void trade(TradeVo tradeVo, OrderInfoVo orderInfoVo) {
        // 根据专辑id查询该用户是否订购过当前专辑，如果订购过则抛出异常
        Result<Boolean> paidAlbumStatRes = this.userInfoFeignClient.getPaidAlbumStat(tradeVo.getItemId());
        Assert.notNull(paidAlbumStatRes, "根据专辑id查询当前用户是否订购过该专辑信息失败！");
        Boolean isPaidAlbum = paidAlbumStatRes.getData();
        Assert.notNull(isPaidAlbum, "未获取到专辑购买信息！");
        if (isPaidAlbum) {
            // 如果购买过则抛出异常：不可重复购买
            throw new GuiguException(ResultCodeEnum.REPEAT_BUY_ERROR);
        }
        // 查询当前用户信息
        Result<UserInfoVo> userInfoVoRes = this.userInfoFeignClient.getUserInfoById(AuthContextHolder.getUserId());
        Assert.notNull(userInfoVoRes, "获取用户信息失败！");
        UserInfoVo userInfoVo = userInfoVoRes.getData();
        Assert.notNull(userInfoVo, "用户信息为空！");
        // 查询专辑信息
        Result<AlbumInfo> albumInfoRes = this.albumInfoFeignClient.getAlbumInfo(tradeVo.getItemId());
        Assert.notNull(albumInfoRes, "查询专辑信息失败");
        AlbumInfo albumInfo = albumInfoRes.getData();
        Assert.notNull(albumInfo, "该专辑不存在！");

        // 计算出价格参数
        // 获取出折扣信息：10 表示 不打折
        BigDecimal discount = BigDecimal.valueOf(10);
        if (userInfoVo.getIsVip() == 0 && albumInfo.getDiscount().compareTo(BigDecimal.valueOf(-1.0)) != 0) {
            // 非VIP 且 打折
            discount = albumInfo.getDiscount();
        } else if (userInfoVo.getIsVip() == 1 && albumInfo.getVipDiscount().compareTo(BigDecimal.valueOf(-1.0)) != 0) {
            // VIP 且 打折
            discount = albumInfo.getVipDiscount();
        }
        BigDecimal originalAmount = albumInfo.getPrice().setScale(2, RoundingMode.HALF_UP);
        BigDecimal orderAmount = originalAmount.multiply(discount).divide(BigDecimal.valueOf(10), 2, RoundingMode.HALF_UP);
        BigDecimal derateAmount = originalAmount.subtract(orderAmount).setScale(2, RoundingMode.HALF_UP);
        orderInfoVo.setOriginalAmount(originalAmount);
        orderInfoVo.setDerateAmount(derateAmount);
        orderInfoVo.setOrderAmount(orderAmount);
        // 组装出订单详情
        orderInfoVo.setOrderDetailVoList(Arrays.asList(
                OrderDetailVo.builder()
                        .itemId(tradeVo.getItemId())
                        .itemPrice(orderAmount)
                        .itemUrl(albumInfo.getCoverUrl())
                        .itemName(albumInfo.getAlbumTitle()).build()
        ));
        // 组装减免信息
        if (derateAmount.compareTo(BigDecimal.ZERO) != 0) {
            orderInfoVo.setOrderDerateVoList(Arrays.asList(
                    OrderDerateVo.builder()
                            .derateType(SystemConstant.ORDER_DERATE_ALBUM_DISCOUNT)
                            .derateAmount(derateAmount).build()
            ));
        }
    }
}
