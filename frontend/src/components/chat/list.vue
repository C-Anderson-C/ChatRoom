<template>
  <div id="list">
    <!-- 用户列表（现在在上面） -->
    <el-scrollbar wrap-class="userList" wrap-style="height:600px;" view-style="height:100%;" :native="false">
      <ul v-if="currentList=='私聊'">
        <p style="padding: 2px 4px;height: 20px">用户列表</p>
        <li v-for="item in users" :class="{ active: currentSession?item.username === currentSession.username:false }"
            v-on:click="changeCurrentSession(item)">
          <div style="display: flex;justify-content: space-between">
            <div>
              <el-badge :is-dot="isDot[user.username+'#'+item.username]" style="">
                <el-image class="avatar"
                          :preview-src-list="[item.userProfile]"
                          :src="item.userProfile"
                          :alt="item.nickname">
                  <div slot="error" class="image-slot">
                    <i class="el-icon-picture-outline"></i>
                  </div>
                </el-image>
              </el-badge>
              <p class="name">{{item.nickname}}</p>
            </div>
            <div>
              <el-badge :value="item.userStateId==1?'在线':'离线'" :type="item.userStateId==1?'danger':'info'"></el-badge>
            </div>
          </div>
        </li>
      </ul>
    </el-scrollbar>

    <!-- 群聊列表（现在在下面） -->
    <ul v-if="currentList=='群聊'">
      <p style="padding: 2px 4px;height: 20px">群聊列表</p>
      <li :class="{ active: currentSession?'群聊'== currentSession.username:false }"
          v-on:click="changeCurrentSession(chatObj)">
        <img class="avatar" src="https://ss1.bdstatic.com/70cFuXSh_Q1YnxGkpoWK1HF6hhy/it/u=1268761962,3976237305&fm=26&gp=0.jpg">
        <el-badge :is-dot="isDot[user.username+'#群聊']"><p class="name">群聊</p></el-badge>
      </li>
    </ul>
  </div>
</template>

<script>
import {mapState} from 'vuex'

export default {
  name: 'list',
  data () {
    return {
      user: this.$store.state.currentUser,
      chatObj: {username: '群聊', nickname: '群聊'} // 群聊实体对象（为方法复用而构造，对于User对象）
    }
  },
  computed: mapState([
    //'sessions',
    'users',
    'currentSession',
    'isDot',
    'currentList'
  ]),
  methods:{
    changeCurrentSession: function (currentSession) {
      this.$store.commit('changeCurrentSession', currentSession)
    }
  }
}
</script>

<style lang="scss" scoped>
#list {
  ul{
    margin-left: 0px;
    padding-left: 0px;
    margin-left: 2px;
  }
  li {
    padding-top: 14px;
    padding-bottom: 14px;
    //padding-right: 40px;
    //border-bottom: 1px solid #292C33;
    list-style: none;
    cursor: pointer;
    &:hover {
      background-color: #D8D6D6;
    }
  }
  li.active {
    background-color: #C8C6C6;
  }
  .avatar {
    border-radius: 2px;
    width: 30px;
    height: 30px;
    vertical-align: middle;
  }
  .name {
    display: inline-block;
    margin-left: 15px;
    margin-top: 0px;
    margin-bottom: 0px;
  }
  .stateItem {
    /*position: absolute;*/
    /*left: 160px;*/
    //margin-left: 100px;
    //margin-right: 10px;
  }
  .userList{
    max-height: 600px;
  }
  .el-scrollbar__wrap.default-scrollbar__wrap {
    overflow-x: auto;
  }
}
</style>