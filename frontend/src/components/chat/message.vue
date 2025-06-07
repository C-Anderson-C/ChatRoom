<template>
  <div id="message-display">
    <!-- 调试工具栏 -->
    <div v-if="debug" class="debug-toolbar">
      <el-button type="primary" size="mini" @click="refreshMessages">刷新消息</el-button>
      <div class="debug-info">
        <div>当前用户: {{ currentUser?.username }}</div>
        <div>聊天对象: {{ currentChat?.username }}</div>
        <div>消息数量: {{ displayMessages.length }}</div>
        <div>消息键: {{ messageKey }}</div>
        <div>
          <el-button size="mini" type="warning" @click="searchAllMessages">查找所有消息</el-button>
        </div>
      </div>
    </div>

    <!-- 消息列表 -->
    <div class="message-list" ref="messageList">
      <div v-if="displayMessages.length === 0" class="empty-message">
        暂无消息记录
      </div>

      <div v-for="(msg, idx) in displayMessages" :key="idx" class="message-item" :class="{ 'self-message': msg.self }">
        <!-- 对方头像 -->
        <template v-if="!msg.self">
          <div class="avatar-container">
            <img class="avatar" :src="msg.fromProfile || defaultAvatar" alt="头像">
          </div>
        </template>

        <!-- 消息内容 -->
        <div class="message-content" :class="{ 'self-content': msg.self }">
          <div class="message-header" v-if="!msg.self">{{ msg.fromName || msg.from }}</div>
          <div class="message-body">{{ msg.content }}</div>
          <div class="message-time">{{ formatTime(msg.createTime) }}</div>
        </div>

        <!-- 自己的头像 -->
        <template v-if="msg.self">
          <div class="avatar-container">
            <img class="avatar" :src="currentUser.userProfile || defaultAvatar" alt="头像">
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'message',
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
      defaultAvatar: '/img/avatar.jpg',
      debug: true, // 开启调试模式
      displayMessages: [],
      messageKey: '',
      allMessages: {} // 存储所有找到的消息
    }
  },
  computed: {
    currentUser() {
      return this.$store.state.currentUser || {};
    },

    // 获取消息但不立即渲染，先在methods中处理
    rawMessages() {
      if (!this.currentChat || !this.currentUser.username) return [];

      if (this.isGroup) {
        // 群聊消息
        const groupId = this.currentChat.id;
        if (!groupId) return [];

        this.messageKey = `群聊_${groupId}`;
        return this.$store.state.sessions[this.messageKey] || [];
      } else {
        // 私聊消息 - 尝试多种可能的键格式
        const currentUsername = this.currentUser.username;
        const chatUsername = this.currentChat.username;
        if (!currentUsername || !chatUsername) return [];

        // 尝试不同的键格式
        const possibleKeys = [
          `${currentUsername}#${chatUsername}`,
          `${chatUsername}#${currentUsername}`
        ];

        let messages = [];

        // 首先尝试推荐的键格式
        this.messageKey = possibleKeys[0];
        messages = this.$store.state.sessions[this.messageKey] || [];

        // 如果没有找到消息，尝试反向键格式
        if (messages.length === 0 && this.$store.state.sessions[possibleKeys[1]]) {
          this.messageKey = possibleKeys[1];
          messages = this.$store.state.sessions[this.messageKey] || [];
          console.log('使用反向键检索到消息:', messages.length);
        }

        // 如果有手动查找的消息，使用它们
        if (this.allMessages[chatUsername] && this.allMessages[chatUsername].length > 0) {
          messages = this.allMessages[chatUsername];
          console.log('使用手动查找的消息:', messages.length);
        }

        return messages;
      }
    }
  },
  mounted() {
    console.log('消息组件已挂载');
    // 初始化消息
    this.refreshMessages();

    // 监听新消息事件
    this.$bus.$on('new-message', this.onNewMessage);

    // 初始添加一些调试日志
    console.log('当前用户:', this.currentUser);
    console.log('当前聊天对象:', this.currentChat);
    console.log('是否群聊:', this.isGroup);
    console.log('当前会话键:', this.messageKey);
    console.log('所有会话键:', Object.keys(this.$store.state.sessions || {}));

    // 如果找不到消息，自动搜索一次
    if (this.displayMessages.length === 0) {
      this.$nextTick(() => {
        this.searchAllMessages();
      });
    }
  },
  beforeDestroy() {
    // 移除事件监听
    this.$bus.$off('new-message', this.onNewMessage);
  },
  methods: {
    // 更新消息列表
    refreshMessages() {
      console.log('刷新消息列表');

      const allSessions = this.$store.state.sessions || {};
      console.log('所有会话键:', Object.keys(allSessions));

      // 获取当前会话的消息
      const messages = [...this.rawMessages];
      console.log(`当前消息键[${this.messageKey}]的消息数量:`, messages.length);

      // 按时间排序
      messages.sort((a, b) => {
        return new Date(a.createTime || 0) - new Date(b.createTime || 0);
      });

      // 直接从自定义属性或通过比较判断是否是自己发送的
      messages.forEach(msg => {
        if (msg.self === undefined) {
          msg.self = msg.from === this.currentUser.username || msg.fromId === this.currentUser.id;
        }
      });

      // 更新显示的消息
      this.displayMessages = messages;

      // 滚动到底部
      this.$nextTick(() => {
        this.scrollToBottom();
      });
    },

    // 搜索所有可能包含当前对话消息的键
    searchAllMessages() {
      if (!this.currentChat || !this.currentUser) {
        this.$message.warning('当前用户或聊天对象未设置');
        return;
      }

      const chatUsername = this.currentChat.username;
      const currentUsername = this.currentUser.username;

      if (!chatUsername || !currentUsername) {
        this.$message.warning('用户名缺失，无法搜索消息');
        return;
      }

      console.log('开始搜索所有可能的消息...');

      const allSessions = this.$store.state.sessions || {};
      const allKeys = Object.keys(allSessions);
      let foundMessages = [];

      // 记录搜索到的消息
      allKeys.forEach(key => {
        // 检查键是否包含当前聊天对象的用户名
        if (
            (key.includes(chatUsername) || key.includes(currentUsername)) &&
            Array.isArray(allSessions[key]) &&
            allSessions[key].length > 0
        ) {
          console.log(`在键[${key}]中找到可能的消息:`, allSessions[key].length);

          // 筛选只与当前聊天对象相关的消息
          const relevantMsgs = allSessions[key].filter(msg =>
              msg.from === chatUsername ||
              msg.to === chatUsername ||
              msg.from === currentUsername ||
              msg.to === currentUsername
          );

          if (relevantMsgs.length > 0) {
            console.log(`筛选出相关消息:`, relevantMsgs.length);
            foundMessages = foundMessages.concat(relevantMsgs);
          }
        }
      });

      if (foundMessages.length > 0) {
        console.log(`共找到${foundMessages.length}条相关消息`);

        // 标记消息来源
        foundMessages.forEach(msg => {
          msg.self = msg.from === currentUsername || msg.fromId === this.currentUser.id;
        });

        // 存储找到的消息
        this.allMessages[chatUsername] = foundMessages;

        // 更新显示
        this.displayMessages = foundMessages.sort((a, b) => {
          return new Date(a.createTime || 0) - new Date(b.createTime || 0);
        });

        // 提示用户
        this.$message.success(`找到${foundMessages.length}条相关消息`);

        // 滚动到底部
        this.$nextTick(() => {
          this.scrollToBottom();
        });
      } else {
        console.log('未找到相关消息');
        this.$message.warning('未找到相关消息');
      }
    },

    // 处理新消息事件
    onNewMessage() {
      console.log('收到新消息事件');
      // 确保及时更新消息
      setTimeout(() => {
        this.refreshMessages();
      }, 10);
    },

    // 格式化时间
    formatTime(time) {
      if (!time) return '';
      const date = new Date(time);
      return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`;
    },

    // 滚动到底部
    scrollToBottom() {
      const messageList = this.$refs.messageList;
      if (messageList) {
        messageList.scrollTop = messageList.scrollHeight;
      }
    }
  },
  watch: {
    // 监听聊天对象变化
    currentChat: {
      handler() {
        console.log('聊天对象变化:', this.currentChat);
        this.refreshMessages();
      },
      deep: true
    },

    // 监听原始消息变化
    rawMessages: {
      handler() {
        this.refreshMessages();
      },
      deep: true
    },

    // 监听 store 中的 sessions 变化
    '$store.state.sessions': {
      handler() {
        console.log('Sessions 对象已更改');
        this.refreshMessages();
      },
      deep: true
    }
  }
}
</script>

<style scoped>
#message-display {
  height: 100%;
  width: 100%;
  display: flex;
  flex-direction: column;
  position: relative;
}

.debug-toolbar {
  position: absolute;
  top: 5px;
  right: 5px;
  z-index: 10;
  background-color: rgba(255, 255, 255, 0.8);
  border: 1px solid #eee;
  border-radius: 4px;
  padding: 5px;
  font-size: 12px;
  max-width: 200px;
}

.debug-info {
  margin-top: 5px;
  color: #666;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
  background-color: #f5f5f5;
}

.message-item {
  display: flex;
  margin-bottom: 15px;
  align-items: flex-start;
}

.self-message {
  flex-direction: row-reverse;
}

.avatar-container {
  margin: 0 10px;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 1px solid #eee;
}

.message-content {
  max-width: 70%;
  background-color: white;
  border-radius: 8px;
  padding: 10px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.self-content {
  background-color: #a0e75a;
}

.message-header {
  font-size: 12px;
  color: #999;
  margin-bottom: 5px;
}

.message-body {
  word-break: break-word;
}

.message-time {
  font-size: 11px;
  color: #bbb;
  text-align: right;
  margin-top: 5px;
}

.empty-message {
  text-align: center;
  color: #999;
  padding: 20px;
}
</style>