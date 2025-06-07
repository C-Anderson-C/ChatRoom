<template>
  <div class="list-container">
    <!-- 调试工具栏 -->
    <div v-if="isDebug" class="debug-toolbar">
      <el-button size="mini" type="primary" @click="loadFriendList">刷新好友</el-button>
      <el-button size="mini" type="success" @click="loadGroupList">刷新群聊</el-button>
    </div>

    <!-- 主要Tab切换区域 -->
    <el-tabs v-model="activeTab" type="card">
      <el-tab-pane label="好友" name="friends">
        <div class="search-bar">
          <el-input
              placeholder="搜索好友"
              prefix-icon="el-icon-search"
              v-model="friendSearchKey"
              clearable
              size="small">
          </el-input>
        </div>

        <div class="friend-list">
          <div v-if="filteredFriendList.length > 0">
            <div v-for="friend in filteredFriendList" :key="friend.id"
                 class="list-item"
                 :class="{active: currentChat && currentChat.id === friend.id && chatType === 'friend'}"
                 @click="selectFriend(friend)">
              <div class="friend-info">
                <el-avatar :size="36" :src="friend.avatar || defaultAvatar" class="avatar">
                  {{ friend.nickname?.[0] || friend.username?.[0] || '?' }}
                </el-avatar>
                <div class="user-details">
                  <p class="name">{{friend.nickname || friend.username}}</p>
                  <p class="status-text">{{ getStatusText(friend) }}</p>
                </div>
              </div>
              <div class="user-status">
                <el-badge :value="friend.userStateId==1?'在线':'离线'" :type="friend.userStateId==1?'success':'info'"></el-badge>
                <div v-if="hasUnreadMessages(friend)" class="unread-badge"></div>
              </div>
            </div>
          </div>
          <div v-else class="empty-list">
            <el-empty description="暂无好友">
              <el-button size="small" @click="loadFriendList">刷新列表</el-button>
            </el-empty>
          </div>

          <!-- 调试信息 -->
          <div v-if="isDebug" class="debug-info">
            <p>好友总数: {{ friendList.length }}</p>
            <p>过滤后: {{ filteredFriendList.length }}</p>
            <p>当前聊天对象: {{ currentChat ? (currentChat.username || '未知') : '无' }}</p>
          </div>
        </div>
      </el-tab-pane>

      <!-- 群聊列表选项卡 -->
      <el-tab-pane label="群聊" name="groups">
        <div class="search-bar">
          <el-input
              placeholder="搜索群组"
              prefix-icon="el-icon-search"
              v-model="groupSearchKey"
              clearable
              size="small">
          </el-input>
        </div>

        <div v-if="filteredGroupList.length > 0">
          <div v-for="group in filteredGroupList"
               :key="'group_'+group.id"
               class="list-item"
               :class="{active: currentChat && currentChat.id === group.id && chatType === 'group'}"
               @click="selectGroup(group)">
            <div class="friend-info">
              <el-avatar :size="36" :src="group.avatar || defaultGroupAvatar" class="avatar">
                {{ group.name?.[0] || 'G' }}
              </el-avatar>
              <div class="user-details">
                <p class="name">{{ group.name }}</p>
                <p class="status-text">{{ group.memberCount || 0 }}人</p>
              </div>
            </div>
            <div class="user-status">
              <div v-if="hasUnreadGroupMessages(group)" class="unread-badge"></div>
            </div>
          </div>
        </div>
        <div v-else class="empty-list">
          <el-empty description="暂无群聊">
            <el-button size="small" @click="loadGroupList">刷新群组</el-button>
          </el-empty>
        </div>

        <!-- 调试信息 -->
        <div v-if="isDebug" class="debug-info">
          <p>群聊总数: {{ groupList.length }}</p>
          <p>过滤后: {{ filteredGroupList.length }}</p>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 连接状态指示器 -->
    <div class="connection-status" :class="connectionStatusClass">
      {{ connectionStatusText }}
    </div>
  </div>
</template>

<script>
import { mapState } from 'vuex';

