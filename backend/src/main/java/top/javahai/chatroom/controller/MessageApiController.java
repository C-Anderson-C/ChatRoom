package top.javahai.chatroom.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.javahai.chatroom.entity.Message;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class MessageApiController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @PostMapping("/messages")
    public Map<String, Object> sendMessage(@RequestBody Message message) {
        try {
            System.out.println("通过HTTP API发送消息: " + message);

            // 设置必要字段
            if (message.getCreateTime() == null) {
                message.setCreateTime(new Date());
            }

            // 处理私聊消息
            if (message.getTo() != null && !message.getFrom().equals(message.getTo())) {
                // 发送给接收者
                messagingTemplate.convertAndSendToUser(
                        message.getTo(),
                        "/queue/chat",
                        message
                );

                // 发送给发送者
                messagingTemplate.convertAndSendToUser(
                        message.getFrom(),
                        "/queue/chat",
                        message
                );

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("message", "消息已发送");
                result.put("timestamp", new Date());
                return result;
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "接收者无效");
            result.put("timestamp", new Date());
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "发送消息失败: " + e.getMessage());
            result.put("timestamp", new Date());
            return result;
        }
    }
}