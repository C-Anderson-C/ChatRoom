package top.javahai.chatroom.entity;

import java.util.Date;

/**
 * 单聊的消息实体
 * @author Hai
 * @date 2025/6/2 - 19:32
 */
public class Message {
  private String from;
  private String to;
  private String content;
  private String messageId; // 消息ID
  private String id;        // 兼容id字段
  private Integer messageTypeId; // 1代表文字消息，2代表图片消息
  private Date createTime;
  private String fromNickname;
  private String fromProfile;
  private Integer fromId;
  private String direction; // 消息方向：sent或received

  // ID字段的getter/setter
  public String getId() {
    return id != null ? id : messageId; // 优先返回id，如果为null则返回messageId
  }

  public void setId(String id) {
    this.id = id;
    // 同时设置messageId，保持兼容性
    if (this.messageId == null) {
      this.messageId = id;
    }
  }

  public String getMessageId() {
    return messageId != null ? messageId : id; // 优先返回messageId，如果为null则返回id
  }

  public void setMessageId(String messageId) {
    this.messageId = messageId;
    // 同时设置id，保持兼容性
    if (this.id == null) {
      this.id = messageId;
    }
  }

  // 基本字段getter/setter
  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public String getTo() {
    return to;
  }

  public void setTo(String to) {
    this.to = to;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  // 方向字段getter/setter
  public String getDirection() {
    return direction;
  }

  public void setDirection(String direction) {
    this.direction = direction;
  }

  // 时间字段getter/setter
  public Date getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }

  // 用户信息字段getter/setter
  public String getFromNickname() {
    return fromNickname;
  }

  public void setFromNickname(String fromNickname) {
    this.fromNickname = fromNickname;
  }

  public String getFromProfile() {
    return fromProfile;
  }

  public void setFromProfile(String fromProfile) {
    this.fromProfile = fromProfile;
  }

  // 兼容旧代码
  public String getFromUserProfile() {
    return fromProfile;
  }

  public void setFromUserProfile(String fromUserProfile) {
    this.fromProfile = fromUserProfile;
  }

  public Integer getFromId() {
    return fromId;
  }

  public void setFromId(Integer fromId) {
    this.fromId = fromId;
  }

  // 消息类型字段getter/setter
  public Integer getMessageTypeId() {
    return messageTypeId;
  }

  public void setMessageTypeId(Integer messageTypeId) {
    this.messageTypeId = messageTypeId;
  }
}