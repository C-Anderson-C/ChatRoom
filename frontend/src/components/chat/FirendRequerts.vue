<template>
  <div class="friend-requests-container">
    <div class="section-header">
      <h3>好友请求
        <el-badge
            :value="friendRequests.length"
            :max="99"
            class="badge"
            v-show="friendRequests.length > 0">
        </el-badge>
      </h3>
      <el-button
          type="text"
          size="mini"
          icon="el-icon-refresh"
          @click="refreshRequests"
          :loading="loading"
          title="刷新好友请求">
      </el-button>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <i class="el-icon-loading"></i>
      <p>正在加载好友请求...</p>
    </div>

    <!-- 空状态 -->
    <el-empty
        v-else-if="friendRequests.length === 0"
        description="没有新的好友请求"
        :image-size="100">
    </el-empty>

    <!-- 好友请求列表 -->
    <div v-else class="requests-list">
      <el-card
          v-for="request in friendRequests"
          :key="request.id"
          class="request-card"
          shadow="hover">
        <div class="request-content">
          <div class="request-info">
            <el-avatar
                :size="50"
                :src="getAvatarUrl(request.friendProfile)"
                @error="() => handleAvatarError(request)">
              {{ getInitials(request) }}
            </el-avatar>

            <div class="user-details">
              <div class="username-info">
                <span class="nickname">{{ request.friendNickname || '未设置昵称' }}</span>
                <span class="username">({{ request.friendUsername }})</span>
              </div>

              <div class="request-time" v-if="request.createTime">
                请求时间: {{ formatTime(request.createTime) }}
              </div>

              <div class="request-message" v-if="request.message">
                <i class="el-icon-chat-dot-square"></i>
                <span>{{ request.message }}</span>
              </div>
            </div>
          </div>

          <div class="request-actions">
            <el-button
                size="small"
                type="primary"
                @click="handleAction(request, 1)"
                :loading="processingRequests[request.id] === 1"
                :disabled="!!processingRequests[request.id]">
              接受
            </el-button>
            <el-button
                size="small"
                type="danger"
                @click="handleAction(request, 2)"
                :loading="processingRequests[request.id] === 2"
                :disabled="!!processingRequests[request.id]">
              拒绝
            </el-button>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 错误提示 -->
    <div v-if="error" class="error-message">
      <el-alert
          title="加载失败"
          type="error"
          :description="errorMessage"
          show-icon
          :closable="false">
      </el-alert>
      <el-button size="small" type="primary" @click="refreshRequests" class="retry-button">
        重试
      </el-button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'FriendRequests',
  data() {
    return {
      friendRequests: [],
      loading: false,
      error: false,
      errorMessage: '',
      processingRequests: {}, // 用于跟踪正在处理的请求
      defaultAvatar: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA0MCA0MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48Y2lyY2xlIGN4PSIyMCIgY3k9IjIwIiByPSIyMCIgZmlsbD0iI2U1ZTVlNSIvPjxjaXJjbGUgY3g9IjIwIiBjeT0iMTYiIHI9IjYiIGZpbGw9IiNhMGEwYTAiLz48cGF0aCBkPSJNMTAgMzJDMTAgMjUuMzcgMTQuNDc3IDIwIDIwIDIwQzI1LjUyMyAyMCAzMCAyNS4zNyAzMCAzMkg0MFY0MEgwVjMySzEwIDMyWiIgZmlsbD0iI2EwYTBhMCIvPjwvc3ZnPg==',
      loadTimer: null, // 用于防抖加载
      autoRefreshInterval: null // 用于自动刷新
    };
  },
  computed: {
    currentUser() {
      return this.$store.state.currentUser || {};
    }
  },
  created() {
    // 初始加载好友请求
    this.loadFriendRequests();

    // 设置定期自动刷新
    this.autoRefreshInterval = setInterval(() => {
      this.refreshRequests(true); // 静默刷新，不显示加载状态
    }, 60000); // 每分钟刷新一次

    // 监听好友请求更新事件
    this.$bus.$on('refresh-friend-requests', this.refreshRequests);
  },
  beforeDestroy() {
    // 清理定时器
    if (this.loadTimer) {
      clearTimeout(this.loadTimer);
    }

    if (this.autoRefreshInterval) {
      clearInterval(this.autoRefreshInterval);
    }

    // 移除事件监听
    this.$bus.$off('refresh-friend-requests', this.refreshRequests);
  },
  methods: {
    // 获取有效的头像URL
    getAvatarUrl(profile) {
      if (!profile || profile === 'null' || profile === 'undefined') {
        return this.defaultAvatar;
      }

      if (profile.startsWith('http') || profile.startsWith('data:')) {
        return profile;
      }

      // 处理相对路径
      return `/img/${profile}`;
    },

    // 头像加载错误处理
    handleAvatarError(request) {
      console.log(`[FriendRequests] 好友请求头像加载失败: ${request.friendUsername}`);
    },

    // 获取用户名首字母
    getInitials(request) {
      if (request.friendNickname) {
        return request.friendNickname.charAt(0).toUpperCase();
      }
      return request.friendUsername.charAt(0).toUpperCase();
    },

    // 刷新请求列表
    refreshRequests(silent = false) {
      // 如果正在加载，取消重复请求
      if (this.loading && !silent) return;

      // 如果不是静默刷新，显示加载状态
      if (!silent) {
        this.loading = true;
      }

      // 清除之前的计时器
      if (this.loadTimer) {
        clearTimeout(this.loadTimer);
      }

      // 设置加载超时
      this.loadTimer = setTimeout(() => {
        if (this.loading) {
          this.loading = false;
          this.error = true;
          this.errorMessage = '加载超时，请稍后再试';
        }
      }, 10000);

      // 重置错误状态
      this.error = false;

      // 加载好友请求
      this.loadFriendRequests(silent);
    },

    // 加载好友请求
    loadFriendRequests(silent = false) {
      // 如果没有用户ID，不执行加载
      if (!this.currentUser || !this.currentUser.id) {
        console.warn('[FriendRequests] 未找到当前用户ID，无法加载好友请求');

        if (!silent) {
          this.loading = false;
          this.error = true;
          this.errorMessage = '用户信息不完整，请重新登录';

          if (this.loadTimer) {
            clearTimeout(this.loadTimer);
          }
        }

        return;
      }

      const userId = this.currentUser.id;

      this.getRequest(`/friend/requests?userId=${userId}`)
          .then(resp => {
            // 清除加载定时器
            if (this.loadTimer) {
              clearTimeout(this.loadTimer);
            }

            if (!silent) {
              this.loading = false;
            }

            if (resp && Array.isArray(resp)) {
              // 更新好友请求列表
              this.friendRequests = resp;

              // 如果有新的好友请求，通过总线通知其他组件
              if (resp.length > 0) {
                this.$bus.$emit('friend-requests-updated', resp.length);
              }
            } else {
              console.warn('[FriendRequests] 加载好友请求返回无效数据:', resp);
              this.friendRequests = [];

              if (!silent) {
                this.error = true;
                this.errorMessage = '加载好友请求数据格式错误';
              }
            }
          })
          .catch(err => {
            console.error('[FriendRequests] 加载好友请求失败:', err);

            // 清除加载定时器
            if (this.loadTimer) {
              clearTimeout(this.loadTimer);
            }

            if (!silent) {
              this.loading = false;
              this.error = true;
              this.errorMessage = err.message || '加载好友请求失败，请稍后再试';
            }

            // 确保有一个空数组
            this.friendRequests = [];
          });
    },

    // 处理好友请求操作
    handleAction(request, response) {
      // 获取请求ID
      const requestId = request.id;

      // 防止重复操作
      if (this.processingRequests[requestId]) {
        return;
      }

      // 设置正在处理的状态
      this.$set(this.processingRequests, requestId, response);

      // 发送响应请求
      this.respondRequest(requestId, response)
          .then(success => {
            if (success) {
              // 操作成功，从列表中移除该请求
              this.friendRequests = this.friendRequests.filter(req => req.id !== requestId);

              // 通知其他组件更新好友请求计数
              this.$bus.$emit('friend-requests-updated', this.friendRequests.length);

              // 如果是接受好友，触发刷新好友列表
              if (response === 1) {
                this.$emit('refresh-friends');
                this.$bus.$emit('refresh-friend-list');
              }
            }
          })
          .finally(() => {
            // 无论成功或失败，清除处理状态
            this.$delete(this.processingRequests, requestId);
          });
    },

    // 响应好友请求
    respondRequest(requestId, response) {
      return new Promise((resolve) => {
        this.postRequest('/friend/respond', {
          requestId: requestId,
          response: response
        }).then(resp => {
          if (resp) {
            const actionText = response === 1 ? '接受' : '拒绝';
            this.$message.success(`已${actionText}好友请求`);
            resolve(true);
          } else {
            this.$message.error('操作失败，请稍后重试');
            resolve(false);
          }
        }).catch(err => {
          console.error('[FriendRequests] 响应好友请求失败:', err);
          this.$message.error(err.message || '操作失败，请稍后重试');
          resolve(false);
        });
      });
    },

    // 格式化时间
    formatTime(timestamp) {
      if (!timestamp) return '';

      try {
        const date = new Date(timestamp);
        if (isNaN(date.getTime())) return '';

        const now = new Date();
        const diffMinutes = Math.floor((now - date) / (60 * 1000));

        if (diffMinutes < 1) {
          return '刚刚';
        } else if (diffMinutes < 60) {
          return `${diffMinutes}分钟前`;
        } else if (diffMinutes < 60 * 24) {
          const hours = Math.floor(diffMinutes / 60);
          return `${hours}小时前`;
        } else {
          return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
        }
      } catch (e) {
        console.error('[FriendRequests] 格式化时间错误:', e);
        return '';
      }
    }
  }
};
</script>

