package top.javahai.chatroom.utils;

import org.springframework.security.core.context.SecurityContextHolder;
import top.javahai.chatroom.entity.User;
import top.javahai.chatroom.security.ChatUserDetails;

public class UserUtil {

  /**
   * 获取当前登录用户
   * @return
   */
  public static User getCurrentUser() {
    Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (principal instanceof ChatUserDetails) {
      return ((ChatUserDetails) principal).getUser();
    }
    return null; // 或者抛出异常，取决于您的业务逻辑
  }
}