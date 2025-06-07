package top.javahai.chatroom.dao;

import org.apache.ibatis.annotations.*;
import top.javahai.chatroom.entity.Group;
import java.util.List;

@Mapper
public interface GroupDao {

    /**
     * 根据用户ID获取用户所在的群聊列表
     */
    @Select("SELECT g.* FROM `group` g " +
            "INNER JOIN group_member gm ON g.id = gm.group_id " +
            "WHERE gm.user_id = #{userId} " +
            "ORDER BY g.create_time DESC")
    List<Group> getGroupsByUserId(Integer userId);

    /**
     * 根据群聊ID获取群聊信息
     */
    @Select("SELECT * FROM `group` WHERE id = #{groupId}")
    Group getGroupById(Integer groupId);

    /**
     * 插入群聊信息
     */
    @Insert("INSERT INTO `group` (name, description, creator_id, create_time) " +
            "VALUES (#{name}, #{description}, #{creatorId}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertGroup(Group group);

    /**
     * 更新群聊信息
     */
    @Update("UPDATE `group` SET name = #{name}, description = #{description} " +
            "WHERE id = #{id}")
    int updateGroup(Group group);

    /**
     * 删除群聊
     */
    @Delete("DELETE FROM `group` WHERE id = #{groupId}")
    int deleteGroup(Integer groupId);
}