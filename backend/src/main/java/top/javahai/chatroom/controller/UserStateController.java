package top.javahai.chatroom.controller;

import org.springframework.http.ResponseEntity;
import top.javahai.chatroom.entity.UserState;
import top.javahai.chatroom.service.UserStateService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * (UserState)表控制层
 */
@RestController
@RequestMapping("/user-state")
public class UserStateController {
    /**
     * 服务对象
     */
    @Resource
    private UserStateService userStateService;

    /**
     * 通过主键查询单条数据
     */
    @GetMapping("/{id}")
    public UserState selectOne(@PathVariable Integer id) {
        return this.userStateService.queryById(id);
    }

    /**
     * 获取用户状态
     */
    @GetMapping("")
    public ResponseEntity<?> getUserState(@RequestParam Integer userId) {
        try {
            UserState userState = this.userStateService.queryById(userId);
            return ResponseEntity.ok(userState);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("获取用户状态失败");
        }
    }

    /**
     * 更新用户状态
     */
    @PostMapping("")
    public ResponseEntity<?> updateUserState(@RequestBody UserState userState) {
        try {
            UserState updated = this.userStateService.update(userState);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("更新用户状态失败");
        }
    }
}