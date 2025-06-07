package top.javahai.chatroom.dao;

import org.apache.ibatis.annotations.*;
import top.javahai.chatroom.entity.FriendRequest;
import top.javahai.chatroom.entity.User;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 好友相关数据库操作接口
 */
@Mapper
public interface FriendDao {

    /**
     * 查询待处理的好友请求数量
     * @param userId 用户ID
     * @return 待处理请求数量
     */
    Integer countPendingRequests(Integer userId);

    /**
     * 获取好友状态
     * @param userId 当前用户ID
     * @param targetId 目标用户ID
     * @return 0: 不是好友, 1: 已是好友, 2: 已发送请求, 3: 已收到请求
     */
    @Select("SELECT " +
            "CASE " +
            "   WHEN EXISTS(SELECT 1 FROM friend_relationship WHERE ((user_id = #{userId} AND friend_id = #{targetId}) OR (user_id = #{targetId} AND friend_id = #{userId})) AND status = 1) THEN 1 " +
            "   WHEN EXISTS(SELECT 1 FROM friend_request WHERE user_id = #{userId} AND friend_id = #{targetId} AND status = 0) THEN 2 " +
            "   WHEN EXISTS(SELECT 1 FROM friend_request WHERE user_id = #{targetId} AND friend_id = #{userId} AND status = 0) THEN 3 " +
            "   ELSE 0 " +
            "END")
    Integer getFriendStatus(@Param("userId") Integer userId, @Param("targetId") Integer targetId);
    /**
     * 插入好友请求
     */
    @Insert("INSERT INTO friend_request (user_id, friend_id, status, create_time) " +
            "VALUES (#{userId}, #{friendId}, 0, NOW())")
    int sendFriendRequest(@Param("userId") Integer userId, @Param("friendId") Integer friendId);

    /**
     * 接受好友请求
     */
    @Update("UPDATE friend_request SET status = 1, update_time = NOW() WHERE id = #{requestId}")
    int acceptFriendRequest(@Param("requestId") Integer requestId);

    /**
     * 拒绝好友请求
     */
    @Update("UPDATE friend_request SET status = 2, update_time = NOW() WHERE id = #{requestId}")
    int rejectFriendRequest(@Param("requestId") Integer requestId);

    /**
     * 根据ID获取好友请求
     */
    @Select("SELECT * FROM friend_request WHERE id = #{requestId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "friendId", column = "friend_id"),
            @Result(property = "status", column = "status"),
            @Result(property = "createTime", column = "create_time"),
            @Result(property = "updateTime", column = "update_time")
    })
    FriendRequest getFriendRequestById(@Param("requestId") Integer requestId);

    /**
     * 获取好友请求列表
     */
    @Select("SELECT fr.id, fr.user_id as userId, fr.friend_id as friendId, " +
            "fr.status, fr.create_time as createTime, " +
            "u.username as friendUsername, u.nickname as friendNickname " +
            "FROM friend_request fr " +
            "JOIN user u ON fr.user_id = u.id " +
            "WHERE fr.friend_id = #{userId} AND fr.status = 0")
    List<Map<String, Object>> getFriendRequests(@Param("userId") Integer userId);

    /**
     * 获取好友列表
     */
    @Select("SELECT u.id, u.username, u.nickname, u.avatar, " +
            "u.user_state_id as userStateId " +
            "FROM friend_relationship fr " +
            "JOIN user u ON fr.friend_id = u.id " +
            "WHERE fr.user_id = #{userId} AND fr.status = 1 " +
            "UNION " +
            "SELECT u.id, u.username, u.nickname, u.avatar, " +
            "u.user_state_id as userStateId " +
            "FROM friend_relationship fr " +
            "JOIN user u ON fr.user_id = u.id " +
            "WHERE fr.friend_id = #{userId} AND fr.status = 1")
    List<User> getFriendList(@Param("userId") Integer userId);

    /**
     * 获取替代好友列表
     */
    @Select("SELECT u.id, u.username, u.nickname, u.avatar, " +
            "u.user_state_id as userStateId " +
            "FROM friend_relationship fr " +
            "JOIN user u ON fr.friend_id = u.id " +
            "WHERE fr.user_id = #{userId} AND fr.status = 1 " +
            "UNION " +
            "SELECT u.id, u.username, u.nickname, u.avatar, " +
            "u.user_state_id as userStateId " +
            "FROM friend_relationship fr " +
            "JOIN user u ON fr.user_id = u.id " +
            "WHERE fr.friend_id = #{userId} AND fr.status = 1")
    List<User> getAlternativeFriendList(@Param("userId") Integer userId);

    /**
     * 添加好友关系
     */
    @Insert("INSERT INTO friend_relationship (user_id, friend_id, status, create_time) " +
            "VALUES (#{userId}, #{friendId}, 1, NOW())")
    int addFriendRelationship(@Param("userId") Integer userId, @Param("friendId") Integer friendId);

    /**
     * 检查好友关系是否存在
     */
    @Select("SELECT COUNT(*) FROM friend_relationship " +
            "WHERE (user_id = #{userId} AND friend_id = #{friendId}) AND status = 1")
    Integer checkFriendRelationship(@Param("userId") Integer userId, @Param("friendId") Integer friendId);

    /**
     * 检查是否已经是好友(布尔值返回)
     */
    @Select("SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END FROM friend_relationship " +
            "WHERE (user_id = #{userId} AND friend_id = #{friendId}) AND status = 1")
    boolean isFriend(@Param("userId") Integer userId, @Param("friendId") Integer friendId);

    /**
     * 检查好友请求是否已存在
     */
    @Select("SELECT COUNT(*) FROM friend_request " +
            "WHERE user_id = #{userId} AND friend_id = #{friendId} AND status = 0")
    Integer checkFriendRequest(@Param("userId") Integer userId, @Param("friendId") Integer friendId);

    /**
     * 获取好友ID列表
     */
    @Select("SELECT friend_id FROM friend_relationship WHERE user_id = #{userId} AND status = 1")
    Set<Integer> findFriendIdsByUserId(@Param("userId") Integer userId);





}