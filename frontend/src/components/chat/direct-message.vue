<template>
  <div class="direct-message-container">
    <!-- 调试控制面板 - 仅在调试模式下显示 -->
    <div v-if="debug" class="debug-panel">
      <div class="debug-header">
        <span>消息组件调试面板</span>
        <el-button size="mini" type="text" icon="el-icon-close" @click="debug = false"></el-button>
      </div>

      <div class="debug-controls">
        <el-button size="mini" type="primary" @click="refreshMessages(true)">刷新消息</el-button>
        <el-button size="mini" type="warning" @click="forceRender">测试消息</el-button>
        <el-button size="mini" type="success" @click="debugSendMessage">发送/接收</el-button>
        <el-button size="mini" type="danger" @click="enableFallbackMode">应急模式</el-button>
      </div>

      <div class="debug-info">
        <div><b>当前用户:</b> {{ currentUser.username }}</div>
        <div><b>聊天对象:</b> {{ currentChat ? (isGroup ? '群聊-' + currentChat.name : '私聊-' + currentChat.username) : '无' }}</div>
        <div><b>消息数:</b> 常规({{ allMessages.length }}) + 注入({{ injectedMessages.length }})</div>
        <div><b>当前键:</b> {{ messageKey }}</div>
        <div><b>WS状态:</b> {{ wsStatus }}</div>
      </div>
    </div>

    <!-- 消息记录为空时的提示 -->
    <div v-if="renderMessages.length === 0" class="empty-messages">
      <div class="empty-icon">
        <i class="el-icon-chat-dot-round"></i>
      </div>
      <p class="empty-text">{{ emptyMessage }}</p>
      <el-button
          v-if="showEmptyAction"
          size="small"
          type="primary"
          @click="handleEmptyAction">
        {{ emptyActionText }}
      </el-button>
    </div>

    <!-- 加载更多按钮 -->
    <div v-if="renderMessages.length > 0 && hasMoreMessages" class="load-more">
      <el-button
          size="small"
          plain
          :loading="loadingMore"
          @click="loadMoreMessages">
        加载更多消息
      </el-button>
    </div>

    <!-- 消息列表 -->
    <div class="message-list" ref="messageList" @scroll="handleScroll">
      <!-- 常规消息展示 -->
      <div v-for="(msg, index) in renderMessages"
           :key="msg.uniqueId || `msg-${index}`"
           class="message-item"
           :class="{
             'self-message': msg.self || msg.isSelf,
             'system-message': msg.isSystem,
             'date-divider': msg.isDateDivider
           }"
           :id="`msg-${msg.uniqueId}`">
        <!-- 日期分隔线 -->
        <div v-if="msg.isDateDivider" class="date-divider-content">
          <span>{{ msg.content }}</span>
        </div>

        <!-- 系统消息 -->
        <div v-else-if="msg.isSystem" class="system-message-content">
          <i class="el-icon-info"></i>
          <span>{{ msg.content }}</span>
        </div>

        <!-- 正常消息 -->
        <template v-else>
          <!-- 对方消息 -->
          <template v-if="!(msg.self || msg.isSelf)">
            <div class="avatar-container">
              <el-avatar
                  :size="40"
                  :src="getAvatarUrl(msg.fromProfile)"
                  @error="() => avatarErrorHandler(msg)">
                {{ getInitials(msg) }}
              </el-avatar>
            </div>
            <div class="message-content" :class="{ 'image-message': isImageMessage(msg) }">
              <div class="message-header">
                <span class="nickname">{{ msg.fromName || msg.sender || msg.from || '未知用户' }}</span>
                <span class="time">{{ formatTime(msg.createTime || msg.time) }}</span>
              </div>
              <div class="content" v-if="!isImageMessage(msg)">{{ msg.content }}</div>
              <div class="image-container" v-else>
                <el-image
                    :src="getImageUrl(msg.content)"
                    :preview-src-list="[getImageUrl(msg.content)]"
                    fit="cover"
                    lazy>
                  <div slot="error" class="image-error">
                    <i class="el-icon-picture-outline"></i>
                    <span>图片加载失败</span>
                  </div>
                </el-image>
              </div>
              <div class="message-status" v-if="msg.status">
                <i :class="getStatusIcon(msg.status)"></i>
                <span>{{ getStatusText(msg.status) }}</span>
              </div>
            </div>
          </template>

          <!-- 自己发送的消息 -->
          <template v-else>
            <div class="message-content self-content" :class="{ 'image-message': isImageMessage(msg) }">
              <div class="message-header">
                <span class="time">{{ formatTime(msg.createTime || msg.time) }}</span>
              </div>
              <div class="content" v-if="!isImageMessage(msg)">{{ msg.content }}</div>
              <div class="image-container" v-else>
                <el-image
                    :src="getImageUrl(msg.content)"
                    :preview-src-list="[getImageUrl(msg.content)]"
                    fit="cover"
                    lazy>
                  <div slot="error" class="image-error">
                    <i class="el-icon-picture-outline"></i>
                    <span>图片加载失败</span>
                  </div>
                </el-image>
              </div>
              <div class="message-status" v-if="msg.status">
                <i :class="getStatusIcon(msg.status)"></i>
                <span>{{ getStatusText(msg.status) }}</span>
              </div>
            </div>
            <div class="avatar-container">
              <el-avatar
                  :size="40"
                  :src="getAvatarUrl(msg.fromProfile || currentUser.userProfile)"
                  @error="() => avatarErrorHandler(msg, true)">
                {{ getInitials(msg, true) }}
              </el-avatar>
            </div>
          </template>
        </template>
      </div>
    </div>

    <!-- 应急聊天模式界面 -->
    <div v-if="fallbackModeEnabled" class="fallback-chat">
      <div class="fallback-header">
        <el-alert type="warning" show-icon :closable="false">
          紧急聊天模式 (不依赖WebSocket)
          <template slot="title">
            <div class="fallback-title">
              <span>紧急聊天模式</span>
              <el-button size="mini" type="text" @click="disableFallbackMode">退出</el-button>
            </div>
          </template>
        </el-alert>
      </div>

      <div class="fallback-messages" ref="fallbackMessages">
        <div v-for="(msg, index) in fallbackMessages" :key="'fb-'+index"
             class="message-item" :class="{'self-message': msg.isSelf}">
          <template v-if="!msg.isSelf">
            <div class="avatar-container">
              <el-avatar :size="40" :src="msg.avatar">
                {{ msg.sender ? msg.sender.substring(0,1).toUpperCase() : '?' }}
              </el-avatar>
            </div>
            <div class="message-content">
              <div class="message-header">
                <span class="nickname">{{ msg.sender }}</span>
                <span class="time">{{ formatTime(msg.time) }}</span>
              </div>
              <div class="content">{{ msg.content }}</div>
            </div>
          </template>
          <template v-else>
            <div class="message-content self-content">
              <div class="message-header">
                <span class="time">{{ formatTime(msg.time) }}</span>
              </div>
              <div class="content">{{ msg.content }}</div>
            </div>
            <div class="avatar-container">
              <el-avatar :size="40" :src="getAvatarUrl(currentUser.userProfile)">
                {{ currentUser.username ? currentUser.username.substring(0,1).toUpperCase() : '?' }}
              </el-avatar>
            </div>
          </template>
        </div>
      </div>

      <div class="fallback-input">
        <el-input
            v-model="fallbackInput"
            type="textarea"
            :rows="2"
            placeholder="输入消息..."
            @keyup.enter.native="sendFallbackMessage"
            resize="none">
        </el-input>
        <el-button type="primary" @click="sendFallbackMessage" :disabled="!fallbackInput.trim()">
          发送
        </el-button>
      </div>
    </div>

    <!-- 新消息提示 -->
    <transition name="fade">
      <div v-if="showNewMessageAlert" class="new-message-alert" @click="scrollToBottom">
        <i class="el-icon-arrow-down"></i> 新消息
      </div>
    </transition>

    <!-- 连接状态提示 -->
    <transition name="fade">
      <div v-if="showConnectionAlert" class="connection-alert" :class="connectionAlertClass">
        <i :class="connectionIconClass"></i> {{ connectionStatusText }}
        <el-button v-if="connectionStatus === 'disconnected'" size="mini" type="text" @click="reconnectWebSocket">
          重新连接
        </el-button>
      </div>
    </transition>
  </div>
