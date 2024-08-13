package com.atguigu.tingshu.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.Suggestion;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.client.AlbumInfoFeignClient;
import com.atguigu.tingshu.album.client.CategoryFeignClient;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.PinYinUtils;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategory3;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.model.search.AttributeValueIndex;
import com.atguigu.tingshu.model.search.SuggestIndex;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.atguigu.tingshu.vo.search.AlbumInfoIndexVo;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.google.common.collect.Lists;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.suggest.Completion;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;


@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class SearchServiceImpl implements SearchService {
    @Resource
    private ElasticsearchTemplate elasticsearchTemplate;

    @Resource
    private ElasticsearchClient esClient;

    @Resource
    private AlbumInfoFeignClient albumInfoFeignClient;

    @Resource
    private UserInfoFeignClient userInfoFeignClient;

    @Resource
    private CategoryFeignClient categoryFeignClient;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private ExecutorService executorService;

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

        // 自动补 全-标题
        SuggestIndex titleSuggestIndex = new SuggestIndex();
        titleSuggestIndex.setId(null);
        titleSuggestIndex.setTitle(albumInfo.getAlbumTitle());
        titleSuggestIndex.setKeyword(new Completion(new String[]{albumInfo.getAlbumTitle()}));
        titleSuggestIndex.setKeywordPinyin(new Completion(new String[]{PinYinUtils.toHanyuPinyin(albumInfo.getAlbumTitle())}));
        titleSuggestIndex.setKeywordSequence(new Completion(new String[]{PinYinUtils.getFirstLetter(albumInfo.getAlbumTitle())}));
        this.elasticsearchTemplate.save(titleSuggestIndex);

        // 根据用户id获取用户信息
        Result<UserInfoVo> userInfoVoRes = this.userInfoFeignClient.getUserInfoById(albumInfo.getUserId());
        Assert.notNull(userInfoVoRes, "获取用户信息失败！");
        UserInfoVo userInfoVo = userInfoVoRes.getData();
        Assert.notNull(userInfoVo, "用户信息为空！");
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
        Assert.notEmpty(albumAttributeValueList, "专辑属性信息为空！");
        List<AttributeValueIndex> attributeValueIndexList = albumAttributeValueList.stream().map(albumAttributeValue -> {
            AttributeValueIndex attributeValueIndex = new AttributeValueIndex();
            attributeValueIndex.setAttributeId(albumAttributeValue.getAttributeId());
            attributeValueIndex.setValueId(albumAttributeValue.getValueId());
            return attributeValueIndex;
        }).toList();
        albumInfoIndex.setAttributeValueIndexList(attributeValueIndexList);

        // 根据专辑Id获取统计信息列表
        Result<AlbumStatVo> albumStatListRes = this.albumInfoFeignClient.getAlbumStatsByAlbumId(albumId);
        Assert.notNull(albumStatListRes, "根据专辑Id获取统计信息列表失败！");
        AlbumStatVo albumStatVo = albumStatListRes.getData();
        Assert.notNull(albumStatVo, "专辑统计信息为空！");
        albumInfoIndex.setPlayStatNum(albumStatVo.getPlayStatNum());
        albumInfoIndex.setSubscribeStatNum(albumStatVo.getSubscribeStatNum());
        albumInfoIndex.setBuyStatNum(albumStatVo.getBuyStatNum());
        albumInfoIndex.setCommentStatNum(albumStatVo.getCommentStatNum());
        // 热度：根据上述指标结合一定的系数进行计算
        albumInfoIndex.setHotScore(albumInfoIndex.getPlayStatNum() * 0.2 +
                albumInfoIndex.getSubscribeStatNum() * 0.3 +
                albumInfoIndex.getBuyStatNum() * 0.4 +
                albumInfoIndex.getCommentStatNum() * 0.1);

        log.info("新增es中新的数据：albumInfoIndex = {}", albumInfoIndex);
        this.elasticsearchTemplate.save(albumInfoIndex);
    }

    @Override
    public void downAlbum(Long albumId) {
        log.info("删除es中AlbumInfoIndex数据：albumId = {}", albumId);
        this.elasticsearchTemplate.delete(albumId.toString(), AlbumInfoIndex.class);
    }

    @Override
    public List<JSONObject> channel(Long category1Id) {
        try {
            // 根据一级分类id查询置顶三级分类
            Result<List<BaseCategory3>> baseCategory3ListRes = this.categoryFeignClient.findTopBaseCategory3ByCategory1Id(category1Id);
            Assert.notNull(baseCategory3ListRes, "根据一级分类id获取三级分类列表失败！");
            List<BaseCategory3> baseCategory3List = baseCategory3ListRes.getData();
            Assert.notEmpty(baseCategory3List, "三级分类列表为空！");
            // 获取三级分类id集合
            List<FieldValue> category3Ids = baseCategory3List.stream().map(baseCategory3 -> FieldValue.of(baseCategory3.getId())).toList();
            // 为了方便获取三级分类对象，把三级分类list集合转化为map集合：key-category3Id，value-BaseCategory3
            Map<Long, BaseCategory3> category3IdToBaseCategory3Map = baseCategory3List.stream()
                    .collect(Collectors.toMap(BaseCategory3::getId, baseCategory3 -> baseCategory3));
            // 编写DSL
            SearchRequest request = SearchRequest.of(
                    s -> s.index("albuminfo")
                            .query(q -> q.terms(t -> t.field("category3Id").terms(ts -> ts.value(category3Ids))))
                            .size(0)
                            .aggregations("category3IdAgg", agg -> agg.terms(t -> t.field("category3Id"))
                                    .aggregations("topHotsAgg", subAgg -> subAgg.topHits(top ->
                                            top.sort(st -> st.field(f -> f.field("hotScore").order(SortOrder.Desc))).size(6))))
            );
            log.info("channel DSL: {}", request);
            // 解析结果
            SearchResponse<AlbumInfoIndex> response = this.esClient.search(request, AlbumInfoIndex.class);
            // 获取响应集中的聚合
            Map<String, Aggregate> aggregations = response.aggregations();
            if (CollectionUtils.isEmpty(aggregations)) return null;
            // 获取category3IdAgg聚合
            Aggregate aggregate = aggregations.get("category3IdAgg");
            if (Objects.isNull(aggregate)) return null;
            // 把桶集合转化为List<JSONObject>
            return aggregate.lterms().buckets().array().stream().map(bucket -> {
                JSONObject map = new JSONObject();
                // 桶的key是三级分类id，根据三级分类id获取三级分类对象
                map.put("baseCategory3", category3IdToBaseCategory3Map.get(bucket.key()));
                // 获取子聚合列表
                Map<String, Aggregate> subAggregates = bucket.aggregations();
                if (CollectionUtils.isEmpty(subAggregates)) return map;
                // 获取top子聚合
                Aggregate topHotsAgg = subAggregates.get("topHotsAgg");
                if (Objects.isNull(topHotsAgg)) return map;
                // 获取top数据集合
                List<AlbumInfoIndex> albumInfoIndexList =
                        topHotsAgg.topHits().hits().hits().stream()
                                .map(hit -> hit.source().to(AlbumInfoIndex.class)).toList();
                map.put("list", albumInfoIndexList);
                return map;
            }).toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public AlbumSearchResponseVo searchAlbumInfoIndex(AlbumIndexQuery albumIndexQuery) {
        try {
            // 构建DSL
            SearchRequest searchRequest = buildSearchDsl(albumIndexQuery);
            log.info("search DSL：{}", searchRequest);
            // 执行搜索获取响应结果集
            SearchResponse<AlbumInfoIndex> response = this.esClient.search(searchRequest, AlbumInfoIndex.class);
            // 解析结果集
            AlbumSearchResponseVo responseVo = this.parseResult(response);
            responseVo.setPageNo(albumIndexQuery.getPageNo());
            responseVo.setPageSize(albumIndexQuery.getPageSize());
            long totalPage = responseVo.getTotal() / responseVo.getPageSize();
            responseVo.setTotalPages(responseVo.getTotal() % responseVo.getPageSize() == 0 ? totalPage : totalPage + 1);
            return responseVo;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // 解析结果集
    private AlbumSearchResponseVo parseResult(SearchResponse<AlbumInfoIndex> response) {
        AlbumSearchResponseVo responseVo = new AlbumSearchResponseVo();
        // 获取响应结果集中的hits
        HitsMetadata<AlbumInfoIndex> hits = response.hits();
        // 设置总记录数
        responseVo.setTotal(hits.total().value());
        // 设置当前页数据
        List<Hit<AlbumInfoIndex>> hitList = hits.hits();
        if (!CollectionUtils.isEmpty(hitList)) {
            List<AlbumInfoIndexVo> albumInfoIndexVos = hitList.stream().map(hit -> {
                // source对应的就是AlbumInfoIndex
                AlbumInfoIndex albumInfoIndex = hit.source();
                // 初始化vo集合，并赋值
                AlbumInfoIndexVo albumInfoIndexVo = new AlbumInfoIndexVo();
                BeanUtils.copyProperties(albumInfoIndex, albumInfoIndexVo);
                // 设置高亮
                Map<String, List<String>> highLight = null;
                if (!CollectionUtils.isEmpty(highLight = hit.highlight()) &&
                        !CollectionUtils.isEmpty(highLight.get("albumTitle"))) {
                    albumInfoIndexVo.setAlbumTitle(highLight.get("albumTitle").get(0));
                }
                return albumInfoIndexVo;
            }).toList();
            responseVo.setList(albumInfoIndexVos);
        }
        return responseVo;
    }

    // 构建搜索条件
    private SearchRequest buildSearchDsl(AlbumIndexQuery albumIndexQuery) {
        // 构建request请求体
        SearchRequest.Builder requestBuilder = new SearchRequest.Builder();
        // 索引库
        requestBuilder.index("albuminfo");
        // 1.构建布尔查询
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
        // 1.1 获取搜索关键字，构建多字段查询
        if (StringUtils.isNotBlank(albumIndexQuery.getKeyword())) {
            boolQueryBuilder.must(m -> m.multiMatch(mm ->
                    mm.fields("albumTitle", "albumIntro", "announcerName")
                            .query(albumIndexQuery.getKeyword())));
        }
        // 1.2 构建三级分类过滤
        if (!Objects.isNull(albumIndexQuery.getCategory1Id())) {
            boolQueryBuilder.filter(f -> f.term(t -> t.field("category1Id").value(albumIndexQuery.getCategory1Id())));
        }
        if (!Objects.isNull(albumIndexQuery.getCategory2Id())) {
            boolQueryBuilder.filter(f -> f.term(t -> t.field("category2Id").value(albumIndexQuery.getCategory2Id())));
        }
        if (!Objects.isNull(albumIndexQuery.getCategory3Id())) {
            boolQueryBuilder.filter(f -> f.term(t -> t.field("category3Id").value(albumIndexQuery.getCategory3Id())));
        }
        // 1.3 构建属性的嵌套过滤
        List<String> attributeList = albumIndexQuery.getAttributeList();
        if (!CollectionUtils.isEmpty(attributeList)) {
            for (String attribute : attributeList) {
                // 以:进行分割，分割后应该是2个元素，属性id:属性值id
                String[] attrs = StringUtils.split(attribute, ":");
                if (attrs == null || attrs.length != 2 ||
                        !StringUtils.isNumeric(attrs[0]) || !StringUtils.isNumeric(attrs[1]))
                    continue;
                // 构建嵌套过滤
                boolQueryBuilder.filter(f -> f.nested(n -> n.path("attributeValueIndexList")
                        .query(q -> q.bool(b -> b.must(m -> m.term(t -> t.field("attributeValueIndexList.attributeId").value(attrs[0])))
                                .must(m -> m.term(t -> t.field("attributeValueIndexList.valueId").value(attrs[1])))))));
            }
        }
        // 1.4 把构建好的boolQueryBuilder放入requestBuilder对象
        requestBuilder.query(boolQueryBuilder.build()._toQuery());

        // 2.构建分页数据
        Integer pageNo = albumIndexQuery.getPageNo();
        Integer pageSize = albumIndexQuery.getPageSize();
        requestBuilder.from((pageNo - 1) * pageSize).size(pageSize);

        // 3.构建排序条件 综合排序[1:desc] 播放量[2:desc] 发布时间[3:desc]；asc:升序 desc:降序
        String order = albumIndexQuery.getOrder();
        if (StringUtils.isNotBlank(order)) {
            // 使用冒号分割字符串 1:desc
            String[] orders = StringUtils.split(order, ":");
            if (orders != null && orders.length == 2 &&
                    StringUtils.isNumeric(orders[0]) && StringUtils.isNotEmpty(orders[1])) {
                // lambda表达式中需要final类型的字符串变量。
                final String orderField = switch (orders[0]) {
                    case "1" -> "hotScore";
                    case "2" -> "playStatNum";
                    case "3" -> "createTime";
                    default -> "hotScore";
                };
                final SortOrder orderType = "asc".equals(orders[1]) ? SortOrder.Asc : SortOrder.Desc;
                requestBuilder.sort(s -> s.field(f -> f.field(orderField).order(orderType)));
            }
        }

        // 4.高亮
        requestBuilder.highlight(h -> h.fields("albumTitle",
                f -> f.preTags("<font color='red'>").postTags("</font>")));

        // 5.结果集过滤
        requestBuilder.source(s -> s.filter(f -> f.excludes("hotScore", "attributeValueIndexList")));

        return requestBuilder.build();
    }

    @Override
    public List<String> completeSuggest(String keyWord) {
        try {
            if (StringUtils.isEmpty(keyWord)) return null;
            // 组装DSL
            SearchRequest searchRequest = SearchRequest.of(s -> s.index("suggestinfo")
                    .suggest(sg -> sg.suggesters("keywordSuggest", ks -> ks.prefix(keyWord).completion(c -> c.field("keyword").size(10).skipDuplicates(true))))
                    .suggest(sg -> sg.suggesters("keywordPinYinSuggest", ks -> ks.prefix(keyWord).completion(c -> c.field("keywordPinyin").size(10).skipDuplicates(true))))
                    .suggest(sg -> sg.suggesters("keywordSequenceSuggest", ks -> ks.prefix(keyWord).completion(c -> c.field("keywordSequence").size(10).skipDuplicates(true))))
            );
            log.info("completeSuggest DSL1：{}", searchRequest);
            // 执行搜索
            SearchResponse<SuggestIndex> response = this.esClient.search(searchRequest, SuggestIndex.class);
            // 解析结果集
            List<String> keywordList = this.parseSuggest(response, "keywordSuggest");
            List<String> keywordPinYinList = this.parseSuggest(response, "keywordPinYinSuggest");
            List<String> keywordSequenceList = this.parseSuggest(response, "keywordSequenceSuggest");
            // 整合到一起并去重
            Set<String> set = new HashSet<>();
            set.addAll(keywordList);
            set.addAll(keywordPinYinList);
            set.addAll(keywordSequenceList);
            // 如果数量充足，则直接返回
            if (set.size() >= 10) {
                return new ArrayList<>(set);
            }
            // 如果不充足，再去根据结果去搜索相关
            SearchResponse<SuggestIndex> response2 = this.esClient.search(s -> s.index("suggestinfo").size(10)
                            .query(q -> q.match(m -> m.field("title").query(keyWord)))
                    , SuggestIndex.class);
            // 查询结果集
            List<Hit<SuggestIndex>> hitList = response2.hits().hits();
            // 查询结果为空，直接返回
            if (CollectionUtils.isEmpty(hitList)) return new ArrayList<>(set);
            // 查询不为空
            hitList.forEach(hit -> {
                set.add(hit.source().getTitle());
                if (set.size() >= 10) return;
            });
            return new ArrayList<>(set);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> parseSuggest(SearchResponse<SuggestIndex> response, String suggestName) {
        List<String> list = new ArrayList<>();
        // 根据suggestName获取suggests
        List<Suggestion<SuggestIndex>> suggestions = response.suggest().get(suggestName);
        if (CollectionUtils.isEmpty(suggestions)) return list;
        // 遍历suggestions
        suggestions.forEach(suggestion -> {
            // 获取options集合
            suggestion.completion().options().forEach(option -> {
                list.add(option.source().getTitle());
            });
        });
        return list;
    }

    @Override
    public List<AlbumInfoIndexVo> findRankingList(Long category1Id, String orderField) {
        try {
            // 装配DSL语句并执行搜索
            SearchResponse<AlbumInfoIndex> response = this.esClient.search(s -> s.index("albuminfo")
                            .query(q -> q.term(t -> t.field("category1Id").value(category1Id)))
                            .sort(ss -> ss.field(f -> f.field(orderField).order(SortOrder.Desc)))
                            .size(20)
                    , AlbumInfoIndex.class);
            // 解析结果集
            List<Hit<AlbumInfoIndex>> hitList = response.hits().hits();
            if (CollectionUtils.isEmpty(hitList)) return null;
            return hitList.stream().map(hit -> {
                AlbumInfoIndex albumInfoIndex = hit.source();
                AlbumInfoIndexVo albumInfoIndexVo = new AlbumInfoIndexVo();
                BeanUtils.copyProperties(albumInfoIndex, albumInfoIndexVo);
                return albumInfoIndexVo;
            }).toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateLatelyAlbumStat() {
        log.info("同步专辑统计信息开始：{}", System.currentTimeMillis());
        try {
            // 为了防止每次统计间隙有数据遗漏
            // 记录统计开始时间到redis，上一次统计的截止时间是这一次的开始时间
            String startTime = (String) this.redisTemplate.opsForValue().get(RedisConstant.ALBUM_STAT_ENDTIME);
            if (StringUtils.isBlank(startTime)) {
                // 如果没有则取一个小时前的时间
                startTime = LocalDateTime.now().minusHours(1).toString("yyyy-MM-dd HH:mm:ss");
            }
            // 统计截止时间
            String endTime = LocalDateTime.now().toString("yyyy-MM-dd HH:mm:ss");
            // 记录到redis中作为下一次的起始时间
            this.redisTemplate.opsForValue().set(RedisConstant.ALBUM_STAT_ENDTIME, endTime);

            // 获取最近统计信息发生变化的专辑列表
            Result<List<Long>> albumIdsRes = this.albumInfoFeignClient.findLatelyUpdateAlbum(startTime, endTime);
            Assert.notNull(albumIdsRes, "同步数据时，获取专辑列表失败");
            List<Long> albumIds = albumIdsRes.getData();
            if (CollectionUtils.isEmpty(albumIds)) return;

            // 如果专辑id列表不为空则需要同步到es
            // 为了提高性能，每1000个专辑id拆分成一个部分，然后使用多线程并发执行
            List<List<Long>> partitions = Lists.partition(albumIds, 1000);
            int count = partitions.size();
            // 初始化countDownLatch
            CountDownLatch countDownLatch = new CountDownLatch(count);
            partitions.forEach(partAlbumIds -> {
                // 通过线程池控制线程数
                executorService.execute(() -> {
                    Result<List<AlbumStatVo>> albumStatVoListRes = this.albumInfoFeignClient.findAlbumStatVoList(partAlbumIds);
                    Assert.notNull(albumStatVoListRes, "同步数据到es时，获取专辑统计信息失败！");
                    List<AlbumStatVo> albumStatVoList = albumStatVoListRes.getData();
                    // 如果为空，则直接返回
                    if (CollectionUtils.isEmpty(albumStatVoList)) return;
                    // 同步到es
                    albumStatVoList.forEach(albumStatVo -> {
                        AlbumInfoIndex albumInfoIndex = new AlbumInfoIndex();
                        albumInfoIndex.setId(albumStatVo.getAlbumId());
                        albumInfoIndex.setPlayStatNum(albumStatVo.getPlayStatNum());
                        albumInfoIndex.setBuyStatNum(albumStatVo.getBuyStatNum());
                        albumInfoIndex.setSubscribeStatNum(albumStatVo.getSubscribeStatNum());
                        albumInfoIndex.setCommentStatNum(albumStatVo.getCommentStatNum());
                        // 设置热度
                        albumInfoIndex.setHotScore(albumStatVo.getPlayStatNum() * 0.2 +
                                albumStatVo.getSubscribeStatNum() * 0.3 +
                                albumStatVo.getBuyStatNum() * 0.4 +
                                albumStatVo.getCommentStatNum() * 0.1);
                        this.elasticsearchTemplate.update(albumInfoIndex);
                    });
                });
                countDownLatch.countDown();
            });
            countDownLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            log.info("同步专辑统计信息结束：{}", System.currentTimeMillis());
        }
    }
}
