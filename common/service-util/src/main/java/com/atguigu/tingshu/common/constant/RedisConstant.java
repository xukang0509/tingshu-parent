package com.atguigu.tingshu.common.constant;

public class RedisConstant {
    // 直播发布订阅模式，即时通讯中接收消息的通道名
    public static final String LIVE_MESSAGE_CHANNEL = "tingshu:live:message";

    // 专辑详情信息前缀
    public static final String ALBUM_INFO_PREFIX = "album:info:";
    // 获取专辑详情信息的分布式锁前缀
    public static final String ALBUM_LOCK_PREFIX = "album:lock:";
    // 专辑详情信息的缓存时间：60分钟
    public static final long ALBUM_TIMEOUT = 30 * 60L;
    // 空数据的缓存时间：10分钟
    public static final long ALBUM_TEMPORARY_TIMEOUT = 10 * 60L;

    // 专辑 布隆过滤器使用！
    public static final String ALBUM_BLOOM_FILTER = "album:bloom:filter";

    // 默认缓存的前缀、分布式锁key前缀、数据的缓存时间
    public static final String CACHE_INFO_PREFIX = "cache:info:";
    public static final String CACHE_LOCK_PREFIX = "cache:lock:";
    public static final long CACHE_INFO_TIMEOUT = 30 * 60L;
    // 防止缓存雪崩设置的随机值范围
    public static final long CACHE_RANDOM_TIMEOUT = 10 * 60L;

    public static final long LOCK_EXPIRE_TIME = 5;

    public static final long CACHE_TIMEOUT = 24 * 60 * 60;
    // 商品如果在数据库中不存在那么会缓存一个空对象进去，但是这个对象是没有用的，所以这个对象的过期时间应该不能太长，
    // 如果太长会占用内存。
    // 定义变量，记录空对象的缓存过期时间
    public static final long CACHE_TEMPORARY_TIMEOUT = 10 * 60;

    //用户登录
    public static final String USER_LOGIN_KEY_PREFIX = "user:login:";
    public static final String USER_LOGIN_REFRESH_KEY_PREFIX = "user:login:refresh:";
    public static final int USER_LOGIN_KEY_TIMEOUT = 60 * 60 * 24 * 100;
    public static final int USER_LOGIN_REFRESH_KEY_TIMEOUT = 60 * 60 * 24 * 365;

    public static final String RANKING_KEY_PREFIX = "ranking:";
    public static final String ALBUM_STAT_ENDTIME = "album:stat:endTime";


}
