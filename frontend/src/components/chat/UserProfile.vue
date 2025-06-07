<!-- src/views/UserProfile.vue -->
<template>
  <div class="user-profile-container">
    <el-card class="box-card">
      <div slot="header" class="clearfix">
        <span>个人中心</span>
        <el-button style="float: right; padding: 3px 0" type="text" @click="editProfile">编辑信息</el-button>
      </div>
      <div v-if="user" class="user-info">
        <div class="avatar-container">
          <el-avatar :size="100" :src="user.userProfile || defaultAvatar"></el-avatar>
          <el-upload
              v-if="isEditing"
              class="avatar-uploader"
              action="/file"
              :show-file-list="false"
              :on-success="handleAvatarSuccess"
              :before-upload="beforeAvatarUpload">
            <el-button size="small" type="primary">更换头像</el-button>
          </el-upload>
        </div>

        <div class="info-section">
          <template v-if="!isEditing">
            <div class="info-item">
              <span class="label">用户名:</span>
              <span class="value">{{ user.username }}</span>
            </div>
            <div class="info-item">
              <span class="label">昵称:</span>
              <span class="value">{{ user.nickname }}</span>
            </div>
            <div class="info-item" v-if="user.email">
              <span class="label">邮箱:</span>
              <span class="value">{{ user.email }}</span>
            </div>
            <div class="info-item">
              <span class="label">账号创建:</span>
              <span class="value">{{ formatDate(user.create_time) }}</span>
            </div>
          </template>

          <template v-else>
            <div class="info-item">
              <span class="label">用户名:</span>
              <span class="value">{{ user.username }}</span>
              <span class="hint">(用户名不可更改)</span>
            </div>
            <div class="info-item">
              <span class="label">昵称:</span>
              <el-input v-model="form.nickname" placeholder="请输入昵称"></el-input>
            </div>
            <div class="info-item">
              <span class="label">邮箱:</span>
              <el-input v-model="form.email" placeholder="请输入邮箱"></el-input>
            </div>
            <div class="button-group">
              <el-button type="primary" @click="saveProfile">保存</el-button>
              <el-button @click="cancelEdit">取消</el-button>
            </div>
          </template>
        </div>
      </div>

      <div v-else class="loading-container">
        <el-skeleton :rows="6" animated />
      </div>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'UserProfile',
  data() {
    return {
      user: null,
      form: {
        nickname: '',
        email: '',
        userProfile: ''
      },
      isEditing: false,
      defaultAvatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
    };
  },
  created() {
    this.loadUserData();
  },
  methods: {
    loadUserData() {
      try {
        const userData = window.sessionStorage.getItem('user');
        if (userData) {
          this.user = JSON.parse(userData);
          console.log('用户数据加载成功:', this.user);
          // 初始化表单数据
          this.form.nickname = this.user.nickname;
          this.form.email = this.user.email || '';
          this.form.userProfile = this.user.userProfile;
        } else {
          console.warn('未找到用户数据，尝试从服务器获取');
          this.getUserDataFromServer();
        }
      } catch (error) {
        console.error('解析用户数据出错:', error);
        this.$message.error('加载用户信息失败');
      }
    },

    getUserDataFromServer() {
      this.getRequest('/user/info').then(resp => {
        if (resp) {
          this.user = resp;
          // 初始化表单数据
          this.form.nickname = this.user.nickname;
          this.form.email = this.user.email || '';
          this.form.userProfile = this.user.userProfile;
        }
      }).catch(error => {
        console.error('获取用户数据失败:', error);
        this.$message.error('无法从服务器获取用户信息');
      });
    },

    editProfile() {
      this.isEditing = true;
    },

    cancelEdit() {
      this.isEditing = false;
      // 重置表单
      this.form.nickname = this.user.nickname;
      this.form.email = this.user.email || '';
    },

    saveProfile() {
      if (!this.form.nickname.trim()) {
        this.$message.warning('昵称不能为空');
        return;
      }

      const params = new FormData();
      params.append('id', this.user.id);
      params.append('nickname', this.form.nickname);
      params.append('email', this.form.email);
      params.append('userProfile', this.form.userProfile);

      this.putRequest('/user/update', params).then(resp => {
        if (resp) {
          this.$message.success('个人信息更新成功');
          // 更新本地存储的用户信息
          this.user.nickname = this.form.nickname;
          this.user.email = this.form.email;
          this.user.userProfile = this.form.userProfile;
          window.sessionStorage.setItem('user', JSON.stringify(this.user));
          this.isEditing = false;
        }
      }).catch(error => {
        console.error('更新个人信息失败:', error);
        this.$message.error('更新个人信息失败，请稍后重试');
      });
    },

    handleAvatarSuccess(res) {
      if (res && res.data) {
        this.form.userProfile = res.data;
        this.$message.success('头像上传成功');
      }
    },

    beforeAvatarUpload(file) {
      const isJPG = file.type === 'image/jpeg' || file.type === 'image/png';
      const isLt2M = file.size / 1024 / 1024 < 2;

      if (!isJPG) {
        this.$message.error('上传头像图片只能是 JPG 或 PNG 格式!');
      }
      if (!isLt2M) {
        this.$message.error('上传头像图片大小不能超过 2MB!');
      }
      return isJPG && isLt2M;
    },

    formatDate(dateString) {
      if (!dateString) return '未知';

      try {
        const date = new Date(dateString);
        return date.toLocaleString();
      } catch (e) {
        return dateString;
      }
    }
  }
};
</script>

<style scoped>
.user-profile-container {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.avatar-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 20px;
}

.avatar-uploader {
  margin-top: 10px;
}

.info-section {
  padding: 10px 0;
}

.info-item {
  margin-bottom: 15px;
  display: flex;
  align-items: center;
}

.label {
  font-weight: bold;
  width: 100px;
  color: #606266;
}

.value {
  flex: 1;
}

.hint {
  color: #909399;
  font-size: 12px;
  margin-left: 10px;
}

.button-group {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.loading-container {
  padding: 20px;
}
</style>