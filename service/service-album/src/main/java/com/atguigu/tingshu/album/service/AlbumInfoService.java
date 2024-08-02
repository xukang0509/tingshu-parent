package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.AlbumStat;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface AlbumInfoService extends IService<AlbumInfo> {
    /**
     * 新增专辑
     *
     * @param albumInfoVo 专辑
     */
    void saveAlbumInfo(AlbumInfoVo albumInfoVo);

    /**
     * 分页条件查询专辑
     *
     * @param pageNum        当前页码
     * @param pageSize       每页显示条数
     * @param albumInfoQuery 查询条件
     * @return 专辑列表
     */
    Page<AlbumListVo> findUserAlbumPage(Integer pageNum, Integer pageSize, AlbumInfoQuery albumInfoQuery);

    /**
     * 删除专辑
     *
     * @param albumId 专辑主键Id
     */
    void removeAlbumInfoById(Long albumId);

    /**
     * 获取专辑详情：数据回显
     *
     * @param albumId 专辑主键Id
     * @return 专辑
     */
    AlbumInfo getAlbumInfoById(Long albumId);

    /**
     * 更新专辑
     *
     * @param albumInfoVo 专辑
     * @param albumId     专辑主键Id
     */
    void updateAlbumInfo(AlbumInfoVo albumInfoVo, Long albumId);

    /**
     * 查看当前用户的专辑列表
     *
     * @return 专辑列表
     */
    List<AlbumInfo> findUserAllAlbumList();

    /**
     * 分页查询所有专辑信息
     *
     * @param pageNum  当前页码
     * @param pageSize 每页显示条数
     * @return 专辑列表
     */
    Page<AlbumListVo> findAllAlbumPage(Integer pageNum, Integer pageSize);


    /**
     * 根据专辑id查询专辑属性信息
     *
     * @param albumInfoId 专辑id
     * @return 专辑属性信息
     */
    List<AlbumAttributeValue> findAlbumInfoAttributeValueByAlbumInfoId(Long albumInfoId);

    /**
     * 根据专辑id获取专辑统计信息
     *
     * @param albumId 专辑id
     * @return 统计信息列表
     */
    List<AlbumStat> getAlbumStatsByAlbumId(Long albumId);
}
