package top.javahai.chatroom.controller;

import com.github.binarywang.java.emoji.EmojiConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import top.javahai.chatroom.entity.GroupMsgContent;
import top.javahai.chatroom.entity.Message;
import top.javahai.chatroom.entity.User;
import top.javahai.chatroom.service.GroupMsgContentService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@Controller
public class WsController {
  @Autowired
  SimpMessagingTemplate simpMessagingTemplate;

  /**
   * 单聊的消息的接受与转发
   * @param authentication
   * @param message
   */
  @MessageMapping("/chat")  // 修改路径，移除/ws前缀
  public void handleMessage(Authentication authentication, Message message) {
    try {
      User user = ((User) authentication.getPrincipal());

      // 设置消息ID，用于前端去重
      if (message.getMessageId() == null) {
        message.setMessageId(UUID.randomUUID().toString());
      }

      message.setFromNickname(user.getNickname());
      message.setFrom(user.getUsername());
      message.setCreateTime(new Date());

      // 添加方向标识
      Message sentMessage = cloneMessageWithDirection(message, "sent");
      Message receivedMessage = cloneMessageWithDirection(message, "received");

      // 发送给接收人(标记为接收)
      simpMessagingTemplate.convertAndSendToUser(message.getTo(), "/queue/chat", receivedMessage);

      // 发回给发送人自己(标记为发送)，便于多端同步
      simpMessagingTemplate.convertAndSendToUser(message.getFrom(), "/queue/chat", sentMessage);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Autowired
  GroupMsgContentService groupMsgContentService;
  EmojiConverter emojiConverter = EmojiConverter.getInstance();

  SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  /**
   * 群聊的消息接受与转发
   * @param authentication
   * @param groupMsgContent
   */
  @MessageMapping("/groupChat") // 修改路径，移除/ws前缀
  public void handleGroupMessage(Authentication authentication, GroupMsgContent groupMsgContent) {
    try {
      User currentUser = (User) authentication.getPrincipal();

      // 处理emoji内容,转换成unicode编码
      groupMsgContent.setContent(emojiConverter.toHtml(groupMsgContent.getContent()));

      // 保证来源正确性，从Security中获取用户信息
      groupMsgContent.setFromId(currentUser.getId());
      groupMsgContent.setFromName(currentUser.getNickname());
      groupMsgContent.setFromProfile(currentUser.getAvatar());
      groupMsgContent.setCreateTime(new Date());

      // 保存该条群聊消息记录到数据库中
      groupMsgContentService.insert(groupMsgContent);

      // 转发该条数据
      simpMessagingTemplate.convertAndSend("/topic/greetings", groupMsgContent);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * 复制消息并添加方向标记
   * @param original 原始消息
   * @param direction 方向(sent或received)
   * @return 附加了方向标记的新消息对象
   */
  private Message cloneMessageWithDirection(Message original, String direction) {
    Message copy = new Message();
    copy.setMessageId(original.getMessageId());
    copy.setFrom(original.getFrom());
    copy.setTo(original.getTo());
    copy.setContent(original.getContent());
    copy.setCreateTime(original.getCreateTime());
    copy.setFromNickname(original.getFromNickname());
    copy.setFromUserProfile(original.getFromUserProfile());
    copy.setMessageTypeId(original.getMessageTypeId());
    copy.setDirection(direction); // 添加方向标记
    return copy;
  }
}