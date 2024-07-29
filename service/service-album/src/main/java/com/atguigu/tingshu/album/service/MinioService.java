package com.atguigu.tingshu.album.service;

import org.springframework.web.multipart.MultipartFile;

public interface MinioService {
    /**
     * 上传文件
     *
     * @param file 文件
     * @return 访问文件url
     */
    String fileUpload(MultipartFile file);

    /**
     * 删除文件
     *
     * @param url 访问文件url
     */
    void deleteFile(String url);
}
