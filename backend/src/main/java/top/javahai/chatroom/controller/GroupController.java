package top.javahai.chatroom.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import top.javahai.chatroom.entity.RespBean;
import top.javahai.chatroom.entity.Group;
import top.javahai.chatroom.service.GroupService;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping("/group")
public class GroupController {

    @Autowired
    private GroupService groupService;

    /**
     * 创建群聊
     */
    @PostMapping("/create")
    public RespBean createGroup(@RequestBody Map<String, Object> params) {
        try {
            String name = (String) params.get("name");
            String description = (String) params.get("description");
            Integer creatorId = (Integer) params.get("creatorId");

            // 注意：前端传递的可能是ArrayList<Integer>或List<LinkedHashMap>
            // 需要确保正确解析成员ID列表
            List<Integer> members = new ArrayList<>();
            Object membersObj = params.get("members");
            if (membersObj instanceof List) {
                // 如果是直接的数字列表
                List<?> membersList = (List<?>) membersObj;
                for (Object item : membersList) {
                    if (item instanceof Integer) {
                        members.add((Integer) item);
                    } else if (item instanceof Number) {
                        members.add(((Number) item).intValue());
                    } else if (item instanceof Map) {
                        // 如果是对象列表，提取ID属性
                        Object id = ((Map<?,?>) item).get("id");
                        if (id instanceof Integer) {
                            members.add((Integer) id);
                        } else if (id instanceof Number) {
                            members.add(((Number) id).intValue());
                        }
                    }
                }
            }

            // 参数验证和日志
            System.out.println("创建群聊：名称=" + name + ", 描述=" + description +
                    ", 创建者ID=" + creatorId + ", 成员=" + members);

            if (name == null || name.trim().isEmpty()) {
                return RespBean.error("群名称不能为空");
            }

            if (creatorId == null) {
                return RespBean.error("创建者ID不能为空");
            }

            if (members.isEmpty()) {
                return RespBean.error("群成员不能为空");
            }

            // 创建群组
            Group group = new Group();
            group.setName(name);
            group.setDescription(description);
            group.setCreatorId(creatorId);
            group.setCreateTime(new Date());

            boolean result = groupService.createGroup(group, members);

            if (result) {
                return RespBean.ok("创建成功", group);
            } else {
                return RespBean.error("创建失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return RespBean.error("创建群聊失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户的群聊列表
     */
    @GetMapping("/list")
    public List<Group> getGroupList(@RequestParam Integer userId) {
        try {
            System.out.println("获取用户 " + userId + " 的群聊列表");
            List<Group> groups = groupService.getGroupsByUserId(userId);

            // 去除可能的重复项
            Set<Integer> groupIds = new HashSet<>();
            List<Group> uniqueGroups = new ArrayList<>();

            for (Group group : groups) {
                if (!groupIds.contains(group.getId())) {
                    groupIds.add(group.getId());
                    uniqueGroups.add(group);
                } else {
                    System.out.println("发现重复群聊ID: " + group.getId() + ", 名称: " + group.getName());
                }
            }

            System.out.println("原始群聊列表大小: " + groups.size() + ", 去重后大小: " + uniqueGroups.size());
            return uniqueGroups;
        } catch (Exception e) {
            System.err.println("获取用户群聊列表失败: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     * 获取群聊详情
     */
    @GetMapping("/{groupId}")
    public Group getGroupById(@PathVariable Integer groupId) {
        try {
            return groupService.getGroupById(groupId);
        } catch (Exception e) {
            System.err.println("获取群聊详情失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 加入群组
     */
    @PostMapping("/join")
    public RespBean joinGroup(@RequestBody Map<String, Integer> params) {
        try {
            Integer userId = params.get("userId");
            Integer groupId = params.get("groupId");

            if (userId == null || groupId == null) {
                return RespBean.error("参数错误：需要用户ID和群组ID");
            }

            boolean result = groupService.addMemberToGroup(groupId, userId);
            if (result) {
                return RespBean.ok("已成功加入群组");
            } else {
                return RespBean.error("加入群组失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return RespBean.error("加入群组失败: " + e.getMessage());
        }
    }
}