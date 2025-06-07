package top.javahai.chatroom.entity;

import java.io.Serializable;
import java.util.Date;

public class User implements Serializable {
    private Integer id;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private String email;
    private Date createTime;
    private Date updateTime;
    private Integer status;
    private Integer userStateId;
    private Boolean enabled;
    private Boolean locked;
    private Boolean expired;
    private Boolean credentialsExpired;

    private static final long serialVersionUID = -6862407809438763680L; // 与错误日志中的一致

    public String getUserProfile() {
        return this.avatar;  // 假设字段名为 avatar
    }

    // Getters and setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getUserStateId() {
        return userStateId;
    }

    public void setUserStateId(Integer userStateId) {
        this.userStateId = userStateId;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getLocked() {
        return locked;
    }

    public void setLocked(Boolean locked) {
        this.locked = locked;
    }

    public Boolean getExpired() {
        return expired;
    }

    public void setExpired(Boolean expired) {
        this.expired = expired;
    }

    public Boolean getCredentialsExpired() {
        return credentialsExpired;
    }

    public void setCredentialsExpired(Boolean credentialsExpired) {
        this.credentialsExpired = credentialsExpired;
    }

    // 兼容Spring Security的方法
    public boolean isEnabled() {
        return enabled != null ? enabled : true;
    }

    public boolean isLocked() {
        return locked != null ? locked : false;
    }

    public boolean isExpired() {
        return expired != null ? expired : false;
    }

    public boolean isCredentialsExpired() {
        return credentialsExpired != null ? credentialsExpired : false;
    }

    // setEnabled(boolean)和setLocked(boolean)已在上面定义

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", nickname='" + nickname + '\'' +
                ", avatar='" + avatar + '\'' +
                ", email='" + email + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                ", status=" + status +
                ", userStateId=" + userStateId +
                ", enabled=" + enabled +
                ", locked=" + locked +
                '}';
    }
}