</template>

<script>
export default {
  name: 'DirectMessage',
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
      allMessages: [], // 常规渲染的消息
      injectedMessages: [], // 直接注入的消息
      debug: false, // 默认关闭调试模式
      defaultAvatar: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA0MCA0MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48Y2lyY2xlIGN4PSIyMCIgY3k9IjIwIiByPSIyMCIgZmlsbD0iI2U1ZTVlNSIvPjxjaXJjbGUgY3g9IjIwIiBjeT0iMTYiIHI9IjYiIGZpbGw9IiNhMGEwYTAiLz48cGF0aCBkPSJNMTAgMzJDMTAgMjUuMzcgMTQuNDc3IDIwIDIwIDIwQzI1LjUyMyAyMCAzMCAyNS4zNyAzMCAzMkg0MFY0MEgwVjMySzEwIDMyWiIgZmlsbD0iI2EwYTBhMCIvPjwvc3ZnPg==',
      defaultGroupAvatar: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA0MCA0MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48Y2lyY2xlIGN4PSIyMCIgY3k9IjIwIiByPSIyMCIgZmlsbD0iIzkyQkFERCIvPjxjaXJjbGUgY3g9IjE1IiBjeT0iMTUiIHI9IjQiIGZpbGw9IiNGRkZGRkYiLz48Y2lyY2xlIGN4PSIyNSIgY3k9IjE1IiByPSI0IiBmaWxsPSIjRkZGRkZGIi8+PGNpcmNsZSBjeD0iMTUiIGN5PSIyNSIgcj0iNCIgZmlsbD0iI0ZGRkZGRiIvPjxjaXJjbGUgY3g9IjI1IiBjeT0iMjUiIHI9IjQiIGZpbGw9IiNGRkZGRkYiLz48L3N2Zz4=',
      messageKey: '', // 当前消息键
      searchedKeys: new Set(), // 已经搜索过的键
      messageCache: new Map(), // 消息缓存，避免重复

      // 应急模式
      fallbackModeEnabled: false,
      fallbackInput: '',
      fallbackMessages: [],

      // 滚动相关
      isUserScrolling: false,
      scrollTimer: null,
      showNewMessageAlert: false,
      isAtBottom: true,

      // 加载更多
      loadingMore: false,
      hasMoreMessages: false,
      pageSize: 20,
      currentPage: 1,

      // 连接状态提示
      showConnectionAlert: false,
      connectionAlertTimer: null,

      // 键盘快捷键监听
      keyboardListener: null,

      // 图像消息正则
      imagePattern: /^(https?:\/\/[^\s]+\.(jpg|jpeg|png|gif|webp)|data:image\/(jpeg|png|gif|webp);base64,[a-zA-Z0-9+/=]+)$/i
    }
  },
  computed: {
    currentUser() {
      return this.$store.state.currentUser || {};
    },

    // 计算WebSocket连接状态
    wsStatus() {
      const connectionState = this.$store.state.connectionState;
      if (connectionState === 'connected') return '已连接';
      if (connectionState === 'connecting') return '连接中';
      if (connectionState === 'disconnected') return '未连接';
      return '未知';
    },

    // 获取连接状态
    connectionStatus() {
      return this.$store.state.connectionState || 'unknown';
    },

    // 连接状态文本
    connectionStatusText() {
      switch (this.connectionStatus) {
        case 'connected': return '聊天服务已连接';
        case 'connecting': return '正在连接聊天服务...';
        case 'disconnected': return '聊天服务已断开';
        default: return '聊天服务状态未知';
      }
    },

    // 连接状态图标
    connectionIconClass() {
      switch (this.connectionStatus) {
        case 'connected': return 'el-icon-success';
        case 'connecting': return 'el-icon-loading';
        case 'disconnected': return 'el-icon-error';
        default: return 'el-icon-question';
      }
    },

    // 连接状态样式
    connectionAlertClass() {
      switch (this.connectionStatus) {
        case 'connected': return 'status-success';
        case 'connecting': return 'status-warning';
        case 'disconnected': return 'status-error';
        default: return '';
      }
    },

    // 计算所有可能的私聊消息键
    possibleMessageKeys() {
      if (!this.currentChat || !this.currentUser.username) {
        return [];
      }

      if (this.isGroup) {
        return [`群聊_${this.currentChat.id}`];
      }

      const currentUsername = this.currentUser.username;
      const chatUsername = this.currentChat.username;

      return [
        `${currentUsername}#${chatUsername}`,
        `${chatUsername}#${currentUsername}`,
        `${currentUsername}_${chatUsername}`,
        `${chatUsername}_${currentUsername}`,
        `chat_${currentUsername}_${chatUsername}`,
        `chat_${chatUsername}_${currentUsername}`
      ];
    },

    // 合并所有消息并排序
    renderMessages() {
      // 合并常规消息和注入消息
      let allMsgs = [
        ...this.allMessages.map(msg => ({
          ...msg,
          uniqueId: `reg-${msg.id || msg.messageId || Date.now() + Math.random()}`,
          timestamp: new Date(msg.createTime || Date.now()).getTime()
        })),
        ...this.injectedMessages.map(msg => ({
          ...msg,
          uniqueId: `inj-${msg.id || Date.now() + Math.random()}`,
          timestamp: new Date(msg.time || Date.now()).getTime()
        }))
      ];

      // 按时间排序
      allMsgs = allMsgs.sort((a, b) => a.timestamp - b.timestamp);

      // 插入日期分隔符
      const result = [];
      let lastDate = null;

      allMsgs.forEach(msg => {
        const msgDate = new Date(msg.createTime || msg.time || Date.now());
        const dateStr = this.formatDate(msgDate);

        if (dateStr !== lastDate) {
          result.push({
            uniqueId: `date-${dateStr}`,
            content: dateStr,
            isDateDivider: true,
            timestamp: msgDate.getTime()
          });
          lastDate = dateStr;
        }

        result.push(msg);
      });

      return result;
    },

    // 空消息提示
    emptyMessage() {
      if (!this.currentChat) {
        return '请选择一个聊天对象';
      }

      if (this.isGroup) {
        return `群聊 "${this.currentChat.name || this.currentChat.nickname}" 还没有任何消息，来发送第一条消息吧！`;
      } else {
        return `与 ${this.currentChat.nickname || this.currentChat.username} 的聊天还没有任何消息，来开始对话吧！`;
      }
    },

    // 是否显示空消息操作按钮
    showEmptyAction() {
      return !!this.currentChat;
    },

    // 空消息操作按钮文本
    emptyActionText() {
      return '发送问候';
    }
  },
  watch: {
    // 当聊天对象改变时刷新消息
    currentChat: {
      handler(newChat, oldChat) {
        if (newChat) {
          console.log('[DirectMessage] 聊天对象改变:', newChat);

          // 如果是完全不同的聊天对象
          if (!oldChat || newChat.id !== oldChat.id) {
            // 清空当前消息和注入消息
            this.allMessages = [];
            this.injectedMessages = [];
            this.searchedKeys.clear();
            this.currentPage = 1;
          }

          this.$nextTick(() => {
            this.refreshMessages(true);
          });
        }
      },
      immediate: true,
      deep: true
    },

    // 监听store中的sessions变化
    '$store.state.sessions': {
      handler() {
        this.refreshMessages();
      },
      deep: true
    },

    // 监听WebSocket连接状态变化
    connectionStatus(newStatus, oldStatus) {
      if (newStatus !== oldStatus) {
        // 显示连接状态提示
        this.showConnectionAlert = true;

        // 清除旧的定时器
        if (this.connectionAlertTimer) {
          clearTimeout(this.connectionAlertTimer);
        }

        // 如果是已连接状态，3秒后自动隐藏提示
        if (newStatus === 'connected') {
          this.connectionAlertTimer = setTimeout(() => {
            this.showConnectionAlert = false;
          }, 3000);
        }

        // 如果从断开连接状态切换到已连接状态
        if (oldStatus === 'disconnected' && newStatus === 'connected') {
          // 刷新消息
          this.refreshMessages(true);
        }
      }
    },

    // 监听渲染消息变化，处理新消息提示
    renderMessages: {
      handler(newMessages, oldMessages) {
        if (newMessages.length > oldMessages.length && !this.isAtBottom && !this.isUserScrolling) {
          this.showNewMessageAlert = true;
        }
      },
      deep: false
    }
  },
  mounted() {
    // 初始加载消息
    this.refreshMessages(true);

    // 监听新消息事件
    this.$bus.$on('new-message', this.onNewMessage);

    // 监听注入消息事件
    this.$bus.$on('inject-message', this.injectMessage);

    // 监听手动刷新消息事件
    this.$bus.$on('refresh-messages', this.refreshMessages);

    // 监听导航到消息事件
    this.$bus.$on('navigate-to-message', this.navigateToMessage);

    // 为全局添加滚动方法
    this.$root.$scrollMessagesToBottom = this.scrollToBottom;

    // 添加全局重新加载消息的方法
    this.$root.$refreshMessages = (force) => this.refreshMessages(force);

    // 监听键盘快捷键
    this.keyboardListener = (e) => {
      // Ctrl+D 切换调试面板
      if (e.ctrlKey && e.key === 'd') {
        e.preventDefault();
        this.debug = !this.debug;
      }
    };
    document.addEventListener('keydown', this.keyboardListener);

    // 检查连接状态
    if (this.$store.state.connectionState === 'disconnected') {
      this.showConnectionAlert = true;
    }
  },
  beforeDestroy() {
    // 移除事件监听
    this.$bus.$off('new-message', this.onNewMessage);
    this.$bus.$off('inject-message', this.injectMessage);
    this.$bus.$off('refresh-messages', this.refreshMessages);
    this.$bus.$off('navigate-to-message', this.navigateToMessage);

    // 清除定时器
    if (this.scrollTimer) {
      clearTimeout(this.scrollTimer);
    }

    if (this.connectionAlertTimer) {
      clearTimeout(this.connectionAlertTimer);
    }

    // 移除键盘监听
    if (this.keyboardListener) {
      document.removeEventListener('keydown', this.keyboardListener);
    }
  },
  methods: {
    // 获取有效的头像URL
    getAvatarUrl(profile) {
      if (!profile || profile === 'null' || profile === 'undefined') {
        // 返回默认头像
        return this.isGroup ? this.defaultGroupAvatar : this.defaultAvatar;
      }

      if (profile.startsWith('http') || profile.startsWith('data:')) {
        return profile;
      }

      // 处理相对路径
      return `/img/${profile}`;
    },

    // 头像加载错误处理
    avatarErrorHandler(msg, isSelf = false) {
      console.log('[DirectMessage] 头像加载错误');
      return this.isGroup ? this.defaultGroupAvatar : this.defaultAvatar;
    },

    // 获取消息发送者首字母
    getInitials(msg, isSelf = false) {
      if (isSelf) {
        return this.currentUser.nickname ? this.currentUser.nickname.charAt(0).toUpperCase() :
            this.currentUser.username ? this.currentUser.username.charAt(0).toUpperCase() : '?';
      }

      const name = msg.fromName || msg.sender || msg.from || '';
      return name ? name.charAt(0).toUpperCase() : '?';
    },

    // 判断是否为图片消息
    isImageMessage(msg) {
      const content = msg.content || '';
      return this.imagePattern.test(content.trim());
    },

    // 获取图片URL
    getImageUrl(content) {
      return content.trim();
    },

    // 获取消息状态图标
    getStatusIcon(status) {
      switch (status) {
        case 'sending': return 'el-icon-loading';
        case 'sent': return 'el-icon-check';
        case 'delivered': return 'el-icon-success';
        case 'read': return 'el-icon-success';
        case 'failed': return 'el-icon-error';
        default: return '';
      }
    },

    // 获取消息状态文本
    getStatusText(status) {
      switch (status) {
        case 'sending': return '发送中';
        case 'sent': return '已发送';
        case 'delivered': return '已送达';
        case 'read': return '已读';
        case 'failed': return '发送失败';
        default: return '';
      }
    },

    // 启用应急模式
    enableFallbackMode() {
      this.fallbackModeEnabled = true;
      this.$message.success('已启用应急聊天模式，此模式下可以进行测试');

      // 添加系统消息
      this.fallbackMessages.push({
        content: '已启用应急聊天模式，此模式下的消息仅在本地显示',
        sender: '系统',
        time: new Date(),
        isSelf: false
      });

      // 添加欢迎消息
      setTimeout(() => {
        this.fallbackMessages.push({
          content: `你好，${this.currentUser.username || '用户'}！你可以在此模式下测试消息显示功能`,
          sender: this.currentChat ? (this.currentChat.nickname || this.currentChat.username) : '系统',
          time: new Date(),
          isSelf: false,
          avatar: this.isGroup ? this.defaultGroupAvatar : this.defaultAvatar
        });
      }, 500);
    },

    // 禁用应急模式
    disableFallbackMode() {
      this.fallbackModeEnabled = false;
      this.fallbackMessages = [];
      this.fallbackInput = '';
      this.$message.success('已退出应急聊天模式');
    },

    // 发送应急消息
    sendFallbackMessage() {
      if (!this.fallbackInput.trim()) return;

      // 添加发送的消息
      this.fallbackMessages.push({
        content: this.fallbackInput,
        sender: this.currentUser.username || '我',
        time: new Date(),
        isSelf: true,
        avatar: this.getAvatarUrl(this.currentUser.userProfile)
      });

      const message = this.fallbackInput;
      this.fallbackInput = '';

      // 滚动到底部
      this.$nextTick(() => {
        if (this.$refs.fallbackMessages) {
          this.$refs.fallbackMessages.scrollTop = this.$refs.fallbackMessages.scrollHeight;
        }
      });

      // 模拟回复
      setTimeout(() => {
        this.fallbackMessages.push({
          content: `收到你的消息: "${message}"`,
          sender: this.currentChat ? (this.currentChat.nickname || this.currentChat.username) : '系统',
          time: new Date(),
          isSelf: false,
          avatar: this.isGroup ? this.defaultGroupAvatar : this.defaultAvatar
        });

        // 滚动到底部
        this.$nextTick(() => {
          if (this.$refs.fallbackMessages) {
            this.$refs.fallbackMessages.scrollTop = this.$refs.fallbackMessages.scrollHeight;
          }
        });
      }, 1000);
    },

    // 添加紧急消息
    addEmergencyMessage(content, isSelf = false) {
      const emergencyMessage = {
        id: 'emergency-' + Date.now(),
        content: content,
        sender: isSelf ? (this.currentUser.username || '我') : (this.currentChat ? (this.currentChat.nickname || this.currentChat.username) : '系统'),
        time: new Date(),
        isSelf: isSelf
      };

      this.injectedMessages.push(emergencyMessage);
      this.scrollToBottom();
      return emergencyMessage.id;
    },

    // 添加紧急响应消息
    addEmergencyResponse(requestId, content) {
      setTimeout(() => {
        this.addEmergencyMessage(content, false);
      }, 800);
    },

    // 强制显示测试消息
    forceRender() {
      // 清除现有的测试消息
      this.injectedMessages = this.injectedMessages.filter(msg => !msg.isTestMessage);

      // 确定对话方 - 如果没有currentChat则显示通用测试消息
      const otherUserName = this.currentChat ? (this.isGroup ? (this.currentChat.name || '群聊成员') : (this.currentChat.nickname || this.currentChat.username)) : '测试用户';
      const currentTime = new Date();

      // 添加系统消息
      this.injectedMessages.push({
        id: 'test-system-' + Date.now(),
        content: '这是一条测试系统消息',
        time: new Date(currentTime.getTime() - 60000),
        isSystem: true,
        isTestMessage: true
      });

      // 添加来自对方的消息
      this.injectedMessages.push({
        id: 'test-received-' + Date.now(),
        content: `这是一条测试消息 - 来自${otherUserName} - ${currentTime.toLocaleTimeString()}`,
        sender: otherUserName,
        from: otherUserName,
        time: currentTime,
        isSelf: false,
        isTestMessage: true
      });

      // 添加自己的消息
      setTimeout(() => {
        this.injectedMessages.push({
          id: 'test-sent-' + Date.now(),
          content: `这是一条回复消息 - ${new Date().toLocaleTimeString()}`,
          sender: this.currentUser.username || '我',
          from: this.currentUser.username,
          time: new Date(),
          isSelf: true,
          isTestMessage: true
        });
        this.scrollToBottom();
      }, 500);

      // 添加图片消息测试
      setTimeout(() => {
        this.injectedMessages.push({
          id: 'test-image-' + Date.now(),
          content: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgdmlld0JveD0iMCAwIDIwMCAyMDAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PHJlY3Qgd2lkdGg9IjIwMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiNlNWU1ZTUiLz48Y2lyY2xlIGN4PSIxMDAiIGN5PSI3MCIgcj0iMzAiIGZpbGw9IiNhMGEwYTAiLz48cGF0aCBkPSJNNDAgMTYwQzQwIDEyNi44NiA2Ni44NiAxMDAgMTAwIDEwMEMxMzMuMTQgMTAwIDE2MCAxMjYuODYgMTYwIDE2MEgyMDBWMjAwSDBWMTYwSDQwWiIgZmlsbD0iI2EwYTBhMCIvPjwvc3ZnPg==',
          sender: otherUserName,
          from: otherUserName,
          time: new Date(new Date().getTime() + 1000),
          isSelf: false,
          isTestMessage: true
        });
        this.scrollToBottom();
      }, 1000);

      this.$message.success('已添加测试消息');
      this.scrollToBottom();
    },

    // 接收注入消息
    injectMessage(msgData) {
      if (!msgData) return;

      console.log('[DirectMessage] 接收到注入消息:', msgData);

      // 确保消息有ID
      const messageId = msgData.id || Date.now() + Math.random().toString(36).substring(2, 10);

      // 防止重复添加相同消息
      if (this.injectedMessages.some(m => m.id === messageId)) {
        console.log('[DirectMessage] 消息已存在，跳过添加:', messageId);
        return;
      }

      // 判断是否是系统消息
      const isSystemMessage = msgData.from === '系统' || msgData.sender === '系统' || msgData.isSystem;

      // 构造完整的消息对象
      const newMessage = {
        id: messageId,
        content: msgData.content,
        sender: msgData.from || msgData.sender || '未知用户',
        from: msgData.from || msgData.sender || '未知用户',
        time: msgData.time || new Date(),
        isSelf: msgData.isSelf === true || (msgData.from && msgData.from === this.currentUser.username),
        isSystem: isSystemMessage
      };

      // 添加消息
      this.injectedMessages.push(newMessage);
      console.log(`[DirectMessage] 已添加注入消息，当前共有 ${this.injectedMessages.length} 条`);

      // 如果用户当前在底部，滚动到底部
      if (this.isAtBottom) {
        this.scrollToBottom();
      } else {
        // 否则显示新消息提示
        this.showNewMessageAlert = true;
      }
    },

    // 刷新消息列表
    refreshMessages(forceReload = false) {
      console.group('[DirectMessage] 刷新消息');
      console.log('当前聊天对象:', this.currentChat);
      console.log('当前用户:', this.currentUser);

      if (!this.currentChat || !this.currentUser || !this.currentUser.username) {
        console.log('当前聊天对象或用户未设置，无法刷新消息');
        this.allMessages = [];
        this.messageKey = '';
        console.groupEnd();
        return;
      }

      // 如果强制重新加载，清空当前消息和搜索记录
      if (forceReload) {
        this.allMessages = [];
        this.searchedKeys.clear();
        this.currentPage = 1;
      }

      const sessions = this.$store.state.sessions || {};
      console.log('当前所有会话键:', Object.keys(sessions));

      // 根据聊天类型获取消息
      if (this.isGroup) {
        // 群聊消息处理
        this.messageKey = `群聊_${this.currentChat.id}`;
        console.log('使用群组消息键:', this.messageKey);

        const groupMessages = sessions[this.messageKey] || [];

        // 检查是否有更多消息
        this.hasMoreMessages = groupMessages.length > this.pageSize * this.currentPage;

        // 如果有消息限制，应用分页
        const pageLimit = this.pageSize * this.currentPage;
        this.allMessages = groupMessages.slice(-Math.min(groupMessages.length, pageLimit));

        console.log(`加载群聊消息: ${this.allMessages.length}/${groupMessages.length}`);
      } else {
        // 私聊消息处理 - 先尝试所有可能的键
        let foundMessages = [];
        let usedKey = '';

        for (const key of this.possibleMessageKeys) {
          if (sessions[key] && Array.isArray(sessions[key]) && sessions[key].length > 0) {
            console.log(`在键 "${key}" 中找到消息:`, sessions[key].length);
            usedKey = key;
            foundMessages = [...sessions[key]];
            break;
          }
        }

        // 如果没找到消息，扩大搜索范围到所有会话
        if (foundMessages.length === 0) {
          console.log('在标准键中未找到消息，搜索所有会话');

          // 获取所有会话键
          const allSessionKeys = Object.keys(sessions);
          console.log('所有会话键:', allSessionKeys);

          // 筛选可能包含当前对话的键
          for (const key of allSessionKeys) {
            // 如果已经搜索过这个键，跳过
            if (this.searchedKeys.has(key)) continue;

            this.searchedKeys.add(key);

            if (Array.isArray(sessions[key]) && sessions[key].length > 0) {
              console.log(`检查键 "${key}" 中的消息...`);

              // 筛选相关消息
              const relevantMessages = sessions[key].filter(msg => {
                return (msg.from === this.currentChat.username && msg.to === this.currentUser.username) ||
                    (msg.from === this.currentUser.username && msg.to === this.currentChat.username);
              });

              if (relevantMessages.length > 0) {
                console.log(`在键 "${key}" 中找到 ${relevantMessages.length} 条相关消息`);
                usedKey = key;
                foundMessages = [...relevantMessages];
                break;
              }
            }
          }
        }

        // 如果找到了消息，更新allMessages
        if (foundMessages.length > 0) {
          console.log(`使用消息键: "${usedKey}", 找到 ${foundMessages.length} 条消息`);
          this.messageKey = usedKey;

          // 检查是否有更多消息
          this.hasMoreMessages = foundMessages.length > this.pageSize * this.currentPage;

          // 如果有消息限制，应用分页
          const pageLimit = this.pageSize * this.currentPage;

          // 确保消息有正确的self标记
          this.allMessages = foundMessages.slice(-Math.min(foundMessages.length, pageLimit)).map(msg => ({
            ...msg,
            self: msg.from === this.currentUser.username || msg.self === true
          }));

          console.log(`加载私聊消息: ${this.allMessages.length}/${foundMessages.length}`);
        } else {
          // 如果没找到消息，使用默认键
          this.messageKey = `${this.currentUser.username}#${this.currentChat.username}`;
          console.log(`未找到相关消息，使用默认键: "${this.messageKey}"`);
          this.allMessages = [];
          this.hasMoreMessages = false;

          // 如果是首次加载，显示一个欢迎消息
          if (forceReload && this.injectedMessages.length === 0) {
            this.injectMessage({
              id: 'welcome-' + Date.now(),
              content: `与 ${this.currentChat.nickname || this.currentChat.username} 的聊天开始了，开始发送消息吧！`,
              from: '系统',
              time: new Date(),
              isSystem: true
            });
          }
        }
      }

      console.log(`消息刷新完成，当前常规消息: ${this.allMessages.length}, 注入消息: ${this.injectedMessages.length}`);
      console.groupEnd();

      // 如果是首次加载或强制加载，滚动到底部
      if (forceReload) {
        this.scrollToBottom();
      }
    },

    // 加载更多历史消息
    loadMoreMessages() {
      if (this.loadingMore || !this.hasMoreMessages) return;

      this.loadingMore = true;
      this.currentPage++;

      console.log(`[DirectMessage] 加载更多消息，页码: ${this.currentPage}`);

      // 记住当前滚动位置
      const scrollElement = this.$refs.messageList;
      const oldScrollHeight = scrollElement ? scrollElement.scrollHeight : 0;

      // 刷新消息，但不强制重载
      this.refreshMessages(false);

      // 完成后恢复滚动位置
      this.$nextTick(() => {
        if (scrollElement) {
          const newScrollHeight = scrollElement.scrollHeight;
          const heightDiff = newScrollHeight - oldScrollHeight;

          // 将滚动位置设置为新高度减去旧高度
          scrollElement.scrollTop = heightDiff;
        }

        this.loadingMore = false;
      });
    },

    // 格式化时间显示 (小时:分钟)
    formatTime(time) {
      if (!time) return '';

      try {
        const date = new Date(time);
        if (isNaN(date.getTime())) return '';

        return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`;
      } catch (e) {
        console.error('[DirectMessage] 时间格式化错误:', e);
        return '';
      }
    },

    // 格式化日期显示 (年月日)
    formatDate(date) {
      if (!date) return '';

      try {
        const now = new Date();
        const yesterday = new Date();
        yesterday.setDate(yesterday.getDate() - 1);

        if (date.toDateString() === now.toDateString()) {
          return '今天';
        } else if (date.toDateString() === yesterday.toDateString()) {
          return '昨天';
        } else {
          return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`;
        }
      } catch (e) {
        console.error('[DirectMessage] 日期格式化错误:', e);
        return '';
      }
    },

    // 滚动到底部
    scrollToBottom() {
      this.$nextTick(() => {
        const messageList = this.$refs.messageList;
        if (messageList) {
          messageList.scrollTop = messageList.scrollHeight;
          this.isAtBottom = true;
          this.showNewMessageAlert = false;
        }
      });
    },

    // 处理滚动事件
    handleScroll() {
      const messageList = this.$refs.messageList;
      if (!messageList) return;

      // 清除之前的计时器
      if (this.scrollTimer) {
        clearTimeout(this.scrollTimer);
      }

      this.isUserScrolling = true;

      // 检查是否滚动到底部
      const isAtBottom = messageList.scrollHeight - messageList.scrollTop - messageList.clientHeight < 50;
      this.isAtBottom = isAtBottom;

      // 如果滚动到底部，隐藏新消息提示
      if (isAtBottom) {
        this.showNewMessageAlert = false;
      }

      // 0.5秒后重置用户滚动状态
      this.scrollTimer = setTimeout(() => {
        this.isUserScrolling = false;
      }, 500);
    },

    // 导航到特定消息
    navigateToMessage(message) {
      if (!message || !message.id) return;

      // 找到对应消息的DOM元素
      const messageId = `msg-reg-${message.id}`;
      const element = document.getElementById(messageId);

      if (element) {
        // 滚动到该元素
        element.scrollIntoView({ behavior: 'smooth', block: 'center' });

        // 高亮显示该消息
        element.classList.add('highlight-message');
        setTimeout(() => {
          element.classList.remove('highlight-message');
        }, 2000);
      } else {
        // 如果没找到，可能需要加载更多历史消息
        this.$message.info('正在查找消息，可能需要加载更多历史记录');
        this.loadMoreMessages();
      }
    },

    // 处理新消息事件
    onNewMessage(eventData) {
      console.log('[DirectMessage] 收到新消息事件:', eventData);

      // 刷新常规消息
      this.refreshMessages();

      // 处理需要注入的消息
      if (eventData && eventData.message && eventData.addToInjected) {
        const message = eventData.message;

        // 检查消息是否与当前聊天相关
        let isCurrentChat = false;

        if (this.isGroup) {
          // 群聊消息匹配
          if (typeof message.to === 'string') {
            // 处理可能的格式: "group_2" 或 "群聊_2"
            const toGroupId = message.to.replace(/^(group_|群聊_)/, '');
            isCurrentChat = toGroupId === this.currentChat.id.toString();
          }
        } else {
          // 私聊消息匹配
          isCurrentChat = (message.from === this.currentChat.username && message.to === this.currentUser.username) ||
              (message.from === this.currentUser.username && message.to === this.currentChat.username);
        }

        if (isCurrentChat) {
          console.log('[DirectMessage] 注入与当前聊天相关的消息:', message);

          // 构造完整的消息对象并注入
          this.injectMessage({
            id: message.id || message.messageId || ('msg-' + Date.now()),
            content: message.content,
            from: message.from,
            sender: message.fromName || message.from,
            time: message.createTime || new Date(),
            isSelf: message.self || message.from === this.currentUser.username
          });
        }
      }
    },

    // 测试发送消息
    debugSendMessage() {
      if (!this.currentChat) {
        this.$message.warning('请先选择聊天对象');
        return;
      }

      // 生成测试消息
      const testContent = `测试消息 ${new Date().toLocaleTimeString()}`;

      // 添加自己的测试消息，带有发送中状态
      const messageId = 'debug-send-' + Date.now();
      this.injectMessage({
        id: messageId,
        content: testContent,
        from: this.currentUser.username,
        time: new Date(),
        isSelf: true,
        status: 'sending'
      });

      // 2秒后更新为已发送状态
      setTimeout(() => {
        const index = this.injectedMessages.findIndex(m => m.id === messageId);
        if (index !== -1) {
          this.$set(this.injectedMessages[index], 'status', 'sent');
        }
      }, 2000);

      // 模拟对方回复
      setTimeout(() => {
        this.injectMessage({
          id: 'debug-reply-' + Date.now(),
          content: `模拟回复: "${testContent}"`,
          from: this.currentChat.username || this.currentChat.name,
          time: new Date(),
          isSelf: false
        });
      }, 3000);
    },

    // 重新连接WebSocket
    reconnectWebSocket() {
      this.$message.info('正在尝试重新连接...');
      this.showConnectionAlert = true;

      this.$store.dispatch('connect')
          .then(() => {
            this.$message.success('连接成功');
            this.refreshMessages(true);
          })
          .catch(err => {
            this.$message.error('连接失败: ' + (err.message || '未知错误'));
          });
    },

    // 处理空状态下的操作
    handleEmptyAction() {
      // 发送问候消息
      const greetings = [
        '你好！',
        '嗨，最近怎么样？',
        '很高兴和你聊天！',
        '你今天过得怎么样？'
      ];

      // 随机选择一条问候语
      const greeting = greetings[Math.floor(Math.random() * greetings.length)];

      // 如果组件中有父组件的发送消息方法，调用它
      if (this.$parent && typeof this.$parent.sendMessage === 'function') {
        this.$parent.sendMessage(greeting);
      } else {
        // 否则添加一条模拟消息
        this.injectMessage({
          id: 'greeting-' + Date.now(),
          content: greeting,
          from: this.currentUser.username,
          time: new Date(),
          isSelf: true
        });

        // 模拟对方回复
        setTimeout(() => {
          this.injectMessage({
            id: 'greeting-reply-' + Date.now(),
            content: '你好啊，很高兴收到你的消息！',
            from: this.currentChat.username || this.currentChat.name,
            time: new Date(),
            isSelf: false
          });
        }, 1500);
      }
    }
  }
}
</script>

