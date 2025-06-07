<template>
  <div class="friend-list-container">
    <!-- 顶部操作区 -->
    <div class="header-actions">
      <el-button
          type="primary"
          size="small"
          @click="$emit('show-add-friend-dialog')"
          :disabled="loading">
        <i class="el-icon-plus"></i> 添加好友
      </el-button>

      <el-badge
          :value="friendRequests.length"
          :max="99"
          class="item"
          v-show="friendRequests.length > 0">
        <el-button
            type="warning"
            size="small"
            @click="showFriendRequestsDialog"
            :disabled="loading">
          好友请求
        </el-button>
      </el-badge>
    </div>

    <el-divider content-position="left">
      <span class="divider-content">
        我的好友
        <span v-if="friendList.length > 0" class="friend-count">({{ friendList.length }})</span>
      </span>
    </el-divider>

    <!-- 好友列表加载中 -->
    <div v-if="loading" class="loading-container">
      <i class="el-icon-loading"></i>
      <p>加载中...</p>
    </div>

    <!-- 没有好友时的空状态 -->
    <el-empty
        v-else-if="friendList.length === 0"
        description="暂无好友"
        :image-size="100">
      <el-button type="primary" size="small" @click="$emit('show-add-friend-dialog')">
        立即添加
      </el-button>
    </el-empty>

    <!-- 好友列表内容 -->
    <div v-else class="friend-list">
      <!-- 搜索框 -->
      <div class="search-container" v-if="friendList.length > 5">
        <el-input
            v-model="searchQuery"
            prefix-icon="el-icon-search"
            placeholder="搜索好友"
            clearable
            size="small">
        </el-input>
      </div>

      <!-- 好友列表项 -->
      <div
          v-for="friend in filteredFriendList"
          :key="friend.id"
          class="friend-item"
          :class="{ active: isActiveFriend(friend) }"
          @click="selectFriend(friend)">

        <el-avatar
            :src="getAvatarUrl(friend.userProfile)"
            :size="40"
            @error="() => handleAvatarError(friend)">
          {{ getInitials(friend) }}
        </el-avatar>

        <div class="friend-info">
          <div class="friend-name">{{ friend.nickname || friend.username }}</div>
          <div class="friend-status">
            <span :class="['status-dot', getStatusClass(friend.status)]"></span>
            {{ getStatusText(friend.status) }}
          </div>
        </div>

        <!-- 操作菜单 -->
        <div class="friend-actions" @click.stop>
          <el-dropdown trigger="click" size="small">
            <i class="el-icon-more"></i>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item @click.native="startChat(friend)">
                <i class="el-icon-chat-dot-square"></i> 发起聊天
              </el-dropdown-item>
              <el-dropdown-item @click.native="viewProfile(friend)">
                <i class="el-icon-user"></i> 查看资料
              </el-dropdown-item>
              <el-dropdown-item divided @click.native="confirmDeleteFriend(friend)">
                <i class="el-icon-delete"></i> 删除好友
              </el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
      </div>
    </div>

    <!-- 好友请求弹窗 -->
    <el-dialog
        title="好友请求"
        :visible.sync="showFriendRequests"
        width="450px"
        :close-on-click-modal="false">
      <friend-requests @refresh-friends="handleFriendListUpdate"></friend-requests>
    </el-dialog>

    <!-- 确认删除好友对话框 -->
    <el-dialog
        title="删除好友"
        :visible.sync="showDeleteConfirm"
        width="320px"
        center>
      <div class="delete-confirm-content">
        <p>确定要删除好友 <strong>{{ pendingDeleteFriend?.nickname || pendingDeleteFriend?.username }}</strong> 吗？</p>
        <p class="delete-warning">删除后将无法恢复聊天记录！</p>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="showDeleteConfirm = false">取 消</el-button>
        <el-button type="danger" @click="deleteFriend" :loading="deletingFriend">确 定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import FriendRequests from './FriendRequests.vue';