<style scoped>
.friend-requests-container {
  padding: 10px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.section-header h3 {
  margin: 0;
  display: flex;
  align-items: center;
}

.badge {
  margin-top: -2px;
  margin-left: 5px;
}

.loading-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
}

.loading-state i {
  font-size: 24px;
  margin-bottom: 10px;
}

.loading-state p {
  margin: 0;
}

.requests-list {
  flex: 1;
  overflow-y: auto;
}

.request-card {
  margin-bottom: 15px;
  border-radius: 8px;
  transition: all 0.3s;
}

.request-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.request-content {
  display: flex;
  flex-direction: column;
}

.request-info {
  display: flex;
  align-items: flex-start;
  margin-bottom: 15px;
}

.user-details {
  flex: 1;
  margin-left: 15px;
  overflow: hidden;
}

.username-info {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 5px;
}

.nickname {
  font-weight: bold;
  margin-right: 5px;
  font-size: 16px;
}

.username {
  color: #909399;
  font-size: 13px;
}

.request-time {
  color: #909399;
  font-size: 12px;
  margin-bottom: 5px;
}

.request-message {
  background-color: #f5f7fa;
  padding: 8px 12px;
  border-radius: 4px;
  margin-top: 5px;
  font-size: 13px;
  color: #606266;
  display: flex;
  align-items: flex-start;
}

.request-message i {
  margin-right: 5px;
  margin-top: 3px;
}

.request-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.error-message {
  margin-top: 15px;
  text-align: center;
}

.retry-button {
  margin-top: 10px;
}
</style>