<style lang="scss">
.direct-message-container {
  height: 100%;
  width: 100%;
  display: flex;
  flex-direction: column;
  position: relative;
  background-color: #f7f7f7;
}

/* 调试面板样式 */
.debug-panel {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 100;
  background-color: rgba(0, 0, 0, 0.7);
  border-radius: 4px;
  padding: 8px;
  font-size: 12px;
  color: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  width: 260px;

  .debug-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
    padding-bottom: 5px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.3);

    span {
      font-weight: bold;
    }
  }

  .debug-controls {
    display: flex;
    flex-wrap: wrap;
    gap: 5px;
    margin-bottom: 8px;
  }

  .debug-info {
    margin-top: 5px;
    line-height: 1.6;
    font-size: 11px;
    opacity: 0.9;

    div {
      word-break: break-all;
    }
  }
}

/* 空消息提示样式 */
.empty-messages {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 100%;
  padding: 20px;
  box-sizing: border-box;

  .empty-icon {
    font-size: 50px;
    color: #dcdfe6;
    margin-bottom: 20px;

    i {
      font-size: inherit;
    }
  }

  .empty-text {
    color: #909399;
    font-size: 14px;
    margin-bottom: 15px;
    text-align: center;
    max-width: 300px;
  }
}

/* 加载更多按钮 */
.load-more {
  padding: 10px;
  text-align: center;
}

