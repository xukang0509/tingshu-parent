package com.atguigu.tingshu.common.login;

import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AuthLoginAspect {
    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 身份认证环绕通知
     */
    @Around("execution(* com.atguigu.tingshu.*.api.*.*(..)) && @annotation(authLogin)")
    public Object loginAspect(ProceedingJoinPoint joinPoint, AuthLogin authLogin) {
        try {
            // 获取请求对象 并 转化为 ServletRequestAttributes
            ServletRequestAttributes sra =
                    (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            // 获取到HttpServletRequest
            HttpServletRequest request = sra.getRequest();
            String token = request.getHeader("token");
            // 不管需不需要强制登录，但是都有可能需要用户信息
            UserInfoVo userInfoVo = null;
            if (StringUtils.isNotBlank(token)) {
                // 如果token不为空，从缓存中获取数据
                userInfoVo = (UserInfoVo) redisTemplate.opsForValue().get(RedisConstant.USER_LOGIN_KEY_PREFIX + token);
                if (userInfoVo != null) {
                    // 将用户信息存储到请求头中
                    AuthContextHolder.setUserId(userInfoVo.getId());
                    AuthContextHolder.setUsername(userInfoVo.getNickname());
                }
            }
            // 如果需要登录，而用户又没有登录，则抛出异常去登录
            if (authLogin.required() && userInfoVo == null) {
                throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
            }
            // 执行目标方法
            return joinPoint.proceed();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            // 方法执行完毕后，清除当前线程 用户信息 缓存(防止内存泄漏)
            AuthContextHolder.removeUserId();
            AuthContextHolder.removeUsername();
        }
    }
}