export default {
  name: 'list',
  data() {
    return {
      activeTab: 'friends',
      friendList: [],
      groupList: [],
      friendSearchKey: '',
      groupSearchKey: '',
      currentChat: null,
      chatType: '',
      defaultAvatar: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA0MCA0MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48Y2lyY2xlIGN4PSIyMCIgY3k9IjIwIiByPSIyMCIgZmlsbD0iI2U1ZTVlNSIvPjxjaXJjbGUgY3g9IjIwIiBjeT0iMTYiIHI9IjYiIGZpbGw9IiNhMGEwYTAiLz48cGF0aCBkPSJNMTAgMzJDMTAgMjUuMzcgMTQuNDc3IDIwIDIwIDIwQzI1LjUyMyAyMCAzMCAyNS4zNyAzMCAzMkg0MFY0MEgwVjMySzEwIDMyWiIgZmlsbD0iI2EwYTBhMCIvPjwvc3ZnPg==',
      defaultGroupAvatar: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA0MCA0MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48Y2lyY2xlIGN4PSIyMCIgY3k9IjIwIiByPSIyMCIgZmlsbD0iIzkyQkFERCIvPjxjaXJjbGUgY3g9IjE1IiBjeT0iMTUiIHI9IjQiIGZpbGw9IiNGRkZGRkYiLz48Y2lyY2xlIGN4PSIyNSIgY3k9IjE1IiByPSI0IiBmaWxsPSIjRkZGRkZGIi8+PGNpcmNsZSBjeD0iMTUiIGN5PSIyNSIgcj0iNCIgZmlsbD0iI0ZGRkZGRiIvPjxjaXJjbGUgY3g9IjI1IiBjeT0iMjUiIHI9IjQiIGZpbGw9IiNGRkZGRkYiLz48L3N2Zz4=',
      isDebug: true, // 开启调试模式
      loadingFriends: false,
      loadingGroups: false,
      loadError: null,
      retryCount: 0,
      lastLoadTime: 0
    };
  },
  computed: {
    ...mapState({
      currentUser: state => state.currentUser,
      currentSession: state => state.currentSession,
      isDot: state => state.isDot,
      connectionState: state => state.connectionState,
      sessions: state => state.sessions
    }),

    // 过滤好友列表
    filteredFriendList() {
      if (!this.friendList || this.friendList.length === 0) return [];

      if (!this.friendSearchKey) return this.friendList;

      const searchKey = this.friendSearchKey.toLowerCase();
      return this.friendList.filter(friend =>
          (friend.username && friend.username.toLowerCase().includes(searchKey)) ||
          (friend.nickname && friend.nickname.toLowerCase().includes(searchKey))
      );
    },

    // 过滤群聊列表
    filteredGroupList() {
      if (!this.groupList || this.groupList.length === 0) return [];

      if (!this.groupSearchKey) return this.groupList;

      const searchKey = this.groupSearchKey.toLowerCase();
      return this.groupList.filter(group =>
          group.name && group.name.toLowerCase().includes(searchKey)
      );
    },

    // 连接状态文字
    connectionStatusText() {
      switch(this.$store.state.connectionState) {
        case 'connected': return '已连接';
        case 'connecting': return '正在连接...';
        case 'disconnecting': return '正在断开连接...';
        case 'disconnected': return '未连接';
        default: return '未知状态';
      }
    },

    // 连接状态CSS类
    connectionStatusClass() {
      return {
        'status-connected': this.$store.state.connectionState === 'connected',
        'status-connecting': this.$store.state.connectionState === 'connecting',
        'status-disconnected': this.$store.state.connectionState === 'disconnected',
        'status-error': this.$store.state.lastConnectionError
      };
    }
  },
  created() {
    console.log('[List] 列表组件已创建，准备加载数据');

    // 确保WebSocket连接
    this.ensureConnection().then(() => {
      this.loadFriendList();
      this.loadGroupList();
    });

    // 监听事件
    if (this.$bus) {
      this.$bus.$on('refresh-friend-list', this.loadFriendList);
      this.$bus.$on('refresh-group-list', this.loadGroupList);
      this.$bus.$on('switch-to-groups', () => {
        this.activeTab = 'groups';
      });
      this.$bus.$on('new-message', this.handleNewMessage);
    }

    // 自动定时刷新列表 (每5分钟)
    this.autoRefreshTimer = setInterval(() => {
      const now = Date.now();
      const fiveMinutes = 5 * 60 * 1000;

      // 只有当最后一次加载是5分钟前时才刷新
      if (now - this.lastLoadTime > fiveMinutes) {
        console.log('[List] 自动刷新好友和群组列表');
        this.loadFriendList();
        this.loadGroupList();
      }
    }, 60000); // 每分钟检查一次
  },
  beforeDestroy() {
    if (this.$bus) {
      this.$bus.$off('refresh-friend-list', this.loadFriendList);
      this.$bus.$off('refresh-group-list', this.loadGroupList);
      this.$bus.$off('switch-to-groups');
      this.$bus.$off('new-message', this.handleNewMessage);
    }

    // 清除定时器
    if (this.autoRefreshTimer) {
      clearInterval(this.autoRefreshTimer);
    }
  },
  methods: {
    // 确保WebSocket连接
    ensureConnection() {
      // 如果已连接，直接返回成功
      if (this.$store.state.stomp && this.$store.state.stomp.connected) {
        return Promise.resolve();
      }

      console.log('[List] 尝试建立WebSocket连接');
      return this.$store.dispatch('connect').catch(err => {
        console.error('[List] WebSocket连接失败，将继续加载列表:', err);
        // 即使WebSocket连接失败，我们仍然尝试加载列表
      });
    },

    loadFriendList() {
      if (this.loadingFriends) {
        console.log('[List] 正在加载好友列表，跳过重复请求');
        return;
      }

      this.loadingFriends = true;
      console.log('[List] 开始加载好友列表');
      const userId = this.$store.state.currentUser.id;

      if (!userId) {
        console.error('[List] 未获取到用户ID，无法加载好友列表');
        this.loadingFriends = false;
        return;
      }

      console.log('[List] 当前用户ID:', userId);

      this.getRequest(`/friend/list?userId=${userId}`)
          .then(resp => {
            console.log('[List] 好友列表API返回数据:', resp);

            if (resp && Array.isArray(resp)) {
              // 对好友列表进行去重处理
              const uniqueFriends = [];
              const friendIds = new Set();

              resp.forEach(friend => {
                // 确保friend.id存在并且是有效的
                if (friend && friend.id && !friendIds.has(friend.id)) {
                  friendIds.add(friend.id);
                  uniqueFriends.push(friend);
                }
              });

              console.log('[List] 好友列表处理完成: 原始数据:', resp.length, '去重后:', uniqueFriends.length);
              this.friendList = uniqueFriends;

              // 更新到Vuex存储
              this.$store.commit('setFriendList', uniqueFriends);
              this.loadError = null;
              this.retryCount = 0;
            } else {
              console.warn('[List] 好友列表API返回无效数据:', resp);
              this.friendList = [];
              this.$store.commit('setFriendList', []);
            }

            // 记录最后加载时间
            this.lastLoadTime = Date.now();
          })
          .catch(err => {
            console.error('[List] 加载好友列表失败:', err);
            this.loadError = err.message || '加载失败';
            this.friendList = [];

            // 如果失败，尝试重试（最多3次）
            if (this.retryCount < 3) {
              this.retryCount++;
              console.log(`[List] 将在3秒后进行第${this.retryCount}次重试`);
              setTimeout(() => this.loadFriendList(), 3000);
            }
          })
          .finally(() => {
            this.loadingFriends = false;
          });
    },

    loadGroupList() {
      if (this.loadingGroups) {
        console.log('[List] 正在加载群组列表，跳过重复请求');
        return;
      }

      this.loadingGroups = true;
      console.log('[List] 开始加载群聊列表');

      const userId = this.$store.state.currentUser.id;
      if (!userId) {
        console.error('[List] 未获取到用户ID，无法加载群组列表');
        this.loadingGroups = false;
        return;
      }

      console.log('[List] 当前用户ID (群聊):', userId);

      this.getRequest(`/group/list?userId=${userId}`)
          .then(resp => {
            console.log('[List] 群聊列表API返回数据:', resp);

            if (resp && Array.isArray(resp)) {
              // 对群聊列表进行去重处理
              const uniqueGroups = [];
              const groupIds = new Set();

              resp.forEach(group => {
                if (group && group.id && !groupIds.has(group.id)) {
                  groupIds.add(group.id);
                  uniqueGroups.push(group);
                }
              });

              console.log('[List] 群聊列表处理完成: 原始数据:', resp.length, '去重后:', uniqueGroups.length);
              this.groupList = uniqueGroups;

              // 更新到Vuex存储
              this.$store.commit('setGroups', uniqueGroups);

              this.loadError = null;
              this.retryCount = 0;
            } else {
              console.warn('[List] 群聊列表API返回无效数据');
              this.groupList = [];
              this.$store.commit('setGroups', []);
            }

            // 记录最后加载时间
            this.lastLoadTime = Date.now();
          })
          .catch(err => {
            console.error('[List] 加载群聊列表失败:', err);
            this.loadError = err.message || '加载失败';
            this.groupList = [];

            // 如果失败，尝试重试（最多3次）
            if (this.retryCount < 3) {
              this.retryCount++;
              console.log(`[List] 将在3秒后进行第${this.retryCount}次重试`);
              setTimeout(() => this.loadGroupList(), 3000);
            }
          })
          .finally(() => {
            this.loadingGroups = false;
          });
    },

    selectFriend(friend) {
      console.log('[List] 选择好友:', friend);

      // 更新当前聊天状态
      this.currentChat = friend;
      this.chatType = 'friend';

      // 确保friend对象包含必要的属性
      const safeFriend = {
        ...friend,
        username: friend.username || `user_${friend.id}`, // 确保有username
        avatar: friend.avatar || this.defaultAvatar
      };

      // 通知其他组件切换聊天
      this.$emit('chat-changed', { type: 'friend', data: safeFriend });

      // 同时保持与原有逻辑的兼容
      this.changeCurrentSession(safeFriend);

      // 如果有未读标记，清除它
      const messageKey = `${this.$store.state.currentUser.username}#${friend.username}`;
      if (this.$store.state.isDot && this.$store.state.isDot[messageKey]) {
        this.$store.state.isDot[messageKey] = false;
      }
    },

    selectGroup(group) {
      console.log('[List] 选择群聊:', group);

      // 更新当前聊天状态
      this.currentChat = group;
      this.chatType = 'group';

      // 通知其他组件切换聊天
      this.$emit('chat-changed', { type: 'group', data: group });

      // 使用专门的群聊会话处理方法
      this.changeGroupSession(group);

      // 如果有未读标记，清除它
      const groupKey = `群聊_${group.id}`;
      if (this.$store.state.isDot && this.$store.state.isDot[groupKey]) {
        this.$store.state.isDot[groupKey] = false;
      }
    },

    changeCurrentSession(currentSession) {
      this.$store.commit('changeCurrentSession', currentSession);

      // 延迟一会儿再刷新消息，确保组件已经更新
      setTimeout(() => {
        this.$bus && this.$bus.$emit('refresh-messages', true);
      }, 100);
    },

    changeGroupSession(group) {
      // 构建群聊会话对象并提交到store
      const groupSession = {
        id: group.id,
        name: group.name,
        nickname: group.name,
        username: 'group_' + group.id,
        isGroup: true,
        avatar: group.avatar || this.defaultGroupAvatar
      };

      // 设置当前聊天对象
      this.$store.commit('changeCurrentSession', groupSession);

      // 设置当前群组ID
      this.$store.commit('setCurrentGroupId', group.id);

      // 延迟一会儿再刷新消息，确保组件已经更新
      setTimeout(() => {
        this.$bus && this.$bus.$emit('refresh-messages', true);
      }, 100);
    },

    // 获取用户状态文本
    getStatusText(friend) {
      return friend.status || (friend.userStateId == 1 ? '在线' : '离线');
    },

    // 检查是否有未读消息
    hasUnreadMessages(friend) {
      if (!friend || !friend.username || !this.$store.state.isDot) return false;

      const messageKey = `${this.$store.state.currentUser.username}#${friend.username}`;
      return !!this.$store.state.isDot[messageKey];
    },

    // 检查是否有未读群聊消息
    hasUnreadGroupMessages(group) {
      if (!group || !group.id || !this.$store.state.isDot) return false;

      const groupKey = `群聊_${group.id}`;
      return !!this.$store.state.isDot[groupKey];
    },

    // 处理新消息事件
    handleNewMessage(eventData) {
      if (!eventData || !eventData.message) return;

      const msg = eventData.message;

      // 如果不是自己发的消息，且不是当前会话的消息，则设置未读标记
      if (!msg.self) {
        if (msg.from && !this.isCurrentChat(msg.from)) {
          // 找到匹配的好友
          const friend = this.friendList.find(f => f.username === msg.from);
          if (friend) {
            this.$forceUpdate(); // 强制更新视图
          }
        } else if (msg.to && msg.to.startsWith('group_')) {
          // 处理群组消息
          const groupId = msg.to.replace('group_', '');
          if (!this.isCurrentGroup(groupId)) {
            this.$forceUpdate(); // 强制更新视图
          }
        }
      }
    },

    // 检查是否是当前聊天对象
    isCurrentChat(username) {
      return this.chatType === 'friend' &&
          this.currentChat &&
          this.currentChat.username === username;
    },

    // 检查是否是当前群聊
    isCurrentGroup(groupId) {
      return this.chatType === 'group' &&
          this.currentChat &&
          this.currentChat.id == groupId;
    }
  }
}
</script>

