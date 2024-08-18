package com.atguigu.tingshu.order.template;

import com.atguigu.tingshu.common.constant.SystemConstant;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * @author xk
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface ConfirmTradeBean {
    String value() default SystemConstant.ORDER_ITEM_TYPE_ALBUM;
}