/* 消息列表样式 */
.message-list {
  flex: 1;
  padding: 10px 15px;
  overflow-y: auto;
  scroll-behavior: smooth;
}

/* 消息项样式 */
.message-item {
  display: flex;
  margin-bottom: 15px;
  position: relative;

  &.highlight-message {
    animation: highlight 1.5s ease-in-out;
  }

  &.self-message {
    flex-direction: row-reverse;

    .message-content {
      background-color: #95ec69;
      margin-right: 12px;

      &:after {
        content: '';
        position: absolute;
        right: -8px;
        top: 15px;
        border-width: 8px 0 8px 8px;
        border-style: solid;
        border-color: transparent transparent transparent #95ec69;
      }
    }
  }

  /* 日期分隔线样式 */
  &.date-divider {
    display: flex;
    justify-content: center;
    align-items: center;
    margin: 15px 0;

    .date-divider-content {
      background-color: rgba(0, 0, 0, 0.05);
      padding: 2px 15px;
      border-radius: 10px;
      font-size: 12px;
      color: #909399;
    }
  }

  /* 系统消息样式 */
  &.system-message {
    justify-content: center;
    margin: 10px 0;

    .system-message-content {
      background-color: rgba(144, 147, 153, 0.1);
      padding: 5px 15px;
      border-radius: 15px;
      font-size: 12px;
      color: #909399;
      display: flex;
      align-items: center;

      i {
        margin-right: 5px;
      }
    }
  }
}

