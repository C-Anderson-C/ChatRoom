import Vue from 'vue'
import Vuex from 'vuex'
import {getRequest, postRequest} from "../utils/api";
import SockJS from '../utils/sockjs'
import '../utils/stomp'
import { Notification } from 'element-ui';

Vue.use(Vuex)

const now = new Date();

const store = new Vuex.Store({
  state: sessionStorage.getItem('state') ? JSON.parse(sessionStorage.getItem('state')) : {
    routes: [],
    sessions: {}, //聊天记录
    users: [], //用户列表
    currentUser: null, //当前登录用户
    currentSession: {username: '群聊', nickname: '群聊'}, //当前选中的用户，默认为群聊
    currentList: '群聊', //当前聊天窗口列表
    filterKey: '',
    stomp: null,
    isDot: {}, //两用户之间是否有未读信息
    errorImgUrl: "http://39.108.169.57/group1/M00/00/00/J2ypOV7wJkyAAv1fAAANuXp4Wt8303.jpg", //错误提示图片
    shotHistory: {} //拍一拍的记录历史
  },
  mutations: {
    initRoutes(state, data) {
      state.routes = data;
    },
    changeCurrentSession(state, currentSession) {
      //切换到当前用户就标识消息已读
      Vue.set(state.isDot, state.currentUser.username + "#" + currentSession.username, false);
      //更新当前选中的用户
      state.currentSession = currentSession;
    },
    //修改当前聊天窗口列表
    changeCurrentList(state, currentList) {
      state.currentList = currentList;
    },
    //保存群聊消息记录
    addGroupMessage(state, msg) {
      let message = state.sessions['群聊'];
      if (!message) {
        Vue.set(state.sessions, '群聊', []);
      }
      state.sessions['群聊'].push({
        fromId: msg.fromId,
        fromName: msg.fromName,
        fromProfile: msg.fromProfile,
        content: msg.content,
        messageTypeId: msg.messageTypeId,
        createTime: msg.createTime,
      })
    },
    //保存单聊数据
    addMessage(state, msg) {
      let message = state.sessions[state.currentUser.username + "#" + msg.to];
      if (!message) {
        Vue.set(state.sessions, state.currentUser.username + "#" + msg.to, []);
      }
      state.sessions[state.currentUser.username + "#" + msg.to].push({
        content: msg.content,
        date: new Date(),
        fromNickname: msg.fromNickname,
        messageTypeId: msg.messageTypeId,
        self: !msg.notSelf
      })
    },
    INIT_DATA(state) {
      getRequest("/groupMsgContent/").then(resp => {
        if (resp) {
          Vue.set(state.sessions, '群聊', resp);
        }
      })
    },
    INIT_USER(state, data) {
      state.users = data;
    },
    GET_USERS(state) {
      getRequest("/chat/users").then(resp => {
        if (resp) {
          state.users = resp;
        }
      })
    }
  },
  actions: {
    initData(context) {
      context.commit('INIT_DATA')
      context.commit('GET_USERS')
    },
    connect(context) {
      // 创建SockJS连接，配置简化请求头
      const sockjsOptions = {
        transports: ['websocket', 'xhr-streaming', 'xhr-polling'],
        headers: {
          // 只保留必要的头信息
          'Content-Type': 'application/json'
        }
      };

      // 创建WebSocket连接实例
      const socket = new SockJS('/ws/ep', null, sockjsOptions);
      context.state.stomp = Stomp.over(socket);

      // 配置Stomp客户端
      const stompConfig = {
        // 只保留必要的头信息，减少请求头大小
        'heart-beat': '0,20000', // 设置心跳间隔，减少频繁通信
        'content-length': false // 禁用content-length头，由浏览器自动计算
      };

      // 连接WebSocket服务器
      context.state.stomp.connect(stompConfig, success => {
        /**
         * 订阅系统广播通知消息
         */
        context.state.stomp.subscribe("/topic/notification", msg => {
          Notification.info({
            title: '系统消息',
            message: msg.body.substr(5),
            position: "top-right"
          });
          context.commit('GET_USERS');
        });
        /**
         * 订阅群聊消息
         */
        context.state.stomp.subscribe("/topic/greetings", msg => {
          let receiveMsg = JSON.parse(msg.body);
          if (context.state.currentSession.username != "群聊") {
            Vue.set(context.state.isDot, context.state.currentUser.username + "#群聊", true);
          }
          context.commit('addGroupMessage', receiveMsg);
        });
        /**
         * 订阅私人消息
         */
        context.state.stomp.subscribe('/user/queue/chat', msg => {
          let receiveMsg = JSON.parse(msg.body);
          if (!context.state.currentSession || receiveMsg.from != context.state.currentSession.username) {
            Notification.info({
              title: '【' + receiveMsg.fromNickname + '】发来一条消息',
              message: receiveMsg.content.length < 8 ? receiveMsg.content : receiveMsg.content.substring(0, 8) + "...",
              position: "bottom-right"
            });
            Vue.set(context.state.isDot, context.state.currentUser.username + "#" + receiveMsg.from, true);
          }
          receiveMsg.notSelf = true;
          receiveMsg.to = receiveMsg.from;
          context.commit('addMessage', receiveMsg);
        });
      }, error => {
        // 连接错误处理
        console.error("WebSocket连接失败:", error);
        Notification.error({
          title: '系统消息',
          message: "无法与服务端建立连接，请尝试重新登陆系统~",
          position: "top-right"
        });
      });

      // 配置断线重连机制
      socket.onclose = function() {
        console.log('WebSocket连接关闭，尝试重新连接...');
        // 清除会话状态，避免重连时携带过多信息
        sessionStorage.removeItem('stomp-transport');
        setTimeout(() => {
          if (context.state.currentUser) {
            context.dispatch('connect');
          }
        }, 5000); // 5秒后尝试重连
      };
    },
    disconnect(context) {
      if (context.state.stomp != null) {
        // 清理订阅，减少重连时的头信息
        context.state.stomp.disconnect(() => {
          console.log("WebSocket连接已关闭");
          // 清除相关会话信息
          sessionStorage.removeItem('stomp-transport');
        });
      }
    },
  }
})

// 保存会话状态到本地存储
store.watch(function (state) {
  return state.sessions
}, function (val) {
  // 限制存储大小，避免localStorage超出限制
  const sessionsToSave = {};
  Object.keys(val).forEach(key => {
    // 每个会话只保存最新的50条消息，避免数据过大
    const messages = val[key];
    sessionsToSave[key] = messages.slice(-50);
  });

  try {
    localStorage.setItem('chat-session', JSON.stringify(sessionsToSave));
  } catch (e) {
    console.error('存储聊天记录失败:', e);
    // 如果存储失败，尝试清理部分历史记录
    localStorage.removeItem('chat-session');
  }
}, {
  deep: true
})

// 更新sessionStorage，确保数据持久化
window.addEventListener('beforeunload', () => {
  // 将状态存入sessionStorage前先清理大型对象
  const stateToSave = {...store.state};
  delete stateToSave.stomp; // 不保存stomp对象，避免序列化问题

  // 限制sessions大小
  const limitedSessions = {};
  Object.keys(stateToSave.sessions).forEach(key => {
    limitedSessions[key] = stateToSave.sessions[key].slice(-30); // 每个会话只保存30条
  });
  stateToSave.sessions = limitedSessions;

  sessionStorage.setItem('state', JSON.stringify(stateToSave));
});

export default store;