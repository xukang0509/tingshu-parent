package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.config.MinioConstantProperties;
import com.atguigu.tingshu.common.result.Result;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@Tag(name = "上传管理接口")
@RestController
@RequestMapping("api/album")
public class FileUploadApiController {
    @Autowired
    private MinioConstantProperties minioConstantProperties;

    // 文件上传
    @Operation(summary = "文件上传")
    @PostMapping("fileUpload")
    public Result<String> fileUpload(MultipartFile file) throws Exception {
        String accessKey = minioConstantProperties.getAccessKey();
        String secreKey = minioConstantProperties.getSecreKey();
        String bucketName = minioConstantProperties.getBucketName();
        String endpointUrl = minioConstantProperties.getEndpointUrl();

        String url = "";
        // 1.初始化minio客户端：minio服务器链接路径、用户名、密码
        MinioClient minioClient = MinioClient.builder()
                .endpoint(endpointUrl)
                .credentials(accessKey, secreKey)
                .build();
        // 2.判断桶是否存在，如果不存在则创建桶
        boolean bucketedExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!bucketedExists) {
            // 如果桶不存在则创建桶
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        } else {
            log.info("Bucket {} already exists.", bucketName);
        }
        // 3.完成文件上传，返回图片的链接路径以方便前端进行回调
        // 生成文件名
        String fileName = UUID.randomUUID().toString().replaceAll("-", "") + "." +
                FilenameUtils.getExtension(file.getOriginalFilename());
        // 调用上传方法
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucketName)
                .object(fileName)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build());
        // 拼接url
        url = String.join("/", endpointUrl, bucketName, fileName);
        log.info("url: {}", url);
        return Result.ok(url);
    }
}
