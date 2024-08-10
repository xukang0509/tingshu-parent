package com.atguigu.tingshu.album.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.config.VodConstantProperties;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.mapper.TrackStatMapper;
import com.atguigu.tingshu.album.service.MinioService;
import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.constant.RabbitMqConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.common.service.RabbitService;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.album.TrackStat;
import com.atguigu.tingshu.model.user.UserPaidTrack;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.user.client.UserListenProcessFeignClient;
import com.atguigu.tingshu.vo.album.*;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class TrackInfoServiceImpl extends ServiceImpl<TrackInfoMapper, TrackInfo> implements TrackInfoService {

    @Resource
    private TrackInfoMapper trackInfoMapper;

    @Resource
    private AlbumInfoMapper albumInfoMapper;

    @Resource
    private TrackStatMapper trackStatMapper;

    @Resource
    private VodService vodService;

    @Resource
    private UserInfoFeignClient userInfoFeignClient;

    @Resource
    private UserListenProcessFeignClient userListenProcessFeignClient;

    @Resource
    private MinioService minioService;

    @Resource
    private RabbitService rabbitService;

    @Resource
    private VodConstantProperties vodConstantProperties;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveTrackInfo(TrackInfoVo trackInfoVo) {
        Long userId = AuthContextHolder.getUserId();
        // 1.根据文件id查询腾讯云中的文件信息
        TrackMediaInfoVo mediaInfoVo = vodService.getMediaInfo(trackInfoVo.getMediaFileId());
        // 2.新增声音信息
        TrackInfo trackInfo = new TrackInfo();
        BeanUtils.copyProperties(trackInfoVo, trackInfo);
        // 设置orderNum：获取上一个插入的同一专辑下的声音，再加1
        TrackInfo preTrackInfo = this.trackInfoMapper.selectOne(Wrappers.lambdaQuery(TrackInfo.class)
                .eq(TrackInfo::getAlbumId, trackInfo.getAlbumId())
                .select(TrackInfo::getOrderNum)
                .orderByDesc(TrackInfo::getOrderNum)
                .last(" limit 1"));
        trackInfo.setOrderNum(Objects.isNull(preTrackInfo) ? 1 : preTrackInfo.getOrderNum() + 1);
        trackInfo.setUserId(userId);
        if (!Objects.isNull(mediaInfoVo)) {
            trackInfo.setMediaDuration(BigDecimal.valueOf(mediaInfoVo.getDuration()));
            trackInfo.setMediaSize(mediaInfoVo.getSize());
            trackInfo.setMediaType(mediaInfoVo.getType());
            trackInfo.setMediaUrl(mediaInfoVo.getMediakUrl());
        }
        trackInfo.setStatus(SystemConstant.TRACK_STATUS_PASS);
        trackInfoMapper.insert(trackInfo);
        // 3.更新专辑信息中的声音数目
        AlbumInfo albumInfo = this.albumInfoMapper.selectOne(Wrappers.lambdaQuery(AlbumInfo.class)
                .eq(AlbumInfo::getId, trackInfo.getAlbumId())
                .select(AlbumInfo::getId, AlbumInfo::getIncludeTrackCount)
                .last(" limit 1"));
        if (!Objects.isNull(albumInfo)) {
            albumInfo.setIncludeTrackCount(albumInfo.getIncludeTrackCount() + 1);
            this.albumInfoMapper.updateById(albumInfo);
            // es中数据更新
            rabbitService.sendMessage(RabbitMqConstant.EXCHANGE_ALBUM_UPPER,
                    RabbitMqConstant.ROUTING_ALBUM_UPPER, albumInfo.getId());
        }
        // 4.新增声音统计信息
        saveTrackStat(trackInfo.getId(), SystemConstant.TRACK_STAT_PLAY);
        saveTrackStat(trackInfo.getId(), SystemConstant.TRACK_STAT_COLLECT);
        saveTrackStat(trackInfo.getId(), SystemConstant.TRACK_STAT_PRAISE);
        saveTrackStat(trackInfo.getId(), SystemConstant.TRACK_STAT_COMMENT);
    }

    private void saveTrackStat(Long trackId, String statType) {
        TrackStat trackStat = new TrackStat();
        trackStat.setTrackId(trackId);
        trackStat.setStatType(statType);
        trackStat.setStatNum(0);
        this.trackStatMapper.insert(trackStat);
    }

    @Override
    public Page<TrackListVo> findUserTrackPage(Integer pageNum, Integer pageSize, TrackInfoQuery trackInfoQuery) {
        // 设置主播的id
        Long userId = AuthContextHolder.getUserId();
        trackInfoQuery.setUserId(userId);
        Page<TrackListVo> page = new Page<>(pageNum, pageSize);
        return this.trackInfoMapper.findUserTrackPage(page, trackInfoQuery);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateTrackInfoById(TrackInfoVo trackInfoVo, Long trackId) {
        // 先根据主键获取声音，拷贝数据
        TrackInfo trackInfo = this.getById(trackId);
        String mediaFileId = trackInfo.getMediaFileId();
        String coverUrl = trackInfo.getCoverUrl();
        BeanUtils.copyProperties(trackInfoVo, trackInfo);
        // 判断是否更新媒体信息及删除旧媒体信息
        if (!Objects.equals(mediaFileId, trackInfoVo.getMediaFileId())) {
            // 根据文件id查询腾讯云中的文件信息
            TrackMediaInfoVo mediaInfoVo = vodService.getMediaInfo(trackInfoVo.getMediaFileId());
            if (Objects.isNull(mediaInfoVo)) {
                throw new GuiguException(ResultCodeEnum.VOD_FILE_ID_ERROR);
            }
            trackInfo.setMediaDuration(BigDecimal.valueOf(mediaInfoVo.getDuration()));
            trackInfo.setMediaSize(mediaInfoVo.getSize());
            trackInfo.setMediaType(mediaInfoVo.getType());
            trackInfo.setMediaUrl(mediaInfoVo.getMediakUrl());
            // 删除旧音频信息
            this.vodService.removeTrackMedia(mediaFileId);
        }
        // 判断是否删除minio服务中的旧图片
        if (!Objects.equals(coverUrl, trackInfo.getCoverUrl())) {
            minioService.deleteFile(coverUrl);
        }
        this.updateById(trackInfo);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void removeTrackInfo(Long trackId) {
        TrackInfo trackInfo = this.trackInfoMapper.selectOne(Wrappers.lambdaQuery(TrackInfo.class)
                .eq(TrackInfo::getId, trackId)
                .select(TrackInfo::getAlbumId, TrackInfo::getMediaFileId, TrackInfo::getCoverUrl)
                .last(" limit 1"));
        if (Objects.isNull(trackInfo)) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        // 删除声音
        this.trackInfoMapper.deleteById(trackId);
        // 删除统计表信息
        this.trackStatMapper.delete(Wrappers.lambdaQuery(TrackStat.class)
                .eq(TrackStat::getTrackId, trackId));
        // 更新专辑中的声音数量
        AlbumInfo albumInfo = this.albumInfoMapper.selectOne(Wrappers.lambdaQuery(AlbumInfo.class)
                .eq(AlbumInfo::getId, trackInfo.getAlbumId())
                .select(AlbumInfo::getId, AlbumInfo::getIncludeTrackCount)
                .last(" limit 1"));
        if (!Objects.isNull(albumInfo)) {
            albumInfo.setIncludeTrackCount(albumInfo.getIncludeTrackCount() - 1);
            this.albumInfoMapper.updateById(albumInfo);
            // es中数据更新
            rabbitService.sendMessage(RabbitMqConstant.EXCHANGE_ALBUM_UPPER,
                    RabbitMqConstant.ROUTING_ALBUM_UPPER, albumInfo.getId());
        }
        // 删除音频信息
        this.vodService.removeTrackMedia(trackInfo.getMediaFileId());
        // 删除minio中的数据
        this.minioService.deleteFile(trackInfo.getCoverUrl());
    }

    @Override
    public IPage<AlbumTrackListVo> findAlbumTrackPage(Long albumId, Page<AlbumTrackListVo> page) {
        // 1.根据专辑id分页查询声音
        IPage<AlbumTrackListVo> albumTrackListVoPage = this.trackInfoMapper.selectAlbumTrackPageByAlbumId(albumId, page);
        List<AlbumTrackListVo> albumTrackListVoList = albumTrackListVoPage.getRecords();

        // 2.根据id查询专辑，获取付费类型：0101-免费、0102-vip免费、0103-付费
        AlbumInfo albumInfo = this.albumInfoMapper.selectById(albumId);
        Assert.notNull(albumInfo, "专辑为空！！");
        // 3.判断付费类型
        String payType = albumInfo.getPayType();
        // 3.1 如果是免费 或者 当前专辑所属的用户是当前登录用户，直接返回声音分页列表（不显示付费标签）
        if (SystemConstant.ALBUM_PAY_TYPE_FREE.equals(payType)
                || Objects.equals(albumInfo.getUserId(), AuthContextHolder.getUserId())) {
            return albumTrackListVoPage;
        }

        // 3.2 获取需要付费的声音集合
        // 免费试听集数
        Integer tracksForFree = albumInfo.getTracksForFree();
        List<AlbumTrackListVo> needPayTracks = albumTrackListVoList.stream()
                .filter(trackVo -> trackVo.getOrderNum() > tracksForFree).toList();
        // 如果付费集数为null则直接返回
        if (CollectionUtils.isEmpty(needPayTracks)) {
            return albumTrackListVoPage;
        }

        // 是否需要显示付费标签
        Boolean isNeedPay = false;
        // 3.3 如果是vip收费，需要显示付费标签
        if (SystemConstant.ALBUM_PAY_TYPE_REQUIRE.equals(payType)) {
            isNeedPay = true;
        }

        // 3.4 如果是vip免费
        if (SystemConstant.ALBUM_PAY_TYPE_VIPFREE.equals(payType)) {
            // 获取登录状态，判断用户是否登录
            Long userId = AuthContextHolder.getUserId();
            if (userId == null) {
                // 如果未登录则需要显示付费标识
                isNeedPay = true;
            } else {
                // 查询用户信息
                Result<UserInfoVo> userInfoVoResult = this.userInfoFeignClient.getUserInfoById(userId);
                Assert.notNull(userInfoVoResult, "查询用户信息失败！");
                UserInfoVo userInfoVo = userInfoVoResult.getData();
                if (userInfoVo.getIsVip() == 0) {
                    // 如果用户不是vip，需要显示付费信息
                    isNeedPay = true;
                } else if (userInfoVo.getIsVip() == 1 && userInfoVo.getVipExpireTime().before(new Date())) {
                    // 如果用户是vip，但是已经过期了，需要显示付费标识
                    isNeedPay = true;
                }
            }
        }
        // 4.如果需要付费，判断用户是否购买过专辑或者声音
        if (isNeedPay) {
            // 根据专辑id查询该用户是否订购过当前专辑，如果订购过不显示付费标签
            Result<Boolean> paidAlbumStatRes = this.userInfoFeignClient.getPaidAlbumStat(albumId);
            Assert.notNull(paidAlbumStatRes, "根据专辑id查询当前用户是否订购过该专辑信息失败！");
            Boolean flag = paidAlbumStatRes.getData();
            if (flag == null || !flag) {
                // 没有订购过
                // 根据专辑id查询该用户订购过的声音列表
                Result<List<UserPaidTrack>> userPaidTracksRes = this.userInfoFeignClient.getPaidTracksByAlbumIdAndUserId(albumId);
                Assert.notNull(userPaidTracksRes, "没有获取到订购的声音列表");
                List<UserPaidTrack> userPaidTrackList = userPaidTracksRes.getData();
                if (!CollectionUtils.isEmpty(userPaidTrackList)) {
                    // 获取订阅过的声音ids
                    List<Long> paidTrackIds = userPaidTrackList.stream().map(UserPaidTrack::getTrackId).toList();
                    needPayTracks.forEach(track -> {
                        // 如果订购过得声音列表中不包含当前声音，则显示付费标识
                        if (!paidTrackIds.contains(track.getTrackId())) {
                            track.setIsShowPaidMark(true);
                        }
                    });
                } else {
                    needPayTracks.forEach(track -> {
                        track.setIsShowPaidMark(true);
                    });
                }
            }
        }
        return albumTrackListVoPage;
    }

    @Override
    public TrackStatVo getTrackStatVo(Long trackId) {
        List<TrackStat> trackStatList = this.trackStatMapper.selectList(Wrappers.lambdaQuery(TrackStat.class)
                .eq(TrackStat::getTrackId, trackId));
        Map<String, Integer> typeToNumMap = trackStatList.stream()
                .collect(Collectors.toMap(TrackStat::getStatType, TrackStat::getStatNum));
        TrackStatVo trackStatVo = new TrackStatVo();
        trackStatVo.setPlayStatNum(typeToNumMap.get(SystemConstant.TRACK_STAT_PLAY));
        trackStatVo.setCollectStatNum(typeToNumMap.get(SystemConstant.TRACK_STAT_COLLECT));
        trackStatVo.setCommentStatNum(typeToNumMap.get(SystemConstant.TRACK_STAT_COMMENT));
        trackStatVo.setPraiseStatNum(typeToNumMap.get(SystemConstant.TRACK_STAT_PRAISE));
        return trackStatVo;
    }

    @Override
    public JSONObject getPlayToken(Long trackId) {
        TrackInfo trackInfo = this.trackInfoMapper.selectById(trackId);
        Assert.notNull(trackInfo, "声音对象不能为空");
        AlbumInfo albumInfo = this.albumInfoMapper.selectById(trackInfo.getAlbumId());
        Assert.notNull(albumInfo, "专辑对象不能为空");

        if (!SystemConstant.ALBUM_PAY_TYPE_FREE.equals(albumInfo.getPayType()) &&
                trackInfo.getOrderNum() > albumInfo.getTracksForFree()) {
            Long userId = AuthContextHolder.getUserId();
            if (userId == null) {
                throw new GuiguException(ResultCodeEnum.NO_BUY_NOT_SEE);
            } else {
                // 查询用户是否购买过专辑
                Result<Boolean> paidAlbumStatRes = this.userInfoFeignClient.getPaidAlbumStat(albumInfo.getId());
                Assert.notNull(paidAlbumStatRes, "没有获取到专辑购买信息");
                Boolean isPaid = paidAlbumStatRes.getData();
                // 如果没有订购过该专辑，则判断是否订购过该声音
                if (isPaid == null || !isPaid) {
                    // 查询用户购买过该专辑下的声音列表
                    Result<List<UserPaidTrack>> userPaidTrackListRes = this.userInfoFeignClient.getPaidTracksByAlbumIdAndUserId(albumInfo.getId());
                    Assert.notNull(userPaidTrackListRes, "没有获取到声音购买信息");
                    List<UserPaidTrack> userPaidTrackList = userPaidTrackListRes.getData();
                    // 没有订购过该声音
                    if (CollectionUtils.isEmpty(userPaidTrackList) ||
                            userPaidTrackList.stream().noneMatch(userPaidTrack -> Objects.equals(userPaidTrack.getTrackId(), trackId))) {
                        // 查询用户信息
                        Result<UserInfoVo> userInfoVoResult = this.userInfoFeignClient.getUserInfoById(userId);
                        Assert.notNull(userInfoVoResult, "没有获取到用户信息");
                        UserInfoVo userInfoVo = userInfoVoResult.getData();
                        if (userInfoVo.getIsVip() == 0 || userInfoVo.getVipExpireTime().before(new Date()) ||
                                SystemConstant.ALBUM_PAY_TYPE_REQUIRE.equals(albumInfo.getPayType())) {
                            throw new GuiguException(ResultCodeEnum.NO_BUY_NOT_SEE);
                        }
                    }
                }
            }
        }

        // 获取播放进度
        Result<BigDecimal> trackBreakSecondRes = this.userListenProcessFeignClient.getTrackBreakSecond(trackId);
        Assert.notNull(trackBreakSecondRes, "获取播放进度失败");
        BigDecimal breakSecond = trackBreakSecondRes.getData();
        String playToken = this.vodService.getPlayToken(trackInfo.getMediaFileId(), trackInfo.getMediaType());
        JSONObject map = new JSONObject();
        map.put("playToken", playToken);
        map.put("mediaFileId", trackInfo.getMediaFileId());
        map.put("breakSecond", breakSecond);
        map.put("appId", vodConstantProperties.getAppId());

        //获取下一个播放声音
        TrackInfo nextTrackInfo = this.trackInfoMapper.selectOne(Wrappers.lambdaQuery(TrackInfo.class)
                .eq(TrackInfo::getAlbumId, trackInfo.getAlbumId())
                .gt(TrackInfo::getOrderNum, trackInfo.getOrderNum())
                .orderByAsc(TrackInfo::getOrderNum)
                .select(TrackInfo::getId)
                .last("limit 1")
        );
        if (null != nextTrackInfo) {
            map.put("nextTrackId", nextTrackInfo.getId());
        } else {
            map.put("nextTrackId", 0L);
        }
        return map;
    }
}
