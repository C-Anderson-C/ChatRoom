package top.javahai.chatroom.service;

import top.javahai.chatroom.entity.Group;
import top.javahai.chatroom.entity.RespBean;
import java.util.List;

public interface GroupService {
    /**
     * 根据用户ID获取群聊列表
     * @param userId 用户ID
     * @return 群聊列表
     */
    List<Group> getGroupsByUserId(Integer userId);

    /**
     * 根据群聊ID获取群聊信息
     * @param groupId 群聊ID
     * @return 群聊信息
     */
    Group getGroupById(Integer groupId);

    /**
     * 创建群聊
     * @param group 群聊信息
     * @param members 成员ID列表
     * @return 是否创建成功
     */
    boolean createGroup(Group group, List<Integer> members);

    /**
     * 添加成员到群聊
     * @param groupId 群聊ID
     * @param userId 用户ID
     * @return 是否添加成功
     */
    boolean addMemberToGroup(Integer groupId, Integer userId);
}