export default {
  name: 'FriendList',
  components: {
    FriendRequests
  },
  props: {
    activeFriendId: {
      type: [String, Number],
      default: null
    }
  },
  data() {
    return {
      friendList: [],
      friendRequests: [],
      showFriendRequests: false,
      defaultAvatar: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA0MCA0MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48Y2lyY2xlIGN4PSIyMCIgY3k9IjIwIiByPSIyMCIgZmlsbD0iI2U1ZTVlNSIvPjxjaXJjbGUgY3g9IjIwIiBjeT0iMTYiIHI9IjYiIGZpbGw9IiNhMGEwYTAiLz48cGF0aCBkPSJNMTAgMzJDMTAgMjUuMzcgMTQuNDc3IDIwIDIwIDIwQzI1LjUyMyAyMCAzMCAyNS4zNyAzMCAzMkg0MFY0MEgwVjMySzEwIDMyWiIgZmlsbD0iI2EwYTBhMCIvPjwvc3ZnPg==',
      loading: false,
      searchQuery: '',
      refreshTimer: null,
      showDeleteConfirm: false,
      pendingDeleteFriend: null,
      deletingFriend: false
    }
  },
  computed: {
    // 当前用户
    currentUser() {
      return this.$store.state.currentUser || {};
    },

    // 筛选后的好友列表
    filteredFriendList() {
      if (!this.searchQuery.trim()) {
        return this.sortFriendList(this.friendList);
      }

      const query = this.searchQuery.toLowerCase();
      return this.sortFriendList(
          this.friendList.filter(friend =>
              (friend.nickname && friend.nickname.toLowerCase().includes(query)) ||
              (friend.username && friend.username.toLowerCase().includes(query))
          )
      );
    }
  },
  created() {
    this.loadFriendList();
    this.loadFriendRequests();

    // 设置定期刷新
    this.refreshTimer = setInterval(() => {
      this.loadFriendRequests();
    }, 60000); // 每分钟刷新一次好友请求

    // 监听事件
    this.$bus.$on('refresh-friend-list', this.handleFriendListUpdate);
    this.$bus.$on('refresh-friend-requests', this.loadFriendRequests);
  },
  beforeDestroy() {
    // 清除定时器
    if (this.refreshTimer) {
      clearInterval(this.refreshTimer);
    }

    // 移除事件监听
    this.$bus.$off('refresh-friend-list', this.handleFriendListUpdate);
    this.$bus.$off('refresh-friend-requests', this.loadFriendRequests);
  },
  methods: {
    // 获取好友列表
    loadFriendList() {
      const userId = this.currentUser.id;
      if (!userId) {
        console.warn('[FriendList] 未找到当前用户ID');
        return;
      }

      this.loading = true;
      this.getRequest(`/friend/list?userId=${userId}`)
          .then(resp => {
            if (resp && Array.isArray(resp)) {
              this.friendList = resp;
              console.log('[FriendList] 加载了', resp.length, '个好友');
            } else {
              console.warn('[FriendList] 获取好友列表响应格式错误', resp);
              this.friendList = [];
            }
          })
          .catch(err => {
            console.error('[FriendList] 加载好友列表失败:', err);
            this.$message.error('加载好友列表失败，请稍后重试');
            this.friendList = [];
          })
          .finally(() => {
            this.loading = false;
          });
    },

    // 加载好友请求
    loadFriendRequests() {
      const userId = this.currentUser.id;
      if (!userId) return;

      this.getRequest(`/friend/requests?userId=${userId}`)
          .then(resp => {
            if (resp && Array.isArray(resp)) {
              const previousCount = this.friendRequests.length;
              this.friendRequests = resp;

              // 如果有新的好友请求，显示通知
              if (resp.length > previousCount && previousCount > 0) {
                this.$notify({
                  title: '新的好友请求',
                  message: `您有 ${resp.length} 个好友请求待处理`,
                  type: 'info',
                  duration: 3000
                });
              }
            } else {
              this.friendRequests = [];
            }
          })
          .catch(err => {
            console.error('[FriendList] 加载好友请求失败:', err);
            this.friendRequests = [];
          });
    },

    // 处理好友列表更新
    handleFriendListUpdate() {
      this.loadFriendList();
      this.loadFriendRequests();
    },

    // 选择好友
    selectFriend(friend) {
      if (!friend) return;
      this.$emit('select-friend', friend);
    },

    // 判断是否是当前激活的好友
    isActiveFriend(friend) {
      return this.activeFriendId && friend.id === this.activeFriendId;
    },

    // 获取状态样式类
    getStatusClass(status) {
      switch (status) {
        case 'online': return 'online';
        case 'offline': return 'offline';
        case 'busy': return 'busy';
        case 'away': return 'away';
        default: return 'offline';
      }
    },

    // 获取状态文本
    getStatusText(status) {
      switch (status) {
        case 'online': return '在线';
        case 'offline': return '离线';
        case 'busy': return '忙碌';
        case 'away': return '离开';
        default: return '离线';
      }
    },

    // 显示好友请求对话框
    showFriendRequestsDialog() {
      this.showFriendRequests = true;
      this.$nextTick(() => {
        // 标记好友请求为已读，将来可以实现
      });
    },

    // 获取用户头像
    getAvatarUrl(profile) {
      if (!profile || profile === 'null' || profile === 'undefined') {
        return this.defaultAvatar;
      }

      if (profile.startsWith('http') || profile.startsWith('data:')) {
        return profile;
      }

      return `/img/${profile}`;
    },

    // 处理头像加载错误
    handleAvatarError(friend) {
      console.warn(`[FriendList] 好友 ${friend.username} 的头像加载失败`);
    },

    // 获取用户名首字母
    getInitials(friend) {
      const name = friend.nickname || friend.username;
      return name ? name.charAt(0).toUpperCase() : '?';
    },

    // 发起聊天
    startChat(friend) {
      this.selectFriend(friend);
    },

    // 查看好友资料
    viewProfile(friend) {
      this.$emit('view-friend-profile', friend);
    },

    // 确认删除好友
    confirmDeleteFriend(friend) {
      this.pendingDeleteFriend = friend;
      this.showDeleteConfirm = true;
    },

    // 删除好友
    deleteFriend() {
      if (!this.pendingDeleteFriend) {
        this.showDeleteConfirm = false;
        return;
      }

      this.deletingFriend = true;

      const friendId = this.pendingDeleteFriend.id;
      const userId = this.currentUser.id;

      this.postRequest('/friend/delete', {
        userId: userId,
        friendId: friendId
      })
          .then(resp => {
            if (resp) {
              // 从列表中移除
              this.friendList = this.friendList.filter(f => f.id !== friendId);
              this.$message.success('好友已删除');

              // 如果当前正在聊天的是这个好友，清空聊天区域
              if (this.activeFriendId === friendId) {
                this.$emit('friend-deleted', friendId);
              }
            } else {
              this.$message.error('删除好友失败');
            }
          })
          .catch(err => {
            console.error('[FriendList] 删除好友失败:', err);
            this.$message.error(err.message || '删除失败，请稍后再试');
          })
          .finally(() => {
            this.deletingFriend = false;
            this.showDeleteConfirm = false;
            this.pendingDeleteFriend = null;
          });
    },

    // 对好友列表进行排序（在线的排前面）
    sortFriendList(list) {
      return [...list].sort((a, b) => {
        // 优先按在线状态排序
        if (a.status === 'online' && b.status !== 'online') return -1;
        if (a.status !== 'online' && b.status === 'online') return 1;

        // 其次按名称字母顺序
        const nameA = (a.nickname || a.username || '').toLowerCase();
        const nameB = (b.nickname || b.username || '').toLowerCase();
        return nameA.localeCompare(nameB);
      });
    }
  }
}
</script>

