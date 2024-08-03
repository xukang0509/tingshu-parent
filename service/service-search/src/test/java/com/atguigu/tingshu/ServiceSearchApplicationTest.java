package com.atguigu.tingshu;

import com.atguigu.tingshu.album.client.AlbumInfoFeignClient;
import com.atguigu.tingshu.album.client.CategoryFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.PinYinUtils;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.model.search.AttributeValueIndex;
import com.atguigu.tingshu.model.search.SuggestIndex;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.suggest.Completion;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.List;

@SpringBootTest
public class ServiceSearchApplicationTest {
    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Resource
    private AlbumInfoFeignClient albumInfoFeignClient;

    @Resource
    private UserInfoFeignClient userInfoFeignClient;

    @Resource
    private CategoryFeignClient categoryFeignClient;

    @Test
    public void importData() {
        // 专辑信息索引
        IndexOperations albumInfoIndexOps = this.elasticsearchTemplate.indexOps(AlbumInfoIndex.class);
        // 关键字补 全建议索引
        IndexOperations suggestIndexOps = this.elasticsearchTemplate.indexOps(SuggestIndex.class);
        if (!albumInfoIndexOps.exists()) {
            albumInfoIndexOps.create();
            albumInfoIndexOps.putMapping();
        }
        if (!suggestIndexOps.exists()) {
            suggestIndexOps.delete();
            suggestIndexOps.create();
            suggestIndexOps.putMapping();
        }

        Integer pageNum = 1;
        Integer pageSize = 100;

        do {
            // 分类查询所有专辑信息
            Result<List<AlbumListVo>> allAlbumPageRes = this.albumInfoFeignClient.findAllAlbumPage(pageNum, pageSize);
            Assert.notNull(allAlbumPageRes, "查询专辑结果集为空");
            List<AlbumListVo> albumListVoList = allAlbumPageRes.getData();
            // 如果为空说明到了最后一页，导入程序可以终止
            if (CollectionUtils.isEmpty(albumListVoList)) {
                return;
            }

            List<AlbumInfoIndex> albumInfoIndexList = albumListVoList.stream().map(albumListVo -> {
                // 自动补 全-标题
                SuggestIndex titleSuggestIndex = new SuggestIndex();
                titleSuggestIndex.setId(null);
                titleSuggestIndex.setTitle(albumListVo.getAlbumTitle());
                titleSuggestIndex.setKeyword(new Completion(new String[]{albumListVo.getAlbumTitle()}));
                titleSuggestIndex.setKeywordPinyin(new Completion(new String[]{PinYinUtils.toHanyuPinyin(albumListVo.getAlbumTitle())}));
                titleSuggestIndex.setKeywordSequence(new Completion(new String[]{PinYinUtils.getFirstLetter(albumListVo.getAlbumTitle())}));
                this.elasticsearchTemplate.save(titleSuggestIndex);

                Long albumId = albumListVo.getAlbumId();
                AlbumInfoIndex albumInfoIndex = new AlbumInfoIndex();
                albumInfoIndex.setId(albumId);
                BeanUtils.copyProperties(albumListVo, albumInfoIndex);
                albumInfoIndex.setHotScore(albumListVo.getPlayStatNum() * 0.2 +
                        albumListVo.getSubscribeStatNum() * 0.3 +
                        albumListVo.getBuyStatNum() * 0.4 +
                        albumListVo.getCommentStatNum() * 0.1);

                // 根据专辑id获取专辑信息
                Result<AlbumInfo> albumInfoRes = this.albumInfoFeignClient.getAlbumInfo(albumId);
                Assert.notNull(albumInfoRes, "查询专辑信息失败");
                AlbumInfo albumInfo = albumInfoRes.getData();
                Assert.notNull(albumInfo, "查询专辑信息失败");
                albumInfoIndex.setAlbumIntro(albumInfo.getAlbumIntro());
                albumInfoIndex.setPayType(albumInfo.getPayType());
                albumInfoIndex.setCreateTime(albumInfo.getCreateTime());

                // 根据用户id获取用户信息
                Result<UserInfoVo> userInfoVoRes = this.userInfoFeignClient.getUserInfoById(albumInfo.getUserId());
                Assert.notNull(userInfoVoRes, "获取用户信息失败！");
                UserInfoVo userInfoVo = userInfoVoRes.getData();
                Assert.notNull(userInfoVo, "用户信息为空");
                albumInfoIndex.setAnnouncerName(userInfoVo.getNickname());

                // 自动补 全-主播
                SuggestIndex announcerSuggestIndex = new SuggestIndex();
                announcerSuggestIndex.setId(null);
                announcerSuggestIndex.setTitle(albumInfoIndex.getAnnouncerName());
                announcerSuggestIndex.setKeyword(new Completion(new String[]{albumInfoIndex.getAnnouncerName()}));
                announcerSuggestIndex.setKeywordPinyin(new Completion(new String[]{PinYinUtils.toHanyuPinyin(albumInfoIndex.getAnnouncerName())}));
                announcerSuggestIndex.setKeywordSequence(new Completion(new String[]{PinYinUtils.getFirstLetter(albumInfoIndex.getAnnouncerName())}));
                this.elasticsearchTemplate.save(announcerSuggestIndex);

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
                if (!CollectionUtils.isEmpty(albumAttributeValueList)) {
                    List<AttributeValueIndex> attributeValueIndexList = albumAttributeValueList.stream().map(albumAttributeValue -> {
                        AttributeValueIndex attributeValueIndex = new AttributeValueIndex();
                        attributeValueIndex.setAttributeId(albumAttributeValue.getAttributeId());
                        attributeValueIndex.setValueId(albumAttributeValue.getValueId());
                        return attributeValueIndex;
                    }).toList();
                    albumInfoIndex.setAttributeValueIndexList(attributeValueIndexList);
                }
                return albumInfoIndex;
            }).toList();

            // 保存到索引库
            this.elasticsearchTemplate.save(albumInfoIndexList);

            // 下一条
            pageSize = albumListVoList.size();
            pageNum++;
        } while (pageSize == 100);// 如果没有100条说明到最后一页了，退出循环
    }
}