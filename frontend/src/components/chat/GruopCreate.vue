<!-- src/components/chat/GroupCreate.vue -->
<template>
  <div>
    <el-form :model="groupForm" :rules="rules" ref="groupForm" label-width="80px">
      <el-form-item label="群名称" prop="name">
        <el-input v-model="groupForm.name" placeholder="请输入群名称"></el-input>
      </el-form-item>

      <el-form-item label="群描述" prop="description">
        <el-input type="textarea" v-model="groupForm.description" placeholder="请输入群描述"></el-input>
      </el-form-item>

      <el-form-item label="选择成员" prop="members">
        <el-transfer
            v-model="selectedMembers"
            :data="friendList"
            :titles="['好友列表', '已选成员']"
            :props="{
            key: 'id',
            label: 'nickname'
          }"
            @change="handleChange">
        </el-transfer>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="submitForm('groupForm')">创建群聊</el-button>
        <el-button @click="resetForm('groupForm')">重置</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
export default {
  data() {
    return {
      groupForm: {
        name: '',
        description: '',
        creatorId: JSON.parse(window.sessionStorage.getItem("user")).id,
        members: []
      },
      rules: {
        name: [
          { required: true, message: '请输入群名称', trigger: 'blur' },
          { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
        ],
        description: [
          { max: 200, message: '长度不能超过 200 个字符', trigger: 'blur' }
        ]
      },
      friendList: [],
      selectedMembers: []
    };
  },
  created() {
    this.loadFriendList();
  },
  methods: {
    loadFriendList() {
      const userId = JSON.parse(window.sessionStorage.getItem("user")).id;
      this.getRequest(`/friend/list?userId=${userId}`).then(resp => {
        if (resp) {
          this.friendList = resp.map(friend => {
            return {
              id: friend.id,
              nickname: friend.nickname,
              username: friend.username
            };
          });
        }
      });
    },
    handleChange(value, direction, movedKeys) {
      this.groupForm.members = this.selectedMembers;
    },
    submitForm(formName) {
      this.$refs[formName].validate((valid) => {
        if (valid) {
          this.postRequest('/group/create', {
            name: this.groupForm.name,
            description: this.groupForm.description,
            creatorId: this.groupForm.creatorId,
            members: this.selectedMembers
          }).then(resp => {
            if (resp) {
              this.$message.success('群聊创建成功');
              this.$emit('created', resp.obj); // 传递新创建的群ID
              this.resetForm(formName);
            }
          });
        } else {
          return false;
        }
      });
    },
    resetForm(formName) {
      this.$refs[formName].resetFields();
      this.selectedMembers = [];
    }

  },
  // 在创建群聊的方法中添加验证
  createGroup() {
    // 验证群聊名称
    if (!this.groupForm.name) {
      this.$message.error('请输入群名称');
      return;
    }

    // 验证群聊名称长度
    if (this.groupForm.name.length < 2 || this.groupForm.name.length > 20) {
      this.$message.error('群聊名称长度必须在2-20个字符之间');
      return;
    }

    // 其他代码...
  }
};
</script>