package top.javahai.chatroom.mapper;

import org.apache.ibatis.annotations.Param;
import top.javahai.chatroom.entity.FriendRelationship;
import top.javahai.chatroom.entity.User;

import java.util.List;

public interface FriendRelationshipMapper {

    // 基本的CRUD操作
    int deleteByPrimaryKey(Integer id);
    int insert(FriendRelationship record);
    FriendRelationship selectByPrimaryKey(Integer id);
    int updateByPrimaryKey(FriendRelationship record);

    // 获取某用户收到的好友请求列表(status=0)
    List<FriendRelationship> getFriendRequests(Integer userId);

    // 获取指定的好友请求
    FriendRelationship getFriendRequest(@Param("userId") Integer userId, @Param("friendId") Integer friendId);

    // 获取某用户的好友列表(status=1)
    List<User> getFriendList(Integer userId);

    // 检查两个用户之间是否存在好友关系(status=1)
    FriendRelationship checkFriendship(@Param("userId") Integer userId, @Param("friendId") Integer friendId);

    // 删除两个用户之间的好友关系
    int deleteFriendship(@Param("userId") Integer userId, @Param("friendId") Integer friendId);
}