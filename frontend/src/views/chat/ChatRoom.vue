<template>
  <div class="chat-container">
    <div id="app-chat">
      <div class="toolbar">
        <toolbar></toolbar>
      </div>
      <div class="sidebar">
        <card @show-add-dialog="showAddFriendDialog"></card>
        <list @chat-changed="handleChatChange" ref="chatList"></list>
      </div>
      <div class="main">
        <div v-if="currentChat" class="chat-area">
          <chattitle :current-chat="currentChat" :is-group="isChatGroup"></chattitle>

          <!-- 消息组件区域 -->
          <div class="message-wrapper">
            <direct-message
                :current-chat="currentChat"
                :is-group="isChatGroup"
                ref="messageComponent"
            ></direct-message>
          </div>

          <usertext :current-chat="currentChat" :is-group="isChatGroup" ref="usertextComponent"></usertext>
        </div>

        <div v-else class="empty-chat">
          <div class="welcome-message">
            <img src="@/assets/chat-icon.png" alt="Chat" class="welcome-icon" />
            <h2>欢迎使用聊天室</h2>
            <p>请从左侧选择一个聊天对象开始交谈</p>
            <el-button type="primary" @click="showAddFriendDialog">添加好友</el-button>
          </div>
        </div>
      </div>

      <!-- 连接状态悬浮提示 -->
      <div v-if="connectionStatusVisible" class="connection-status" :class="connectionStatusClass">
        {{ connectionStatusText }}
        <el-button v-if="canReconnect" size="mini" type="text" @click="reconnectWebSocket">重新连接</el-button>
      </div>
    </div>

    <friend-search :visible.sync="friendSearchVisible" @friend-added="onFriendAdded"></friend-search>

    <!-- 调试面板 -->
    <div v-if="isDebugMode" class="debug-panel">
      <div class="debug-header">
        <span>调试面板</span>
        <el-button size="mini" type="text" @click="isDebugMode = false">关闭</el-button>
      </div>
      <div class="debug-content">
        <p>WebSocket: {{ wsStatus }}</p>
        <p>当前聊天: {{ chatTypeText }}</p>
        <div class="debug-actions">
          <el-button size="mini" @click="ensureWebSocketConnection">连接WS</el-button>
          <el-button size="mini" @click="refreshAll">刷新数据</el-button>
          <el-button size="mini" @click="forceRenderMessages">测试消息</el-button>
        </div>
      </div>
    </div>

    <!-- 快捷键提示 -->
    <el-tooltip content="按下 Ctrl+D 打开调试面板" placement="top" :open-delay="500">
      <div class="debug-trigger" @click="toggleDebugMode"></div>
    </el-tooltip>
  </div>
</template>

<script>
import card from '../../components/chat/card'
import list from '../../components/chat/list.vue'
import DirectMessage from '../../components/chat/direct-message.vue'
import usertext from '../../components/chat/usertext.vue'
import toolbar from "../../components/chat/toolbar";
import chattitle from "../../components/chat/chattitle"
import FriendSearch from '@/components/chat/FriendSearch.vue';

