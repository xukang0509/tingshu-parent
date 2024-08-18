package com.atguigu.tingshu.order.template;

import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author xk
 * @since 2024-08-18 16:00
 */
@Component
public class ConfirmTradeClient implements ApplicationContextAware {
    private static final Map<String, ConfirmTradeTemplate> CONFIRM_TRADE_MAP = new ConcurrentHashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(ConfirmTradeBean.class);
        beans.forEach((itemType, tradeConfirm) -> {
            CONFIRM_TRADE_MAP.put(itemType, (ConfirmTradeTemplate) tradeConfirm);
        });
    }

    public OrderInfoVo execute(TradeVo tradeVo) {
        return CONFIRM_TRADE_MAP.get(tradeVo.getItemType()).confirmTrade(tradeVo);
    }
}
