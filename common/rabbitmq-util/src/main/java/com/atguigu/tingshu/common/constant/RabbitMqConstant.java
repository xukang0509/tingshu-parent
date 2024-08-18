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

    /**
     * 账户余额支付成功：发送消息减余额
     */
    public static final String EXCHANGE_ACCOUNT_MINUS = "tingshu.account:minus";
    public static final String QUEUE_ACCOUNT_MINUS = "tingshu.account.minus";
    public static final String ROUTING_ACCOUNT_MINUS = "tingshu.account.minus";

    /**
     * 账户余额支付失败：发送消息解锁余额
     */
    public static final String EXCHANGE_ACCOUNT_UNLOCK = "tingshu.account:unlock";
    public static final String QUEUE_ACCOUNT_UNLOCK = "tingshu.account.unlock";
    public static final String ROUTING_ACCOUNT_UNLOCK = "tingshu.account.unlock";

    /**
     * 支付成功
     */
    public static final String EXCHANGE_ORDER_PAY_SUCCESS = "tingshu.order.pay";
    public static final String QUEUE_ORDER_PAY_SUCCESS = "tingshu.order.pay.success";
    public static final String ROUTING_ORDER_PAY_SUCCESS = "tingshu.order.pay.success";

    /**
     * 订单支付成功后，更新用户购买记录
     */
    public static final String EXCHANGE_USER_PAY_RECORD = "tingshu.user.pay.record";
    public static final String QUEUE_USER_PAY_RECORD = "tingshu.user.pay.record";
    public static final String ROUTING_USER_PAY_RECORD = "tingshu.user.pay.record";

    /**
     * 延迟队列(死信队列实现)---定时取消订单
     */
    public static final String EXCHANGE_ORDER = "tingshu.order";
    public static final String QUEUE_ORDER_DEAD = "tingshu.order.dead";
    public static final String ROUTING_ORDER_DEAD = "tingshu.order.dead";

    /**
     * 取消订单
     */
    public static final String QUEUE_CANCEL_ORDER = "tingshu.order.cancel";
    public static final String ROUTING_CANCEL_ORDER = "tingshu.order.cancel";

    /**
     * 延迟队列(死信队列实现)---定时解锁余额
     */
    public static final String EXCHANGE_ACCOUNT = "tingshu.account";
    public static final String QUEUE_ACCOUNT_UNLOCK_DEAD = "tingshu.account.unlock.dead";
    public static final String ROUTING_ACCOUNT_UNLOCK_DEAD = "tingshu.account.unlock.dead";

    /*==============================================================================*/

    public static final String QUEUE_ALBUM_STAT_UPDATE = "tingshu.album.stat.update";
    public static final String QUEUE_ALBUM_ES_STAT_UPDATE = "tingshu.album.es.stat.update";
    public static final String QUEUE_ALBUM_RANKING_UPDATE = "tingshu.album.ranking.update";
    /**
     * 支付
     */
    public static final String QUEUE_RECHARGE_PAY_SUCCESS = "tingshu.recharge.pay.success";

    /**
     * 用户
     */

    public static final String QUEUE_USER_REGISTER = "tingshu.user.register";
    public static final String QUEUE_USER_VIP_EXPIRE_STATUS = "tingshu.user.vip.expire.status";

    /**
     * 热门关键字
     */
    public static final String QUEUE_KEYWORD_INPUT = "tingshu.keyword.input";
    public static final String QUEUE_KEYWORD_OUT = "tingshu.keyword.out";
}
