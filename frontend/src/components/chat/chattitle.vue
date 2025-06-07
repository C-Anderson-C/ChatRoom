<template>
  <div class="chatTitle" v-if="currentChat">
    <!-- 聊天标题信息区域 -->
    <div class="title-info">
      <!-- 显示聊天对象头像 -->
      <el-avatar
          :size="32"
          :src="chatAvatar"
          class="chat-avatar"
          @error="handleAvatarError">
        {{ chatInitials }}
      </el-avatar>

      <!-- 标题文字信息 -->
      <div class="title-text">
        <div class="title-name">{{ chatDisplayName }}</div>
        <div class="title-status" v-if="showStatus">
          <span v-if="isGroup">{{ memberCount }}人</span>
          <span v-else :class="{'online': isOnline, 'offline': !isOnline}">
            {{ isOnline ? '在线' : '离线' }}
          </span>
        </div>
      </div>
    </div>

    <!-- 操作按钮区域 -->
    <div class="action-buttons">
      <!-- 搜索聊天记录 -->
      <el-tooltip content="搜索聊天记录" placement="bottom" :open-delay="300">
        <el-button
            class="action-btn"
            size="small"
            circle
            icon="el-icon-search"
            @click="openSearchHistory">
        </el-button>
      </el-tooltip>

      <!-- 更多选项下拉菜单 -->
      <el-dropdown trigger="click" @command="handleCommand">
        <el-button class="action-btn" size="small" circle icon="el-icon-more"></el-button>
        <el-dropdown-menu slot="dropdown">
          <!-- 群聊相关选项 -->
          <template v-if="isGroup">
            <el-dropdown-item command="viewMembers">
              <i class="el-icon-user"></i> 查看成员
            </el-dropdown-item>
            <el-dropdown-item command="inviteFriend">
              <i class="el-icon-plus"></i> 邀请好友
            </el-dropdown-item>
            <el-dropdown-item command="groupSettings" divided>
              <i class="el-icon-setting"></i> 群聊设置
            </el-dropdown-item>
            <el-dropdown-item command="leaveGroup" divided>
              <i class="el-icon-close"></i> 退出群聊
            </el-dropdown-item>
          </template>

          <!-- 私聊相关选项 -->
          <template v-else>
            <el-dropdown-item command="viewProfile">
              <i class="el-icon-user"></i> 查看资料
            </el-dropdown-item>
            <el-dropdown-item command="clearHistory">
              <i class="el-icon-delete"></i> 清空聊天记录
            </el-dropdown-item>
            <el-dropdown-item command="blockUser" divided>
              <i class="el-icon-turn-off"></i> 屏蔽用户
            </el-dropdown-item>
            <el-dropdown-item command="removeFriend" divided>
              <i class="el-icon-close"></i> 删除好友
            </el-dropdown-item>
          </template>
        </el-dropdown-menu>
      </el-dropdown>
    </div>

    <!-- 聊天记录搜索对话框 -->
    <el-dialog
        title="搜索聊天记录"
        :visible.sync="searchDialogVisible"
        width="500px"
        :append-to-body="true">
      <div class="search-dialog-content">
        <el-input
            v-model="searchQuery"
            placeholder="输入关键词搜索聊天记录"
            prefix-icon="el-icon-search"
            clearable
            @keyup.enter.native="searchMessages">
        </el-input>

        <div class="search-results" v-if="searchResults.length > 0">
          <div
              v-for="(result, index) in searchResults"
              :key="index"
              class="search-result-item"
              @click="navigateToMessage(result)">
            <div class="result-sender">{{ result.fromName || result.from }}</div>
            <div class="result-content" v-html="highlightContent(result.content)"></div>
            <div class="result-time">{{ formatTime(result.createTime) }}</div>
          </div>
        </div>

        <div v-else-if="hasSearched" class="no-results">
          <i class="el-icon-search"></i>
          <p>没有找到匹配的聊天记录</p>
        </div>
      </div>

      <span slot="footer" class="dialog-footer">
        <el-button @click="searchDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="searchMessages" :disabled="!searchQuery">搜索</el-button>
      </span>
    </el-dialog>

    <!-- 成员列表对话框 -->
    <el-dialog
        title="群组成员"
        :visible.sync="membersDialogVisible"
        width="400px"
        :append-to-body="true">
      <div class="members-list" v-loading="loadingMembers">
        <div v-for="member in groupMembers" :key="member.id" class="member-item">
          <el-avatar :size="36" :src="member.avatar || defaultAvatar">
            {{ member.nickname?.[0] || member.username?.[0] || '?' }}
          </el-avatar>
          <div class="member-info">
            <div class="member-name">{{ member.nickname || member.username }}</div>
            <div class="member-role">{{ member.role || '成员' }}</div>
          </div>
          <div class="member-status" :class="{'is-online': member.userStateId === 1}">
            {{ member.userStateId === 1 ? '在线' : '离线' }}
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 确认对话框 -->
    <el-dialog
        title="确认操作"
        :visible.sync="confirmDialogVisible"
        width="300px"
        :append-to-body="true">
      <div class="confirm-content">
        <i class="el-icon-warning"></i>
        <p>{{ confirmMessage }}</p>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="confirmDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmAction">确认</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: "chattitle",
  props: {
    currentChat: {
      type: Object,
      default: null
    },
    isGroup: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      avatarError: false,
      defaultAvatar: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA0MCA0MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48Y2lyY2xlIGN4PSIyMCIgY3k9IjIwIiByPSIyMCIgZmlsbD0iI2U1ZTVlNSIvPjxjaXJjbGUgY3g9IjIwIiBjeT0iMTYiIHI9IjYiIGZpbGw9IiNhMGEwYTAiLz48cGF0aCBkPSJNMTAgMzJDMTAgMjUuMzcgMTQuNDc3IDIwIDIwIDIwQzI1LjUyMyAyMCAzMCAyNS4zNyAzMCAzMkg0MFY0MEgwVjMySzEwIDMyWiIgZmlsbD0iI2EwYTBhMCIvPjwvc3ZnPg==',
      defaultGroupAvatar: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA0MCA0MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48Y2lyY2xlIGN4PSIyMCIgY3k9IjIwIiByPSIyMCIgZmlsbD0iIzkyQkFERCIvPjxjaXJjbGUgY3g9IjE1IiBjeT0iMTUiIHI9IjQiIGZpbGw9IiNGRkZGRkYiLz48Y2lyY2xlIGN4PSIyNSIgY3k9IjE1IiByPSI0IiBmaWxsPSIjRkZGRkZGIi8+PGNpcmNsZSBjeD0iMTUiIGN5PSIyNSIgcj0iNCIgZmlsbD0iI0ZGRkZGRiIvPjxjaXJjbGUgY3g9IjI1IiBjeT0iMjUiIHI9IjQiIGZpbGw9IiNGRkZGRkYiLz48L3N2Zz4=',
      isOnline: false,
      memberCount: 0,
      searchDialogVisible: false,
      searchQuery: '',
      searchResults: [],
      hasSearched: false,
      membersDialogVisible: false,
      groupMembers: [],
      loadingMembers: false,
      confirmDialogVisible: false,
      confirmMessage: '',
      pendingAction: null,
      lastStatusCheck: 0
    }
  },
  computed: {
    chatDisplayName() {
      if (!this.currentChat) return '';

      // 对于群组聊天
      if (this.isGroup) {
        return this.currentChat.name || this.currentChat.nickname || `群聊(${this.currentChat.id})`;
      }

      // 对于私聊
      return this.currentChat.nickname || this.currentChat.username || '';
    },
    chatAvatar() {
      if (this.avatarError) return null;

      if (this.isGroup) {
        return this.currentChat?.avatar || this.defaultGroupAvatar;
      }

      return this.currentChat?.avatar || this.currentChat?.userProfile || this.defaultAvatar;
    },
    chatInitials() {
      const name = this.chatDisplayName;
      return name ? name.charAt(0).toUpperCase() : '?';
    },
    showStatus() {
      return !!this.currentChat;
    }
  },
  watch: {
    currentChat: {
      immediate: true,
      handler(newVal) {
        if (newVal) {
          this.avatarError = false;
          this.fetchChatInfo();
        }
      }
    }
  },
  mounted() {
    // 设置定时器定期检查聊天状态
    this.statusCheckInterval = setInterval(() => {
      this.fetchChatInfo();
    }, 60000); // 每分钟检查一次
  },
  beforeDestroy() {
    // 清除定时器
    if (this.statusCheckInterval) {
      clearInterval(this.statusCheckInterval);
    }
  },
  methods: {
    // 获取当前聊天的信息
    fetchChatInfo() {
      const now = Date.now();
      // 限制请求频率，至少间隔30秒
      if (now - this.lastStatusCheck < 30000) return;

      this.lastStatusCheck = now;

      if (!this.currentChat) return;

      if (this.isGroup) {
        this.fetchGroupInfo();
      } else {
        this.fetchUserStatus();
      }
    },

    // 获取群组信息
    fetchGroupInfo() {
      const groupId = this.currentChat.id;
      if (!groupId) return;

      this.getRequest(`/group/info?groupId=${groupId}`)
          .then(resp => {
            if (resp) {
              this.memberCount = resp.memberCount || 0;
            }
          })
          .catch(err => {
            console.error('[ChatTitle] 获取群组信息失败:', err);
          });
    },

    // 获取用户在线状态
    fetchUserStatus() {
      const userId = this.currentChat.id;
      if (!userId) return;

      this.postRequest(`/user-state`, { userId: userId })
          .then(resp => {
            if (resp) {
              this.isOnline = resp.userStateId === 1;
            }
          })
          .catch(err => {
            console.error('[ChatTitle] 获取用户状态失败:', err);
          });
    },

    // 处理头像加载错误
    handleAvatarError() {
      this.avatarError = true;
    },

    // 处理下拉菜单命令
    handleCommand(command) {
      console.log(`[ChatTitle] 执行命令: ${command}`);

      switch (command) {
        case 'viewMembers':
          this.showGroupMembers();
          break;
        case 'inviteFriend':
          this.showInviteFriendDialog();
          break;
        case 'groupSettings':
          this.showGroupSettings();
          break;
        case 'leaveGroup':
          this.confirmLeaveGroup();
          break;
        case 'viewProfile':
          this.viewUserProfile();
          break;
        case 'clearHistory':
          this.confirmClearHistory();
          break;
        case 'blockUser':
          this.confirmBlockUser();
          break;
        case 'removeFriend':
          this.confirmRemoveFriend();
          break;
      }
    },

    // 打开聊天记录搜索对话框
    openSearchHistory() {
      this.searchDialogVisible = true;
      this.searchQuery = '';
      this.searchResults = [];
      this.hasSearched = false;
    },

    // 搜索聊天消息
    searchMessages() {
      if (!this.searchQuery.trim()) {
        this.$message.warning('请输入搜索关键词');
        return;
      }

      this.hasSearched = true;
      this.searchResults = [];

      // 获取聊天会话键
      let sessionKey;
      if (this.isGroup) {
        sessionKey = `群聊_${this.currentChat.id}`;
      } else {
        const currentUser = this.$store.state.currentUser;
        sessionKey = `${currentUser.username}#${this.currentChat.username}`;
      }

      // 从store中获取消息
      const messages = this.$store.state.sessions[sessionKey] || [];

      // 搜索关键词
      const keyword = this.searchQuery.toLowerCase();
      this.searchResults = messages.filter(msg =>
          msg.content && msg.content.toLowerCase().includes(keyword)
      );

      // 根据时间排序，最新的在前
      this.searchResults.sort((a, b) =>
          new Date(b.createTime) - new Date(a.createTime)
      );

      if (this.searchResults.length === 0) {
        this.$message.info('没有找到匹配的聊天记录');
      }
    },

    // 高亮搜索结果中的关键词
    highlightContent(content) {
      if (!content) return '';

      const keyword = this.searchQuery;
      if (!keyword) return content;

      // 使用正则表达式进行大小写不敏感的替换
      const regex = new RegExp(`(${keyword})`, 'gi');
      return content.replace(regex, '<span class="highlight">$1</span>');
    },

    // 导航到指定消息（通知父组件滚动到该消息）
    navigateToMessage(message) {
      this.$emit('navigate-to-message', message);
      this.searchDialogVisible = false;
    },

    // 格式化时间
    formatTime(time) {
      if (!time) return '';

      const date = new Date(time);
      if (isNaN(date.getTime())) return '';

      const today = new Date();
      const yesterday = new Date(today);
      yesterday.setDate(yesterday.getDate() - 1);

      const isToday = date.toDateString() === today.toDateString();
      const isYesterday = date.toDateString() === yesterday.toDateString();

      if (isToday) {
        return `今天 ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`;
      } else if (isYesterday) {
        return `昨天 ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`;
      } else {
        return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`;
      }
    },

    // 显示群组成员列表
    showGroupMembers() {
      if (!this.isGroup || !this.currentChat.id) return;

      this.membersDialogVisible = true;
      this.loadingMembers = true;
      this.groupMembers = [];

      this.getRequest(`/group/members?groupId=${this.currentChat.id}`)
          .then(resp => {
            if (resp && Array.isArray(resp)) {
              this.groupMembers = resp;
            }
          })
          .catch(err => {
            console.error('[ChatTitle] 获取群组成员失败:', err);
            this.$message.error('获取群组成员失败');
          })
          .finally(() => {
            this.loadingMembers = false;
          });
    },

    // 显示邀请好友到群组对话框
    showInviteFriendDialog() {
      this.$bus.$emit('show-invite-friend-dialog', this.currentChat.id);
    },

    // 显示群组设置
    showGroupSettings() {
      this.$bus.$emit('show-group-settings', this.currentChat);
    },

    // 确认退出群组
    confirmLeaveGroup() {
      this.confirmMessage = '确定要退出该群组吗？';
      this.pendingAction = 'leaveGroup';
      this.confirmDialogVisible = true;
    },

    // 执行退出群组
    leaveGroup() {
      const userId = this.$store.state.currentUser?.id;
      const groupId = this.currentChat.id;

      if (!userId || !groupId) {
        this.$message.error('退出群组失败：缺少必要参数');
        return;
      }

      this.postRequest('/group/leave', {
        userId: userId,
        groupId: groupId
      })
          .then(resp => {
            if (resp && resp.status === 200) {
              this.$message.success('已成功退出群组');

              // 通知其他组件刷新群组列表
              this.$bus.$emit('refresh-group-list');

              // 切换到其他聊天对象或默认界面
              this.$bus.$emit('switch-to-default');

              // 切换到群组列表标签
              this.$bus.$emit('switch-to-groups');
            } else {
              this.$message.error(resp.msg || '退出群组失败');
            }
          })
          .catch(err => {
            console.error('[ChatTitle] 退出群组失败:', err);
            this.$message.error('退出群组失败，请稍后重试');
          });
    },

    // 查看用户资料
    viewUserProfile() {
      this.$bus.$emit('view-user-profile', this.currentChat);
    },

    // 确认清空聊天记录
    confirmClearHistory() {
      this.confirmMessage = '确定要清空与该好友的所有聊天记录吗？';
      this.pendingAction = 'clearHistory';
      this.confirmDialogVisible = true;
    },

    // 执行清空聊天记录
    clearHistory() {
      const currentUser = this.$store.state.currentUser;

      if (!currentUser || !this.currentChat) {
        this.$message.error('清空聊天记录失败：缺少必要参数');
        return;
      }

      // 获取会话键
      const sessionKey = `${currentUser.username}#${this.currentChat.username}`;

      // 清空本地存储的聊天记录
      if (this.$store.state.sessions[sessionKey]) {
        this.$set(this.$store.state.sessions, sessionKey, []);

        try {
          // 更新localStorage
          localStorage.setItem('chat-session', JSON.stringify(this.$store.state.sessions));

          // 通知消息组件刷新
          this.$bus.$emit('refresh-messages', true);

          this.$message.success('聊天记录已清空');
        } catch (e) {
          console.error('[ChatTitle] 清空聊天记录失败:', e);
          this.$message.error('清空聊天记录失败，请重试');
        }
      }
    },

    // 确认屏蔽用户
    confirmBlockUser() {
      this.confirmMessage = '确定要屏蔽该用户吗？屏蔽后将不再收到对方的消息';
      this.pendingAction = 'blockUser';
      this.confirmDialogVisible = true;
    },

    // 执行屏蔽用户
    blockUser() {
      const currentUser = this.$store.state.currentUser;
      const targetUserId = this.currentChat.id;

      if (!currentUser || !currentUser.id || !targetUserId) {
        this.$message.error('屏蔽用户失败：缺少必要参数');
        return;
      }

      this.postRequest('/user/block', {
        userId: currentUser.id,
        targetUserId: targetUserId
      })
          .then(resp => {
            if (resp && resp.status === 200) {
              this.$message.success('用户已被屏蔽');

              // 切换到默认界面
              this.$bus.$emit('switch-to-default');
            } else {
              this.$message.error(resp.msg || '屏蔽用户失败');
            }
          })
          .catch(err => {
            console.error('[ChatTitle] 屏蔽用户失败:', err);
            this.$message.error('屏蔽用户失败，请稍后重试');
          });
    },

    // 确认删除好友
    confirmRemoveFriend() {
      this.confirmMessage = '确定要删除该好友吗？删除后将无法恢复';
      this.pendingAction = 'removeFriend';
      this.confirmDialogVisible = true;
    },

    // 执行删除好友
    removeFriend() {
      const currentUser = this.$store.state.currentUser;
      const friendId = this.currentChat.id;

      if (!currentUser || !currentUser.id || !friendId) {
        this.$message.error('删除好友失败：缺少必要参数');
        return;
      }

      this.postRequest('/friend/remove', {
        userId: currentUser.id,
        friendId: friendId
      })
          .then(resp => {
            if (resp && resp.status === 200) {
              this.$message.success('好友已删除');

              // 通知其他组件刷新好友列表
              this.$bus.$emit('refresh-friend-list');

              // 切换到默认界面
              this.$bus.$emit('switch-to-default');
            } else {
              this.$message.error(resp.msg || '删除好友失败');
            }
          })
          .catch(err => {
            console.error('[ChatTitle] 删除好友失败:', err);
            this.$message.error('删除好友失败，请稍后重试');
          });
    },

    // 确认对话框的确认动作
    confirmAction() {
      this.confirmDialogVisible = false;

      switch (this.pendingAction) {
        case 'leaveGroup':
          this.leaveGroup();
          break;
        case 'clearHistory':
          this.clearHistory();
          break;
        case 'blockUser':
          this.blockUser();
          break;
        case 'removeFriend':
          this.removeFriend();
          break;
      }

      this.pendingAction = null;
    }
  }
}
</script>

