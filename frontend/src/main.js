import Vue from 'vue'
import App from './App.vue'
import router from './router/index.js'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import store from './store/index.js'
import 'font-awesome/css/font-awesome.min.css'

// 只需一次性导入所有请求方法，并且带 .js 后缀
import { postKeyValueRequest, postRequest, getRequest, putRequest, deleteRequest } from './utils/api.js'

// 封装请求方法,供全局调用
Vue.prototype.postKeyValueRequest = postKeyValueRequest
Vue.prototype.postRequest = postRequest
Vue.prototype.getRequest = getRequest
Vue.prototype.putRequest = putRequest
Vue.prototype.deleteRequest = deleteRequest

// 创建一个事件总线并将其附加到 Vue 原型上
Vue.prototype.$bus = new Vue();
// 全局消息服务
Vue.prototype.$messageService = {
  // 存储所有消息
  messages: {},

  // 添加消息
  addMessage(key, message) {
    if (!this.messages[key]) {
      this.messages[key] = [];
    }

    // 添加消息
    this.messages[key].push({
      ...message,
      id: message.id || Date.now() + Math.random().toString(36).substring(2, 8),
      createTime: message.createTime || new Date()
    });

    // 触发事件
    Vue.prototype.$bus.$emit('direct-message-added', { key, message });

    console.log(`全局消息服务: 消息已添加到 ${key}, 当前消息数:`, this.messages[key].length);
  },

  // 获取指定键的消息
  getMessages(key) {
    return this.messages[key] || [];
  },

  // 获取所有消息键
  getAllKeys() {
    return Object.keys(this.messages);
  },

  // 查找与用户相关的消息
  findUserMessages(username) {
    const result = [];

    Object.keys(this.messages).forEach(key => {
      if (key.includes(username)) {
        result.push(...this.messages[key]);
      }
    });

    return result.sort((a, b) => new Date(a.createTime) - new Date(b.createTime));
  }
};

// 添加全局方法：滚动消息到底部
Vue.prototype.$scrollMessagesToBottom = function() {
  setTimeout(() => {
    const messageList = document.querySelector('.message-list');
    if (messageList) {
      messageList.scrollTop = messageList.scrollHeight;
    }
  }, 50);
};

/*路由前置守卫
to：去哪，from：从哪来，调用next()：通过本次路由请求*/
router.beforeEach((to, from, next) => {
  if (to.path === '/' || to.path === '/adminlogin') {
    // 首页和管理员登录页不需要请求菜单
    next()
  } else if (to.path === '/home' && !window.sessionStorage.getItem('admin')) {
    ElementUI.Message.error({ message: '不具有访问权限！' })
    next(from)
  } else {
    if (
        window.sessionStorage.getItem('user') ||
        window.sessionStorage.getItem('admin')
    ) {
      // 登录后才请求菜单
      next()
    } else {
      // 没登录就跳转到登陆页
      ElementUI.Message.error({ message: '请登录后访问！' })
      next('/?redirect=' + to.path)
    }
  }
})

Vue.config.productionTip = false
Vue.use(ElementUI)

new Vue({
  router,
  store,
  render: h => h(App)
}).$mount('#app')