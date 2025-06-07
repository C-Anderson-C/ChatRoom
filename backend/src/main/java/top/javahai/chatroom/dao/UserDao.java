package top.javahai.chatroom.dao;

import org.apache.ibatis.annotations.*;
import top.javahai.chatroom.entity.User;
import java.util.List;
import java.util.Map;

@Mapper
public interface UserDao {
    /**
     * 根据关键词搜索用户，并排除当前用户自己
     * @param keyword 搜索关键词
     * @param currentUserId 当前用户ID
     * @return 用户列表
     */
    List<User> searchUsersByKeyword(@Param("keyword") String keyword, @Param("currentUserId") Integer currentUserId);

    /**
     * 根据关键词搜索用户
     * @param keyword 搜索关键词
     * @return 用户列表
     */
    @Select("SELECT * FROM user WHERE nickname LIKE CONCAT('%', #{keyword}, '%') OR username LIKE CONCAT('%', #{keyword}, '%') LIMIT 20")
    List<User> searchUsers(String keyword);

    /**
     * 根据用户名加载用户
     * @param username 用户名
     * @return 用户对象
     */
    @Select("select * from user where username=#{username}")
    User loadUserByUsername(String username);

    /**
     * 获取除当前用户外的所有用户
     * @param id 当前用户ID
     * @return 用户列表
     */
    @Select("select id, username, nickname, avatar, email, create_time, update_time, status from user where id!=#{id}")
    List<User> getUsersWithoutCurrentUser(Integer id);

    /**
     * 设置用户状态为在线
     * @param id 用户ID
     */
    @Update("update user set user_state_id = 1 where id = #{id}")
    void setUserStateToOn(Integer id);

    /**
     * 设置用户状态为离线
     * @param id 用户ID
     */
    @Update("update user set user_state_id = 2 where id = #{id}")
    void setUserStateToLeave(Integer id);

    /**
     * 根据ID查询用户
     * @param id 用户ID
     * @return 用户对象
     */
    @Select("select id, username, nickname, password, avatar, email, create_time, update_time, status from user where id = #{id}")
    User queryById(Integer id);

    /**
     * 分页查询用户
     * @param offset 偏移量
     * @param limit 限制数
     * @return 用户列表
     */
    @Select("select id, username, nickname, password, avatar, email, create_time, update_time, status from user limit #{offset}, #{limit}")
    List<User> queryAllByLimit(int offset, int limit);

    /**
     * 插入用户
     * @param user 用户对象
     * @return 影响行数
     */
    Integer insert(User user);

    /**
     * 更新用户
     * @param user 用户对象
     * @return 影响行数
     */
    Integer update(User user);

    /**
     * 删除用户
     * @param id 用户ID
     * @return 影响行数
     */
    Integer deleteById(Integer id);

    /**
     * 检查用户名是否存在
     * @param username 用户名
     * @return 计数
     */
    @Select("select count(*) from user where username=#{username}")
    Integer checkUsername(String username);

    /**
     * 检查昵称是否存在
     * @param nickname 昵称
     * @return 计数
     */
    @Select("select count(*) from user where nickname=#{nickname}")
    Integer checkNickname(String nickname);

    /**
     * 分页获取用户列表
     * @param page 页数
     * @param size 每页大小
     * @param keyword 关键词
     * @param isLocked 是否锁定
     * @param status 状态
     * @return 用户列表
     */
    List<User> getAllUserByPage(@Param("page") Integer page, @Param("size") Integer size,
                                @Param("keyword") String keyword, @Param("isLocked") Integer isLocked,
                                @Param("status") Integer status);

    /**
     * 获取用户总数
     * @param keyword 关键词
     * @param isLocked 是否锁定
     * @param status 状态
     * @return 总数
     */
    Long getTotal(@Param("keyword") String keyword, @Param("isLocked") Integer isLocked,
                  @Param("status") Integer status);

    /**
     * 更改用户锁定状态
     * @param id 用户ID
     * @param isLocked 是否锁定
     * @return 影响行数
     */
    @Update("update user set locked = #{isLocked} where id = #{id}")
    Integer changeLockedStatus(@Param("id") Integer id, @Param("isLocked") Boolean isLocked);

    /**
     * 批量删除用户
     * @param ids 用户ID数组
     * @return 影响行数
     */
    Integer deleteByIds(@Param("ids") Integer[] ids);

    /**
     * 根据关键词搜索用户，并排除当前用户自己，同时标记好友状态
     * @param keyword 搜索关键词
     * @param currentUserId 当前用户ID
     * @return 用户列表，包含好友状态信息
     */
    @Select("SELECT u.id, u.username, u.nickname, u.avatar, " +
            "CASE " +
            "   WHEN EXISTS(SELECT 1 FROM friend_relationship WHERE ((user_id = #{currentUserId} AND friend_id = u.id) OR (user_id = u.id AND friend_id = #{currentUserId})) AND status = 1) THEN 1 " +
            "   WHEN EXISTS(SELECT 1 FROM friend_request WHERE user_id = #{currentUserId} AND friend_id = u.id AND status = 0) THEN 2 " +
            "   WHEN EXISTS(SELECT 1 FROM friend_request WHERE user_id = u.id AND friend_id = #{currentUserId} AND status = 0) THEN 3 " +
            "   ELSE 0 " +
            "END as friendStatus " +
            "FROM user u " +
            "WHERE u.id != #{currentUserId} AND (u.username LIKE CONCAT('%', #{keyword}, '%') OR u.nickname LIKE CONCAT('%', #{keyword}, '%')) " +
            "LIMIT 20")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "username", column = "username"),
            @Result(property = "nickname", column = "nickname"),
            @Result(property = "avatar", column = "avatar"),
            @Result(property = "friendStatus", column = "friendStatus")
    })
    List<Map<String, Object>> searchUsersWithFriendStatus(@Param("keyword") String keyword, @Param("currentUserId") Integer currentUserId);

}