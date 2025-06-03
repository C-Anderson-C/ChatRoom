package top.javahai.chatroom.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import top.javahai.chatroom.dao.UserDao;
import top.javahai.chatroom.entity.RespBean;
import top.javahai.chatroom.entity.RespPageBean;
import top.javahai.chatroom.entity.User;
import top.javahai.chatroom.service.UserService;
import top.javahai.chatroom.utils.UserUtil;

import javax.annotation.Resource;
import java.util.List;

/**
 * (User)表服务实现类
 */
@Service("userService")
public class UserServiceImpl implements UserService, UserDetailsService {
    @Resource
    private UserDao userDao;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userDao.loadUserByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        return user;
    }

    @Override
    public List<User> getUsersWithoutCurrentUser() {
        return userDao.getUsersWithoutCurrentUser(UserUtil.getCurrentUser().getId());
    }

    @Override
    public void setUserStateToOn(Integer id) {
        userDao.setUserStateToOn(id);
    }

    @Override
    public void setUserStateToLeave(Integer id) {
        userDao.setUserStateToLeave(id);
    }

    @Override
    public User queryById(Integer id) {
        return this.userDao.queryById(id);
    }

    @Override
    public List<User> queryAllByLimit(int offset, int limit) {
        return this.userDao.queryAllByLimit(offset, limit);
    }

    @Override
    public Integer insert(User user) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        user.setPassword(encoder.encode(user.getPassword()));
        user.setUserStateId(2);
        user.setEnabled(true);
        user.setLocked(false);
        return this.userDao.insert(user);
    }

    @Override
    public Integer update(User user) {
        return this.userDao.update(user);
    }

    @Override
    public boolean deleteById(Integer id) {
        return this.userDao.deleteById(id) > 0;
    }

    @Override
    public Integer checkUsername(String username) {
        return userDao.checkUsername(username);
    }

    @Override
    public Integer checkNickname(String nickname) {
        return userDao.checkNickname(nickname);
    }

    /**
     * 分页获取用户
     * 注意：调用该方法时需要传入status参数
     */
    @Override
    public RespPageBean getAllUserByPage(Integer page, Integer size, String keyword, Integer isLocked, Integer status) {
        if (page != null && size != null) {
            page = (page - 1) * size; // 起始下标
        }
        // 获取用户数据
        List<User> userList = userDao.getAllUserByPage(page, size, keyword, isLocked, status);
        // 获取用户数据的总数
        Long total = userDao.getTotal(keyword, isLocked, status);
        RespPageBean respPageBean = new RespPageBean();
        respPageBean.setData(userList);
        respPageBean.setTotal(total);
        return respPageBean;
    }

    @Override
    public Integer changeLockedStatus(Integer id, Boolean isLocked) {
        return userDao.changeLockedStatus(id, isLocked);
    }

    @Override
    public Integer deleteByIds(Integer[] ids) {
        return userDao.deleteByIds(ids);
    }

}