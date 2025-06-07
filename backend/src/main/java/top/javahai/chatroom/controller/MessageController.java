package top.javahai.chatroom.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.javahai.chatroom.entity.Message;
import top.javahai.chatroom.entity.RespBean;

import java.util.Date;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @PostMapping("/send")
    public RespBean sendMessage(@RequestBody Message message) {
        try {
            System.out.println("通过HTTP API发送消息: " + message);

            // 确保消息有创建时间
            if (message.getCreateTime() == null) {
                message.setCreateTime(new Date());
            }

            // 确保消息有ID
            if (message.getMessageId() == null) {
                message.setMessageId("api-" + System.currentTimeMillis());
            }

            // 根据消息类型发送
            if (message.getTo() != null && message.getTo().startsWith("group_")) {
                // 群聊消息
                simpMessagingTemplate.convertAndSend("/topic/greetings", message);
            } else {
                // 私聊消息
                simpMessagingTemplate.convertAndSendToUser(
                        message.getTo(),
                        "/queue/chat",
                        message
                );

                // 同时发送副本给发送者
                simpMessagingTemplate.convertAndSendToUser(
                        message.getFrom(),
                        "/queue/chat",
                        message
                );
            }

            return RespBean.ok("消息已发送");
        } catch (Exception e) {
            e.printStackTrace();
            return RespBean.error("发送消息失败: " + e.getMessage());
        }
    }
}