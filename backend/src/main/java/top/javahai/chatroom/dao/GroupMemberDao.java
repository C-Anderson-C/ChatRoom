package top.javahai.chatroom.dao;

import org.apache.ibatis.annotations.*;

@Mapper
public interface GroupMemberDao {

    /**
     * 添加群成员
     * @param groupId 群ID
     * @param userId 用户ID
     * @param role 角色 (0-普通成员, 1-管理员)
     * @return 影响的行数
     */
    @Insert("INSERT INTO group_member (group_id, user_id, role, join_time) " +
            "VALUES (#{groupId}, #{userId}, #{role}, NOW())")
    int addGroupMember(@Param("groupId") Integer groupId,
                       @Param("userId") Integer userId,
                       @Param("role") Integer role);

    /**
     * 检查用户是否是群成员
     */
    @Select("SELECT COUNT(*) FROM group_member " +
            "WHERE group_id = #{groupId} AND user_id = #{userId}")
    boolean isMember(@Param("groupId") Integer groupId, @Param("userId") Integer userId);

    /**
     * 移除群成员
     */
    @Delete("DELETE FROM group_member " +
            "WHERE group_id = #{groupId} AND user_id = #{userId}")
    int removeGroupMember(@Param("groupId") Integer groupId, @Param("userId") Integer userId);

    /**
     * 获取群成员数量
     */
    @Select("SELECT COUNT(*) FROM group_member WHERE group_id = #{groupId}")
    int getGroupMemberCount(Integer groupId);

    /**
     * 更新成员角色
     */
    @Update("UPDATE group_member SET role = #{role} " +
            "WHERE group_id = #{groupId} AND user_id = #{userId}")
    int updateMemberRole(@Param("groupId") Integer groupId,
                         @Param("userId") Integer userId,
                         @Param("role") Integer role);
}