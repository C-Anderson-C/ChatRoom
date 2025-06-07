<template>
  <div id="usertext-container">
    <!-- 输入区域上方的工具栏 -->
    <div class="usertext-toolbar">
      <el-popover placement="top-start" width="400" trigger="click" class="emoBox">
        <div class="emotionList">
          <a href="javascript:void(0);" @click="getEmo(index)"
             v-for="(item,index) in faceList" :key="'face_'+index">{{ item }}</a>
        </div>
        <el-button id="emojiBtn" class="emotionSelect" slot="reference">
          <i class="fa fa-smile-o" aria-hidden="true"></i>
        </el-button>
      </el-popover>

      <el-upload
          class="upload-btn"
          action="/ossFileUpload?module=group-chat"
          :before-upload="beforeAvatarUpload"
          :on-success="imgSuccess"
          :on-error="imgError"
          :show-file-list="false"
          accept=".jpg,.jpeg,.png,.JPG,JPEG,.PNG,.gif,.GIF"
      >
        <el-button id="uploadImgBtn" icon="el-icon-picture-outline"></el-button>
      </el-upload>

      <!-- 调试与应急选项下拉菜单 -->
      <el-dropdown trigger="click" @command="handleEmergencySend" v-if="debug">
        <el-button type="text" icon="el-icon-more"></el-button>
        <el-dropdown-menu slot="dropdown">
          <el-dropdown-item command="reset">重置发送状态</el-dropdown-item>
          <el-dropdown-item command="emergency">应急发送</el-dropdown-item>
          <el-dropdown-item command="test">发送测试消息</el-dropdown-item>
          <el-dropdown-item command="check">检查聊天对象</el-dropdown-item>
          <el-dropdown-item command="reconnect">重新连接WebSocket</el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </div>

    <!-- 发送状态指示器 -->
    <div v-if="sendingMessage" class="sending-indicator">
      <i class="el-icon-loading"></i> 发送中...
    </div>

    <!-- 调试面板 -->
    <div v-if="debug" class="debug-panel">
      <div class="debug-status">
        <span :class="{'status-ok': !sendingMessage, 'status-busy': sendingMessage}">
          状态: {{ sendingMessage ? '发送中' : '就绪' }}
        </span>
        <el-button size="mini" type="danger" @click="resetSendingState"
                   :disabled="!sendingMessage">重置</el-button>
      </div>
      <div class="connection-status" v-if="wsStatus">
        <span :class="{
          'status-ok': wsStatus === 'connected',
          'status-busy': wsStatus === 'connecting',
          'status-error': wsStatus === 'error'
        }">
          WS: {{ wsStatus }}
        </span>
      </div>
      <div class="debug-chat-info" v-if="currentChat">
        <div>类型: {{ isGroup ? '群聊' : '私聊' }}</div>
        <div>ID: {{ currentChat.id }}</div>
        <div>用户名: {{ currentChat.username }}</div>
      </div>
      <div class="debug-chat-info" v-else>未选择聊天对象</div>
    </div>

    <!-- 输入区域 -->
    <div class="input-area">
      <textarea
          ref="messageInput"
          id="textarea"
          placeholder="按 Ctrl + Enter 发送"
          v-model="content"
          @keyup="addMessage"
          :disabled="!currentChat"
      ></textarea>
    </div>

    <!-- 发送按钮 -->
    <div class="send-button-area">
      <el-button
          id="sendBtn"
          type="primary"
          size="mini"
          @click="addMessageByClick"
          :disabled="!currentChat || !content.trim() || sendingMessage"
      >发送</el-button>
    </div>
  </div>
</template>

<script>
import { mapState } from 'vuex';
const appData = require("../../utils/emoji.json"); // 引入表情包数据

