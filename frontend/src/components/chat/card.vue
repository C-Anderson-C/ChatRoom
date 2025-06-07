<template>
  <div id="card">
    <!-- 用户信息区域 -->
    <header class="user-info">
      <div class="user-avatar">
        <el-avatar
            :size="40"
            :src="userAvatar"
            @error="handleAvatarError">
          {{ userInitials }}
        </el-avatar>
      </div>
      <div class="user-details">
        <p class="name">{{ userDisplayName }}</p>
        <div class="status" :class="{ 'status-online': isOnline }">
          {{ isOnline ? '在线' : '离线' }}
        </div>
      </div>
    </header>

    <!-- 搜索与操作区域 -->
    <div class="search-container">
      <el-input
          class="search"
          type="text"
          size="mini"
          v-model="searchText"
          placeholder="搜索好友或群组"
          prefix-icon="el-icon-search"
          @input="updateFilterKey"
          @keyup.enter.native="handleSearch"
          clearable>
      </el-input>

      <!-- 添加好友按钮 -->
      <el-dropdown
          trigger="click"
          @command="handleCommand"
          placement="bottom-end"
          size="mini">
        <el-button
            class="searchBtn"
            size="mini"
            type="primary"
            icon="el-icon-plus">
        </el-button>

        <el-dropdown-menu slot="dropdown">
          <el-dropdown-item command="addFriend">
            <i class="el-icon-user-solid"></i> 添加好友
          </el-dropdown-item>
          <el-dropdown-item command="createGroup">
            <i class="el-icon-s-cooperation"></i> 创建群组
          </el-dropdown-item>
          <el-dropdown-item command="joinGroup">
            <i class="el-icon-s-fold"></i> 加入群组
          </el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </div>

    <!-- 导航标签页 -->
    <div class="nav-tabs">
      <div
          class="tab"
          :class="{ active: currentList === 'friends' }"
          @click="switchTab('friends')">
        好友
      </div>
      <div
          class="tab"
          :class="{ active: currentList === 'groups' }"
          @click="switchTab('groups')">
        群组
      </div>
      <div
          class="tab"
          :class="{ active: currentList === 'requests' }"
          @click="switchTab('requests')">
        请求
        <el-badge
            v-if="pendingRequestsCount > 0"
            :value="pendingRequestsCount"
            class="request-badge">
        </el-badge>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'card',
  data() {
    return {
      user: JSON.parse(window.sessionStorage.getItem('user')) || {},
      searchText: '',
      avatarError: false,
      pendingRequestsCount: 0,
      currentList: 'friends', // 默认显示好友列表
      activeButtonKey: null,
      isOnline: true, // 默认在线状态
      lastHeartbeat: Date.now()
    }
  },
  computed: {
    userDisplayName() {
      return this.user.nickname || this.user.username || '未登录';
    },
    userAvatar() {
      if (this.avatarError || !this.user.userProfile) {
        return null; // 返回null会触发el-avatar的默认行为，显示文字头像
      }
      return this.user.userProfile;
    },
    userInitials() {
      const name = this.userDisplayName;
      return name ? name.charAt(0).toUpperCase() : '?';
    }
  },
  watch: {
    // 监听Vuex中的filterKey变化，保持双向同步
    '$store.state.filterKey': {
      handler(newVal) {
        if (this.searchText !== newVal) {
          this.searchText = newVal;
        }
      },
      immediate: true
    }
  },
  mounted() {
    console.log('[Card] 组件已挂载');

    // 初始化搜索文本
    if (this.$store.state.filterKey) {
      this.searchText = this.$store.state.filterKey;
    }

    // 监听友好请求更新事件
    this.$bus.$on('friend-requests-updated', this.updatePendingRequestsCount);

    // 初始加载好友请求数量
    this.loadPendingRequestsCount();

    // 设置定时器定期检查好友请求
    this.requestCheckInterval = setInterval(() => {
      this.loadPendingRequestsCount();
    }, 60000); // 每分钟检查一次

    // 定期发送心跳，更新在线状态
    this.heartbeatInterval = setInterval(() => {
      this.sendHeartbeat();
    }, 30000); // 每30秒发送一次心跳

    // 初始发送一次心跳
    this.sendHeartbeat();
  },
  beforeDestroy() {
    // 清除定时器
    if (this.requestCheckInterval) {
      clearInterval(this.requestCheckInterval);
    }
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
    }

    // 移除事件监听
    this.$bus.$off('friend-requests-updated', this.updatePendingRequestsCount);
  },
  methods: {
    // 更新 Vuex store 中的 filterKey
    updateFilterKey() {
      this.$store.commit('updateFilterKey', this.searchText);
    },

    // 处理搜索
    handleSearch() {
      this.updateFilterKey();
      // 触发总线事件通知其他组件执行搜索
      this.$bus.$emit('perform-search', this.searchText);
    },

    // 处理下拉菜单命令
    handleCommand(command) {
      console.log(`[Card] 执行命令: ${command}`);

      switch (command) {
        case 'addFriend':
          this.showAddFriendDialog();
          break;
        case 'createGroup':
          this.showCreateGroupDialog();
          break;
        case 'joinGroup':
          this.showJoinGroupDialog();
          break;
      }
    },

    // 显示添加好友对话框
    showAddFriendDialog() {
      console.log('[Card] 显示添加好友对话框');

      // 通过全局事件总线触发事件
      if (this.$root) {
        this.$root.$emit('global-add-friend-dialog');
        console.log('[Card] 已触发全局添加好友对话框事件');
      }

      // 触发组件事件
      this.$emit('show-add-dialog');
      console.log('[Card] 已触发组件添加好友对话框事件');
    },

    // 显示创建群组对话框
    showCreateGroupDialog() {
      console.log('[Card] 显示创建群组对话框');
      this.$bus.$emit('show-create-group-dialog');
    },

    // 显示加入群组对话框
    showJoinGroupDialog() {
      console.log('[Card] 显示加入群组对话框');
      this.$bus.$emit('show-join-group-dialog');
    },

    // 切换标签页
    switchTab(tab) {
      if (this.currentList === tab) return;

      this.currentList = tab;
      console.log(`[Card] 切换到标签页: ${tab}`);

      // 通知其他组件切换列表
      this.$store.commit('changeCurrentList', tab);

      // 发送事件通知其他组件
      if (tab === 'friends') {
        this.$bus.$emit('switch-to-friends');
      } else if (tab === 'groups') {
        this.$bus.$emit('switch-to-groups');
      } else if (tab === 'requests') {
        this.$bus.$emit('switch-to-requests');
      }
    },

    // 加载待处理的好友请求数量
    loadPendingRequestsCount() {
      if (!this.user || !this.user.id) return;

      console.log('[Card] 加载待处理的好友请求数量');

      this.getRequest(`/friend/pendingRequestsCount?userId=${this.user.id}`)
          .then(resp => {
            if (resp && typeof resp.count === 'number') {
              this.pendingRequestsCount = resp.count;
            } else if (Array.isArray(resp)) {
              // 如果返回的是请求列表而不是计数
              this.pendingRequestsCount = resp.length;
            }
          })
          .catch(err => {
            console.error('[Card] 获取待处理好友请求数量失败:', err);
          });
    },

    // 更新待处理请求计数
    updatePendingRequestsCount(count) {
      if (typeof count === 'number') {
        this.pendingRequestsCount = count;
      } else {
        // 如果没有提供计数，则重新加载
        this.loadPendingRequestsCount();
      }
    },

    // 处理头像加载错误
    handleAvatarError() {
      console.log('[Card] 头像加载失败，使用文字头像');
      this.avatarError = true;
    },

    // 发送心跳包更新在线状态
    sendHeartbeat() {
      if (!this.user || !this.user.id) return;

      const now = Date.now();
      // 如果距离上次发送时间超过5分钟，才发送新的心跳
      if (now - this.lastHeartbeat > 300000) {
        this.lastHeartbeat = now;

        if (this.$store.state.stomp && this.$store.state.stomp.connected) {
          try {
            this.$store.state.stomp.send("/app/heartbeat", {}, JSON.stringify({
              userId: this.user.id,
              timestamp: now
            }));
            console.log('[Card] 心跳包已发送');
            this.isOnline = true;
          } catch (e) {
            console.warn('[Card] 发送心跳包失败:', e);
            this.isOnline = false;
          }
        } else {
          console.warn('[Card] WebSocket未连接，无法发送心跳包');
          this.isOnline = false;
        }
      }
    }
  }
}
</script>

