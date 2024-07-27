package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import com.atguigu.tingshu.vo.album.VodFileUploadVo;
import org.springframework.web.multipart.MultipartFile;

public interface VodService {

    /**
     * 上传声音
     *
     * @param file
     * @return
     */
    VodFileUploadVo uploadTrack(MultipartFile file);

    /**
     * 根据媒体文件id获取声音媒体信息
     *
     * @param mediaFileId 媒体文件id
     * @return 声音媒体信息
     */
    TrackMediaInfoVo getMediaInfo(String mediaFileId);

    /**
     * 删除音频信息
     *
     * @param mediaFileId 媒体文件id
     */
    void removeTrackMedia(String mediaFileId);
}