/* 头像容器样式 */
.avatar-container {
  margin: 0 12px;
  align-self: flex-start;
}

/* 消息内容样式 */
.message-content {
  position: relative;
  max-width: 70%;
  background-color: white;
  border-radius: 8px;
  padding: 10px 12px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  word-break: break-word;
  margin-left: 12px;

  &:after {
    content: '';
    position: absolute;
    left: -8px;
    top: 15px;
    border-width: 8px 8px 8px 0;
    border-style: solid;
    border-color: transparent white transparent transparent;
  }

  /* 图片消息样式 */
  &.image-message {
    padding: 6px;
    max-width: 250px;

    .image-container {
      width: 100%;

      .el-image {
        width: 100%;
        max-height: 300px;
        border-radius: 4px;
        overflow: hidden;
      }

      .image-error {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        background-color: #f5f7fa;
        height: 150px;
        color: #909399;

        i {
          font-size: 24px;
          margin-bottom: 5px;
        }

        span {
          font-size: 12px;
        }
      }
    }
  }
}

/* 消息头部样式 */
.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 3px;

  .nickname {
    font-size: 12px;
    color: #909399;
  }

  .time {
    font-size: 10px;
    color: #c0c4cc;
    margin-left: 10px;
  }
}

/* 消息内容样式 */
.content {
  font-size: 14px;
  line-height: 1.5;
  white-space: pre-wrap;
}

