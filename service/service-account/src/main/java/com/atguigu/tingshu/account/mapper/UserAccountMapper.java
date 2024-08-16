package com.atguigu.tingshu.account.mapper;

import com.atguigu.tingshu.model.account.UserAccount;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {

    UserAccount check(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    int lock(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    int minus(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    int unlock(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
}
