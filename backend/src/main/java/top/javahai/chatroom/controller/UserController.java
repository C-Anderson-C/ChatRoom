package top.javahai.chatroom.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import top.javahai.chatroom.entity.RespBean;
import top.javahai.chatroom.entity.RespPageBean;
import top.javahai.chatroom.entity.User;
import top.javahai.chatroom.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * (User)表控制层
 *
 * @author makejava
 * @since 2020-06-16 11:37:09
 */
@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 搜索用户
     * 如果提供了currentUserId，则返回包含好友状态的结果
     * 否则返回基本用户信息
     */
    /**
     * 搜索用户
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "currentUserId", required = false) Integer currentUserId) {
        try {
            System.out.println("搜索用户请求参数 - keyword: " + keyword + ", currentUserId: " + currentUserId);

            if (keyword == null || keyword.trim().isEmpty()) {
                return ResponseEntity.ok(Collections.emptyList());
            }

            if (currentUserId != null) {
                // 带好友状态的搜索
                List<Map<String, Object>> results = userService.searchUsersWithFriendStatus(keyword, currentUserId);
                System.out.println("搜索结果数量: " + (results != null ? results.size() : 0));
                return ResponseEntity.ok(results);
            } else {
                // 基本搜索
                List<User> users = userService.searchUserByKeyword(keyword);
                System.out.println("搜索结果数量: " + (users != null ? users.size() : 0));
                return ResponseEntity.ok(users);
            }
        } catch (Exception e) {
            e.printStackTrace(); // 打印完整堆栈
            System.err.println("搜索用户时发生错误: " + e.getMessage() + ", 原因: " + e.getCause());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(RespBean.error("搜索失败: " + e.getMessage()));
        }
    }

    /**
     * 注册操作
     */
    @PostMapping("/register")
    public RespBean addUser(@RequestBody User user) {
        if (userService.insert(user) == 1) {
            return RespBean.ok("注册成功！");
        } else {
            return RespBean.error("注册失败！");
        }
    }

    /**
     * 注册操作，检查用户名是否已被注册
     */
    @GetMapping("/checkUsername")
    public Integer checkUsername(@RequestParam("username") String username) {
        return userService.checkUsername(username);
    }

    /**
     * 注册操作，检查昵称是否已被注册
     */
    @GetMapping("/checkNickname")
    public Integer checkNickname(@RequestParam("nickname") String nickname) {
        return userService.checkNickname(nickname);
    }

    /**
     * 通过主键查询单条数据
     *
     * @param id 主键
     * @return 单条数据
     */
    @GetMapping("/selectOne")
    public User selectOne(@RequestParam("id") Integer id) {
        return this.userService.queryById(id);
    }

    /**
     * 分页获取用户列表
     * @author luo
     * @param page  页数，对应数据库查询的起始行数
     * @param size  数据量，对应数据库查询的偏移量
     * @param keyword 关键词，用于搜索
     * @param isLocked  是否锁定，用于搜索
     * @param status 状态，用于搜索
     * @return 分页数据
     */
    @GetMapping("/")
    public RespPageBean getAllUserByPage(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer isLocked,
            @RequestParam(required = false) Integer status) {
        return userService.getAllUserByPage(page, size, keyword, isLocked, status);
    }

    /**
     * 更新用户的锁定状态
     * @author luo
     */
    @PutMapping("/")
    public RespBean changeLockedStatus(
            @RequestParam("id") Integer id,
            @RequestParam("isLocked") Boolean isLocked) {
        if (userService.changeLockedStatus(id, isLocked) == 1) {
            return RespBean.ok("更新成功！");
        } else {
            return RespBean.error("更新失败！");
        }
    }

    /**
     * 删除单一用户
     * @author luo
     */
    @DeleteMapping("/{id}")
    public RespBean deleteUser(@PathVariable Integer id) {
        if (userService.deleteById(id)) {
            return RespBean.ok("删除成功！");
        } else {
            return RespBean.error("删除失败！");
        }
    }

    /**
     * 批量删除用户
     * @author luo
     */
    @DeleteMapping("/")
    public RespBean deleteUserByIds(@RequestParam("ids") Integer[] ids) {
        if (userService.deleteByIds(ids) == ids.length) {
            return RespBean.ok("删除成功！");
        } else {
            return RespBean.error("删除失败！");
        }
    }
}