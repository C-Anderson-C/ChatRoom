import  Vue from 'vue'
import  Vuex from 'vuex'
import {getRequest, postRequest} from "../utils/api";
import SockJS from '../utils/sockjs'
import  '../utils/stomp'
import { Notification } from 'element-ui';

Vue.use(Vuex)

const now = new Date();

const store =  new Vuex.Store({
  state:sessionStorage.getItem('state') ? JSON.parse(sessionStorage.getItem('state')) :{
    routes:[],
    sessions:{},//聊天记录
    users:[],//用户列表
    currentUser:null,//当前登录用户
    currentSession:{username:'群聊',nickname:'群聊'},//当前选中的用户，默认为群聊
    currentList:'群聊',//当前聊天窗口列表
    filterKey:'',
    stomp:null,
    isDot:{},//两用户之间是否有未读信息
    errorImgUrl:"http://39.108.169.57/group1/M00/00/00/J2ypOV7wJkyAAv1fAAANuXp4Wt8303.jpg",//错误提示图片
    shotHistory:{}//拍一拍的记录历史
  },
  mutations:{
    initRoutes(state,data){
      state.routes=data;
    },
    changeCurrentSession (state,currentSession) {
      //切换到当前用户就标识消息已读
      Vue.set(state.isDot,state.currentUser.username+"#"+currentSession.username,false);
      //更新当前选中的用户
      state.currentSession =currentSession;
    },
    //修改当前聊天窗口列表
    changeCurrentList(state,currentList){
      state.currentList=currentList;
    },
    //保存群聊消息记录
    addGroupMessage(state,msg){
      let message=state.sessions['群聊'];
      if (!message){
        Vue.set(state.sessions,'群聊',[]);
      }
      state.sessions['群聊'].push({
        fromId:msg.fromId,
        fromName:msg.fromName,
        fromProfile:msg.fromProfile,
        content:msg.content,
        messageTypeId:msg.messageTypeId,
        createTime: msg.createTime,
      })
    },
    //保存单聊数据
    addMessage (state,msg) {
      let message=state.sessions[state.currentUser.username+"#"+msg.to];
      if (!message){
        Vue.set(state.sessions,state.currentUser.username+"#"+msg.to,[]);
      }
      state.sessions[state.currentUser.username+"#"+msg.to].push({
        content:msg.content,
        date: new Date(),
        fromNickname:msg.fromNickname,
        messageTypeId:msg.messageTypeId,
        self:!msg.notSelf
      })
    },
    INIT_DATA (state) {
      getRequest("/groupMsgContent/").then(resp=>{
        if (resp){
          Vue.set(state.sessions,'群聊',resp);
        }
      })
    },
    INIT_USER(state,data){
      state.users=data;
    },
    GET_USERS(state){
      getRequest("/chat/users").then(resp=>{
        if (resp){
          state.users=resp;
        }
      })
    }
  },
  actions:{
    initData (context) {
      context.commit('INIT_DATA')
      context.commit('GET_USERS')
    },
    connect(context){
      context.state.stomp=Stomp.over(new SockJS('/ws/ep'));
      context.state.stomp.connect({},success=>{
        /**
         * 订阅系统广播通知消息
         */
        context.state.stomp.subscribe("/topic/notification",msg=>{
          Notification.info({
            title: '系统消息',
            message: msg.body.substr(5),
            position:"top-right"
          });
          context.commit('GET_USERS');
        });
        /**
         * 订阅群聊消息
         */
        context.state.stomp.subscribe("/topic/greetings",msg=>{
          let receiveMsg=JSON.parse(msg.body);
          if (context.state.currentSession.username!="群聊"){
            Vue.set(context.state.isDot,context.state.currentUser.username+"#群聊",true);
          }
          context.commit('addGroupMessage',receiveMsg);
        });
        /**
         * 订阅私人消息
         */
        context.state.stomp.subscribe('/user/queue/chat',msg=>{
          let receiveMsg=JSON.parse(msg.body);
          if (!context.state.currentSession||receiveMsg.from!=context.state.currentSession.username){
            Notification.info({
              title:'【'+receiveMsg.fromNickname+'】发来一条消息',
              message:receiveMsg.content.length<8?receiveMsg.content:receiveMsg.content.substring(0,8)+"...",
              position:"bottom-right"
            });
            Vue.set(context.state.isDot,context.state.currentUser.username+"#"+receiveMsg.from,true);
          }
          receiveMsg.notSelf=true;
          receiveMsg.to=receiveMsg.from;
          context.commit('addMessage',receiveMsg);
        })
      },error=>{
        Notification.info({
          title: '系统消息',
          message: "无法与服务端建立连接，请尝试重新登陆系统~",
          position:"top-right"
        });
      })
    },
    disconnect(context){
      if (context.state.stomp!=null) {
        context.state.stomp.disconnect();
        console.log("关闭连接~");
      }
    },
  }
})

store.watch(function (state) {
  return state.sessions
},function (val) {
  console.log('CHANGE: ', val);
  localStorage.setItem('chat-session', JSON.stringify(val));
},{
  deep:true
})

export default store;