<style lang="scss" scoped>
.list-container {
  position: relative;
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #f9f9f9;
  border-right: 1px solid #eaeaea;

  .debug-toolbar {
    padding: 5px;
    display: flex;
    justify-content: space-between;
    background-color: #f0f9ff;
    border-bottom: 1px dashed #c6e2ff;
  }

  .search-bar {
    padding: 8px;
    background-color: #fafafa;
    border-bottom: 1px solid #eaeaea;
  }

  .el-tabs {
    flex: 1;
    display: flex;
    flex-direction: column;

    ::v-deep .el-tabs__content {
      flex: 1;
      overflow-y: auto;
    }

    ::v-deep .el-tab-pane {
      height: 100%;
      display: flex;
      flex-direction: column;
    }
  }

  .friend-list {
    flex: 1;
    overflow-y: auto;
  }

  .list-item {
    padding: 10px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    cursor: pointer;
    border-bottom: 1px solid #f0f0f0;
    transition: background-color 0.2s;

    &:hover {
      background-color: #f0f7ff;
    }

    &.active {
      background-color: #e6f2ff;
    }
  }

  .friend-info {
    display: flex;
    align-items: center;

    .user-details {
      margin-left: 10px;

      .name {
        margin: 0;
        font-size: 14px;
        line-height: 1.4;
        font-weight: 500;
      }

      .status-text {
        margin: 0;
        font-size: 12px;
        color: #909399;
      }
    }
  }

  .user-status {
    display: flex;
    align-items: center;

    .unread-badge {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background-color: #f56c6c;
      margin-left: 5px;
    }
  }

  .empty-list {
    padding: 20px 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100%;
  }

  .debug-info {
    background-color: #f8f8f8;
    padding: 8px;
    border-top: 1px dashed #ddd;
    font-size: 12px;
    color: #606266;
    margin-top: auto;

    p {
      margin: 3px 0;
    }
  }

  .connection-status {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    padding: 4px;
    text-align: center;
    font-size: 12px;
    background-color: #f5f5f5;
    border-top: 1px solid #eaeaea;
    transition: background-color 0.3s;

    &.status-connected {
      background-color: #f0f9eb;
      color: #67c23a;
    }

    &.status-connecting {
      background-color: #f4f4f5;
      color: #909399;
    }

    &.status-disconnected {
      background-color: #fef0f0;
      color: #f56c6c;
    }

    &.status-error {
      background-color: #fef0f0;
      color: #f56c6c;
    }
  }
}
</style>