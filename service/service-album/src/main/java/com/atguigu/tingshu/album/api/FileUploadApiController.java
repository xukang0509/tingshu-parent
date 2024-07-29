package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.MinioService;
import com.atguigu.tingshu.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "上传管理接口")
@RestController
@RequestMapping("api/album")
public class FileUploadApiController {
    @Resource
    private MinioService minioService;

    // 文件上传
    @Operation(summary = "文件上传")
    @PostMapping("fileUpload")
    public Result<String> fileUpload(MultipartFile file) {
        return Result.ok(minioService.fileUpload(file));
    }
}
