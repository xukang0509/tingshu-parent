package com.atguigu.tingshu.user.login;

import lombok.Getter;

@Getter
public enum LoginType {
    WX_LOGIN("1", "微信登录"),
    PHONE_LOGIN("2", "手机号登录"),
    QQ_LOGIN("3", "QQ登录"),
    WB_LOGIN("4", "微博登录"),
    ACCOUNT_LOGIN("5", "账号登录");
    
    private String type;
    private String desc;

    LoginType(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }
}
