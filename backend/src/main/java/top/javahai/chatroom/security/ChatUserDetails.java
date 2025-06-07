package top.javahai.chatroom.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import top.javahai.chatroom.entity.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ChatUserDetails implements UserDetails {

    private final User user;
    private final Collection<GrantedAuthority> authorities;

    public ChatUserDetails(User user) {
        this.user = user;
        List<GrantedAuthority> auths = new ArrayList<>();
        // 默认赋予用户ROLE_USER权限，如果你有更复杂的权限管理，可以从数据库加载
        auths.add(new SimpleGrantedAuthority("ROLE_USER"));
        this.authorities = auths;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;  // 可以根据实际需求返回
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;  // 可以根据实际需求返回
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;  // 可以根据实际需求返回
    }

    @Override
    public boolean isEnabled() {
        return true;  // 可以根据实际需求返回
    }

    // 提供获取原始用户对象的方法
    public User getUser() {
        return user;
    }
}