<style lang="scss" scoped>
#card {
  padding: 12px;
  margin-bottom: 5px;
  position: relative;
  background-color: #f9f9f9;
  border-bottom: 1px solid #e8e8e8;

  // 用户信息部分
  .user-info {
    display: flex;
    align-items: center;
    margin-bottom: 12px;

    .user-avatar {
      margin-right: 12px;
    }

    .user-details {
      flex: 1;

      .name {
        font-weight: bold;
        margin: 0 0 4px 0;
        font-size: 15px;
        line-height: 1.2;
      }

      .status {
        font-size: 12px;
        color: #999;

        &.status-online {
          color: #67C23A;
        }
      }
    }
  }

  // 搜索容器
  .search-container {
    display: flex;
    margin-bottom: 12px;
    gap: 5px;

    .search {
      flex: 1;
    }

    .searchBtn {
      background-color: #1890ff;
      color: white;
      border: 0;
      cursor: pointer;
      padding: 7px 10px;

      &:hover {
        background-color: #40a9ff;
      }

      i {
        font-size: 14px;
      }
    }
  }

  // 导航标签
  .nav-tabs {
    display: flex;
    border-bottom: 1px solid #e8e8e8;

    .tab {
      flex: 1;
      text-align: center;
      padding: 8px 0;
      cursor: pointer;
      position: relative;
      font-size: 14px;

      &:hover {
        color: #1890ff;
      }

      &.active {
        color: #1890ff;
        font-weight: 500;

        &::after {
          content: '';
          position: absolute;
          bottom: -1px;
          left: 0;
          right: 0;
          height: 2px;
          background-color: #1890ff;
        }
      }

      .request-badge {
        margin-left: 4px;
        margin-top: -8px;
      }
    }
  }
}
</style>

<style>
/*当前组件的el-input样式设置*/
#card .el-input__inner {
  background-color: #f2f2f2;
  border-radius: 4px;
  border: 1px solid #e0e0e0;
}

#card .el-input:hover .el-input__inner {
  border-color: #c0c4cc;
}

#card .el-input.is-focus .el-input__inner {
  border-color: #1890ff;
}

/* 下拉菜单样式调整 */
.el-dropdown-menu__item {
  font-size: 14px;
}

.el-dropdown-menu__item i {
  margin-right: 5px;
}

/* 徽章样式调整 */
.el-badge__content.is-fixed {
  top: 8px;
  right: 8px;
  transform: translateY(-50%) translateX(100%);
}
</style>