/* 消息状态样式 */
.message-status {
  text-align: right;
  font-size: 10px;
  color: #c0c4cc;
  margin-top: 3px;

  i {
    margin-right: 3px;
  }
}

/* 新消息提示样式 */
.new-message-alert {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  background-color: rgba(0, 0, 0, 0.6);
  color: white;
  padding: 5px 15px;
  border-radius: 20px;
  font-size: 12px;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  display: flex;
  align-items: center;

  i {
    margin-right: 5px;
  }

  &:hover {
    background-color: rgba(0, 0, 0, 0.8);
  }
}

/* 连接状态提示样式 */
.connection-alert {
  position: absolute;
  top: 10px;
  left: 50%;
  transform: translateX(-50%);
  padding: 5px 15px;
  border-radius: 4px;
  font-size: 12px;
  display: flex;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);

  &.status-success {
    background-color: #f0f9eb;
    color: #67c23a;
  }

  &.status-warning {
    background-color: #fdf6ec;
    color: #e6a23c;
  }

  &.status-error {
    background-color: #fef0f0;
    color: #f56c6c;
  }

  i {
    margin-right: 5px;
  }

  button {
    margin-left: 10px;
    padding: 0;
  }
}

/* 应急模式样式 */
.fallback-chat {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #f9f9f9;
  display: flex;
  flex-direction: column;
  z-index: 500;

  .fallback-title {
    display: flex;
    justify-content: space-between;
    align-items: center;
    width: 100%;
  }

  .fallback-header {
    padding: 10px;

    .el-alert {
      width: calc(100% - 20px);
    }
  }

  .fallback-messages {
    flex: 1;
    overflow-y: auto;
    padding: 15px;
  }

  .fallback-input {
    padding: 10px;
    border-top: 1px solid #e8e8e8;
    display: flex;
    align-items: flex-end;
    gap: 10px;

    .el-input {
      flex: 1;
    }

    .el-button {
      height: 60px;
    }
  }
}

/* 动画效果 */
@keyframes highlight {
  0% { background-color: rgba(255, 220, 80, 0.3); }
  100% { background-color: transparent; }
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter, .fade-leave-to {
  opacity: 0;
}
</style>