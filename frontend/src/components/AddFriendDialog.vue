<template>
  <el-dialog title="添加好友" :visible.sync="dialogVisible" width="30%">
    <el-input
        v-model="searchUserText"
        placeholder="请输入用户名或昵称"
        prefix-icon="el-icon-search"
        clearable>
    </el-input>

    <div v-if="searchResults.length > 0" class="search-results">
      <div
          v-for="user in searchResults"
          :key="user.id"
          class="user-item">
        <el-avatar :src="user.avatar || defaultAvatar" :size="40"></el-avatar>
        <div class="user-info">
          <div class="nickname">{{ user.nickname }}</div>
          <div class="username">{{ user.username }}</div>
        </div>
        <el-button
            size="small"
            type="primary"
            @click="addFriend(user)"
            :disabled="user.isFriend">
          {{ user.isFriend ? '已是好友' : '添加' }}
        </el-button>
      </div>
    </div>

    <div v-else-if="searchUserText && searchCompleted" class="no-result">
      未找到匹配的用户
    </div>

    <div slot="footer" class="dialog-footer">
      <el-button @click="dialogVisible = false">取 消</el-button>
      <el-button type="primary" @click="searchUser">搜 索</el-button>
    </div>
  </el-dialog>
</template>

<script>
export default {
  name: 'AddFriendDialog',
  props: {
    visible: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      dialogVisible: false,
      searchUserText: '',
      searchResults: [],
      searchCompleted: false,
      defaultAvatar: '/path/to/default-avatar.png'
    }
  },
  watch: {
    visible(val) {
      this.dialogVisible = val;
    },
    dialogVisible(val) {
      if (!val) {
        this.$emit('update:visible', false);
      }
    }
  },
  methods: {
    searchUser() {
      if (!this.searchUserText) {
        this.$message.warning('请输入搜索内容');
        return;
      }

      const userId = this.$store.state.currentUser.id;

      // 使用 axios 或您的请求方法搜索用户
      this.$http.get(`/user/search?keyword=${this.searchUserText}&currentUserId=${userId}`)
          .then(response => {
            if (response.status === 200) {
              this.searchResults = response.data;
              this.searchCompleted = true;
            }
          })
          .catch(err => {
            console.error('搜索用户失败:', err);
            this.$message.error('搜索失败，请稍后再试');
          });
    },

    addFriend(user) {
      const userId = this.$store.state.currentUser.id;

      // 发送添加好友请求
      this.$http.post('/friend/request', {
        userId: userId,
        friendId: user.id
      })
          .then(response => {
            if (response.status === 200) {
              this.$message.success('好友请求已发送');
              // 标记为已是好友（或已发送请求）
              user.isFriend = true;
            }
          })
          .catch(err => {
            console.error('添加好友失败:', err);
            this.$message.error('添加失败，请稍后再试');
          });
    }
  }
}
</script>

<style scoped>
.search-results {
  margin-top: 15px;
  max-height: 300px;
  overflow-y: auto;
}

.user-item {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #eee;
}

.user-info {
  margin-left: 10px;
  flex: 1;
}

.nickname {
  font-size: 16px;
  font-weight: 500;
}

.username {
  font-size: 12px;
  color: #999;
}

.no-result {
  text-align: center;
  padding: 20px;
  color: #999;
}
</style>