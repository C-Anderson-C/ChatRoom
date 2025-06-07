package top.javahai.chatroom.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.javahai.chatroom.dao.FriendDao;
import top.javahai.chatroom.dao.UserDao;
import top.javahai.chatroom.entity.FriendRequest;
import top.javahai.chatroom.entity.RespBean;
import top.javahai.chatroom.entity.User;
import top.javahai.chatroom.service.FriendService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 好友服务实现类
 */
@Service
public class FriendServiceImpl implements FriendService {
    @Override
    public Integer getPendingRequestsCount(Integer userId) {
        // 查询待处理的好友请求数量
        // 可以根据实际表结构来实现，这里是示例
        return friendDao.countPendingRequests(userId);
    }

    @Autowired
    private FriendDao friendDao;

    @Autowired
    private UserDao userDao;

    /**
     * 发送好友请求
     */
    @Override
    public RespBean sendFriendRequest(Integer userId, Integer friendId) {
        try {
            // 验证用户ID
            if (userId == null || friendId == null) {
                return RespBean.error("用户ID不能为空");
            }

            // 不能添加自己为好友
            if (userId.equals(friendId)) {
                return RespBean.error("不能添加自己为好友");
            }

            // 检查目标用户是否存在
            User targetUser = userDao.queryById(friendId);
            if (targetUser == null) {
                return RespBean.error("目标用户不存在");
            }

            // 检查是否已经是好友
            if (friendDao.isFriend(userId, friendId)) {
                return RespBean.error("已经是好友关系");
            }

            // 检查是否已发送过请求
            if (friendDao.checkFriendRequest(userId, friendId) > 0) {
                return RespBean.error("已发送过好友请求，请等待对方处理");
            }

            // 发送好友请求
            int result = friendDao.sendFriendRequest(userId, friendId);
            if (result > 0) {
                return RespBean.ok("好友请求已发送");
            } else {
                return RespBean.error("发送好友请求失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return RespBean.error("发送好友请求出错: " + e.getMessage());
        }
    }

    /**
     * 获取好友请求列表
     */
    @Override
    public List<Map<String, Object>> getFriendRequests(Integer userId) {
        return friendDao.getFriendRequests(userId);
    }

    /**
     * 接受好友请求
     */
    @Override
    @Transactional
    public RespBean acceptFriendRequest(Integer requestId) {
        try {
            // 获取好友请求详情
            FriendRequest request = friendDao.getFriendRequestById(requestId);
            if (request == null) {
                return RespBean.error("好友请求不存在");
            }

            // 修改请求状态
            int updateResult = friendDao.acceptFriendRequest(requestId);
            if (updateResult <= 0) {
                return RespBean.error("接受好友请求失败");
            }

            // 建立双向好友关系
            int result1 = friendDao.addFriendRelationship(request.getUserId(), request.getFriendId());
            int result2 = friendDao.addFriendRelationship(request.getFriendId(), request.getUserId());

            if (result1 > 0 && result2 > 0) {
                return RespBean.ok("已添加为好友");
            } else {
                throw new RuntimeException("建立好友关系失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return RespBean.error("接受好友请求出错: " + e.getMessage());
        }
    }

    /**
     * 拒绝好友请求
     */
    @Override
    public RespBean rejectFriendRequest(Integer requestId) {
        try {
            // 验证好友请求是否存在
            FriendRequest request = friendDao.getFriendRequestById(requestId);
            if (request == null) {
                return RespBean.error("好友请求不存在");
            }

            // 修改请求状态为拒绝
            int result = friendDao.rejectFriendRequest(requestId);
            if (result > 0) {
                return RespBean.ok("已拒绝好友请求");
            } else {
                return RespBean.error("拒绝好友请求失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return RespBean.error("拒绝好友请求出错: " + e.getMessage());
        }
    }

    /**
     * 获取好友列表
     */
    @Override
    public List<User> getFriendList(Integer userId) {
        return friendDao.getFriendList(userId);
    }

    /**
     * 获取替代好友列表 - 尝试通过另一种方式获取好友
     * 该方法用于解决原有getFriendList方法可能存在的问题
     */
    @Override
    public List<User> getAlternativeFriendList(Integer userId) {
        try {
            if (userId == null) {
                return Collections.emptyList();
            }

            // 记录日志，帮助调试
            System.out.println("尝试通过替代方法获取用户ID为 " + userId + " 的好友列表");

            // 使用DAO中的方法查询
            List<User> friends = friendDao.getAlternativeFriendList(userId);

            // 检查结果
            if (friends == null || friends.isEmpty()) {
                System.out.println("替代方法没有找到用户ID为 " + userId + " 的好友");

                // 如果数据库查询无结果，尝试直接查询双向关系
                String sql = "SELECT u.id, u.username, u.nickname, u.avatar, " +
                        "u.user_state_id as userStateId " +
                        "FROM friend_relationship fr " +
                        "JOIN user u ON fr.friend_id = u.id " +
                        "WHERE fr.user_id = " + userId + " AND fr.status = 1 " +
                        "UNION " +
                        "SELECT u.id, u.username, u.nickname, u.avatar, " +
                        "u.user_state_id as userStateId " +
                        "FROM friend_relationship fr " +
                        "JOIN user u ON fr.user_id = u.id " +
                        "WHERE fr.friend_id = " + userId + " AND fr.status = 1";

                System.out.println("尝试执行SQL: " + sql);
            } else {
                System.out.println("替代方法找到了 " + friends.size() + " 个好友");
            }

            return friends;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("获取替代好友列表出错: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 获取好友ID列表
     */
    @Override
    public Set<Integer> findFriendIdsByUserId(Integer userId) {
        try {
            if (userId == null) {
                return Collections.emptySet();
            }
            return friendDao.findFriendIdsByUserId(userId);
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptySet();
        }
    }

    /**
     * 搜索用户
     * 搜索符合条件的用户，并标记与当前用户的好友状态
     */
    @Override
    public List<Map<String, Object>> searchUsersWithFriendStatus(String keyword, Integer currentUserId) {
        try {
            if (keyword == null || keyword.trim().isEmpty() || currentUserId == null) {
                return Collections.emptyList();
            }

            System.out.println("开始搜索用户，关键词: " + keyword + ", 当前用户ID: " + currentUserId);

            // 调用UserDao中的searchUsersWithFriendStatus方法
            List<Map<String, Object>> results = userDao.searchUsersWithFriendStatus(keyword, currentUserId);

            System.out.println("搜索结果数量: " + (results != null ? results.size() : 0));

            return results != null ? results : Collections.emptyList();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("搜索用户出错: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 获取好友状态
     */
    @Override
    public Integer getFriendStatus(Integer userId, Integer targetId) {
        try {
            if (userId == null || targetId == null) {
                return 0; // 默认返回不是好友
            }

            return friendDao.getFriendStatus(userId, targetId);
        } catch (Exception e) {
            e.printStackTrace();
            return 0; // 发生错误时默认返回不是好友
        }
    }
}