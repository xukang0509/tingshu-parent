package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.user.service.VipServiceConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "VIP服务配置管理接口")
@RestController
@RequestMapping("api/user/vipServiceConfig")
@SuppressWarnings({"unchecked", "rawtypes"})
public class VipServiceConfigApiController {

    @Autowired
    private VipServiceConfigService vipServiceConfigService;

    // http://127.0.0.1:8500/api/user/vipServiceConfig/findAll
    @Operation(summary = "查询VIP套餐信息")
    @GetMapping("findAll")
    public Result<List<VipServiceConfig>> findAll() {
        return Result.ok(this.vipServiceConfigService.list());
    }

    @Operation(summary = "根据id查询套餐信息")
    @GetMapping("getVipServiceConfig/{id}")
    public Result<VipServiceConfig> getVipServiceConfig(@PathVariable("id") Long id) {
        VipServiceConfig vipServiceConfig = this.vipServiceConfigService.getById(id);
        return Result.ok(vipServiceConfig);
    }
}

