package com.atguigu.tingshu.common.constant;

public class RabbitMqConstant {

    /**
     * 专辑上架
     */
    public static final String EXCHANGE_ALBUM_UPPER = "tingshu.album.upper";
    public static final String QUEUE_ALBUM_UPPER = "tingshu.album.upper";
    public static final String ROUTING_ALBUM_UPPER = "tingshu.album.upper";

    /**
     * 专辑下架
     */
    public static final String EXCHANGE_ALBUM_LOWER = "tingshu.album.lower";
    public static final String QUEUE_ALBUM_LOWER = "tingshu.album.lower";
    public static final String ROUTING_ALBUM_LOWER = "tingshu.album.lower";

    /**
     * 更新 声音及专辑 的统计状态
     */
    public static final String EXCHANGE_STAT_UPDATE = "tingshu.stat.update";
    public static final String QUEUE_STAT_UPDATE = "tingshu.stat.update";
    public static final String ROUTING_STAT_UPDATE = "tingshu.stat.update";


    /*==============================================================================*/

    public static final String QUEUE_ALBUM_STAT_UPDATE = "tingshu.album.stat.update";
    public static final String QUEUE_ALBUM_ES_STAT_UPDATE = "tingshu.album.es.stat.update";
    public static final String QUEUE_ALBUM_RANKING_UPDATE = "tingshu.album.ranking.update";

    /**
     * 取消订单
     */
    //延迟取消订单队列
    public static final String QUEUE_ORDER_CANCEL = "tingshu.queue.order.cancel";
    //取消订单 延迟时间 单位：秒
    public static final int DELAY_TIME = 5 * 60;

    /**
     * 支付
     */
    public static final String QUEUE_ORDER_PAY_SUCCESS = "tingshu.order.pay.success";
    public static final String QUEUE_RECHARGE_PAY_SUCCESS = "tingshu.recharge.pay.success";


    /**
     * 账户
     */
    public static final String QUEUE_ACCOUNT_UNLOCK = "tingshu.account.unlock";
    public static final String QUEUE_ACCOUNT_MINUS = "tingshu.account.minus";

    /**
     * 用户
     */
    public static final String QUEUE_USER_PAY_RECORD = "tingshu.user.pay.record";
    public static final String QUEUE_USER_REGISTER = "tingshu.user.register";
    public static final String QUEUE_USER_VIP_EXPIRE_STATUS = "tingshu.user.vip.expire.status";

    /**
     * 热门关键字
     */
    public static final String QUEUE_KEYWORD_INPUT = "tingshu.keyword.input";
    public static final String QUEUE_KEYWORD_OUT = "tingshu.keyword.out";
}
