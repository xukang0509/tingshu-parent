package com.atguigu.tingshu.album.service.impl;

import com.atguigu.tingshu.album.config.MinioConstantProperties;
import com.atguigu.tingshu.album.service.MinioService;
import com.atguigu.tingshu.album.util.MinioUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import io.minio.MinioClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@Service
public class MinioServiceImpl implements MinioService {
    @Resource
    private MinioConstantProperties minioConstantProperties;

    @Resource
    private MinioClient minioClient;

    @Override
    public String fileUpload(MultipartFile file) {
        try {
            String url = "";
            // 判断桶是否存在，如果不存在则创建桶
            MinioUtils.createBucket(minioClient, minioConstantProperties.getBucketName());
            // 完成文件上传，返回图片的链接路径以方便前端进行回调
            // 生成文件名
            String fileName = UUID.randomUUID().toString().replaceAll("-", "") + "." +
                    FilenameUtils.getExtension(file.getOriginalFilename());
            // 调用上传方法
            MinioUtils.uploadObject(minioClient, minioConstantProperties.getBucketName(), fileName,
                    file.getInputStream(), file.getSize(), file.getContentType());
            // 拼接url
            url = String.join("/", minioConstantProperties.getEndpointUrl(),
                    minioConstantProperties.getBucketName(), fileName);
            log.info("url: {}", url);
            return url;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Async
    @Override
    public void deleteFile(String url) {
        try {
            if (StringUtils.isBlank(url)) return;
            // 获取文件名
            String[] split = url.split("/");
            String fileName = split[split.length - 1];
            // 删除文件
            MinioUtils.deleteObject(minioClient, minioConstantProperties.getBucketName(), fileName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
