package com.atguigu.tingshu.live.api;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.live.util.WebSocketLocalContainer;
import com.atguigu.tingshu.model.live.FromUser;
import com.atguigu.tingshu.model.live.SocketMsg;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;

/**
 * WebSocket常用注解：
 *
 * @ServerEndpoint 通过这个 spring boot 就可以知道你暴露出去的 ws 应用的路径，
 * 有点类似我们经常用的@RequestMapping。比如你的启动端口是8080，
 * 而这个注解的值是api，那我们就可以通过 ws://127.0.0.1:8080/api 来连接你的应用
 * @OnOpen 当websocket建立连接成功后会触发这个注解修饰的方法，注意它有一个Session参数
 * @OnClose 当websocket建立的连接断开后会触发这个注解修饰的方法，注意它有一个Session参数
 * @OnMessage 当客户端发送消息到服务端时，会触发这个注解修改的方法，它有一个String入参表明客户端传入的值
 * @OnError 当websocket建立连接时出现异常会触发这个注解修饰的方法，注意它有一个Session参数
 */
@Slf4j
@Tag(name = "直播间即时通信接口")
@ServerEndpoint("/api/websocket/{liveRoomId}/{token}")
@Controller
public class WebSocketApiController {
    private static RedisTemplate redisTemplate;

    @Autowired
    public void setRedisTemplate(RedisTemplate redisTemplate) {
        WebSocketApiController.redisTemplate = redisTemplate;
    }

    @OnOpen
    public void onOpen(@PathParam("liveRoomId") Long liveRoomId, @PathParam("token") String token, Session session) {
        log.info("建立连接；liveRoomId：{}，token：{}", liveRoomId, token);
        // 根据token查询用户信息
        UserInfoVo userInfoVo = (UserInfoVo) redisTemplate.opsForValue().get(RedisConstant.USER_LOGIN_KEY_PREFIX + token);
        // 构建用户基本信息
        FromUser fromUser = new FromUser();
        fromUser.setUserId(userInfoVo.getId());
        fromUser.setNickname(userInfoVo.getNickname());
        fromUser.setAvatarUrl(userInfoVo.getAvatarUrl());
        // 添加到容器工具类
        WebSocketLocalContainer.addUserToLiveRoom(liveRoomId, fromUser.getUserId());
        WebSocketLocalContainer.addFromUser(fromUser.getUserId(), fromUser);
        WebSocketLocalContainer.addSession(fromUser.getUserId(), session);

        // 进入直播间提示
        SocketMsg socketMsg = new SocketMsg();
        socketMsg.setLiveRoomId(liveRoomId);
        socketMsg.setMsgType(SocketMsg.MsgTypeEnum.JOIN_CHAT.getCode());
        socketMsg.setMsgContent(fromUser.getNickname() + "加入了直播间！");
        socketMsg.setFromUser(fromUser);
        socketMsg.setTime(new DateTime().toString("HH:mm:ss"));
        // 发送消息
        //WebSocketLocalContainer.sendMsg(socketMsg);
        redisTemplate.convertAndSend("tingshu:live:message", socketMsg);
    }

    @OnClose
    public void onClose(@PathParam("liveRoomId") Long liveRoomId, @PathParam("token") String token, Session session) {
        log.info("关闭连接；liveRoomId：{}，token：{}", liveRoomId, token);
        // 根据token查询用户信息
        UserInfoVo userInfoVo = (UserInfoVo) redisTemplate.opsForValue().get(RedisConstant.USER_LOGIN_KEY_PREFIX + token);
        FromUser fromUser = WebSocketLocalContainer.getFromUser(userInfoVo.getId());

        // 从容器中移除用户
        WebSocketLocalContainer.removeUserFromLiveRoom(liveRoomId, userInfoVo.getId());
        WebSocketLocalContainer.removeFromUser(userInfoVo.getId());
        WebSocketLocalContainer.removeSession(userInfoVo.getId());

        // 退出直播间提示
        SocketMsg socketMsg = new SocketMsg();
        socketMsg.setLiveRoomId(liveRoomId);
        socketMsg.setMsgType(SocketMsg.MsgTypeEnum.CLOSE_SOCKET.getCode());
        socketMsg.setMsgContent(fromUser.getNickname() + "离开了直播间！");
        socketMsg.setFromUser(fromUser);
        socketMsg.setTime(new DateTime().toString("HH:mm:ss"));
        // 发送消息
        //WebSocketLocalContainer.sendMsg(socketMsg);
        redisTemplate.convertAndSend("tingshu:live:message", socketMsg);
    }

    @OnMessage
    public void onMessage(Session session, String msg) {
        log.info("获取了消息。消息内容：{}", msg);
        //WebSocketLocalContainer.sendMsg(JSON.parseObject(msg, SocketMsg.class));
        redisTemplate.convertAndSend("tingshu:live:message", JSON.parseObject(msg, SocketMsg.class));
    }

    @OnError
    public void onError(Session session, Throwable err) {
        log.info("连接出错。错误信息：{}", err.getMessage());
        err.printStackTrace();
    }
}