export default {
  name: 'ChatRoom',
  components: {
    toolbar,
    card,
    list,
    DirectMessage,
    usertext,
    chattitle,
    FriendSearch
  },
  data() {
    return {
      currentChat: null,
      isChatGroup: false,
      friendSearchVisible: false,
      isDebugMode: false,
      connectionStatusVisible: false,
      reconnectAttempts: 0,
      connectionCheckTimer: null,
      lastConnectionCheck: 0,
      chatInitialized: false
    }
  },
  computed: {
    wsStatus() {
      const state = this.$store.state.connectionState;
      const stomp = this.$store.state.stomp;

      if (stomp && stomp.connected) {
        return '已连接';
      }

      return state || '未连接';
    },

    chatTypeText() {
      if (!this.currentChat) return '无';
      return this.isChatGroup
          ? `群聊: ${this.currentChat.name || this.currentChat.id}`
          : `私聊: ${this.currentChat.nickname || this.currentChat.username}`;
    },

    connectionStatusClass() {
      return {
        'status-connected': this.$store.state.connectionState === 'connected',
        'status-connecting': this.$store.state.connectionState === 'connecting',
        'status-disconnected': this.$store.state.connectionState === 'disconnected',
        'status-error': this.$store.state.lastConnectionError
      };
    },

    connectionStatusText() {
      switch(this.$store.state.connectionState) {
        case 'connected': return '聊天服务已连接';
        case 'connecting': return '正在连接聊天服务...';
        case 'disconnecting': return '正在断开聊天服务...';
        case 'disconnected': return '聊天服务未连接，部分功能可能不可用';
        default: return '聊天服务状态未知';
      }
    },

    canReconnect() {
      return this.$store.state.connectionState === 'disconnected' ||
          this.$store.state.lastConnectionError;
    }
  },
  watch: {
    // 监听连接状态变化
    '$store.state.connectionState'(newState) {
      if (newState === 'connected') {
        // 连接成功，显示成功提示，短暂后消失
        this.connectionStatusVisible = true;
        setTimeout(() => {
          this.connectionStatusVisible = false;
        }, 3000);

        // 重置重连尝试次数
        this.reconnectAttempts = 0;

        // 确保消息组件刷新
        this.ensureMessagesRefresh();
      } else if (newState === 'disconnected' || newState === 'connecting') {
        // 连接断开或正在连接，显示状态提示
        this.connectionStatusVisible = true;
      }
    }
  },
  created() {
    // 在页面刷新时将vuex里的最新信息保存到sessionStorage里
    window.addEventListener("beforeunload", () => {
      sessionStorage.setItem("state", JSON.stringify(this.$store.state));
    });

    // 添加键盘快捷键监听
    document.addEventListener('keydown', this.handleKeyDown);
  },
  mounted() {
    console.log('[ChatRoom] 组件挂载开始');

    // 初始化数据
    this.$store.commit('initData');

    // 连接WebSocket服务
    this.ensureWebSocketConnection();

    // 加载好友和群组列表
    this.loadFriends();
    this.loadGroups();

    // 全局事件监听
    this.$root.$on('global-add-friend-dialog', () => {
      console.log('[ChatRoom] 通过全局事件触发显示添加好友对话框');
      this.friendSearchVisible = true;
    });

    // 初始化确保 store 中的必要属性存在
    if (!this.$store.state.sessions) {
      this.$store.state.sessions = {};
    }

    if (!this.$store.state.currentSession) {
      this.$store.state.currentSession = {};
    }

    // 监听新消息事件，确保滚动到底部
    this.$bus.$on('new-message', this.handleNewMessage);

    // 创建全局滚动方法
    this.$root.$scrollMessagesToBottom = this.scrollMessagesToBottom;

    // 添加一个全局方法，用于确保消息显示
    this.$root.$ensureMessagesVisible = this.ensureMessagesRefresh;

    console.log('[ChatRoom] 组件挂载完成');

    // 设置定期检查WebSocket连接状态
    this.connectionCheckTimer = setInterval(() => this.checkConnection(), 30000);

    // 标记初始化完成
    this.chatInitialized = true;
  },
  methods: {
    // 处理新消息事件
    handleNewMessage(eventData) {
      // 滚动到消息底部
      this.scrollMessagesToBottom();

      // 如果有错过的消息提醒，短暂显示连接状态
      if (eventData && eventData.missed) {
        this.connectionStatusVisible = true;
        setTimeout(() => {
          this.connectionStatusVisible = false;
        }, 3000);
      }
    },

    // 确保WebSocket连接
    ensureWebSocketConnection() {
      console.log('[ChatRoom] 检查WebSocket连接状态');

      // 如果已经连接，无需操作
      if (this.$store.state.stomp && this.$store.state.stomp.connected) {
        console.log('[ChatRoom] WebSocket已连接');
        return Promise.resolve();
      }

      console.log('[ChatRoom] 尝试建立WebSocket连接');
      this.connectionStatusVisible = true;

      return this.$store.dispatch('connect')
          .then(() => {
            console.log('[ChatRoom] WebSocket连接成功');

            // 短暂显示成功提示，然后隐藏
            setTimeout(() => {
              this.connectionStatusVisible = false;
            }, 3000);

            // 重置重连尝试次数
            this.reconnectAttempts = 0;

            // 确保消息显示
            this.ensureMessagesRefresh();

            return true;
          })
          .catch(err => {
            console.error('[ChatRoom] WebSocket连接失败:', err);

            // 保持显示错误状态
            this.connectionStatusVisible = true;

            // 增加重连尝试次数
            this.reconnectAttempts++;

            // 如果重试次数较少，自动尝试重连
            if (this.reconnectAttempts < 3) {
              console.log(`[ChatRoom] ${5 * this.reconnectAttempts}秒后自动重试连接`);
              setTimeout(() => this.reconnectWebSocket(), 5000 * this.reconnectAttempts);
            } else {
              this.$message.error('聊天服务连接失败，请点击"重新连接"或刷新页面');
            }

            return false;
          });
    },

    // 重新连接WebSocket
    reconnectWebSocket() {
      console.log('[ChatRoom] 手动触发重新连接WebSocket');

      if (this.$store.state.stomp && this.$store.state.stomp.connected) {
        // 先断开现有连接
        this.$store.dispatch('disconnect').finally(() => {
          // 然后重新连接
          this.ensureWebSocketConnection();
        });
      } else {
        // 直接连接
        this.ensureWebSocketConnection();
      }
    },

    // 定期检查WebSocket连接
    checkConnection() {
      // 避免短时间内多次检查
      const now = Date.now();
      if (now - this.lastConnectionCheck < 10000) {
        return;
      }

      this.lastConnectionCheck = now;

      // 如果未连接，尝试重连
      if (!this.$store.state.stomp || !this.$store.state.stomp.connected) {
        console.log('[ChatRoom] 定期检查发现WebSocket未连接，尝试重连');
        this.ensureWebSocketConnection();
      }
    },

    // 滚动消息到底部
    scrollMessagesToBottom() {
      this.$nextTick(() => {
        if (this.$refs.messageComponent && typeof this.$refs.messageComponent.scrollToBottom === 'function') {
          this.$refs.messageComponent.scrollToBottom();
        }
      });
    },

    // 确保消息组件刷新
    ensureMessagesRefresh() {
      this.$nextTick(() => {
        if (!this.currentChat) return;

        if (this.$refs.messageComponent && typeof this.$refs.messageComponent.refreshMessages === 'function') {
          console.log('[ChatRoom] 刷新消息组件');
          this.$refs.messageComponent.refreshMessages(true);

          // 滚动到底部
          this.scrollMessagesToBottom();
        }
      });
    },

    // 强制渲染测试消息
    forceRenderMessages() {
      if (!this.currentChat) {
        this.$message.warning('请先选择一个聊天对象');
        return;
      }

      if (this.$refs.messageComponent && typeof this.$refs.messageComponent.forceRender === 'function') {
        this.$refs.messageComponent.forceRender();
        this.$message.success('已添加测试消息');
      } else {
        this.$message.error('消息组件未就绪');
      }
    },

    // 处理聊天对象变更
    handleChatChange(chatInfo) {
      console.group('[ChatRoom] 聊天对象变更');
      console.log('新聊天对象:', chatInfo);

      if (chatInfo.type === 'group') {
        // 切换到群聊
        this.currentChat = chatInfo.data;
        this.isChatGroup = true;
        console.log('设置为群聊, ID:', this.currentChat.id);

        // 更新Vuex中的当前群组ID
        if (this.currentChat && this.currentChat.id) {
          console.log('设置当前群组ID:', this.currentChat.id);
          this.$store.commit('setCurrentGroupId', this.currentChat.id);
        }
      } else {
        // 切换到好友聊天
        this.currentChat = chatInfo.data;
        this.isChatGroup = false;
        console.log('设置为私聊, 用户名:', this.currentChat.username);

        // 清除当前群组ID
        this.$store.commit('setCurrentGroupId', null);
      }

      console.log('当前聊天对象属性:', Object.keys(this.currentChat || {}));
      console.groupEnd();

      // 确保消息组件刷新
      this.$nextTick(() => {
        this.ensureMessagesRefresh();
      });
    },

    // 显示添加好友对话框
    showAddFriendDialog() {
      console.log('[ChatRoom] 显示添加好友对话框');
      this.friendSearchVisible = true;
    },

    // 好友添加成功处理
    onFriendAdded(friend) {
      console.log('[ChatRoom] 添加好友成功:', friend);

      // 刷新好友列表
      this.loadFriends();

      // 显示成功提示
      this.$message.success(`已成功添加好友: ${friend.nickname || friend.username}`);

      // 可选：自动切换到新添加的好友
      if (friend) {
        this.$nextTick(() => {
          if (this.$refs.chatList && typeof this.$refs.chatList.selectFriend === 'function') {
            this.$refs.chatList.selectFriend(friend);
          }
        });
      }
    },

    // 加载好友列表
    loadFriends() {
      console.log('[ChatRoom] 开始加载好友列表');

      if (!this.$store.state.currentUser || !this.$store.state.currentUser.id) {
        console.error('[ChatRoom] 当前用户信息不完整，无法加载好友列表');
        return Promise.reject(new Error('用户信息不完整'));
      }

      const userId = this.$store.state.currentUser.id;
      return this.getRequest('/friend/list?userId=' + userId)
          .then(resp => {
            if (resp) {
              // 处理可能的空响应
              const friendList = Array.isArray(resp) ? resp : [];
              console.log(`[ChatRoom] 好友列表加载成功，共 ${friendList.length} 个好友`);

              // 更新Vuex store中的好友列表
              this.$store.commit('setFriendList', friendList);

              return friendList;
            }
            return [];
          })
          .catch(err => {
            console.error('[ChatRoom] 获取好友列表失败:', err);
            return Promise.reject(err);
          });
    },

    // 加载群组列表
    loadGroups() {
      console.log('[ChatRoom] 开始加载群组列表');

      if (!this.$store.state.currentUser || !this.$store.state.currentUser.id) {
        console.error('[ChatRoom] 当前用户信息不完整，无法加载群组列表');
        return Promise.reject(new Error('用户信息不完整'));
      }

      const userId = this.$store.state.currentUser.id;
      return this.getRequest('/group/list?userId=' + userId)
          .then(resp => {
            if (resp && Array.isArray(resp)) {
              console.log(`[ChatRoom] 群组列表加载成功，共 ${resp.length} 个群组`);

              // 更新Vuex store中的群组列表
              this.$store.commit('setGroups', resp);

              return resp;
            }
            return [];
          })
          .catch(err => {
            console.error('[ChatRoom] 获取群组列表失败:', err);
            return Promise.reject(err);
          });
    },

    // 刷新所有数据
    refreshAll() {
      this.$message.info('正在刷新数据...');

      // 并行加载好友和群组列表
      Promise.all([
        this.loadFriends(),
        this.loadGroups()
      ])
          .then(() => {
            this.$message.success('数据刷新成功');

            // 如果列表组件存在刷新方法，也调用它
            if (this.$refs.chatList) {
              if (typeof this.$refs.chatList.loadFriendList === 'function') {
                this.$refs.chatList.loadFriendList();
              }
              if (typeof this.$refs.chatList.loadGroupList === 'function') {
                this.$refs.chatList.loadGroupList();
              }
            }

            // 刷新消息
            this.ensureMessagesRefresh();
          })
          .catch(err => {
            this.$message.error('数据刷新失败: ' + (err.message || '未知错误'));
          });
    },

    // 切换调试模式
    toggleDebugMode() {
      this.isDebugMode = !this.isDebugMode;
    },

    // 键盘快捷键处理
    handleKeyDown(e) {
      // Ctrl+D 切换调试面板
      if (e.ctrlKey && e.key === 'd') {
        e.preventDefault();
        this.toggleDebugMode();
      }
    }
  },
  beforeDestroy() {
    console.log('[ChatRoom] 组件销毁中');

    // 清除全局事件监听
    this.$root.$off('global-add-friend-dialog');
    this.$bus.$off('new-message', this.handleNewMessage);

    // 清除定时器
    if (this.connectionCheckTimer) {
      clearInterval(this.connectionCheckTimer);
    }

    // 移除键盘事件监听
    document.removeEventListener('keydown', this.handleKeyDown);

    // 断开WebSocket连接
    if (this.chatInitialized) {
      this.$store.dispatch('disconnect');
    }

    console.log('[ChatRoom] 组件销毁完成');
  }
}
</script>

