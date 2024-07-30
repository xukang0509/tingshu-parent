package com.atguigu.tingshu.user.login;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginClient implements ApplicationContextAware {
    private static final Map<String, LoginStrategy> LOGIN_STRATEGY_MAP = new ConcurrentHashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(TSLogin.class);
        for (Object value : beans.values()) {
            LoginStrategy loginStrategy = (LoginStrategy) value;
            LOGIN_STRATEGY_MAP.put(loginStrategy.getClass().getAnnotation(TSLogin.class).value().getType(), loginStrategy);
        }
    }

    public Map<String, Object> execute(String type, LoginForm loginForm) {
        return LOGIN_STRATEGY_MAP.get(type).login(loginForm);
    }
}
