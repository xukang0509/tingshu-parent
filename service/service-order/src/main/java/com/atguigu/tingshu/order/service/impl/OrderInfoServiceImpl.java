package com.atguigu.tingshu.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.album.client.AlbumInfoFeignClient;
import com.atguigu.tingshu.album.client.TrackInfoFeignClient;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.order.helper.SignHelper;
import com.atguigu.tingshu.order.mapper.OrderInfoMapper;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.user.client.VipServiceConfigFeignClient;
import com.atguigu.tingshu.vo.order.OrderDerateVo;
import com.atguigu.tingshu.vo.order.OrderDetailVo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Resource
    private UserInfoFeignClient userInfoFeignClient;

    @Resource
    private VipServiceConfigFeignClient vipServiceConfigFeignClient;

    @Resource
    private AlbumInfoFeignClient albumInfoFeignClient;

    @Resource
    private TrackInfoFeignClient trackInfoFeignClient;

    @Resource
    private RedisTemplate redisTemplate;

    @Override
    public OrderInfoVo trade(TradeVo tradeVo) {
        // 创建一个订单确认页所需的vo模型
        OrderInfoVo orderInfoVo = new OrderInfoVo();
        // 生成交易号(订单编号)，防重的唯一标识(幂等性)
        orderInfoVo.setTradeNo(IdWorker.getIdStr());
        // 设置默认的支付方式(微信支付)
        orderInfoVo.setPayWay(SystemConstant.ORDER_PAY_WAY_WEIXIN);
        // 设置订单的交易类型
        orderInfoVo.setItemType(tradeVo.getItemType());
        // 为了防重：在redis中保存一份
        this.redisTemplate.opsForValue().set(RedisConstant.USER_TRADE_PREFIX + orderInfoVo.getTradeNo(), orderInfoVo.getTradeNo(),
                RedisConstant.USER_TRADE_TIMEOUT, TimeUnit.SECONDS);

        // 判断订单类型
        if (SystemConstant.ORDER_ITEM_TYPE_VIP.equals(tradeVo.getItemType())) {
            // 用户vip订单
            vipTrade(tradeVo, orderInfoVo);
        } else if (SystemConstant.ORDER_ITEM_TYPE_ALBUM.equals(tradeVo.getItemType())) {
            // 整张专辑订单
            albumTrade(tradeVo, orderInfoVo);
        } else if (SystemConstant.ORDER_ITEM_TYPE_TRACK.equals(tradeVo.getItemType())) {
            // 声音订单
            trackTrade(tradeVo, orderInfoVo);
        }

        // 设置时间戳和签名
        orderInfoVo.setTimestamp(SignHelper.getTimestamp());
        orderInfoVo.setSign(SignHelper.getSign(JSON.parseObject(JSON.toJSONString(orderInfoVo), HashMap.class)));
        // 返回vo模型
        return orderInfoVo;
    }

    private void trackTrade(TradeVo tradeVo, OrderInfoVo orderInfoVo) {
        // 获取声音信息 及 判断 count 是否大于0
        Long trackId = tradeVo.getItemId();
        Integer count = tradeVo.getTrackCount();
        if (trackId == null || count == null || count <= 0) {
            throw new GuiguException(ResultCodeEnum.ARGUMENT_VALID_ERROR);
        }
        // 根据声音id及购买数量查询购买的声音列表
        Result<List<TrackInfo>> needBuyTrackListRes = this.trackInfoFeignClient.findTrackInfosByIdAndCount(trackId, count);
        Assert.notNull(needBuyTrackListRes, "查询本次购买声音列表失败！");
        // 如果本次购买的声音列表为空则抛出异常
        if (CollectionUtils.isEmpty(needBuyTrackListRes.getData())) {
            throw new GuiguException(ResultCodeEnum.ARGUMENT_VALID_ERROR);
        }
        List<TrackInfo> needBuyTrackList = needBuyTrackListRes.getData();
        // 获取专辑信息
        Result<AlbumInfo> albumInfoRes = this.albumInfoFeignClient.getAlbumInfo(needBuyTrackList.get(0).getAlbumId());
        if (albumInfoRes == null || albumInfoRes.getData() == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        AlbumInfo albumInfo = albumInfoRes.getData();
        BigDecimal singlePrice = albumInfo.getPrice();
        // 计算价格
        orderInfoVo.setOriginalAmount(singlePrice.multiply(BigDecimal.valueOf(needBuyTrackList.size())));
        orderInfoVo.setOrderAmount(orderInfoVo.getOriginalAmount());
        orderInfoVo.setDerateAmount(BigDecimal.ZERO);

        // 组装出订单详情
        List<OrderDetailVo> orderDetailVos = needBuyTrackList.stream().map(trackInfo -> OrderDetailVo.builder()
                .itemId(trackInfo.getId())
                .itemPrice(singlePrice)
                .itemUrl(trackInfo.getCoverUrl())
                .itemName("声音：" + trackInfo.getTrackTitle()).build()).toList();
        orderInfoVo.setOrderDetailVoList(orderDetailVos);
    }

    private void albumTrade(TradeVo tradeVo, OrderInfoVo orderInfoVo) {
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
            discount = albumInfo.getDiscount();
        } else if (userInfoVo.getIsVip() == 1 && albumInfo.getVipDiscount().compareTo(BigDecimal.valueOf(-1.0)) != 0) {
            discount = albumInfo.getVipDiscount();
        }
        BigDecimal originalAmount = albumInfo.getPrice();
        BigDecimal orderAmount = originalAmount.multiply(discount).divide(BigDecimal.valueOf(10));
        BigDecimal derateAmount = originalAmount.subtract(orderAmount);
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

    private void vipTrade(TradeVo tradeVo, OrderInfoVo orderInfoVo) {
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
