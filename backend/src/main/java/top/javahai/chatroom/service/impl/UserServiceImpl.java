package top.javahai.chatroom.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import top.javahai.chatroom.dao.FriendDao;
import top.javahai.chatroom.dao.UserDao;
import top.javahai.chatroom.entity.RespPageBean;
import top.javahai.chatroom.entity.User;
import top.javahai.chatroom.security.ChatUserDetails;
import top.javahai.chatroom.service.UserService;
import top.javahai.chatroom.utils.UserUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * (User)表服务实现类
 */
@Service("userService")
public class UserServiceImpl implements UserService, UserDetailsService {

    @Autowired
    private final UserDao userDao;
    @Autowired
    private final FriendDao friendDao;

    @Autowired
    public UserServiceImpl(UserDao userDao, FriendDao friendDao) {
        this.userDao = userDao;
        this.friendDao = friendDao;
    }



    @Override
    public List<User> searchUserByKeyword(String keyword) {
        return userDao.searchUsers(keyword);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userDao.loadUserByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        // 返回Spring Security兼容的UserDetails实现
        return new ChatUserDetails(user);
    }

    public User loadUserEntityByUsername(String username) throws UsernameNotFoundException {
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


    /**
     * 获取好友状态
     * @param userId 当前用户ID
     * @param targetId 目标用户ID
     * @return 0: 不是好友, 1: 已是好友, 2: 已发送请求, 3: 已收到请求
     */
    @Override
    public Integer getFriendStatus(Integer userId, Integer targetId) {
        try {
            if (userId == null || targetId == null) {
                return 0; // 默认返回不是好友
            }

            // 使用DAO方法查询好友状态
            return friendDao.getFriendStatus(userId, targetId);
        } catch (Exception e) {
            e.printStackTrace();
            return 0; // 发生错误时默认返回不是好友
        }
    }

    /**
     * 搜索用户并标记好友状态
     * @param keyword 搜索关键词
     * @param currentUserId 当前用户ID
     * @return 搜索结果列表，包含好友状态
     */
    @Override
    public List<Map<String, Object>> searchUsersWithFriendStatus(String keyword, Integer currentUserId) {
        try {
            System.out.println("执行searchUsersWithFriendStatus - keyword: " + keyword + ", currentUserId: " + currentUserId);

            // 确保参数有效
            if (keyword == null) {
                System.out.println("关键词为空，返回空列表");
                return new ArrayList<>();
            }

            // 添加通配符
            String searchKeyword = "%" + keyword + "%";
            System.out.println("使用搜索关键词: " + searchKeyword);

            // 查询用户列表
            List<User> users = userDao.searchUsersByKeyword(searchKeyword, currentUserId);
            System.out.println("搜索到用户数量: " + (users != null ? users.size() : 0));

            if (users == null) {
                System.out.println("用户列表为null，返回空列表");
                return new ArrayList<>();
            }

            List<Map<String, Object>> result = new ArrayList<>();

            // 处理每个用户
            for (User user : users) {
                try {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", user.getId());
                    map.put("username", user.getUsername());
                    map.put("nickname", user.getNickname());

                    // 这里根据User实体的实际属性选择正确的获取头像的方法
                    // 如果User实体使用avatar属性，则使用getAvatar()
                    // 否则使用getUserProfile() (根据实体定义)
                    map.put("avatar", user.getAvatar());

                    // 检查好友状态前记录
                    System.out.println("检查用户 " + user.getId() + " 的好友状态");

                    // 检查好友关系状态
                    Integer friendStatus = friendDao.getFriendStatus(currentUserId, user.getId());
                    System.out.println("用户 " + user.getId() + " 的好友状态: " + friendStatus);

                    map.put("friendStatus", friendStatus);
                    map.put("isFriend", friendStatus == 1);

                    result.add(map);
                } catch (Exception e) {
                    System.err.println("处理用户 " + user.getId() + " 时发生错误: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            return result;
        } catch (Exception e) {
            System.err.println("searchUsersWithFriendStatus异常: " + e.getMessage());
            e.printStackTrace();
            throw e; // 重新抛出以便控制器捕获
        }
    }
}