<template>
  <el-dialog
      title="添加好友"
      :visible.sync="dialogVisible"
      width="500px"
      @open="onDialogOpen"
      :close-on-click-modal="false"
      :destroy-on-close="true">

    <!-- 搜索区域 -->
    <div class="search-container">
      <el-input
          placeholder="搜索用户昵称或用户名..."
          v-model="searchText"
          class="search-input"
          :disabled="isSearching"
          @keyup.enter.native="searchUser"
          prefix-icon="el-icon-user">
        <el-button
            slot="append"
            icon="el-icon-search"
            @click="searchUser"
            :loading="isSearching">
        </el-button>
      </el-input>

      <!-- 搜索结果展示 -->
      <div class="search-results">
        <!-- 搜索中状态 -->
        <div v-if="isSearching" class="searching">
          <i class="el-icon-loading"></i> 正在搜索...
        </div>

        <!-- 搜索结果列表 -->
        <el-table
            v-else-if="searchResults.length > 0"
            :data="searchResults"
            style="width: 100%"
            height="300"
            border>
          <!-- 用户头像 -->
          <el-table-column label="头像" width="70" align="center">
            <template slot-scope="scope">
              <el-avatar
                  :size="40"
                  :src="scope.row.avatar || defaultAvatar">
                {{ getInitials(scope.row) }}
              </el-avatar>
            </template>
          </el-table-column>

          <!-- 用户信息 -->
          <el-table-column prop="nickname" label="用户昵称">
            <template slot-scope="scope">
              <div class="user-info">
                <div class="nickname">{{ scope.row.nickname || '未设置昵称' }}</div>
                <div class="username">@{{ scope.row.username }}</div>
              </div>
            </template>
          </el-table-column>

          <!-- 操作按钮 -->
          <el-table-column label="操作" width="130" align="center">
            <template slot-scope="scope">
              <el-tooltip :content="getFriendStatusTooltip(scope.row)" placement="top" :disabled="!getFriendStatusTooltip(scope.row)">
                <el-button
                    size="mini"
                    :type="getFriendStatusType(scope.row)"
                    @click="handleFriendAction(scope.row)"
                    :loading="addingFriend === scope.row.id"
                    :icon="getFriendStatusIcon(scope.row)"
                    :disabled="isButtonDisabled(scope.row)">
                  {{ getFriendStatusText(scope.row) }}
                </el-button>
              </el-tooltip>
            </template>
          </el-table-column>
        </el-table>

        <!-- 搜索错误状态 -->
        <div v-else-if="searchError" class="search-error">
          <i class="el-icon-warning-outline"></i>
          <p>{{ searchErrorMessage }}</p>
          <el-button type="primary" size="small" @click="searchUser">重试</el-button>
        </div>

        <!-- 无搜索结果状态 -->
        <div v-else-if="hasSearched && !isSearching && searchResults.length === 0" class="no-result">
          <i class="el-icon-search"></i>
          <p>没有找到匹配的用户</p>
          <el-button type="text" @click="showSearchTips">查看搜索提示</el-button>
        </div>

        <!-- 初始状态提示 -->
        <div v-else class="initial-state">
          <i class="el-icon-user-solid"></i>
          <p>输入用户名或昵称搜索好友</p>
        </div>
      </div>
    </div>

    <!-- 对话框底部按钮 -->
    <span slot="footer" class="dialog-footer">
      <el-button @click="dialogVisible = false">关闭</el-button>
      <el-button
          type="primary"
          @click="searchUser"
          :disabled="!searchText || isSearching">
        {{ isSearching ? '搜索中...' : '搜索' }}
      </el-button>
    </span>

    <!-- 好友请求结果对话框 -->
    <el-dialog
        title="好友请求已发送"
        :visible.sync="requestSentDialogVisible"
        width="300px"
        append-to-body
        center>
      <div class="request-sent-dialog">
        <i class="el-icon-success"></i>
        <p>好友请求已成功发送</p>
        <p class="subtitle">请等待对方接受</p>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button type="primary" @click="requestSentDialogVisible = false">确定</el-button>
      </span>
    </el-dialog>

    <!-- 搜索提示对话框 -->
    <el-dialog
        title="搜索提示"
        :visible.sync="searchTipsVisible"
        width="400px"
        append-to-body>
      <div class="search-tips">
        <p><i class="el-icon-info"></i> 您可以通过以下方式搜索用户：</p>
        <ul>
          <li>输入完整的用户名（例如：user123）</li>
          <li>输入用户昵称（例如：张三）</li>
          <li>输入用户名或昵称的一部分进行模糊搜索</li>
        </ul>
        <p>搜索结果将显示与您输入内容最匹配的用户。</p>
      </div>
    </el-dialog>
  </el-dialog>
