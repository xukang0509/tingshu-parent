package com.atguigu.tingshu.order.template.template;

import com.atguigu.tingshu.album.client.AlbumInfoFeignClient;
import com.atguigu.tingshu.album.client.TrackInfoFeignClient;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.order.template.ConfirmTradeBean;
import com.atguigu.tingshu.order.template.ConfirmTradeTemplate;
import com.atguigu.tingshu.vo.order.OrderDetailVo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import jakarta.annotation.Resource;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * @author xk
 * @since 2024-08-18 15:56
 */
@ConfirmTradeBean(value = SystemConstant.ORDER_ITEM_TYPE_TRACK)
public class TrackTradeConfirm extends ConfirmTradeTemplate {
    @Resource
    private AlbumInfoFeignClient albumInfoFeignClient;

    @Resource
    private TrackInfoFeignClient trackInfoFeignClient;

    @Override
    protected void trade(TradeVo tradeVo, OrderInfoVo orderInfoVo) {
        // 获取声音信息 及 判断 count 是否大于0
        Long trackId = tradeVo.getItemId();
        Integer count = tradeVo.getTrackCount();
        if (count == null || count <= 0) {
            throw new GuiguException(ResultCodeEnum.ARGUMENT_VALID_ERROR);
        }
        // 根据声音id及购买数量查询购买的声音列表
        Result<List<TrackInfo>> needBuyTrackListRes = this.trackInfoFeignClient.findTrackInfosByIdAndCount(trackId, count);
        Assert.notNull(needBuyTrackListRes, "查询本次购买声音列表失败！");
        if (!Objects.equals(needBuyTrackListRes.getCode(), ResultCodeEnum.SUCCESS.getCode())) {
            throw new GuiguException(needBuyTrackListRes.getCode(), needBuyTrackListRes.getMessage());
        }
        // 如果本次购买的声音列表为空则抛出异常
        List<TrackInfo> needBuyTrackList = needBuyTrackListRes.getData();
        if (CollectionUtils.isEmpty(needBuyTrackList)) {
            throw new GuiguException(ResultCodeEnum.ARGUMENT_VALID_ERROR);
        }
        // 获取专辑信息
        Result<AlbumInfo> albumInfoRes = this.albumInfoFeignClient.getAlbumInfo(needBuyTrackList.get(0).getAlbumId());
        Assert.notNull(albumInfoRes, "远程调用：获取专辑信息失败！");
        AlbumInfo albumInfo = albumInfoRes.getData();
        if (albumInfo == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        BigDecimal singlePrice = albumInfo.getPrice();
        // 计算价格：声音单集购买无优惠
        orderInfoVo.setOriginalAmount(singlePrice.multiply(BigDecimal.valueOf(needBuyTrackList.size())));
        orderInfoVo.setOrderAmount(orderInfoVo.getOriginalAmount());
        orderInfoVo.setDerateAmount(new BigDecimal("0.00"));

        // 组装出订单详情
        List<OrderDetailVo> orderDetailVos = needBuyTrackList.stream().map(
                trackInfo -> OrderDetailVo.builder()
                        .itemId(trackInfo.getId())
                        .itemPrice(singlePrice)
                        .itemUrl(trackInfo.getCoverUrl())
                        .itemName("声音：" + trackInfo.getTrackTitle()).build()
        ).toList();
        orderInfoVo.setOrderDetailVoList(orderDetailVos);
    }
}