<style lang="scss" scoped>
.chat-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  overflow: hidden;
  position: relative;
}

#app-chat {
  margin: 20px auto;
  width: 900px;
  height: 650px;
  overflow: hidden;
  border-radius: 10px;
  display: flex;
  box-shadow: 0 2px 10px rgba(0,0,0,0.2);
  position: relative;

  .toolbar {
    color: #f4f4f4;
    background-color: #2e3238;
    width: 60px;
    height: 100%;
  }

  .sidebar {
    color: #000000;
    background-color: #ECEAE8;
    width: 240px;
    height: 100%;
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }

  .main {
    position: relative;
    flex: 1;
    background-color: #eee;
    display: flex;
    flex-direction: column;
    height: 100%;
    overflow: hidden;

    .chat-area {
      display: flex;
      flex-direction: column;
      height: 100%;
    }

    .message-wrapper {
      flex: 1;
      overflow: hidden;
      position: relative;
    }
  }

  .empty-chat {
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    height: 100%;
    background-color: #f8f8f8;

    .welcome-message {
      text-align: center;

      .welcome-icon {
        width: 80px;
        height: 80px;
        margin-bottom: 20px;
        opacity: 0.7;
      }

      h2 {
        color: #333;
        margin-bottom: 10px;
      }

      p {
        color: #666;
        margin-bottom: 20px;
      }
    }
  }

  .connection-status {
    position: absolute;
    top: 10px;
    left: 50%;
    transform: translateX(-50%);
    padding: 8px 16px;
    border-radius: 4px;
    font-size: 14px;
    display: flex;
    align-items: center;
    z-index: 100;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
    transition: all 0.3s;

    &.status-connected {
      background-color: #f0f9eb;
      color: #67c23a;
    }

    &.status-connecting {
      background-color: #e6f7ff;
      color: #1890ff;
    }

    &.status-disconnected, &.status-error {
      background-color: #fff2f0;
      color: #f56c6c;
    }

    .el-button--text {
      margin-left: 10px;
      padding: 0 5px;
    }
  }
}

.debug-panel {
  position: absolute;
  bottom: 20px;
  right: 20px;
  background-color: rgba(0, 0, 0, 0.7);
  border-radius: 4px;
  color: white;
  padding: 8px;
  width: 250px;
  z-index: 1000;

  .debug-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 5px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.3);
    margin-bottom: 5px;

    span {
      font-weight: bold;
    }
  }

  .debug-content {
    p {
      margin: 5px 0;
      font-size: 12px;
    }

    .debug-actions {
      display: flex;
      justify-content: space-around;
      margin-top: 10px;
    }
  }
}

.debug-trigger {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 20px;
  height: 20px;
  cursor: pointer;
}
</style>