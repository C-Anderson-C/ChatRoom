package top.javahai.chatroom.service;

import top.javahai.chatroom.entity.RespBean;
import top.javahai.chatroom.entity.User;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface FriendService {
    /**
     * 发送好友请求
     */
    RespBean sendFriendRequest(Integer userId, Integer friendId);

    /**
     * 获取好友请求列表
     */
    List<Map<String, Object>> getFriendRequests(Integer userId);

    /**
     * 接受好友请求
     */
    RespBean acceptFriendRequest(Integer requestId);

    /**
     * 拒绝好友请求
     */
    RespBean rejectFriendRequest(Integer requestId);

    /**
     * 获取好友列表
     */
    List<User> getFriendList(Integer userId);

    /**
     * 获取替代好友列表
     * 当常规的好友列表获取方法出现问题时使用
     */
    List<User> getAlternativeFriendList(Integer userId);

    /**
     * 获取好友ID列表
     */
    Set<Integer> findFriendIdsByUserId(Integer userId);

    /**
     * 搜索用户并标记与当前用户的好友状态
     */
    List<Map<String, Object>> searchUsersWithFriendStatus(String keyword, Integer currentUserId);

    Integer getFriendStatus(Integer userId, Integer targetId);

    /**
     * 获取待处理的好友请求数量
     */
    Integer getPendingRequestsCount(Integer userId);
}