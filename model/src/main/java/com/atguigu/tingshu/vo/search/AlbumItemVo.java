package com.atguigu.tingshu.vo.search;

import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import lombok.Data;

@Data
public class AlbumItemVo {
    private AlbumInfo albumInfo;
    private UserInfoVo announcer;
    private BaseCategoryView baseCategoryView;
    private AlbumStatVo albumStatVo;
}
