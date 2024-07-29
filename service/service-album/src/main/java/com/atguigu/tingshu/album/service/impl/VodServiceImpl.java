package com.atguigu.tingshu.album.service.impl;

import com.atguigu.tingshu.album.config.VodConstantProperties;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.util.UploadFileUtil;
import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import com.atguigu.tingshu.vo.album.VodFileUploadVo;
import com.qcloud.vod.VodUploadClient;
import com.qcloud.vod.model.VodUploadRequest;
import com.qcloud.vod.model.VodUploadResponse;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.vod.v20180717.VodClient;
import com.tencentcloudapi.vod.v20180717.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class VodServiceImpl implements VodService {

    @Autowired
    private VodConstantProperties vodConstantProperties;

    @Override
    public VodFileUploadVo uploadTrack(MultipartFile file) {
        // 把上传的媒体文件，保存到临时路径下
        String tempPath = UploadFileUtil.uploadTempPath(vodConstantProperties.getTempPath(), file);
        // 初始化一个上传客户端对象
        VodUploadClient client = new VodUploadClient(vodConstantProperties.getSecretId(), vodConstantProperties.getSecretKey());
        // 构造上传请求对象
        VodUploadRequest request = new VodUploadRequest();
        request.setMediaFilePath(tempPath);
        //  指定任务流
        request.setProcedure(vodConstantProperties.getProcedure());
        // 调用上传
        try {
            VodUploadResponse response = client.upload(vodConstantProperties.getRegion(), request);
            return new VodFileUploadVo(response.getFileId(), response.getMediaUrl());
        } catch (Exception e) {
            // 业务方进行异常处理
            log.error(e.toString());
        }
        return null;
    }

    @Override
    public TrackMediaInfoVo getMediaInfo(String mediaFileId) {
        try {
            // 实例化一个认证对象，入参需要传入腾讯云账户 SecretId 和 SecretKey，此处还需注意密钥对的保密
            Credential cred = new Credential(vodConstantProperties.getSecretId(), vodConstantProperties.getSecretKey());
            // 实例化要请求产品的client对象，clientProfile是可选的
            VodClient vodClient = new VodClient(cred, vodConstantProperties.getRegion());
            // 实例化一个请求对象,每个接口都会对应一个request对象
            DescribeMediaInfosRequest req = new DescribeMediaInfosRequest();
            req.setFileIds(new String[]{mediaFileId});
            // 返回的resp是一个DescribeMediaInfosResponse的实例，与请求对象对应
            DescribeMediaInfosResponse resp = vodClient.DescribeMediaInfos(req);
            // 输出json格式的字符串回包
            log.info(DescribeMediaInfosResponse.toJsonString(resp));

            // 获取媒体信息，如果不为空
            MediaInfo[] mediaInfoSet = resp.getMediaInfoSet();
            if (mediaInfoSet != null && mediaInfoSet.length > 0) {
                MediaInfo mediaInfo = mediaInfoSet[0];
                // 组装对象并返回
                TrackMediaInfoVo trackMediaInfoVo = new TrackMediaInfoVo();
                trackMediaInfoVo.setDuration(mediaInfo.getMetaData().getDuration());
                trackMediaInfoVo.setSize(mediaInfo.getMetaData().getSize());
                trackMediaInfoVo.setType(mediaInfo.getBasicInfo().getType());
                trackMediaInfoVo.setMediakUrl(mediaInfo.getBasicInfo().getMediaUrl());
                return trackMediaInfoVo;
            }
        } catch (TencentCloudSDKException e) {
            log.error(e.toString());
        }
        return null;
    }

    @Async
    @Override
    public void removeTrackMedia(String mediaFileId) {
        try {
            // 实例化一个认证对象，入参需要传入腾讯云账户 SecretId 和 SecretKey，此处还需注意密钥对的保密
            Credential cred = new Credential(vodConstantProperties.getSecretId(), vodConstantProperties.getSecretKey());
            // 实例化要请求产品的client对象
            VodClient client = new VodClient(cred, vodConstantProperties.getRegion());
            // 实例化一个请求对象,每个接口都会对应一个request对象
            DeleteMediaRequest req = new DeleteMediaRequest();
            req.setFileId(mediaFileId);
            // 返回的resp是一个DeleteMediaResponse的实例，与请求对象对应
            DeleteMediaResponse resp = client.DeleteMedia(req);
            // 输出json格式的字符串回包
            log.info(DeleteMediaResponse.toJsonString(resp));
        } catch (TencentCloudSDKException e) {
            log.error(e.toString());
        }
    }
}