</template>

<script>
export default {
  name: 'FriendSearch',
  props: {
    visible: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      dialogVisible: false,
      searchText: '',
      searchResults: [],
      hasSearched: false,
      isSearching: false,
      addingFriend: null,
      searchError: false,
      searchErrorMessage: '',
      defaultAvatar: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA0MCA0MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48Y2lyY2xlIGN4PSIyMCIgY3k9IjIwIiByPSIyMCIgZmlsbD0iI2U1ZTVlNSIvPjxjaXJjbGUgY3g9IjIwIiBjeT0iMTYiIHI9IjYiIGZpbGw9IiNhMGEwYTAiLz48cGF0aCBkPSJNMTAgMzJDMTAgMjUuMzcgMTQuNDc3IDIwIDIwIDIwQzI1LjUyMyAyMCAzMCAyNS4zNyAzMCAzMkg0MFY0MEgwVjMySzEwIDMyWiIgZmlsbD0iI2EwYTBhMCIvPjwvc3ZnPg==',
      requestSentDialogVisible: false,
      searchTipsVisible: false,
      lastSearchTime: 0,
      searchCount: 0
    }
  },
  watch: {
    visible: {
      immediate: true,
      handler(val) {
        console.log('[FriendSearch] visible prop changed to', val);
        this.dialogVisible = val;
      }
    },
    dialogVisible(val) {
      console.log('[FriendSearch] dialog visibility changed to', val);
      this.$emit('update:visible', val);
    }
  },
  methods: {
    onDialogOpen() {
      console.log('[FriendSearch] dialog opened');
      this.searchText = '';
      this.searchResults = [];
      this.hasSearched = false;
      this.searchError = false;

      // 获取焦点
      this.$nextTick(() => {
        const inputEl = this.$el.querySelector('.search-input input');
        if (inputEl) inputEl.focus();
      });
    },

    // 获取用户头像缺失时显示的文字头像
    getInitials(user) {
      if (user.nickname) return user.nickname.charAt(0).toUpperCase();
      if (user.username) return user.username.charAt(0).toUpperCase();
      return '?';
    },

    getFriendStatusType(user) {
      if (user.friendStatus === 1 || user.isFriend) return 'success';
      if (user.friendStatus === 2) return 'info';
      if (user.friendStatus === 3) return 'warning';
      return 'primary';
    },

    getFriendStatusIcon(user) {
      if (user.friendStatus === 1 || user.isFriend) return 'el-icon-check';
      if (user.friendStatus === 2) return 'el-icon-time';
      if (user.friendStatus === 3) return 'el-icon-bell';
      return 'el-icon-plus';
    },

    getFriendStatusText(user) {
      if (user.friendStatus === 1 || user.isFriend) return '已是好友';
      if (user.friendStatus === 2) return '已发送';
      if (user.friendStatus === 3) return '接受请求';
      return '添加好友';
    },

    getFriendStatusTooltip(user) {
      if (user.friendStatus === 1 || user.isFriend) return '该用户已经是您的好友';
      if (user.friendStatus === 2) return '您已向该用户发送了好友请求';
      if (user.friendStatus === 3) return '该用户向您发送了好友请求';
      return '';
    },

    isButtonDisabled(user) {
      return user.friendStatus === 1 || user.isFriend || user.friendStatus === 2;
    },

    handleFriendAction(user) {
      if (user.friendStatus === 3) {
        this.acceptFriendRequest(user.id);
      } else if (user.friendStatus !== 1 && !user.isFriend && user.friendStatus !== 2) {
        this.addFriend(user.id);
      }
    },

    searchUser() {
      if (!this.searchText || this.searchText.trim() === '') {
        this.$message.warning('请输入搜索内容');
        return;
      }

      // 限制搜索频率，避免过于频繁
      const now = Date.now();
      if (now - this.lastSearchTime < 2000) {
        this.$message.warning('请勿频繁搜索，请稍后再试');
        return;
      }

      this.lastSearchTime = now;
      this.searchCount++;

      this.isSearching = true;
      this.hasSearched = true;
      this.searchError = false;

      // 确保获取当前用户ID
      const currentUserId = this.$store.state.currentUser?.id;
      if (!currentUserId) {
        this.$message.error('未获取到用户信息，请重新登录');
        this.isSearching = false;
        return;
      }

      console.log('[FriendSearch] 开始搜索用户:', {
        keyword: this.searchText,
        currentUserId: currentUserId
      });

      // 发送搜索请求
      this.getRequest(`/user/search?keyword=${encodeURIComponent(this.searchText.trim())}&currentUserId=${currentUserId}`)
          .then(resp => {
            this.isSearching = false;
            console.log('[FriendSearch] 搜索用户响应:', resp);

            if (resp && Array.isArray(resp)) {
              // 检查结果数组
              const validResults = resp.filter(user =>
                  user && typeof user === 'object' && user.id && user.username
              );

              if (validResults.length < resp.length) {
                console.warn('[FriendSearch] 某些搜索结果缺少必要字段:',
                    resp.filter(user => !validResults.includes(user)));
              }

              this.searchResults = validResults;

              if (validResults.length === 0) {
                this.$message.info('没有找到匹配的用户');
              }
            } else if (resp && resp.message) {
              // 服务器返回错误消息
              this.searchError = true;
              this.searchErrorMessage = resp.message;
              this.searchResults = [];
              this.$message.error(resp.message);
            } else {
              // 意外的响应格式
              this.searchResults = [];
              this.searchError = true;
              this.searchErrorMessage = '服务器返回了意外的数据格式';
              this.$message.error('搜索失败：意外的数据格式');
            }
          })
          .catch(err => {
            this.isSearching = false;
            this.searchError = true;
            this.searchResults = [];
            console.error('[FriendSearch] 搜索用户失败:', err);

            if (err.response) {
              console.error('[FriendSearch] 错误状态:', err.response.status);
              console.error('[FriendSearch] 错误数据:', err.response.data);

              const errorMsg = err.response.data?.msg || err.response.data?.message || `搜索失败(${err.response.status})`;
              this.searchErrorMessage = errorMsg;
              this.$message.error(errorMsg);
            } else if (err.request) {
              this.searchErrorMessage = '无法连接到服务器，请检查网络连接';
              this.$message.error(this.searchErrorMessage);
            } else {
              this.searchErrorMessage = '搜索请求发送失败';
              this.$message.error(this.searchErrorMessage);
            }
          });
    },

    addFriend(friendId) {
      // 检查是否已是好友
      const targetUser = this.searchResults.find(u => u.id === friendId);
      if (!targetUser) {
        this.$message.error('用户信息不完整');
        return;
      }

      // 判断好友状态
      if (targetUser.friendStatus === 1 || targetUser.isFriend) {
        this.$message.info('该用户已经是您的好友');
        return;
      } else if (targetUser.friendStatus === 2) {
        this.$message.info('已发送好友请求，请等待对方处理');
        return;
      } else if (targetUser.friendStatus === 3) {
        // 处理接收到的好友请求
        this.acceptFriendRequest(friendId);
        return;
      }

      // 检查当前用户信息
      if (!this.$store.state.currentUser || !this.$store.state.currentUser.id) {
        this.$message.error('用户信息不完整，请重新登录');
        return;
      }

      this.addingFriend = friendId;

      // 准备请求数据
      const requestData = {
        userId: this.$store.state.currentUser.id,
        friendId: friendId
      };

      console.log('[FriendSearch] 发送添加好友请求:', requestData);

      // 发送添加好友请求
      this.postRequest('/friend/sendRequest', requestData)
          .then(resp => {
            this.addingFriend = null;
            console.log('[FriendSearch] 添加好友响应:', resp);

            if (resp && resp.status === 200) {
              // 成功添加好友请求
              this.$message.success(resp.msg || '好友请求已发送');

              // 显示成功发送对话框
              this.requestSentDialogVisible = true;

              // 更新搜索结果中的用户状态
              const index = this.searchResults.findIndex(u => u.id === friendId);
              if (index !== -1) {
                // 更新好友状态为"已发送请求"
                this.$set(this.searchResults[index], 'friendStatus', 2);
              }

              // 通知其他组件刷新好友列表
              this.$nextTick(() => {
                this.$bus && this.$bus.$emit('refresh-friend-list');
              });

              // 如果有回调函数，触发它
              if (typeof this.$emit === 'function') {
                this.$emit('friend-added', targetUser);
              }
            } else {
              // 处理错误
              this.$message.error(resp.msg || '发送好友请求失败');
            }
          })
          .catch(err => {
            this.addingFriend = null;
            console.error('[FriendSearch] 添加好友错误:', err);

            if (err.response && err.response.data) {
              this.$message.error(err.response.data.msg || '发送好友请求失败，请稍后重试');
            } else {
              this.$message.error('发送好友请求失败，请检查网络连接');
            }
          });
    },

    acceptFriendRequest(friendId) {
      // 检查当前用户信息
      if (!this.$store.state.currentUser || !this.$store.state.currentUser.id) {
        this.$message.error('用户信息不完整，请重新登录');
        return;
      }

      const currentUserId = this.$store.state.currentUser.id;

      this.addingFriend = friendId;

      console.log('[FriendSearch] 接受好友请求:', { userId: currentUserId, friendId });

      this.postRequest('/friend/acceptRequest', {
        userId: currentUserId,
        friendId: friendId
      })
          .then(resp => {
            this.addingFriend = null;

            if (resp && resp.status === 200) {
              this.$message.success(resp.msg || '已接受好友请求');

              // 更新用户状态
              const index = this.searchResults.findIndex(u => u.id === friendId);
              if (index !== -1) {
                // 更新为已是好友
                this.$set(this.searchResults[index], 'friendStatus', 1);
                this.$set(this.searchResults[index], 'isFriend', true);
              }

              // 通知其他组件刷新好友列表
              this.$nextTick(() => {
                this.$bus && this.$bus.$emit('refresh-friend-list');
              });
            } else {
              this.$message.error(resp.msg || '接受好友请求失败');
            }
          })
          .catch(err => {
            this.addingFriend = null;
            console.error('[FriendSearch] 接受好友请求错误:', err);

            if (err.response && err.response.data) {
              this.$message.error(err.response.data.msg || '接受好友请求失败，请稍后重试');
            } else {
              this.$message.error('接受好友请求失败，请检查网络连接');
            }
          });
    },

    showSearchTips() {
      this.searchTipsVisible = true;
    }
  }
}
</script>

