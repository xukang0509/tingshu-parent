package com.atguigu.tingshu.album.service.impl;

import com.atguigu.tingshu.album.config.MinioConstantProperties;
import com.atguigu.tingshu.album.service.MinioService;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import io.minio.*;
import io.minio.errors.*;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Slf4j
@Service
public class MinioServiceImpl implements MinioService {
    @Resource
    private MinioConstantProperties minioConstantProperties;

    @SneakyThrows
    @Override
    public String fileUpload(MultipartFile file) {
        String url = "";
        // 1.获取minio客户端
        MinioClient minioClient = getMinioClient();
        // 2.判断桶是否存在，如果不存在则创建桶
        if (!isBucketedExists(minioClient)) {
            // 如果桶不存在则创建桶
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioConstantProperties.getBucketName()).build());
            log.info("Build New Bucket {}.", minioConstantProperties.getBucketName());
        } else {
            log.info("Bucket {} already exists.", minioConstantProperties.getBucketName());
        }
        // 3.完成文件上传，返回图片的链接路径以方便前端进行回调
        // 生成文件名
        String fileName = UUID.randomUUID().toString().replaceAll("-", "") + "." +
                FilenameUtils.getExtension(file.getOriginalFilename());
        // 调用上传方法
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(minioConstantProperties.getBucketName())
                .object(fileName)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build());
        // 拼接url
        url = String.join("/", minioConstantProperties.getEndpointUrl(),
                minioConstantProperties.getBucketName(), fileName);
        log.info("url: {}", url);
        return url;
    }

    @Async
    @SneakyThrows
    @Override
    public void deleteFile(String url) {
        if (StringUtils.isBlank(url)) return;
        // 获取文件名
        String[] split = url.split("/");
        String fileName = split[split.length - 1];
        // 获取minio客户端
        MinioClient minioClient = getMinioClient();
        // 删除文件
        minioClient.removeObject(RemoveObjectArgs.builder()
                .bucket(minioConstantProperties.getBucketName())
                .object(fileName).build());
    }

    private boolean isBucketedExists(MinioClient minioClient) throws ErrorResponseException, InsufficientDataException, InternalException, InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException, XmlParserException {
        return minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(minioConstantProperties.getBucketName()).build());
    }

    private MinioClient getMinioClient() {
        // 初始化minio客户端：minio服务器链接路径、用户名、密码
        return MinioClient.builder()
                .endpoint(minioConstantProperties.getEndpointUrl())
                .credentials(minioConstantProperties.getAccessKey(), minioConstantProperties.getSecreKey())
                .build();
    }
}
