package top.javahai.chatroom.entity;  // 调整为你的实际包名

import java.util.Date;

public class FriendRequest {
    private Integer id;
    private Integer userId;
    private Integer friendId;
    private Integer status;   // 0-待处理, 1-已接受, 2-已拒绝
    private Date createTime;
    private Date updateTime;

    // 无参构造函数
    public FriendRequest() {
    }

    // 带参构造函数
    public FriendRequest(Integer userId, Integer friendId) {
        this.userId = userId;
        this.friendId = friendId;
        this.status = 0; // 默认待处理
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getFriendId() {
        return friendId;
    }

    public void setFriendId(Integer friendId) {
        this.friendId = friendId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "FriendRequest{" +
                "id=" + id +
                ", userId=" + userId +
                ", friendId=" + friendId +
                ", status=" + status +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}