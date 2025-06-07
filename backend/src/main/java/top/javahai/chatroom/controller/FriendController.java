package top.javahai.chatroom.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.javahai.chatroom.entity.RespBean;
import top.javahai.chatroom.entity.User;
import top.javahai.chatroom.service.FriendService;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 好友相关控制器
 */
@RestController
@RequestMapping("/friend")
public class FriendController {

    @Autowired
    private FriendService friendService;

    /**
     * 发送好友请求
     */
    @PostMapping("/sendRequest")
    public RespBean sendFriendRequest(@RequestBody Map<String, Integer> params) {
        if (!params.containsKey("userId") || !params.containsKey("friendId")) {
            return RespBean.error("参数错误：需要userId和friendId");
        }

        Integer userId = params.get("userId");
        Integer friendId = params.get("friendId");

        if (userId == null || friendId == null) {
            return RespBean.error("用户ID不能为空");
        }

        return friendService.sendFriendRequest(userId, friendId);
    }

    /**
     * 获取好友请求列表
     */
    @GetMapping("/requests")
    public List<Map<String, Object>> getFriendRequests(@RequestParam("userId") Integer userId) {
        try {
            if (userId == null) {
                return Collections.emptyList();
            }
            return friendService.getFriendRequests(userId);
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     * 响应好友请求
     */
    @PostMapping("/respond")
    public RespBean respondToFriendRequest(
            @RequestParam("requestId") Integer requestId,
            @RequestParam("response") Integer response) {
        try {
            if (requestId == null) {
                return RespBean.error("请求ID不能为空");
            }

            if (response == 1) {
                return friendService.acceptFriendRequest(requestId);
            } else {
                return friendService.rejectFriendRequest(requestId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return RespBean.error("处理好友请求失败: " + e.getMessage());
        }
    }

    /**
     * 获取好友列表
     */
    @GetMapping("/list")
    public List<User> getFriendList(@RequestParam("userId") Integer userId) {
        try {
            if (userId == null) {
                return Collections.emptyList();
            }

            // 记录一条日志，方便调试
            System.out.println("获取用户ID为 " + userId + " 的好友列表");

            return friendService.getFriendList(userId);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("获取好友列表出错: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 获取用户的好友列表(替代方法)
     */
    @GetMapping("/listAlternative")
    public List<User> getAlternativeFriendList(@RequestParam("userId") Integer userId) {
        try {
            if (userId == null) {
                return Collections.emptyList();
            }

            // 记录一条日志，方便调试
            System.out.println("获取用户ID为 " + userId + " 的替代好友列表");

            // 查询所有与该用户有关联的好友关系(双向)
            return friendService.getAlternativeFriendList(userId);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("获取替代好友列表出错: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 获取待处理的好友请求数量
     */
    @GetMapping("/pendingRequestsCount")
    public ResponseEntity<?> getPendingRequestsCount(@RequestParam("userId") Integer userId) {
        try {
            if (userId == null) {
                return ResponseEntity.badRequest().body(0);
            }

            Integer count = friendService.getPendingRequestsCount(userId);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(0); // 出错时返回0而不是错误，避免前端崩溃
        }
    }

    /**
     * 搜索用户并获取与当前用户的好友状态
     * 返回的Map中包含用户信息和好友状态
     */
    @GetMapping("/search")
    public List<Map<String, Object>> searchUsersWithFriendStatus(
            @RequestParam("keyword") String keyword,
            @RequestParam("userId") Integer userId) {
        try {
            if (keyword == null || keyword.trim().isEmpty() || userId == null) {
                return Collections.emptyList();
            }

            System.out.println("搜索关键词: " + keyword + ", 用户ID: " + userId);

            return friendService.searchUsersWithFriendStatus(keyword, userId);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("搜索用户出错: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 检查两个用户之间的好友状态
     * 返回状态码：0-不是好友，1-已是好友，2-已发送请求，3-已收到请求
     */
    /**
     * 检查两个用户之间的好友状态
     * 返回状态码：0-不是好友，1-已是好友，2-已发送请求，3-已收到请求
     */
    @GetMapping("/checkStatus")
    public Integer checkFriendStatus(
            @RequestParam("userId") Integer userId,
            @RequestParam("targetId") Integer targetId) {
        try {
            if (userId == null || targetId == null) {
                return 0; // 默认返回不是好友
            }

            Integer status = friendService.getFriendStatus(userId, targetId);
            return status != null ? status : 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0; // 发生错误时默认返回不是好友
        }
    }
}