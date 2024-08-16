package com.atguigu.tingshu.account.api;

import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.login.AuthLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.vo.account.AccountLockResultVo;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Tag(name = "用户账户管理")
@RestController
@RequestMapping("api/account/userAccount")
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserAccountApiController {

    @Resource
    private UserAccountService userAccountService;

    @Operation(summary = "新增用户账户")
    @PostMapping("save/{userId}")
    public Result saveUserAccount(@PathVariable("userId") Long userId) {
        this.userAccountService.saveUserAccount(userId);
        return Result.ok();
    }

    // http://127.0.0.1:8500/api/account/userAccount/getAvailableAmount
    @AuthLogin(required = false)
    @Operation(summary = "获取用户账户余额")
    @GetMapping("getAvailableAmount")
    public Result<BigDecimal> getAvailableAmount() {
        return Result.ok(this.userAccountService.getAvailableAmount());
    }

    // http://127.0.0.1:8500/api/account/userAccount/checkAndLock
    @AuthLogin
    @Operation(summary = "验余额并锁账户")
    @PostMapping("checkAndLock")
    public Result<AccountLockResultVo> checkAndLock(@RequestBody AccountLockVo accountLockVo) {
        return this.userAccountService.checkAndLock(accountLockVo);
    }

}

