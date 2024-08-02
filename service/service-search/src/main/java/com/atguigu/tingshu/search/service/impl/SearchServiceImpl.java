package com.atguigu.tingshu.search.service.impl;

import com.atguigu.tingshu.album.client.AlbumInfoFeignClient;
import com.atguigu.tingshu.album.client.CategoryFeignClient;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.AlbumStat;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.model.search.AttributeValueIndex;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class SearchServiceImpl implements SearchService {
    @Resource
    private ElasticsearchTemplate elasticsearchTemplate;

    @Resource
    private AlbumInfoFeignClient albumInfoFeignClient;

    @Resource
    private UserInfoFeignClient userInfoFeignClient;

    @Resource
    private CategoryFeignClient categoryFeignClient;

    @Override
    public void upperAlbum(Long albumId) {
        AlbumInfoIndex albumInfoIndex = new AlbumInfoIndex();
        albumInfoIndex.setId(albumId);

        // 根据专辑id获取专辑信息
        Result<AlbumInfo> albumInfoRes = this.albumInfoFeignClient.getAlbumInfo(albumId);
        Assert.notNull(albumInfoRes, "查询专辑信息失败");
        AlbumInfo albumInfo = albumInfoRes.getData();
        Assert.notNull(albumInfo, "该专辑不存在！");
        BeanUtils.copyProperties(albumInfo, albumInfoIndex);

        // 根据用户id获取用户信息
        Result<UserInfoVo> userInfoVoRes = this.userInfoFeignClient.getUserInfoById(albumInfo.getUserId());
        Assert.notNull(userInfoVoRes, "获取用户信息失败！");
        UserInfoVo userInfoVo = userInfoVoRes.getData();
        Assert.notNull(userInfoVo, "用户信息为空！");
        albumInfoIndex.setAnnouncerName(userInfoVo.getNickname());

        // 根据三级分类id获取分类信息
        Result<BaseCategoryView> baseCategoryViewRes = this.categoryFeignClient.findBaseCategoryViewByCategory3Id(albumInfo.getCategory3Id());
        Assert.notNull(baseCategoryViewRes, "根据三级分类id获取分类信息失败！");
        BaseCategoryView baseCategoryView = baseCategoryViewRes.getData();
        Assert.notNull(baseCategoryView, "获取分类信息失败！");
        albumInfoIndex.setCategory1Id(baseCategoryView.getCategory1Id());
        albumInfoIndex.setCategory2Id(baseCategoryView.getCategory2Id());
        albumInfoIndex.setCategory3Id(baseCategoryView.getCategory3Id());

        // 根据专辑id查询属性信息
        Result<List<AlbumAttributeValue>> albumAttributeValueRes = this.albumInfoFeignClient.findAlbumInfoAttributeValueByAlbumInfoId(albumId);
        Assert.notNull(albumAttributeValueRes, "根据专辑id查询属性信息失败！");
        List<AlbumAttributeValue> albumAttributeValueList = albumAttributeValueRes.getData();
        Assert.notEmpty(albumAttributeValueList, "专辑属性信息为空！");
        List<AttributeValueIndex> attributeValueIndexList = albumAttributeValueList.stream().map(albumAttributeValue -> {
            AttributeValueIndex attributeValueIndex = new AttributeValueIndex();
            attributeValueIndex.setAttributeId(albumAttributeValue.getAttributeId());
            attributeValueIndex.setValueId(albumAttributeValue.getValueId());
            return attributeValueIndex;
        }).toList();
        albumInfoIndex.setAttributeValueIndexList(attributeValueIndexList);

        // 根据专辑Id获取统计信息列表
        Result<List<AlbumStat>> albumStatListRes = this.albumInfoFeignClient.getAlbumStatsByAlbumId(albumId);
        Assert.notNull(albumStatListRes, "根据专辑Id获取统计信息列表失败！");
        List<AlbumStat> albumStatList = albumStatListRes.getData();
        Assert.notEmpty(albumStatList, "统计信息列表为空！");
        Map<String, Integer> typeToNumMap = albumStatList.stream()
                .collect(Collectors.toMap(AlbumStat::getStatType, AlbumStat::getStatNum));
        albumInfoIndex.setPlayStatNum(typeToNumMap.get(SystemConstant.ALBUM_STAT_PLAY));
        albumInfoIndex.setSubscribeStatNum(typeToNumMap.get(SystemConstant.ALBUM_STAT_SUBSCRIBE));
        albumInfoIndex.setBuyStatNum(typeToNumMap.get(SystemConstant.ALBUM_STAT_BROWSE));
        albumInfoIndex.setCommentStatNum(typeToNumMap.get(SystemConstant.ALBUM_STAT_COMMENT));
        // 热度：根据上述指标结合一定的系数进行计算
        albumInfoIndex.setHotScore(albumInfoIndex.getPlayStatNum() * 0.2 +
                albumInfoIndex.getSubscribeStatNum() * 0.3 +
                albumInfoIndex.getBuyStatNum() * 0.4 +
                albumInfoIndex.getCommentStatNum() * 0.1);
        
        this.elasticsearchTemplate.save(albumInfoIndex);
    }

    @Override
    public void downAlbum(Long albumId) {
        this.elasticsearchTemplate.delete(albumId.toString(), AlbumInfoIndex.class);
    }
}
