package top.javahai.chatroom.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.javahai.chatroom.dao.GroupDao;
import top.javahai.chatroom.dao.GroupMemberDao;
import top.javahai.chatroom.entity.Group;
import top.javahai.chatroom.service.GroupService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class GroupServiceImpl implements GroupService {

    @Autowired
    private GroupDao groupDao;

    @Autowired
    private GroupMemberDao groupMemberDao;

    @Override
    public List<Group> getGroupsByUserId(Integer userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        return groupDao.getGroupsByUserId(userId);
    }

    @Override
    public Group getGroupById(Integer groupId) {
        if (groupId == null) {
            return null;
        }
        return groupDao.getGroupById(groupId);
    }

    @Override
    @Transactional
    public boolean createGroup(Group group, List<Integer> members) {
        try {
            // 确保数据有效
            if (group == null || group.getName() == null || group.getName().trim().isEmpty() ||
                    group.getCreatorId() == null || members == null || members.isEmpty()) {
                return false;
            }

            // 设置创建时间
            if (group.getCreateTime() == null) {
                group.setCreateTime(new Date());
            }

            // 保存群组信息
            int insertResult = groupDao.insertGroup(group);
            if (insertResult <= 0) {
                return false;
            }

            // 获取生成的群组ID
            Integer groupId = group.getId();
            if (groupId == null) {
                throw new RuntimeException("群组ID生成失败");
            }

            // 添加创建者为管理员
            groupMemberDao.addGroupMember(groupId, group.getCreatorId(), 1); // 1-管理员

            // 添加其他成员
            for (Integer memberId : members) {
                // 跳过重复添加创建者
                if (!memberId.equals(group.getCreatorId())) {
                    groupMemberDao.addGroupMember(groupId, memberId, 0); // 0-普通成员
                }
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("创建群组失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean addMemberToGroup(Integer groupId, Integer userId) {
        try {
            // 检查参数
            if (groupId == null || userId == null) {
                return false;
            }

            // 检查群组是否存在
            Group group = groupDao.getGroupById(groupId);
            if (group == null) {
                return false;
            }

            // 检查用户是否已在群组中
            if (groupMemberDao.isMember(groupId, userId)) {
                return true; // 已是成员，视为添加成功
            }

            // 添加成员
            int result = groupMemberDao.addGroupMember(groupId, userId, 0); // 0-普通成员
            return result > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}