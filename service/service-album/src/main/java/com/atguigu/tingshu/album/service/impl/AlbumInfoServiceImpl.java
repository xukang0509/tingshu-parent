package com.atguigu.tingshu.album.service.impl;

import com.atguigu.tingshu.album.mapper.AlbumAttributeValueMapper;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.AlbumStatMapper;
import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.album.service.MinioService;
import com.atguigu.tingshu.common.constant.RabbitMqConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.service.RabbitService;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.AlbumStat;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumAttributeValueVo;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class AlbumInfoServiceImpl extends ServiceImpl<AlbumInfoMapper, AlbumInfo> implements AlbumInfoService {

    @Resource
    private AlbumInfoMapper albumInfoMapper;

    @Resource
    private AlbumAttributeValueMapper albumAttributeValueMapper;

    @Resource
    private AlbumStatMapper albumStatMapper;

    @Resource
    private MinioService minioService;

    @Resource
    private RabbitService rabbitService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveAlbumInfo(AlbumInfoVo albumInfoVo) {
        // 0.拷贝专辑信息
        AlbumInfo albumInfo = new AlbumInfo();
        BeanUtils.copyProperties(albumInfoVo, albumInfo);
        // 1.保存专辑信息AlbumInfo
        // 获取userId，这里没有实现登录，先设置为1L，等之后完成登录后，再获取userId
        Long userId = AuthContextHolder.getUserId();
        albumInfo.setUserId(userId);
        // 审核通过
        albumInfo.setStatus(SystemConstant.ALBUM_STATUS_PASS);
        // 如果是付费专辑：设置前五集为免费试听；每集前30s免费试听
        if (!SystemConstant.ALBUM_PAY_TYPE_FREE.equals(albumInfo.getPayType())) {
            albumInfo.setTracksForFree(5);
            albumInfo.setSecondsForFree(30);
        }
        this.albumInfoMapper.insert(albumInfo);
        Long albumInfoId = albumInfo.getId();
        // 2.保存专辑属性值关联表 AlbumAttributeValue
        List<AlbumAttributeValueVo> albumAttributeValues = albumInfoVo.getAlbumAttributeValueVoList();
        if (!CollectionUtils.isEmpty(albumAttributeValues)) {
            for (AlbumAttributeValueVo albumAttributeValueVo : albumAttributeValues) {
                AlbumAttributeValue albumAttributeValue = new AlbumAttributeValue();
                BeanUtils.copyProperties(albumAttributeValueVo, albumAttributeValue);
                albumAttributeValue.setAlbumId(albumInfoId);
                this.albumAttributeValueMapper.insert(albumAttributeValue);
            }
        }
        // 3.保存专辑统计 AlbumStat (4个维度，统计数目初始为0)
        saveAlbumStat(albumInfoId, SystemConstant.ALBUM_STAT_PLAY);
        saveAlbumStat(albumInfoId, SystemConstant.ALBUM_STAT_SUBSCRIBE);
        saveAlbumStat(albumInfoId, SystemConstant.ALBUM_STAT_BROWSE);
        saveAlbumStat(albumInfoId, SystemConstant.ALBUM_STAT_COMMENT);
        // 4.如果新增专辑时isOpen是1(上架)，则发送消息新增到es
        if ("1".equals(albumInfo.getIsOpen())) {
            rabbitService.sendMessage(RabbitMqConstant.EXCHANGE_ALBUM_UPPER,
                    RabbitMqConstant.ROUTING_ALBUM_UPPER, albumInfoId);
        }
    }

    private void saveAlbumStat(Long albumId, String statType) {
        AlbumStat albumStat = new AlbumStat();
        albumStat.setAlbumId(albumId);
        albumStat.setStatType(statType);
        albumStat.setStatNum(new Random().nextInt(100));
        this.albumStatMapper.insert(albumStat);
    }

    @Override
    public Page<AlbumListVo> findUserAlbumPage(Integer pageNum, Integer pageSize, AlbumInfoQuery albumInfoQuery) {
        // 设置主播的id
        Long userId = AuthContextHolder.getUserId();
        albumInfoQuery.setUserId(userId);
        Page<AlbumListVo> albumListVoPage = new Page<>(pageNum, pageSize);
        return this.albumInfoMapper.selectUserAlbumPage(albumListVoPage, albumInfoQuery);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void removeAlbumInfoById(Long albumId) {
        AlbumInfo albumInfo = this.albumInfoMapper.selectOne(Wrappers.lambdaQuery(AlbumInfo.class)
                .eq(AlbumInfo::getId, albumId)
                .select(AlbumInfo::getCoverUrl)
                .last("limit 1"));
        // 0.删除minio中的图片
        minioService.deleteFile(albumInfo.getCoverUrl());
        // 1.删除专辑
        this.albumInfoMapper.deleteById(albumId);
        // 2.删除专辑属性值
        this.albumAttributeValueMapper.delete(Wrappers.lambdaQuery(AlbumAttributeValue.class)
                .eq(AlbumAttributeValue::getAlbumId, albumId));
        // 3.删除专辑统计信息
        this.albumStatMapper.delete(Wrappers.lambdaQuery(AlbumStat.class)
                .eq(AlbumStat::getAlbumId, albumId));
        // 4.删除es中对应的数据
        rabbitService.sendMessage(RabbitMqConstant.EXCHANGE_ALBUM_LOWER,
                RabbitMqConstant.ROUTING_ALBUM_LOWER, albumId);
    }

    @Override
    public AlbumInfo getAlbumInfoById(Long albumId) {
        AlbumInfo albumInfo = this.albumInfoMapper.selectById(albumId);
        if (!Objects.isNull(albumInfo)) {
            List<AlbumAttributeValue> albumAttributeValueList = this.albumAttributeValueMapper.selectList(Wrappers.lambdaQuery(AlbumAttributeValue.class)
                    .eq(AlbumAttributeValue::getAlbumId, albumId));
            albumInfo.setAlbumAttributeValueVoList(albumAttributeValueList);
        }
        return albumInfo;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateAlbumInfo(AlbumInfoVo albumInfoVo, Long albumId) {
        // 0.拷贝专辑信息
        AlbumInfo albumInfo = this.getById(albumId);
        String coverUrl = albumInfo.getCoverUrl();
        BeanUtils.copyProperties(albumInfoVo, albumInfo);
        // 1.更新专辑信息AlbumInfo
        if (SystemConstant.ALBUM_PAY_TYPE_FREE.equals(albumInfo.getPayType())) {
            albumInfo.setPrice(new BigDecimal("0.00"));
        }
        this.albumInfoMapper.updateById(albumInfo);
        // 2.删除专辑属性值关联信息
        this.albumAttributeValueMapper.delete(Wrappers.lambdaQuery(AlbumAttributeValue.class)
                .eq(AlbumAttributeValue::getAlbumId, albumId));
        // 3.保存专辑属性值关联表 AlbumAttributeValue
        List<AlbumAttributeValueVo> albumAttributeValues = albumInfoVo.getAlbumAttributeValueVoList();
        if (!CollectionUtils.isEmpty(albumAttributeValues)) {
            for (AlbumAttributeValueVo albumAttributeValueVo : albumAttributeValues) {
                // 拷贝专辑属性对象信息
                AlbumAttributeValue albumAttributeValue = new AlbumAttributeValue();
                BeanUtils.copyProperties(albumAttributeValueVo, albumAttributeValue);
                // 赋值专辑id
                albumAttributeValue.setAlbumId(albumId);
                this.albumAttributeValueMapper.insert(albumAttributeValue);
            }
        }
        // 4.判断是否删除minio服务中的旧图片
        if (!Objects.equals(coverUrl, albumInfo.getCoverUrl())) {
            minioService.deleteFile(coverUrl);
        }
        // 5.如果新增专辑时isOpen是1(上架)，则发送消息新增到es，否则发送消息删除es记录（不管es记录是否存在）
        if ("1".equals(albumInfo.getIsOpen())) {
            rabbitService.sendMessage(RabbitMqConstant.EXCHANGE_ALBUM_UPPER,
                    RabbitMqConstant.ROUTING_ALBUM_UPPER, albumId);
        } else {
            rabbitService.sendMessage(RabbitMqConstant.EXCHANGE_ALBUM_LOWER,
                    RabbitMqConstant.ROUTING_ALBUM_LOWER, albumId);
        }
    }

    @Override
    public List<AlbumInfo> findUserAllAlbumList() {
        //	查询数据：为了能查询到数据，如果userId为null则根据userId=1查询用户
        Long userId = AuthContextHolder.getUserId();
        return this.albumInfoMapper.selectList(Wrappers.lambdaQuery(AlbumInfo.class)
                .eq(AlbumInfo::getUserId, userId)
                .eq(AlbumInfo::getIsDeleted, 0)
                // 查询专辑id、标题即可
                .select(AlbumInfo::getId, AlbumInfo::getAlbumTitle)
                // 由于id是递增的，故id越大就是越接近添加的专辑，故降序排列
                .orderByDesc(AlbumInfo::getId));
    }

    @Override
    public Page<AlbumListVo> findAllAlbumPage(Integer pageNum, Integer pageSize) {
        Page<AlbumListVo> albumListVoPage = new Page<>(pageNum, pageSize);
        return this.albumInfoMapper.selectUserAlbumPage(albumListVoPage, new AlbumInfoQuery());
    }

    @Override
    public List<AlbumAttributeValue> findAlbumInfoAttributeValueByAlbumInfoId(Long albumInfoId) {
        return this.albumAttributeValueMapper.selectList(Wrappers.lambdaQuery(AlbumAttributeValue.class)
                .eq(AlbumAttributeValue::getAlbumId, albumInfoId));
    }

    @Override
    public AlbumStatVo getAlbumStatsByAlbumId(Long albumId) {
        List<AlbumStat> albumStatList = this.albumStatMapper.selectList(Wrappers.lambdaQuery(AlbumStat.class)
                .eq(AlbumStat::getAlbumId, albumId));
        if (CollectionUtils.isEmpty(albumStatList)) return null;
        Map<String, Integer> typeToNumMap = albumStatList.stream().collect(Collectors.toMap(AlbumStat::getStatType, AlbumStat::getStatNum));
        AlbumStatVo albumStatVo = new AlbumStatVo();
        albumStatVo.setAlbumId(albumId);
        albumStatVo.setPlayStatNum(typeToNumMap.get(SystemConstant.ALBUM_STAT_PLAY));
        albumStatVo.setSubscribeStatNum(typeToNumMap.get(SystemConstant.ALBUM_STAT_SUBSCRIBE));
        albumStatVo.setBuyStatNum(typeToNumMap.get(SystemConstant.ALBUM_STAT_BROWSE));
        albumStatVo.setCommentStatNum(typeToNumMap.get(SystemConstant.ALBUM_STAT_COMMENT));
        return albumStatVo;
    }
}
