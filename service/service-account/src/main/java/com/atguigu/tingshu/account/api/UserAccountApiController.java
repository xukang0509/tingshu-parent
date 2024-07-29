package com.atguigu.tingshu.account.api;

import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.account.UserAccount;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        UserAccount userAccount = new UserAccount();
        userAccount.setUserId(userId);
        this.userAccountService.save(userAccount);
        return Result.ok();
    }

}

