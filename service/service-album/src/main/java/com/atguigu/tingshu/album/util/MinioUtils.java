package com.atguigu.tingshu.album.util;

import io.minio.*;
import io.minio.errors.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * minio工具类
 *
 * @author xk
 * @since 2024-08-10 20:38
 */
@Slf4j
public class MinioUtils {

    /**
     * 上串文件至minio服务器
     *
     * @param minioClient minio客户端
     * @param bucketName  桶名称
     * @param fileName    文件名
     * @param inputStream 文件流
     * @param size        文件大小
     * @param contentType 文件类型
     */
    public static void uploadObject(MinioClient minioClient, String bucketName, String fileName, InputStream inputStream, long size, String contentType) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucketName)
                .object(fileName)
                .stream(inputStream, size, -1)
                .contentType(contentType)
                .build());
    }

    /**
     * 删除minio服务器中的文件
     *
     * @param minioClient minio客户端
     * @param bucketName  桶名称
     * @param fileName    文件名
     */
    public static void deleteObject(MinioClient minioClient, String bucketName, String fileName) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        minioClient.removeObject(RemoveObjectArgs.builder()
                .bucket(bucketName)
                .object(fileName).build());
    }

    /**
     * 判断桶是否存在，如果不存在则创建新的桶
     *
     * @param minioClient minio客户端
     * @param bucketName  同名称
     */
    public static void createBucket(MinioClient minioClient, String bucketName) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(bucketName).build());
        if (!bucketExists) {
            // 如果桶不存在则创建桶
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            log.info("Build New Bucket {}.", bucketName);
        }
    }
}