<style lang="scss" scoped>
.friend-list-container {
  padding: 10px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.header-actions {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
}

.divider-content {
  display: flex;
  align-items: center;
  font-size: 14px;

  .friend-count {
    margin-left: 5px;
    font-size: 12px;
    color: #909399;
  }
}

.loading-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;

  i {
    font-size: 24px;
    color: #909399;
    margin-bottom: 10px;
  }

  p {
    color: #909399;
    margin: 0;
  }
}

.friend-list {
  flex: 1;
  overflow-y: auto;
  padding-right: 5px;

  &::-webkit-scrollbar {
    width: 5px;
  }

  &::-webkit-scrollbar-thumb {
    background-color: #dcdfe6;
    border-radius: 3px;
  }
}

.search-container {
  margin-bottom: 15px;
  position: sticky;
  top: 0;
  z-index: 1;
  background: white;
  padding: 5px 0;
}

.friend-item {
  display: flex;
  align-items: center;
  padding: 10px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: 2px;
  position: relative;

  &:hover {
    background-color: #f5f7fa;

    .friend-actions {
      opacity: 1;
    }
  }

  &.active {
    background-color: #ecf5ff;
    border-left: 3px solid #409EFF;
  }
}

.friend-info {
  margin-left: 10px;
  flex: 1;
  overflow: hidden;
}

.friend-name {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 3px;
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
}

.friend-status {
  font-size: 12px;
  color: #909399;
  display: flex;
  align-items: center;
}

.friend-actions {
  opacity: 0;
  transition: opacity 0.2s;
  margin-left: 10px;
  color: #909399;

  i {
    font-size: 16px;
    cursor: pointer;

    &:hover {
      color: #409EFF;
    }
  }
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 5px;

  &.online {
    background-color: #67C23A;
  }

  &.offline {
    background-color: #909399;
  }

  &.busy {
    background-color: #F56C6C;
  }

  &.away {
    background-color: #E6A23C;
  }
}

.delete-confirm-content {
  text-align: center;
  padding: 10px 0;

  .delete-warning {
    font-size: 12px;
    color: #F56C6C;
    margin-top: 10px;
  }
}
</style>