export default {
  name: 'usertext',
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
      faceList: [], // 表情包数据
      content: '', // 输入框内容
      sendingMessage: false, // 发送状态标记
      sendingTimeout: null, // 发送超时计时器
      sendingStartTime: null, // 发送开始时间
      lastSentMessageId: null, // 最后发送的消息ID
      debug: true, // 是否显示调试信息
      messageLog: [], // 消息发送日志
      wsStatus: null, // WebSocket连接状态
      wsCheckInterval: null, // WebSocket检查定时器
      localMessages: new Set() // 本地已显示的消息ID列表
    }
  },
  computed: {
    ...mapState({
      currentUser: state => state.currentUser || {},
      storeSessions: state => state.sessions || {}
    }),

    // 获取当前会话ID或用户名
    currentChatIdentifier() {
      if (!this.currentChat) return null;

      if (this.isGroup) {
        return `group_${this.currentChat.id}`;
      } else {
        return this.currentChat.username;
      }
    },

    // 确定消息会话的键名
    messageSessionKey() {
      if (!this.currentChat || !this.currentUser.username) return null;

      if (this.isGroup) {
        return `群聊_${this.currentChat.id}`;
      } else {
        return `${this.currentUser.username}#${this.currentChat.username}`;
      }
    }
  },
  mounted() {
    // 加载表情包数据
    this.loadEmojis();

    // 确保store中的必要对象已初始化
    this.ensureStoreInitialized();

    // 输出调试信息
    console.log('[usertext] 组件已挂载');
    console.log('[usertext] 当前聊天对象:', this.currentChat);
    console.log('[usertext] 当前用户:', this.currentUser);
    console.log('[usertext] 是否群聊:', this.isGroup);

    // 监听消息发送超时
    this.setupMessageTimeoutMonitor();

    // 定期检查WebSocket连接状态
    this.startWebSocketMonitoring();
  },
  beforeDestroy() {
    // 清除定时器
    if (this.sendingTimeout) {
      clearTimeout(this.sendingTimeout);
      this.sendingTimeout = null;
    }

    // 清除WebSocket检查定时器
    if (this.wsCheckInterval) {
      clearInterval(this.wsCheckInterval);
      this.wsCheckInterval = null;
    }
  },
  watch: {
    // 当聊天对象变化时重置状态
    currentChat() {
      this.resetSendingState();
    }
  },
  methods: {
    // 初始化相关函数
    loadEmojis() {
      // 读取json文件保存表情数据
      for (let i in appData) {
        this.faceList.push(appData[i].char);
      }
    },

    ensureStoreInitialized() {
      // 确保store中必要的状态存在
      if (!this.$store.state.sessions) {
        this.$store.state.sessions = {};
      }
      if (!this.$store.state.sentMessageIds) {
        this.$store.state.sentMessageIds = new Set();
      }
      if (!this.$store.state.processedMessages) {
        this.$store.state.processedMessages = new Set();
      }
    },

    setupMessageTimeoutMonitor() {
      // 定时检查发送状态，避免卡死
      setInterval(() => {
        if (this.sendingMessage && this.sendingStartTime) {
          const timeElapsed = Date.now() - this.sendingStartTime;
          if (timeElapsed > 10000) { // 10秒超时
            console.warn('[usertext] 消息发送超时，自动重置状态');
            this.resetSendingState();
          }
        }
      }, 5000);
    },

    startWebSocketMonitoring() {
      // 立即检查状态
      this.checkWebSocketStatus();

      // 定期检查WebSocket连接状态
      this.wsCheckInterval = setInterval(() => {
        this.checkWebSocketStatus();
      }, 10000); // 每10秒检查一次
    },

    checkWebSocketStatus() {
      try {
        const stomp = this.$store.state.stomp;
        if (!stomp) {
          this.wsStatus = 'disconnected';
        } else if (stomp.connected) {
          this.wsStatus = 'connected';
        } else {
          this.wsStatus = 'error';
        }
      } catch (e) {
        this.wsStatus = 'error';
        console.error('[usertext] 检查WebSocket状态出错:', e);
      }
    },

    // 应急和调试功能
    handleEmergencySend(command) {
      switch(command) {
        case 'reset':
          this.resetSendingState();
          break;
        case 'emergency':
          this.forceEmergencySend();
          break;
        case 'test':
          this.sendTestMessage();
          break;
        case 'check':
          this.checkChat();
          break;
        case 'reconnect':
          this.reconnectWebSocket();
          break;
      }
    },

    reconnectWebSocket() {
      this.$store.dispatch('connect').then(() => {
        this.$message.success('WebSocket已重新连接');
        this.checkWebSocketStatus();
      }).catch(err => {
        this.$message.error('WebSocket连接失败: ' + (err.message || String(err)));
        console.error('[usertext] WebSocket重连失败:', err);
      });
    },

    resetSendingState() {
      this.sendingMessage = false;
      if (this.sendingTimeout) {
        clearTimeout(this.sendingTimeout);
        this.sendingTimeout = null;
      }
      this.sendingStartTime = null;
      console.log('[usertext] 发送状态已重置');
      this.$message.success('发送状态已重置');
    },

    forceEmergencySend() {
      this.resetSendingState();
      console.log('[usertext] 应急发送触发');
      if (!this.content.trim()) {
        this.$message.warning('请先输入消息内容');
        return;
      }
      this.sendPrivateMessage(true); // 传递应急标志
    },

    sendTestMessage() {
      const originalContent = this.content;
      this.content = `测试消息 ${new Date().toLocaleTimeString()}`;
      this.resetSendingState();

      // 直接注入测试消息到UI
      this.$bus.$emit('inject-message', {
        id: 'test-' + Date.now(),
        content: this.content,
        from: this.currentUser.username,
        time: new Date(),
        isSelf: true
      });

      // 如果有聊天对象，还模拟一条回复
      if (this.currentChat) {
        setTimeout(() => {
          this.$bus.$emit('inject-message', {
            id: 'test-reply-' + Date.now(),
            content: `这是一条测试回复 (${new Date().toLocaleTimeString()})`,
            from: this.currentChat.username,
            time: new Date(),
            isSelf: false
          });
        }, 1000);
      }

      this.content = originalContent;
      this.$message.success('测试消息已发送');
    },

    checkChat() {
      console.log('[usertext] 当前聊天对象:', this.currentChat);
      console.log('[usertext] 当前用户:', this.currentUser);
      console.log('[usertext] WebSocket状态:', this.wsStatus);

      // 详细检查WebSocket状态
      const stomp = this.$store.state.stomp;
      console.log('[usertext] WebSocket实例:', stomp);
      if (stomp) {
        console.log('[usertext] WebSocket连接状态:', stomp.connected ? '已连接' : '未连接');
      }

      if (!this.currentChat) {
        this.$message.error('未选择聊天对象');
      } else if (this.isGroup) {
        this.$message.info(`当前聊天对象: 群聊-${this.currentChat.id}`);
      } else if (!this.currentChat.username) {
        this.$message.warning('当前聊天对象缺少username属性');
      } else {
        this.$message.success(`当前聊天对象: ${this.currentChat.username}`);
      }
    },

    // WebSocket连接检查
    checkWebSocketConnection() {
      return new Promise((resolve, reject) => {
        // 如果WebSocket未初始化或未连接
        if (!this.$store.state.stomp || !this.$store.state.stomp.connected) {
          console.log('[usertext] WebSocket未连接，尝试重新连接');
          this.$message.warning('正在连接服务器...');

          // 尝试重新连接
          this.$store.dispatch('connect')
              .then(() => {
                console.log('[usertext] WebSocket连接成功');
                this.wsStatus = 'connected';
                resolve(true);
              })
              .catch(err => {
                console.error('[usertext] WebSocket连接失败:', err);
                this.$message.error('服务器连接失败，请刷新页面重试');
                this.wsStatus = 'error';
                reject(err);
              });
        } else {
          // WebSocket已连接
          console.log('[usertext] WebSocket连接状态正常');
          this.wsStatus = 'connected';
          resolve(true);
        }
      });
    },

    // 消息输入和发送
    addMessage(e) {
      // Ctrl + Enter 发送消息
      if (e.ctrlKey && e.keyCode === 13 && this.content.trim()) {
        this.addMessageByClick();
      }
    },

    addMessageByClick() {
      // 检查发送前提条件
      if (!this.content.trim()) {
        this.$message.warning('不能发送空消息');
        return;
      }

      if (!this.currentChat) {
        this.$message.error('请先选择聊天对象');
        return;
      }

      // 检查是否已有消息正在发送
      if (this.sendingMessage) {
        console.log('[usertext] 已有消息正在发送，请稍候...');

        // 如果发送状态卡住超过5秒，强制重置
        const timeStuck = Date.now() - (this.sendingStartTime || 0);
        if (timeStuck > 5000) {
          console.warn('[usertext] 发送状态卡住已超过5秒，强制重置');
          this.resetSendingState();
        } else {
          this.$message.warning('请等待上一条消息发送完成');
          return;
        }
      }

      // 记录发送开始时间
      this.sendingStartTime = Date.now();

      // 根据聊天类型选择发送方法
      if (this.isGroup) {
        this.sendGroupMessage();
      } else {
        this.sendPrivateMessage();
      }
    },

    // 发送群聊消息
    async sendGroupMessage() {
      try {
        // 设置发送状态
        this.sendingMessage = true;

        if (!this.currentChat || !this.currentChat.id) {
          this.$message.error('请先选择一个群组');
          return;
        }

        // 生成唯一消息ID
        const messageId = Date.now() + '-' + Math.random().toString(36).substring(2, 9);

        // 构造消息对象
        const msgObj = {
          messageId: messageId,
          id: messageId, // 同时设置id字段，确保兼容性
          content: this.content.trim(),
          messageTypeId: 1,
          from: this.currentUser.username,
          fromId: this.currentUser.id,
          fromNickname: this.currentUser.nickname || this.currentUser.username,
          fromProfile: this.currentUser.userProfile || this.currentUser.avatar,
          to: `group_${this.currentChat.id}`,
          createTime: new Date().toISOString(),
          direction: 'sent' // 添加方向属性
        };

        console.log('[usertext] 准备发送群聊消息:', msgObj);

        // 1. 先在本地显示消息
        const groupKey = `群聊_${this.currentChat.id}`;
        const localMsg = {...msgObj, self: true};

        // 确保会话对象存在
        if (!this.$store.state.sessions[groupKey]) {
          this.$store.state.sessions[groupKey] = [];
        }

        // 添加消息到本地存储
        this.$store.state.sessions[groupKey].push(localMsg);

        // 记录最后发送的消息ID
        this.lastSentMessageId = messageId;
        this.localMessages.add(messageId);

        // 通知UI立即显示
        this.$bus.$emit('new-message', {
          message: localMsg,
          addToInjected: true
        });

        // 清空输入框
        this.content = '';

        // 2. 检查WebSocket连接并发送
        try {
          await this.checkWebSocketConnection();

          // 发送到服务器，注意修改路径
          this.$store.state.stomp.send("/app/groupChat", {}, JSON.stringify(msgObj));
          console.log('[usertext] 群聊消息已发送到服务器');

          // 发送成功，添加到已发送列表
          if (!this.$store.state.sentMessageIds) {
            this.$store.state.sentMessageIds = new Set();
          }
          this.$store.state.sentMessageIds.add(messageId);

          // 同时添加到处理过的消息ID集合，避免接收重复
          if (!this.$store.state.processedMessages) {
            this.$store.state.processedMessages = new Set();
          }
          this.$store.state.processedMessages.add(messageId);

        } catch (error) {
          console.error('[usertext] WebSocket发送失败:', error);
          this.$message.warning('消息已在本地显示，但可能未成功发送到服务器');

          // 尝试通过备用API发送
          try {
            await fetch('/api/messages', {
              method: 'POST',
              headers: {
                'Content-Type': 'application/json'
              },
              body: JSON.stringify(msgObj)
            });
          } catch (apiError) {
            console.error('[usertext] 备用API发送失败:', apiError);
          }
        }

      } catch (e) {
        console.error('[usertext] 发送群聊消息过程中出现错误:', e);
        this.$message.error('消息发送失败，请重试');
      } finally {
        // 重置发送状态
        this.sendingMessage = false;
      }
    },

    // 发送私聊消息
    async sendPrivateMessage(isEmergency = false) {
      try {
        // 设置发送状态
        if (!isEmergency) {
          this.sendingMessage = true;
        }

        // 检查基本条件
        if (!this.content.trim()) {
          this.$message.warning('不能发送空消息');
          return;
        }

        if (!this.currentChat) {
          this.$message.error('请先选择聊天对象');
          return;
        }

        // 生成唯一消息ID
        const messageId = Date.now() + '-' + Math.random().toString(36).substring(2, 10);
        const messageContent = this.content.trim();

        // 构造消息对象
        const msgObj = {
          messageId: messageId,
          id: messageId, // 同时设置id字段，确保兼容性
          content: messageContent,
          messageTypeId: 1,
          from: this.currentUser.username,
          fromId: this.currentUser.id,
          fromNickname: this.currentUser.nickname || this.currentUser.username,
          fromProfile: this.currentUser.userProfile || this.currentUser.avatar,
          to: this.currentChat.username,
          toId: this.currentChat.id,
          createTime: new Date().toISOString(),
          direction: 'sent' // 添加方向属性
        };

        console.log('[usertext] 准备发送私聊消息:', msgObj);

        // 1. 先在本地显示消息
        const messageKey = `${this.currentUser.username}#${this.currentChat.username}`;

        // 确保会话对象存在
        if (!this.$store.state.sessions) {
          this.$store.state.sessions = {};
        }

        if (!this.$store.state.sessions[messageKey]) {
          this.$store.state.sessions[messageKey] = [];
        }

        // 构造本地消息对象
        const localMessage = {
          ...msgObj,
          self: true
        };

        // 添加到本地存储
        this.$store.state.sessions[messageKey].push(localMessage);

        // 记录最后发送的消息ID
        this.lastSentMessageId = messageId;
        this.localMessages.add(messageId);

        // 通知UI立即显示
        this.$bus.$emit('new-message', {
          message: localMessage,
          addToInjected: true
        });

        // 清空输入框
        this.content = '';

        // 添加到消息日志
        this.messageLog.push({
          id: messageId,
          time: new Date(),
          status: 'local-added'
        });

        // 2. 尝试通过WebSocket发送
        try {
          if (!isEmergency) {
            await this.checkWebSocketConnection();
          }

          if (this.$store.state.stomp && this.$store.state.stomp.connected) {
            // 使用新的路径 /app/chat 而不是 /ws/chat
            this.$store.state.stomp.send("/app/chat", {}, JSON.stringify(msgObj));
            console.log('[usertext] 私聊消息已通过WebSocket发送');

            // 更新消息日志状态
            const logEntry = this.messageLog.find(log => log.id === messageId);
            if (logEntry) logEntry.status = 'ws-sent';

            // 发送成功，添加到已发送列表
            if (!this.$store.state.sentMessageIds) {
              this.$store.state.sentMessageIds = new Set();
            }
            this.$store.state.sentMessageIds.add(messageId);

            // 同时添加到处理过的消息ID集合，避免接收重复
            if (!this.$store.state.processedMessages) {
              this.$store.state.processedMessages = new Set();
            }
            this.$store.state.processedMessages.add(messageId);

          } else {
            throw new Error('WebSocket未连接');
          }
        } catch (error) {
          console.error('[usertext] WebSocket发送失败:', error);

          // 更新消息日志状态
          const logEntry = this.messageLog.find(log => log.id === messageId);
          if (logEntry) logEntry.status = 'ws-failed';

          // 3. 尝试通过备用API发送
          try {
            console.log('[usertext] 尝试通过备用API发送消息');

            const response = await fetch('/api/messages', {
              method: 'POST',
              headers: {
                'Content-Type': 'application/json'
              },
              body: JSON.stringify(msgObj)
            });

            if (response.ok) {
              console.log('[usertext] 备用API发送成功');

              // 更新消息日志状态
              if (logEntry) logEntry.status = 'api-sent';

              // 发送成功，添加到已发送列表
              if (!this.$store.state.sentMessageIds) {
                this.$store.state.sentMessageIds = new Set();
              }
              this.$store.state.sentMessageIds.add(messageId);
            } else {
              throw new Error(`API响应错误: ${response.status}`);
            }
          } catch (apiError) {
            console.error('[usertext] 备用API发送失败:', apiError);

            // 更新消息日志状态
            if (logEntry) logEntry.status = 'all-failed';

            if (!isEmergency) {
              this.$message.warning('消息已在界面显示，但未能发送到服务器');
            }
          }
        }

      } catch (e) {
        console.error('[usertext] 发送私聊消息过程中出现错误:', e);
        this.$message.error('发送消息失败: ' + e.message);
      } finally {
        // 重置发送状态
        this.sendingMessage = false;

        // 设置安全超时，确保状态重置
        if (this.sendingTimeout) {
          clearTimeout(this.sendingTimeout);
        }

        this.sendingTimeout = setTimeout(() => {
          if (this.sendingMessage) {
            console.warn('[usertext] 发送状态被超时强制重置');
            this.sendingMessage = false;
          }
        }, 5000);
      }
    },

    // 选择表情
    getEmo(index) {
      const textArea = this.$refs.messageInput;
      const emojiText = this.faceList[index];

      // 在当前光标位置插入表情
      if (textArea) {
        try {
          if (window.getSelection) {
            textArea.setRangeText(emojiText);
            textArea.selectionStart += emojiText.length;
            textArea.focus();
          } else if (document.selection) {
            textArea.focus();
            const sel = document.selection.createRange();
            sel.text = emojiText;
          }
          this.content = textArea.value;
        } catch (e) {
          // 如果setRangeText不可用，直接在末尾添加表情
          this.content += emojiText;
        }
      } else {
        this.content += emojiText;
      }
    },

    // 图片上传与发送
    beforeAvatarUpload(file) {
      // 检查图片大小
      const isLt1M = file.size / 1024 / 1024 < 1;
      if (!isLt1M) {
        this.$message.error('上传图片大小不能超过 1MB!');
        return false;
      }

      // 检查图片格式
      const imgType = file.name.substring(file.name.lastIndexOf(".") + 1).toLowerCase();
      const isImg = imgType === 'jpg' || imgType === 'png' || imgType === 'jpeg' || imgType === 'gif';
      if (!isImg) {
        this.$message.error('上传图片格式不符合要求！');
        return false;
      }

      return isLt1M && isImg;
    },

    imgSuccess(response, file, fileList) {
      try {
        // 检查基本条件
        if (!this.currentChat) {
          this.$message.error('请先选择聊天对象');
          return;
        }

        // 生成唯一消息ID
        const messageId = Date.now() + '-' + Math.random().toString(36).substring(2, 9);

        // 构造图片消息对象
        const msgObj = {
          messageId: messageId,
          id: messageId, // 同时设置id字段，确保兼容性
          content: response, // 图片URL
          messageTypeId: 2, // 设置消息类型为图片
          from: this.currentUser.username,
          fromId: this.currentUser.id,
          fromNickname: this.currentUser.nickname || this.currentUser.username,
          fromProfile: this.currentUser.userProfile || this.currentUser.avatar,
          createTime: new Date().toISOString(),
          direction: 'sent' // 添加方向属性
        };

        // 根据聊天类型设置目标
        if (this.isGroup) {
          msgObj.to = `group_${this.currentChat.id}`;
        } else {
          msgObj.to = this.currentChat.username;
          msgObj.toId = this.currentChat.id;
        }

        // 在本地显示图片消息
        const key = this.isGroup ?
            `群聊_${this.currentChat.id}` :
            `${this.currentUser.username}#${this.currentChat.username}`;

        // 确保会话对象存在
        if (!this.$store.state.sessions[key]) {
          this.$store.state.sessions[key] = [];
        }

        // 构造本地消息对象
        const localMsg = {
          ...msgObj,
          self: true
        };

        // 添加到本地存储
        this.$store.state.sessions[key].push(localMsg);
        this.localMessages.add(messageId);

        // 通知UI立即显示
        this.$bus.$emit('new-message', {
          message: localMsg,
          addToInjected: true
        });

        // 记录最后发送的消息ID
        this.lastSentMessageId = messageId;

        // 尝试通过WebSocket发送
        this.checkWebSocketConnection()
            .then(() => {
              // 使用新的路径 /app/chat 而不是 /ws/chat
              this.$store.state.stomp.send("/app/chat", {}, JSON.stringify(msgObj));
              console.log('[usertext] 图片消息已通过WebSocket发送');

              // 发送成功，添加到已发送和已处理列表
              if (!this.$store.state.sentMessageIds) {
                this.$store.state.sentMessageIds = new Set();
              }
              this.$store.state.sentMessageIds.add(messageId);

              if (!this.$store.state.processedMessages) {
                this.$store.state.processedMessages = new Set();
              }
              this.$store.state.processedMessages.add(messageId);
            })
            .catch(error => {
              console.error('[usertext] 发送图片消息失败:', error);
              this.$message.warning('图片已在界面显示，但可能未能发送到服务器');

              // 尝试通过备用API发送
              fetch('/api/messages', {
                method: 'POST',
                headers: {
                  'Content-Type': 'application/json'
                },
                body: JSON.stringify(msgObj)
              }).catch(apiError => {
                console.error('[usertext] 备用API发送图片消息失败:', apiError);
              });
            });

      } catch (error) {
        console.error('[usertext] 处理图片消息错误:', error);
        this.$message.error('图片消息处理失败');
      }
    },

    imgError(err, file, fileList) {
      console.error('[usertext] 上传图片失败:', err);
      this.$message.error('图片上传失败');
    }
  }
}
</script>

