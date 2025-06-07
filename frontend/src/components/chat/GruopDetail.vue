<!-- src/components/chat/GroupDetail.vue -->
<template>
  <div class="group-detail">
    <div class="group-header">
      <div class="group-info">
        <h3>{{ group.name }}</h3>
        <p class="group-desc">{{ group.description }}</p>
      </div>
    </div>

    <el-divider content-position="left">成员列表</el-divider>

    <div class="member-list">
      <div v-for="member in members" :key="member.userId" class="member-item">
        <img :src="member.userProfile" class="member-avatar">
        <div class="member-info">
          <div class="member-name">{{ member.nickname }}</div>
          <div class="member-role">
            <span v-if="member.role === 2">群主</span>
            <span v-else-if="member.role === 1">管理员</span>
          </div>
        </div>
      </div>
    </div>

    <div class="group-actions">
      <el-button type="danger" @click="quitGroup" v-if="currentUserRole !== 2">退出群聊</el-button>
      <el-button type="danger" @click="dismissGroup" v-if="currentUserRole === 2">解散群聊</el-button>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    group: {
      type: Object,
      required: true
    }
  },
  data() {
    return {
      members: [],
      currentUserRole: 0,
      currentUser: JSON.parse(window.sessionStorage.getItem("user"))
    };
  },
  created() {
    this.loadGroupMembers();
  },
  methods: {
    loadGroupMembers() {
      this.getRequest(`/group/${this.group.id}/members`).then(resp => {
        if (resp) {
          this.members = resp;
          // 查找当前用户角色
          const currentUser = resp.find(member => member.userId === this.currentUser.id);
          if (currentUser) {
            this.currentUserRole = currentUser.role;
          }
        }
      });
    },
    quitGroup() {
      this.$confirm('确定要退出该群聊吗?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.postRequest(`/group/${this.group.id}/quit`, {
          userId: this.currentUser.id
        }).then(resp => {
          if (resp) {
            this.$message.success('已退出群聊');
            this.$emit('quit');
          }
        });
      }).catch(() => {});
    },
    dismissGroup() {
      this.$confirm('确定要解散该群聊吗? 此操作不可逆!', '警告', {
        confirmButtonText: '确定解散',
        cancelButtonText: '取消',
        type: 'danger'
      }).then(() => {
        this.postRequest(`/group/${this.group.id}/dismiss`, {
          creatorId: this.currentUser.id
        }).then(resp => {
          if (resp) {
            this.$message.success('群聊已解散');
            this.$emit('dismiss');
          }
        });
      }).catch(() => {});
    }
  }
};
</script>

<style scoped>
.group-detail {
  padding: 10px;
}
.group-header {
  margin-bottom: 20px;
}
.group-info h3 {
  margin: 0 0 5px 0;
}
.group-desc {
  margin: 0;
  color: #606266;
  font-size: 14px;
}
.member-list {
  margin-top: 10px;
  max-height: 300px;
  overflow-y: auto;
}
.member-item {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #EBEEF5;
}
.member-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  margin-right: 10px;
}
.member-info {
  flex: 1;
}
.member-name {
  font-weight: bold;
}
.member-role {
  font-size: 12px;
  color: #909399;
}
.group-actions {
  margin-top: 20px;
  text-align: center;
}
</style>