<style lang="scss" scoped>
.chatTitle {
  height: 60px;
  width: 100%;
  font-size: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #e8e8e8;
  padding: 0 10px;
  background-color: #f9f9f9;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);

  .title-info {
    display: flex;
    align-items: center;

    .chat-avatar {
      margin-right: 10px;
    }

    .title-text {
      display: flex;
      flex-direction: column;

      .title-name {
        font-weight: 500;
        font-size: 16px;
        color: #333;
      }

      .title-status {
        font-size: 12px;
        color: #999;
        margin-top: 3px;

        .online {
          color: #67C23A;
        }

        .offline {
          color: #909399;
        }
      }
    }
  }

  .action-buttons {
    display: flex;
    gap: 5px;

    .action-btn {
      background-color: transparent;
      border: none;
      color: #606266;

      &:hover {
        background-color: #f0f0f0;
        color: #409EFF;
      }
    }
  }
}

.search-dialog-content {
  max-height: 400px;
  overflow-y: auto;

  .search-results {
    margin-top: 15px;

    .search-result-item {
      padding: 10px;
      border-bottom: 1px solid #f0f0f0;
      cursor: pointer;

      &:hover {
        background-color: #f9f9f9;
      }

      .result-sender {
        font-weight: 500;
        font-size: 14px;
        color: #333;
        margin-bottom: 5px;
      }

      .result-content {
        font-size: 14px;
        color: #606266;
        margin-bottom: 5px;

        .highlight {
          background-color: #FFE58F;
          padding: 0 2px;
          border-radius: 2px;
        }
      }

      .result-time {
        font-size: 12px;
        color: #909399;
        text-align: right;
      }
    }
  }

  .no-results {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 30px 0;
    color: #909399;

    i {
      font-size: 24px;
      margin-bottom: 10px;
    }

    p {
      margin: 0;
    }
  }
}

.members-list {
  max-height: 400px;
  overflow-y: auto;

  .member-item {
    display: flex;
    align-items: center;
    padding: 10px;
    border-bottom: 1px solid #f0f0f0;

    .member-info {
      flex: 1;
      margin-left: 10px;

      .member-name {
        font-weight: 500;
        font-size: 14px;
        color: #333;
      }

      .member-role {
        font-size: 12px;
        color: #909399;
      }
    }

    .member-status {
      font-size: 12px;
      color: #909399;

      &.is-online {
        color: #67C23A;
      }
    }
  }
}

.confirm-content {
  text-align: center;
  padding: 20px 0;

  i {
    font-size: 30px;
    color: #E6A23C;
    margin-bottom: 10px;
  }

  p {
    margin: 10px 0 0;
    font-size: 16px;
  }
}
</style>