package com.atguigu.tingshu.live.util;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.model.live.FromUser;
import com.atguigu.tingshu.model.live.SocketMsg;
import jakarta.websocket.Session;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketLocalContainer {
    // 聊天群体：直播间id和用户之间的对应关系，可以根据直播间id获取直播间用户集合
    private static Map<Long, Set<Long>> userIdsMap = new ConcurrentHashMap<>();

    // 用户id与用户基本信息之间的对应关系，通过用户id可以获取用户的基本信息
    // 建立连接时，可以根据token获取用户id，获取用户的基本信息，放入该容器
    // 将来用户发送消息时，可以根据用户id获取用户的基本信息
    private static Map<Long, FromUser> fromUserMap = new ConcurrentHashMap<>();

    // 每个用户对应的session会话列表
    private static Map<Long, Session> sessionMap = new ConcurrentHashMap<>();

    // ====================userIdsMap====================
    public static void addUserToLiveRoom(Long liveRoomId, Long userId) {
        Set<Long> userIds = userIdsMap.computeIfAbsent(liveRoomId, k -> new HashSet<>());
        userIds.add(userId);
    }

    public static void removeUserFromLiveRoom(Long liveRoomId, Long userId) {
        Set<Long> userIds = userIdsMap.get(liveRoomId);
        if (userIds == null) return;
        userIds.remove(userId);
    }

    public static Set<Long> getUserIds(Long liveRoomId) {
        return userIdsMap.computeIfAbsent(liveRoomId, k -> new HashSet<>());
    }

    public static Integer getUserCount(Long liveRoomId) {
        return userIdsMap.computeIfAbsent(liveRoomId, k -> new HashSet<>()).size();
    }

    // ====================fromUserMap====================
    public static void addFromUser(Long userId, FromUser fromUser) {
        fromUserMap.put(userId, fromUser);
    }

    public static void removeFromUser(Long userId) {
        fromUserMap.remove(userId);
    }

    public static FromUser getFromUser(Long userId) {
        return fromUserMap.get(userId);
    }

    // ====================sessionMap====================
    public static void addSession(Long userId, Session session) {
        sessionMap.put(userId, session);
    }

    public static void removeSession(Long userId) {
        sessionMap.remove(userId);
    }

    public static Session getSession(Long userId) {
        return sessionMap.get(userId);
    }

    /**
     * 封装群发消息的方法
     *
     * @param socketMsg 消息体
     */
    public static void sendMsg(SocketMsg socketMsg) {
        // 获取同一直播间的用户id集合
        Set<Long> userIds = WebSocketLocalContainer.getUserIds(socketMsg.getLiveRoomId());
        if (CollectionUtils.isEmpty(userIds)) return;
        // 群发消息
        for (Long userId : userIds) {
            // 获取对应用户的session
            Session session = WebSocketLocalContainer.getSession(userId);
            if (session != null) {
                // 异步发送消息
                session.getAsyncRemote().sendText(JSON.toJSONString(socketMsg));
            }
        }
    }
}
