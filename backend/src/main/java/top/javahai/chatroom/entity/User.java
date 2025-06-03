package top.javahai.chatroom.entity;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Date;

/**
 * (User)实体类
 *
 * @author makejava
 * @since 2020-06-16 12:08:01
 */
public class User implements UserDetails {

    private Integer id;
    /**
     * 登录账号
     */
    private String username;
    /**
     * 昵称
     */
    private String nickname;
    /**
     * 密码
     */
    private String password;
    /**
     * 用户头像
     */
    private String avatar;
    /**
     * 邮箱
     */
    private String email;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 更新时间
     */
    private Date updateTime;
    /**
     * 状态
     */
    private Integer status;
    /**
     * 用户状态id（如数据库有 user_state_id 字段则保留，否则可去掉）
     */
    private Integer userStateId;

    // ------ UserDetails相关字段 ------
    private Boolean isEnabled;
    private Boolean isLocked;

    // ------ getter/setter ------

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Integer getUserStateId() { return userStateId; }
    public void setUserStateId(Integer userStateId) { this.userStateId = userStateId; }

    public Boolean getEnabled() { return isEnabled; }
    public void setEnabled(Boolean enabled) { isEnabled = enabled; }

    public Boolean getLocked() { return isLocked; }
    public void setLocked(Boolean locked) { isLocked = locked; }

    // ------ UserDetails接口方法实现 ------

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return isLocked == null ? true : !isLocked; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return isEnabled == null ? true : isEnabled; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null; // 如有角色权限可补充
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", nickname='" + nickname + '\'' +
                ", password='" + password + '\'' +
                ", avatar='" + avatar + '\'' +
                ", email='" + email + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                ", status=" + status +
                ", userStateId=" + userStateId +
                ", isEnabled=" + isEnabled +
                ", isLocked=" + isLocked +
                '}';
    }
}