<style scoped>
.search-container {
  margin: 10px 0;
}

.search-input {
  margin-bottom: 15px;
}

.search-results {
  min-height: 100px;
  position: relative;
}

.searching, .no-result, .search-error, .initial-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 30px 0;
  text-align: center;
}

.searching i, .no-result i, .search-error i, .initial-state i {
  font-size: 24px;
  margin-bottom: 10px;
}

.searching {
  color: #909399;
}

.no-result {
  color: #909399;
}

.search-error {
  color: #F56C6C;
}

.initial-state {
  color: #909399;
}

.search-error p {
  margin: 5px 0;
}

.user-info {
  display: flex;
  flex-direction: column;
}

.nickname {
  font-weight: bold;
}

.username {
  font-size: 12px;
  color: #909399;
}

.request-sent-dialog {
  text-align: center;
  padding: 10px 0;
}

.request-sent-dialog i {
  color: #67C23A;
  font-size: 40px;
  margin-bottom: 15px;
}

.request-sent-dialog .subtitle {
  color: #909399;
  font-size: 14px;
  margin-top: 5px;
}

.search-tips {
  padding: 10px;
}

.search-tips p {
  margin: 10px 0;
}

.search-tips i {
  color: #409EFF;
  margin-right: 5px;
}

.search-tips ul {
  margin: 10px 0;
  padding-left: 25px;
}

.search-tips li {
  margin: 5px 0;
}
</style>