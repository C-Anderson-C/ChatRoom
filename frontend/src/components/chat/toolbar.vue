<template>
  <div id="toolbar">
    <!-- 顶部用户头像区域 -->
    <div class="avatar-container">
      <el-dropdown
          trigger="click"
          placement="right-start"
          @command="handleUserCommand">
        <el-avatar
            class="user-avatar"
            :size="48"
            :src="userAvatar"
            @error="handleAvatarError"
            :alt="user.nickname || user.username">
          {{ userInitials }}
        </el-avatar>

        <el-dropdown-menu slot="dropdown">
          <el-dropdown-item command="profile">
            <div class="user-info-dropdown">
              <div class="user-name">{{ user.nickname || user.username }}</div>
              <div class="user-status">
                <span class="status-dot online"></span>在线
              </div>
            </div>
          </el-dropdown-item>
          <el-dropdown-item divided command="userCenter">
            <i class="el-icon-user"></i> 个人中心
          </el-dropdown-item>
          <el-dropdown-item command="settings">
            <i class="el-icon-setting"></i> 设置
          </el-dropdown-item>
          <el-dropdown-item divided command="logout">
            <i class="el-icon-switch-button"></i> 退出登录
          </el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </div>

    <!-- 主导航区 -->
    <div id="btnBar">
      <div class="topBtnBar">
        <!-- 好友列表按钮 -->
        <el-tooltip effect="dark" content="好友列表" placement="right">
          <el-button
              @click="switchTab('friends')"
              class="toolBtn"
              size="medium"
              :class="{'active': currentTab === 'friends'}">
            <i class="fa fa-address-book-o fa-lg"></i>
          </el-button>
        </el-tooltip>

        <!-- 群聊列表按钮 -->
        <el-tooltip effect="dark" content="群聊列表" placement="right">
          <el-button
              @click="switchTab('groups')"
              class="toolBtn"
              size="medium"
              :class="{'active': currentTab === 'groups'}">
            <i class="fa fa-users fa-lg"></i>
          </el-button>
        </el-tooltip>

        <!-- 好友请求按钮 -->
        <el-tooltip effect="dark" content="好友请求" placement="right">
          <div class="button-badge-container">
            <el-badge
                :value="friendRequestCount"
                :max="99"
                :hidden="friendRequestCount === 0"
                class="friend-request-badge">
              <el-button
                  @click="showFriendRequests"
                  class="toolBtn"
                  size="medium"
                  :class="{'active': currentTab === 'requests'}">
                <i class="fa fa-bell fa-lg"></i>
              </el-button>
            </el-badge>
          </div>
        </el-tooltip>
      </div>

      <div class="bottomBtnBar">
        <!-- 创建群聊按钮 -->
        <el-tooltip effect="dark" content="创建群聊" placement="right">
          <el-button
              @click="showCreateGroupDialog"
              class="toolBtn action-btn"
              size="medium">
            <i class="fa fa-plus-circle fa-lg"></i>
          </el-button>
        </el-tooltip>

        <!-- 更多功能按钮 -->
        <el-tooltip effect="dark" content="更多功能" placement="right">
          <el-dropdown
              trigger="click"
              placement="right-start"
              @command="handleMoreCommand">
            <el-button class="toolBtn" size="medium">
              <i class="fa fa-bars fa-lg"></i>
            </el-button>

            <el-dropdown-menu slot="dropdown" class="more-dropdown">
              <el-dropdown-item command="feedback">
                <i class="fa fa-comment"></i> 意见反馈
              </el-dropdown-item>
              <el-dropdown-item command="report">
                <i class="fa fa-flag"></i> 举报
              </el-dropdown-item>
              <el-dropdown-item command="clearHistory" divided>
                <i class="fa fa-trash"></i> 清空聊天记录
              </el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </el-tooltip>
      </div>
    </div>

    <!-- 意见反馈对话框 -->
    <el-dialog
        title="意见反馈"
        :visible.sync="feedBackDialogVisible"
        width="500px"
        :close-on-click-modal="false">
      <el-form :model="feedbackForm" ref="feedbackForm" :rules="feedbackRules">
        <el-form-item label="反馈类型" prop="type">
          <el-select v-model="feedbackForm.type" placeholder="请选择反馈类型">
            <el-option label="功能建议" value="feature"></el-option>
            <el-option label="界面体验" value="ui"></el-option>
            <el-option label="性能问题" value="performance"></el-option>
            <el-option label="其他" value="other"></el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="反馈内容" prop="content">
          <el-input
              type="textarea"
              v-model="feedbackForm.content"
              rows="6"
              placeholder="请描述您遇到的问题或建议...">
          </el-input>
        </el-form-item>

        <el-form-item label="联系方式" prop="contact">
          <el-input
              v-model="feedbackForm.contact"
              placeholder="选填，方便我们与您沟通">
          </el-input>
        </el-form-item>
      </el-form>

      <span slot="footer">
        <el-button @click="feedBackDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitFeedback" :loading="submittingFeedback">提 交</el-button>
      </span>
    </el-dialog>

    <!-- 举报对话框 -->
    <el-dialog
        title="举报"
        :visible.sync="reportDialogVisible"
        width="500px"
        :close-on-click-modal="false">
      <el-form :model="reportForm" :rules="reportRules" ref="reportForm" label-width="80px">
        <!-- 举报对象类型 -->
        <el-form-item label="举报类型" prop="targetType">
          <el-radio-group v-model="reportForm.targetType" @change="handleTargetTypeChange">
            <el-radio label="user">用户</el-radio>
            <el-radio label="group">群组</el-radio>
            <el-radio label="message">消息</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 举报对象选择 -->
        <el-form-item label="举报对象" prop="targetId">
          <el-select
              v-model="reportForm.targetId"
              placeholder="请选择举报对象"
              filterable
              :loading="loadingTargets">
            <el-option
                v-for="target in filteredReportTargets"
                :key="target.id"
                :label="target.name"
                :value="target.id">
            </el-option>
          </el-select>
        </el-form-item>

        <!-- 举报原因 -->
        <el-form-item label="举报原因" prop="type">
          <el-select v-model="reportForm.type" placeholder="请选择举报原因">
            <el-option label="垃圾信息" value="spam"></el-option>
            <el-option label="色情内容" value="porn"></el-option>
            <el-option label="政治敏感" value="politics"></el-option>
            <el-option label="骚扰" value="harassment"></el-option>
            <el-option label="违法内容" value="illegal"></el-option>
            <el-option label="侵犯隐私" value="privacy"></el-option>
            <el-option label="其他" value="other"></el-option>
          </el-select>
        </el-form-item>

        <!-- 举报描述 -->
        <el-form-item label="详细描述" prop="content">
          <el-input
              type="textarea"
              v-model="reportForm.content"
              rows="5"
              placeholder="请详细描述举报原因，有助于我们更快处理">
          </el-input>
        </el-form-item>
      </el-form>

      <span slot="footer">
        <el-button @click="reportDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitReport" :loading="submittingReport">提 交</el-button>
      </span>
    </el-dialog>

    <!-- 好友请求对话框 -->
    <el-dialog
        title="好友请求"
        :visible.sync="friendRequestsVisible"
        width="500px">
      <div v-loading="loadingRequests" class="friend-requests-container">
        <el-empty v-if="!loadingRequests && friendRequests.length === 0" description="没有新的好友请求"></el-empty>

        <el-card
            v-for="request in friendRequests"
            :key="request.id"
            class="request-card"
            shadow="hover">
          <div class="request-user">
            <el-avatar
                :size="40"
                :src="request.friendAvatar || defaultAvatar">
              {{ request.friendNickname ? request.friendNickname.charAt(0) : '?' }}
            </el-avatar>

            <div class="request-info">
              <div class="user-info">
                <span class="nickname">{{ request.friendNickname || request.friendUsername }}</span>
                <span class="username">({{ request.friendUsername }})</span>
              </div>
              <div class="request-time">{{ formatTime(request.createTime) }}</div>
              <div class="request-message" v-if="request.message">
                <i class="el-icon-chat-dot-square"></i> {{ request.message }}
              </div>
            </div>
          </div>

          <div class="request-actions">
            <el-button
                size="small"
                type="primary"
                @click="respondRequest(request.id, 1)"
                :loading="processingRequest === request.id">
              接受
            </el-button>
            <el-button
                size="small"
                type="danger"
                @click="respondRequest(request.id, 2)"
                :loading="processingRequest === request.id">
              拒绝
            </el-button>
          </div>
        </el-card>
      </div>
    </el-dialog>

    <!-- 创建群聊对话框 -->
    <el-dialog
        title="创建群聊"
        :visible.sync="createGroupDialogVisible"
        width="650px"
        :close-on-click-modal="false">
      <el-form
          :model="groupForm"
          :rules="groupRules"
          ref="groupForm"
          label-width="80px"
          v-loading="loadingFriends">
        <el-form-item label="群头像">
          <el-upload
              class="avatar-uploader"
              action="/api/upload/groupAvatar"
              :show-file-list="false"
              :on-success="handleAvatarSuccess"
              :before-upload="beforeAvatarUpload">
            <img v-if="groupForm.avatar" :src="groupForm.avatar" class="group-avatar">
            <i v-else class="el-icon-plus avatar-uploader-icon"></i>
          </el-upload>
          <div class="avatar-tip">点击上传群头像（可选）</div>
        </el-form-item>

        <el-form-item label="群名称" prop="name">
          <el-input v-model="groupForm.name" placeholder="请输入群名称"></el-input>
        </el-form-item>

        <el-form-item label="群描述" prop="description">
          <el-input
              type="textarea"
              v-model="groupForm.description"
              placeholder="请输入群描述（选填）">
          </el-input>
        </el-form-item>

        <el-form-item label="选择成员" prop="members">
          <div class="members-selection">
            <div class="search-box">
              <el-input
                  v-model="memberSearchText"
                  placeholder="搜索好友"
                  prefix-icon="el-icon-search"
                  clearable>
              </el-input>
            </div>

            <div class="member-list">
              <div v-if="filteredFriends.length === 0" class="no-friends">
                <p>没有可选择的好友或没有匹配的搜索结果</p>
              </div>

              <el-checkbox-group v-model="selectedMembers">
                <el-checkbox
                    v-for="friend in filteredFriends"
                    :key="friend.id"
                    :label="friend.id"
                    class="member-item">
                  <div class="member-info">
                    <el-avatar :size="32" :src="friend.avatar || defaultAvatar">
                      {{ friend.nickname ? friend.nickname.charAt(0) : '?' }}
                    </el-avatar>
                    <span class="member-name">{{ friend.nickname || friend.username }}</span>
                  </div>
                </el-checkbox>
              </el-checkbox-group>
            </div>

            <div class="selected-count">
              已选择 {{ selectedMembers.length }} 人
              <el-button
                  type="text"
                  size="small"
                  @click="selectedMembers = []"
                  v-if="selectedMembers.length > 0">
                清空
              </el-button>
            </div>
          </div>
        </el-form-item>
      </el-form>

      <span slot="footer">
        <el-button @click="createGroupDialogVisible = false">取 消</el-button>
        <el-button
            type="primary"
            @click="createGroup"
            :loading="creatingGroup"
            :disabled="!groupForm.name || selectedMembers.length === 0">
          创建群聊
        </el-button>
      </span>
    </el-dialog>

    <!-- 确认对话框 -->
    <el-dialog
        title="确认操作"
        :visible.sync="confirmDialogVisible"
        width="350px">
      <div class="confirm-content">
        <i :class="confirmIcon"></i>
        <p>{{ confirmMessage }}</p>
      </div>
      <span slot="footer">
        <el-button @click="confirmDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="executeConfirmedAction">确 定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: "toolbar",
  data(){
    return{
      user: JSON.parse(window.sessionStorage.getItem('user')) || {},
      currentTab: 'friends',
      avatarError: false,
      defaultAvatar: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA0MCA0MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48Y2lyY2xlIGN4PSIyMCIgY3k9IjIwIiByPSIyMCIgZmlsbD0iI2U1ZTVlNSIvPjxjaXJjbGUgY3g9IjIwIiBjeT0iMTYiIHI9IjYiIGZpbGw9IiNhMGEwYTAiLz48cGF0aCBkPSJNMTAgMzJDMTAgMjUuMzcgMTQuNDc3IDIwIDIwIDIwQzI1LjUyMyAyMCAzMCAyNS4zNyAzMCAzMkg0MFY0MEgwVjMySzEwIDMyWiIgZmlsbD0iI2EwYTBhMCIvPjwvc3ZnPg==',

      // 意见反馈相关
      feedBackDialogVisible: false,
      feedbackForm: {
        type: '',
        content: '',
        contact: ''
      },
      feedbackRules: {
        type: [{ required: true, message: '请选择反馈类型', trigger: 'change' }],
        content: [{ required: true, message: '请输入反馈内容', trigger: 'blur' }]
      },
      submittingFeedback: false,

      // 举报相关
      reportDialogVisible: false,
      reportForm: {
        type: '',
        content: '',
        targetType: 'user',
        targetId: null
      },
      reportRules: {
        targetType: [{ required: true, message: '请选择举报类型', trigger: 'change' }],
        targetId: [{ required: true, message: '请选择举报对象', trigger: 'change' }],
        type: [{ required: true, message: '请选择举报原因', trigger: 'change' }],
        content: [{ required: true, message: '请描述举报详情', trigger: 'blur' }]
      },
      reportTargets: {
        user: [],
        group: [],
        message: []
      },
      loadingTargets: false,
      submittingReport: false,

      // 好友请求相关
      friendRequestsVisible: false,
      friendRequests: [],
      friendRequestCount: 0,
      loadingRequests: false,
      processingRequest: null,

      // 创建群聊相关
      createGroupDialogVisible: false,
      groupForm: {
        name: '',
        description: '',
        avatar: '',
        members: []
      },
      groupRules: {
        name: [
          { required: true, message: '请输入群名称', trigger: 'blur' },
          { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
        ]
      },
      friendList: [],
      selectedMembers: [],
      memberSearchText: '',
      loadingFriends: false,
      creatingGroup: false,

      // 确认对话框
      confirmDialogVisible: false,
      confirmMessage: '',
      confirmIcon: 'el-icon-question',
      pendingAction: null
    }
  },
  computed: {
    userAvatar() {
      if (this.avatarError || !this.user.userProfile) {
        return null; // 返回null会触发默认文字头像
      }
      return this.user.userProfile;
    },
    userInitials() {
      const name = this.user.nickname || this.user.username || '';
      return name ? name.charAt(0).toUpperCase() : '?';
    },
    filteredFriends() {
      if (!this.friendList || this.friendList.length === 0) {
        return [];
      }

      if (!this.memberSearchText) {
        return this.friendList;
      }

      const searchText = this.memberSearchText.toLowerCase();
      return this.friendList.filter(friend =>
          (friend.nickname && friend.nickname.toLowerCase().includes(searchText)) ||
          (friend.username && friend.username.toLowerCase().includes(searchText))
      );
    },
    filteredReportTargets() {
      return this.reportTargets[this.reportForm.targetType] || [];
    }
  },
  created() {
    // 每分钟检查一次新的好友请求
    this.friendRequestTimer = setInterval(this.loadFriendRequests, 60000);

    // 初始加载好友请求
    this.loadFriendRequests();
  },
  mounted() {
    // 监听事件
    if (this.$bus) {
      this.$bus.$on('refresh-friend-list', this.loadFriendList);
      this.$bus.$on('refresh-group-list', this.loadGroupList);
    }

    // 初始加载好友列表
    this.loadFriendList();
  },
  beforeDestroy() {
    // 清除定时器
    if (this.friendRequestTimer) {
      clearInterval(this.friendRequestTimer);
    }

    // 移除事件监听
    if (this.$bus) {
      this.$bus.$off('refresh-friend-list', this.loadFriendList);
      this.$bus.$off('refresh-group-list', this.loadGroupList);
    }
  },
  methods: {
    // 切换标签页
    switchTab(tab) {
      if (this.currentTab === tab) return;

      this.currentTab = tab;
      console.log('[Toolbar] 切换到标签页:', tab);

      if (tab === 'friends') {
        this.$store.commit('changeCurrentList', '私聊');
        this.$bus.$emit('switch-to-friends');
      } else if (tab === 'groups') {
        this.$store.commit('changeCurrentList', '群聊');
        this.$bus.$emit('switch-to-groups');
      } else if (tab === 'requests') {
        this.showFriendRequests();
      }
    },

    // 处理头像加载错误
    handleAvatarError() {
      console.log('[Toolbar] 头像加载失败');
      this.avatarError = true;
    },

    // 处理用户头像下拉菜单命令
    handleUserCommand(command) {
      console.log('[Toolbar] 用户命令:', command);

      switch(command) {
        case 'profile':
          // 查看个人资料
          this.toUserCenter();
          break;
        case 'userCenter':
          // 个人中心
          this.toUserCenter();
          break;
        case 'settings':
          // 设置
          this.$bus.$emit('show-settings');
          break;
        case 'logout':
          // 退出登录
          this.exitSystem();
          break;
      }
    },

    // 处理更多功能下拉菜单命令
    handleMoreCommand(command) {
      console.log('[Toolbar] 更多功能命令:', command);

      switch(command) {
        case 'feedback':
          this.showFeedbackDialog();
          break;
        case 'report':
          this.showReportDialog();
          break;
        case 'clearHistory':
          this.confirmClearHistory();
          break;
      }
    },

    // 个人中心
    toUserCenter() {
      console.log('[Toolbar] 导航到个人中心');

      try {
        const isAdmin = !!sessionStorage.getItem('admin');
        if (isAdmin) {
          this.$message.info('正在跳转到管理员个人中心');
          this.$router.push('/admin/info');
        } else {
          this.$router.push('/user/profile');
        }
      } catch (error) {
        console.error('[Toolbar] 导航到个人中心失败:', error);
        this.$message.error('无法访问个人中心，请检查页面配置');
      }
    },

    // 退出系统
    exitSystem() {
      this.confirmMessage = '确定要退出系统吗？';
      this.confirmIcon = 'el-icon-warning';
      this.pendingAction = 'logout';
      this.confirmDialogVisible = true;
    },

    // 执行退出登录
    doLogout() {
      this.getRequest("/logout").finally(() => {
        // 清除会话数据
        sessionStorage.removeItem("user");
        if (sessionStorage.getItem("state")) {
          sessionStorage.removeItem("state");
        }

        // 断开WebSocket连接
        if (this.$store && this.$store.dispatch) {
          this.$store.dispatch("disconnect");
        }

        // 跳转到登录页
        this.$router.replace("/");
      });
    },

    // 显示意见反馈对话框
    showFeedbackDialog() {
      this.feedbackForm = {
        type: '',
        content: '',
        contact: ''
      };
      this.feedBackDialogVisible = true;
    },

    // 提交反馈
    submitFeedback() {
      this.$refs.feedbackForm.validate(valid => {
        if (!valid) {
          return false;
        }

        this.submittingFeedback = true;

        const feedbackData = {
          userId: this.user.id,
          nickname: this.user.nickname,
          username: this.user.username,
          type: this.feedbackForm.type,
          content: this.feedbackForm.content,
          contact: this.feedbackForm.contact
        };

        this.postRequest("/mail/feedback", feedbackData)
            .then(resp => {
              if (resp) {
                this.$message.success('感谢您的反馈，我们会认真考虑您的建议');
                this.feedBackDialogVisible = false;
              }
            })
            .catch(err => {
              console.error('[Toolbar] 发送反馈失败:', err);
              this.$message.error('发送反馈失败，请稍后重试');
            })
            .finally(() => {
              this.submittingFeedback = false;
            });
      });
    },

    // 显示举报对话框
    showReportDialog() {
      this.reportForm = {
        type: '',
        content: '',
        targetType: 'user',
        targetId: null
      };
      this.reportDialogVisible = true;
      this.loadReportableTargets('user');
    },

    // 处理举报目标类型变化
    handleTargetTypeChange(targetType) {
      this.reportForm.targetId = null;
      this.loadReportableTargets(targetType);
    },

    // 加载可举报目标
    loadReportableTargets(targetType) {
      if (!this.user || !this.user.id) return;

      this.loadingTargets = true;

      let apiPath;
      switch(targetType) {
        case 'user':
          apiPath = `/friend/list?userId=${this.user.id}`;
          break;
        case 'group':
          apiPath = `/group/list?userId=${this.user.id}`;
          break;
        case 'message':
          apiPath = `/message/recent?userId=${this.user.id}`;
          break;
        default:
          this.loadingTargets = false;
          return;
      }

      this.getRequest(apiPath)
          .then(resp => {
            if (resp && Array.isArray(resp)) {
              // 根据目标类型处理数据
              switch(targetType) {
                case 'user':
                  this.reportTargets.user = resp.map(item => ({
                    id: item.id,
                    name: item.nickname || item.username,
                    type: 'user'
                  }));
                  break;
                case 'group':
                  this.reportTargets.group = resp.map(item => ({
                    id: item.id,
                    name: item.name,
                    type: 'group'
                  }));
                  break;
                case 'message':
                  this.reportTargets.message = resp.map(item => ({
                    id: item.id || item.messageId,
                    name: `来自 ${item.fromName || item.from} 的消息: ${item.content.substring(0, 20)}${item.content.length > 20 ? '...' : ''}`,
                    type: 'message'
                  }));
                  break;
              }
            }
          })
          .catch(err => {
            console.error(`[Toolbar] 加载${targetType}举报目标失败:`, err);
          })
          .finally(() => {
            this.loadingTargets = false;
          });
    },

    // 提交举报
    submitReport() {
      this.$refs.reportForm.validate(valid => {
        if (!valid) {
          return false;
        }

        this.submittingReport = true;

        const reportData = {
          userId: this.user.id,
          type: this.reportForm.type,
          content: this.reportForm.content,
          targetType: this.reportForm.targetType,
          targetId: this.reportForm.targetId,
          time: new Date().toISOString()
        };

        this.postRequest("/report/submit", reportData)
            .then(resp => {
              if (resp) {
                this.$message.success('举报已提交，我们会尽快处理');
                this.reportDialogVisible = false;
              }
            })
            .catch(err => {
              console.error('[Toolbar] 提交举报失败:', err);
              this.$message.error('举报提交失败，请稍后再试');
            })
            .finally(() => {
              this.submittingReport = false;
            });
      });
    },

    // 确认清空聊天记录
    confirmClearHistory() {
      this.confirmMessage = '此操作将永久删除本地聊天记录(群聊记录会在下次登录时恢复), 是否继续?';
      this.confirmIcon = 'el-icon-warning';
      this.pendingAction = 'clearHistory';
      this.confirmDialogVisible = true;
    },

    // 执行清空聊天记录
    doClearHistory() {
      if (localStorage.getItem("chat-session")) {
        localStorage.removeItem("chat-session");
      }
      if (this.$store && this.$store.state) {
        this.$store.state.sessions = {};
      }
      if (sessionStorage.getItem("state")) {
        sessionStorage.removeItem("state");
      }
      this.$message.success('聊天记录已清空');

      // 通知其他组件刷新
      this.$bus.$emit('refresh-messages', true);
    },

    // 显示好友请求对话框
    showFriendRequests() {
      this.loadFriendRequests();
      this.friendRequestsVisible = true;
    },

    // 加载好友请求
    loadFriendRequests() {
      if (!this.user || !this.user.id) {
        console.warn('[Toolbar] 用户ID不存在，无法加载好友请求');
        return;
      }

      this.loadingRequests = true;

      const userId = this.user.id;
      this.getRequest(`/friend/requests?userId=${userId}`)
          .then(resp => {
            if (resp && Array.isArray(resp)) {
              this.friendRequests = resp;
              this.friendRequestCount = resp.length;

              // 如果有新的好友请求且当前不在请求对话框中，显示通知
              if (resp.length > 0 && !this.friendRequestsVisible) {
                this.$notify({
                  title: '新的好友请求',
                  message: `您有 ${resp.length} 个新的好友请求`,
                  type: 'info',
                  position: 'bottom-right'
                });
              }
            } else {
              this.friendRequests = [];
              this.friendRequestCount = 0;
            }
          })
          .catch(err => {
            console.error('[Toolbar] 加载好友请求错误:', err);
            this.friendRequests = [];
            this.friendRequestCount = 0;
          })
          .finally(() => {
            this.loadingRequests = false;
          });
    },

    // 响应好友请求
    respondRequest(requestId, response) {
      if (this.processingRequest) return;

      this.processingRequest = requestId;

      const formData = {
        requestId: requestId,
        response: response
      };

      this.postRequest('/friend/respond', formData)
          .then(resp => {
            if (resp) {
              this.$message.success(response === 1 ? '已添加为好友' : '已拒绝请求');

              // 从列表中移除已处理的请求
              this.friendRequests = this.friendRequests.filter(req => req.id !== requestId);
              this.friendRequestCount = this.friendRequests.length;

              // 如果接受了好友请求，通知更新好友列表
              if (response === 1) {
                this.$bus.$emit('refresh-friend-list');
              }

              // 如果没有更多请求，关闭对话框
              if (this.friendRequests.length === 0) {
                setTimeout(() => {
                  this.friendRequestsVisible = false;
                }, 500);
              }
            }
          })
          .catch(err => {
            console.error('[Toolbar] 响应好友请求错误:', err);
            this.$message.error('处理好友请求失败，请稍后重试');
          })
          .finally(() => {
            this.processingRequest = null;
          });
    },

    // 格式化时间
    formatTime(time) {
      if (!time) return '';

      const date = new Date(time);
      if (isNaN(date.getTime())) return '';

      const now = new Date();
      const diff = now - date;

      // 不到1分钟
      if (diff < 60000) {
        return '刚刚';
      }

      // 不到1小时
      if (diff < 3600000) {
        return `${Math.floor(diff / 60000)}分钟前`;
      }

      // 不到24小时
      if (diff < 86400000) {
        return `${Math.floor(diff / 3600000)}小时前`;
      }

      // 不到7天
      if (diff < 604800000) {
        return `${Math.floor(diff / 86400000)}天前`;
      }

      // 超过7天显示完整日期
      return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`;
    },

    // 加载好友列表
    loadFriendList() {
      if (!this.user || !this.user.id) return;

      this.loadingFriends = true;

      this.getRequest(`/friend/list?userId=${this.user.id}`)
          .then(response => {
            if (response && Array.isArray(response)) {
              this.friendList = response;
            } else {
              this.friendList = [];
            }
          })
          .catch(err => {
            console.error('[Toolbar] 加载好友列表失败:', err);
            this.friendList = [];
          })
          .finally(() => {
            this.loadingFriends = false;
          });
    },

    // 加载群组列表
    loadGroupList() {
      // 由于组件中不直接使用群组列表，此方法仅作为事件回调存在
      console.log('[Toolbar] 群组列表已刷新');
    },

    // 显示创建群聊对话框
    showCreateGroupDialog() {
      this.groupForm = {
        name: '',
        description: '',
        avatar: '',
        members: []
      };
      this.selectedMembers = [];
      this.memberSearchText = '';
      this.loadFriendList();
      this.createGroupDialogVisible = true;
    },

    // 群头像上传前验证
    beforeAvatarUpload(file) {
      const isImage = file.type.startsWith('image/');
      const isLt2M = file.size / 1024 / 1024 < 2;

      if (!isImage) {
        this.$message.error('上传头像图片只能是图片格式!');
        return false;
      }

      if (!isLt2M) {
        this.$message.error('上传头像图片大小不能超过 2MB!');
        return false;
      }

      return isImage && isLt2M;
    },

    // 群头像上传成功回调
    handleAvatarSuccess(res, file) {
      if (res && res.status === 200) {
        this.groupForm.avatar = res.data || res.url || URL.createObjectURL(file.raw);
      } else {
        this.$message.error('头像上传失败');
      }
    },

    // 创建群聊
    createGroup() {
      this.$refs.groupForm.validate(valid => {
        if (!valid) {
          return false;
        }

        if (this.selectedMembers.length === 0) {
          this.$message.error('请选择群成员');
          return;
        }

        this.creatingGroup = true;

        const data = {
          name: this.groupForm.name,
          description: this.groupForm.description || '',
          avatar: this.groupForm.avatar || '',
          creatorId: this.user.id,
          members: this.selectedMembers
        };

        console.log('[Toolbar] 创建群聊数据:', data);

        this.postRequest('/group/create', data)
            .then(resp => {
              if (resp && resp.status === 200) {
                this.$message.success('创建群聊成功');
                this.createGroupDialogVisible = false;

                // 通知刷新群组列表
                this.$bus.$emit('refresh-group-list');

                // 延迟一会，等群组创建完成后切换到群组标签
                setTimeout(() => {
                  this.switchTab('groups');
                }, 500);
              } else {
                this.$message.error(resp?.msg || '创建群聊失败');
              }
            })
            .catch(err => {
              console.error('[Toolbar] 创建群聊失败:', err);
              this.$message.error('创建群聊失败: ' + (err.message || '未知错误'));
            })
            .finally(() => {
              this.creatingGroup = false;
            });
      });
    },

    // 执行确认对话框的确认操作
    executeConfirmedAction() {
      this.confirmDialogVisible = false;

      switch (this.pendingAction) {
        case 'logout':
          this.doLogout();
          break;
        case 'clearHistory':
          this.doClearHistory();
          break;
      }

      this.pendingAction = null;
    }
  }
}
</script>

<style lang="scss" scoped>
#toolbar {
  width: 100%;
  height: 100%;
  background-color: #2e3238;
  display: flex;
  flex-direction: column;

  .avatar-container {
    display: flex;
    justify-content: center;
    padding: 15px 0;

    .user-avatar {
      cursor: pointer;
      border: 2px solid transparent;
      transition: all 0.3s;

      &:hover {
        border-color: #1890ff;
      }
    }
  }

  #btnBar {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    padding: 10px 0;

    .topBtnBar, .bottomBtnBar {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 10px;
    }
  }

  .toolBtn {
    width: 40px;
    height: 40px;
    background-color: transparent;
    border: none;
    color: #a8a8a8;
    transition: all 0.3s;
    border-radius: 5px;
    padding: 0;
    display: flex;
    justify-content: center;
    align-items: center;

    &:hover {
      background-color: #444;
      color: white;
    }

    &.active {
      background-color: #1890ff;
      color: white;
    }

    &.action-btn {
      color: #1890ff;

      &:hover {
        background-color: rgba(24, 144, 255, 0.2);
      }
    }
  }

  .button-badge-container {
    width: 40px;
    height: 40px;
    position: relative;
  }
}

.user-info-dropdown {
  padding: 5px 0;

  .user-name {
    font-weight: bold;
    margin-bottom: 5px;
  }

  .user-status {
    font-size: 12px;
    display: flex;
    align-items: center;

    .status-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      margin-right: 5px;

      &.online {
        background-color: #67c23a;
      }
    }
  }
}

.more-dropdown {
  width: 150px;
}

.friend-requests-container {
  max-height: 400px;
  overflow-y: auto;

  .request-card {
    margin-bottom: 15px;
    transition: all 0.3s;

    &:hover {
      box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
    }

    .request-user {
      display: flex;
      margin-bottom: 15px;

      .request-info {
        margin-left: 10px;
        flex: 1;

        .user-info {
          margin-bottom: 5px;

          .nickname {
            font-weight: bold;
          }

          .username {
            color: #909399;
            font-size: 12px;
          }
        }

        .request-time {
          font-size: 12px;
          color: #909399;
        }

        .request-message {
          margin-top: 5px;
          font-size: 13px;
          color: #606266;
          background-color: #f5f7fa;
          padding: 5px 8px;
          border-radius: 4px;

          i {
            margin-right: 5px;
            color: #909399;
          }
        }
      }
    }

    .request-actions {
      display: flex;
      justify-content: flex-end;
      gap: 10px;
    }
  }
}

.avatar-uploader {
  display: inline-block;

  .el-upload {
    border: 1px dashed #d9d9d9;
    border-radius: 50%;
    cursor: pointer;
    position: relative;
    overflow: hidden;
    width: 100px;
    height: 100px;
    display: flex;
    justify-content: center;
    align-items: center;

    &:hover {
      border-color: #409EFF;
    }
  }

  .avatar-uploader-icon {
    font-size: 28px;
    color: #8c939d;
    width: 100px;
    height: 100px;
    line-height: 100px;
    text-align: center;
  }

  .group-avatar {
    width: 100px;
    height: 100px;
    border-radius: 50%;
    object-fit: cover;
  }
}

.avatar-tip {
  margin-top: 5px;
  font-size: 12px;
  color: #909399;
}

.members-selection {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 10px;

  .search-box {
    margin-bottom: 10px;
  }

  .member-list {
    max-height: 200px;
    overflow-y: auto;
    border-top: 1px solid #ebeef5;
    border-bottom: 1px solid #ebeef5;
    padding: 5px 0;
    margin-bottom: 10px;

    .no-friends {
      padding: 20px 0;
      text-align: center;
      color: #909399;
    }

    .member-item {
      margin: 8px 0;
      padding: 5px;
      border-radius: 4px;
      transition: all 0.3s;

      &:hover {
        background-color: #f5f7fa;
      }

      .member-info {
        display: flex;
        align-items: center;
        margin-left: 24px;

        .member-name {
          margin-left: 10px;
        }
      }
    }
  }

  .selected-count {
    text-align: right;
    font-size: 12px;
    color: #606266;
  }
}

.confirm-content {
  text-align: center;
  padding: 20px 0;

  i {
    font-size: 32px;
    color: #E6A23C;
    margin-bottom: 15px;
  }

  p {
    font-size: 16px;
    color: #606266;
  }
}
</style>

<style>
/* 全局样式 */
.el-popover.moreListPopoverClass {
  padding: 0;
  min-width: 120px;
  border: none;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

/* 徽章样式修复 */
.button-badge-container .el-badge__content {
  transform: translate(40%, -40%) !important;
  z-index: 10;
}

/* 成员选择区域的复选框样式 */
.members-selection .el-checkbox {
  display: block;
  margin-right: 0;
  width: 100%;
}

/* 转账面板样式调整 */
.el-transfer-panel {
  width: 220px;
  height: 300px;
}

.el-transfer-panel__body {
  height: 250px;
}
</style>