<style lang="scss">
/* el-popover是和app同级的，所以scoped的局部属性设置无效 */
.el-popover {
  max-height: 300px;
  max-width: 450px;
  overflow-y: auto;

  .emotionList {
    display: flex;
    flex-wrap: wrap;
    gap: 5px;

    a {
      width: 32px;
      height: 32px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 20px;
      border-radius: 4px;

      &:hover {
        background-color: #eef5fe;
      }
    }
  }
}
</style>

<style lang="scss" scoped>
#usertext-container {
  width: 100%;
  position: relative;
  display: flex;
  flex-direction: column;
  border-top: solid 1px #DDD;
  background-color: white;
  height: 150px;

  .usertext-toolbar {
    display: flex;
    align-items: center;
    padding: 5px;
    border-bottom: 1px solid #eee;

    #emojiBtn, #uploadImgBtn {
      border: none;
      padding: 5px 10px;
      background: transparent;

      &:hover {
        color: #409EFF;
        background-color: #f0f9ff;
      }

      &:focus {
        background-color: transparent;
      }
    }

    .el-dropdown {
      margin-left: auto;
    }
  }

  .input-area {
    flex: 1;
    position: relative;

    textarea {
      width: 100%;
      height: 100%;
      border: none;
      outline: none;
      resize: none;
      padding: 10px;
      box-sizing: border-box;
      font-size: 14px;

      &:disabled {
        background-color: #f9f9f9;
        color: #999;
      }
    }
  }

  .send-button-area {
    padding: 5px;
    text-align: right;

    #sendBtn {
      margin-right: 10px;
      padding: 8px 15px;
    }
  }

  .sending-indicator {
    position: absolute;
    top: 45px;
    left: 50%;
    transform: translateX(-50%);
    background-color: rgba(0, 0, 0, 0.7);
    color: white;
    padding: 5px 15px;
    border-radius: 15px;
    font-size: 12px;
    z-index: 100;
  }

  .debug-panel {
    position: absolute;
    bottom: 40px;
    left: 10px;
    background-color: rgba(255, 239, 219, 0.95);
    border: 1px solid #ffebc4;
    border-radius: 4px;
    padding: 8px;
    font-size: 12px;
    z-index: 999;
    max-width: 200px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);

    .debug-status, .connection-status {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 5px;
      padding-bottom: 5px;
      border-bottom: 1px dashed #ffd89e;

      .status-ok {
        color: #67c23a;
        font-weight: bold;
      }

      .status-busy {
        color: #e6a23c;
        font-weight: bold;
      }

      .status-error {
        color: #f56c6c;
        font-weight: bold;
      }
    }

    .debug-chat-info {
      font-family: monospace;
      color: #606266;
      line-height: 1.5;
    }
  }
}
</style>