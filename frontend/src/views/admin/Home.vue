<template>
  <el-container>
    <el-header class="homeHeader">
      <div class="title">ChatRoom管理端</div>
      <div>
        <el-dropdown class="choices" @command="commandHandler">
          <span class="el-dropdown-link">
            {{user?.nickname || user?.username || '未登录'}}<i><img :src="user?.avatar || defaultAvatar"></i>
          </span>
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item command="userInfo">个人中心</el-dropdown-item>
            <el-dropdown-item command="setting">设置</el-dropdown-item>
            <el-dropdown-item command="logout" divided>注销登录</el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
      </div>
    </el-header>
    <el-container>
      <el-aside width="200px">
        <el-menu router unique-opened>
          <el-submenu
              :index="'index+'"
              v-for="(item,index) in routes"
              v-if="!item.hidden"
              :key="item.path || 'route_'+index"
          >
            <template slot="title">
              <i style="color: #2F86D2;margin-right: 8px" :class="item.iconCls"></i>
              <span>{{item.name}}</span>
            </template>
            <el-menu-item
                :index="child.path"
                v-for="(child,index) in item.children"
                :key="child.path || 'child_'+index"
            />
          </el-submenu>
        </el-menu>
      </el-aside>
      <el-main>
        <el-breadcrumb separator-class="el-icon-arrow-right" v-if="this.$router.currentRoute.path!='/home'">
          <el-breadcrumb-item :to="{ path: '/home' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item>{{this.$router.currentRoute.name}}</el-breadcrumb-item>
        </el-breadcrumb>
        <div class="homeWelcome" v-if="this.$router.currentRoute.path=='/home'">
          欢迎来到Chatroom管理端！

          <!-- 可选：如果想在主页显示好友管理，可以放在这里 -->
          <!--
          <div class="friend-management">
            <h2>好友管理</h2>
            <friend-list @show-add-friend-dialog="showAddFriendDialog" @select-friend="handleSelectFriend"></friend-list>
          </div>
          -->
        </div>
        <router-view class="homeRouterView"/>

        <!-- 添加好友对话框 -->
        <friend-search :visible.sync="addFriendDialogVisible"></friend-search>
      </el-main>
    </el-container>
  </el-container>
</template>

<script>
// 导入FriendSearch组件
import FriendSearch from '@/components/chat/FriendSearch.vue';

export default {
  name: "Home",
  components: {
    FriendSearch
    // 如果需要在主页显示FriendList，取消下面的注释
    // FriendList: () => import('@/components/chat/FriendList.vue')
  },
  data() {
    return {
      user: null,
      defaultAvatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',
      addFriendDialogVisible: false
    }
  },
  computed: {
    routes() {
      return this.$router.options.routes;
    }
  },
  created() {
    // 从sessionStorage获取用户信息并进行安全处理
    try {
      const adminData = window.sessionStorage.getItem("admin");
      if (adminData) {
        this.user = JSON.parse(adminData);
        console.log('用户数据加载成功:', this.user);
      } else {
        console.warn('未找到管理员数据');
      }
    } catch (error) {
      console.error('解析用户数据出错:', error);
      this.user = {
        username: '未知用户',
        avatar: this.defaultAvatar
      };
    }
  },
  methods: {
    test() {
      this.$router.push("/userinfo")
    },
    showAddFriendDialog() {
      this.addFriendDialogVisible = true;
    },
    handleSelectFriend(friend) {
      console.log('选中好友:', friend);
      // 实现选择好友后的逻辑
    },
    commandHandler(cmd) {
      // 注销登录操作
      if (cmd == 'logout') {
        this.$confirm('此操作将注销登录, 是否继续?', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          this.getRequest("/admin/logout");
          sessionStorage.removeItem("admin"); // 删除session
          this.$router.replace('/adminlogin'); // 页面替换为登陆页Login.vue
        }).catch(() => {
          this.$message({
            type: 'info',
            message: '已取消操作'
          });
        });
      } else if (cmd == 'userInfo') {
        this.$router.push('/userinfo');
      } else if (cmd == 'setting') {
        this.$message.info('设置功能开发中...');
      }
    }
  }
}
</script>

<style>
.homeHeader {
  background-color: #409eff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0px;
  box-sizing: border-box;
}
.homeHeader .title {
  font-size: 30px;
  font-family: 华文行楷;
  color: #ffffff;
}
.homeHeader .choices {
  cursor: pointer;
}
.choices img {
  width: 48px;
  height: 48px;
  border-radius: 24px;
  margin-left: 10px;
  object-fit: cover;
  background-color: #ffffff;
}
.el-dropdown-link {
  display: flex;
  align-items: center;
  color: white;
}
.homeWelcome {
  text-align: center;
  font-size: 30px;
  font-family: 华文行楷;
  color: #409eff;
  margin-bottom: 20px;
}
.homeRouterView {
  margin-top: 15px;
}

/* 如果在主页显示好友管理 */
.friend-management {
  margin-top: 30px;
  text-align: left;
  padding: 20px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}
</style>