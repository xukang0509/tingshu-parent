package com.atguigu.tingshu.user.login;

import lombok.Data;

@Data
public class LoginForm {
    // 微信的临时登录凭证code
    private String code;

    // 账号登录
    private String userName;
    private String passWord;

    // 手机号登录
    private String phone;
}
