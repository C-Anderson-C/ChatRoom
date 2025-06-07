import Vue from 'vue'
import Vuex from 'vuex'
import SockJS from 'sockjs-client'
import Stomp from 'stompjs'
import {Notification} from 'element-ui';
import message from "@/components/chat/message.vue";

Vue.use(Vuex)

const store = new Vuex.Store({
    state: {
        user: JSON.parse(window.sessionStorage.getItem("user")),
        currentUser: JSON.parse(window.sessionStorage.getItem("user")) || {},
        stomp: null,
        isDot: {},
        currentSession: {},
        currentGroupId: null,
        filterKey: '',
        sessions: JSON.parse(localStorage.getItem('chat-session')) || {},
        groupSessions: {},
        friendList: [],
        groups: [],
        sentMessageIds: new Set(),
        reconnectAttempts: 0,
        subscriptions: [],
        connectingPromise: null, // 添加连接Promise追踪
        heartbeatTimer: null,    // 心跳定时器
        connectionState: 'disconnected', // 连接状态：disconnected, connecting, connected
        processedMessageIds: new Set(), // 已处理的消息ID集合
        lastConnectionError: null, // 最后一次连接错误
        debugMode: true // 调试模式
    },

    mutations: {
        // 初始化数据
        initData(state) {
            try {
                const data = localStorage.getItem('chat-session');
                if (data) {
                    state.sessions = JSON.parse(data);
                }

                if (!state.sessions) {
                    state.sessions = {};
                }

                if (state.debugMode) {
                    console.log('[Store] 初始化完成, 已加载会话数:', Object.keys(state.sessions).length);
                }
            } catch (e) {
                console.error('[Store] 初始化数据出错:', e);
                state.sessions = {};
            }
        },

        // 设置WebSocket客户端
        SET_STOMP(state, stompClient) {
            state.stomp = stompClient;
        },

        // 设置连接状态
        SET_CONNECTION_STATE(state, newState) {
            state.connectionState = newState;
        },

        // 设置连接错误
        SET_CONNECTION_ERROR(state, error) {
            state.lastConnectionError = error;
        },

        // 更新当前会话对象
        changeCurrentSession(state, currentSession) {
            state.currentSession = currentSession;
            // 标记会话消息为已读
            if (currentSession && currentSession.username && state.currentUser && state.currentUser.username) {
                const sessionKey = state.currentUser.username + "#" + currentSession.username;
                Vue.set(state.isDot, sessionKey, false);
                if (state.debugMode) {
                    console.log(`[Store] 切换到会话: ${sessionKey}, 标记为已读`);
                }
            }
        },

        // 设置当前群组ID
        setCurrentGroupId(state, groupId) {
            state.currentGroupId = groupId;
            if (state.debugMode) {
                console.log('[Store] 设置当前群组ID:', groupId);
            }
        },

        // 添加私聊消息
        addMessage(state, msg) {
            if (state.debugMode) {
                console.group('[Store] 添加私聊消息');
                console.log('消息详情:', msg);
            }

            // 确保 state.sessions 存在
            if (!state.sessions) {
                Vue.set(state, 'sessions', {});
            }

            // 确定消息键格式为 "当前用户#对方用户"
            let messageKey;
            let isSelf = msg.self === true ||
                msg.from === state.currentUser.username ||
                msg.fromId === state.currentUser.id;

            // 根据消息方向确定键
            if (isSelf) {
                // 自己发的消息: "自己#对方"
                messageKey = `${state.currentUser.username}#${msg.to}`;
            } else {
                // 收到的消息: "自己#发送者"
                messageKey = `${state.currentUser.username}#${msg.from}`;
            }

            if (state.debugMode) {
                console.log('使用私聊消息键:', messageKey);
            }

            // 确保会话消息数组存在
            if (!state.sessions[messageKey]) {
                Vue.set(state.sessions, messageKey, []);
            }

            // 为消息添加唯一ID（如果没有）
            const messageId = msg.messageId || msg.id || (Date.now() + Math.random().toString(36).substring(2, 9));

            // 检查消息是否已存在（防止重复）
            const existingMessageIndex = state.sessions[messageKey].findIndex(m =>
                (m.id === messageId || m.messageId === messageId || (m.content === msg.content &&
                    Math.abs(new Date(m.createTime) - new Date(msg.createTime || new Date())) < 3000))
            );

            if (existingMessageIndex !== -1) {
                if (state.debugMode) {
                    console.log(`私聊消息已存在，不再添加:`, messageId);
                }
                console.groupEnd();
                return;
            }

            // 添加消息
            const newMessage = {
                id: messageId,
                messageId: messageId, // 同时设置两个ID，确保一致性
                content: msg.content,
                createTime: msg.createTime || new Date(),
                fromId: msg.fromId || msg.from,
                from: msg.from,
                to: msg.to,
                fromName: msg.fromNickname || msg.fromName || msg.from,
                fromProfile: msg.fromProfile,
                self: isSelf
            };

            state.sessions[messageKey].push(newMessage);

            // 限制消息数量，保留最新的 200 条消息
            if (state.sessions[messageKey].length > 200) {
                state.sessions[messageKey] = state.sessions[messageKey].slice(-200);
                if (state.debugMode) {
                    console.log(`私聊[${messageKey}]消息数量已限制为最新的200条`);
                }
            }

            if (state.debugMode) {
                console.log(`私聊[${messageKey}]消息已添加, 当前消息数:`, state.sessions[messageKey].length);
            }

            // 保存到本地
            try {
                localStorage.setItem('chat-session', JSON.stringify(state.sessions));
            } catch (e) {
                console.error('[Store] 保存消息到本地存储失败:', e);
                // 如果存储失败（可能是存储空间已满），清除一部分旧消息
                try {
                    const oldSessions = JSON.parse(localStorage.getItem('chat-session') || '{}');
                    // 对每个会话保留最新的50条消息
                    Object.keys(oldSessions).forEach(key => {
                        if (Array.isArray(oldSessions[key]) && oldSessions[key].length > 50) {
                            oldSessions[key] = oldSessions[key].slice(-50);
                        }
                    });
                    localStorage.setItem('chat-session', JSON.stringify(oldSessions));
                    console.log('[Store] 已清理部分聊天记录，重新保存成功');
                } catch (e2) {
                    console.error('[Store] 尝试清理后仍无法保存:', e2);
                }
            }

            // 如果不是自己发送的消息，且不是当前会话的消息，则设置未读标记
            if (!isSelf && state.currentSession && state.currentSession.username !== msg.from) {
                Vue.set(state.isDot, messageKey, true);

                // 如果不是当前激活的会话，显示通知
                Notification.info({
                    title: '新消息',
                    message: `${msg.fromName || msg.from}：${msg.content.length > 30 ? msg.content.substring(0, 30) + '...' : msg.content}`,
                    position: 'bottom-right'
                });
            }

            // 触发视图更新事件
            Vue.prototype.$bus && Vue.prototype.$bus.$emit('new-message', {
                message: newMessage,
                addToInjected: true
            });

            if (state.debugMode) {
                console.groupEnd();
            }
        },

        // 添加群聊消息
        addGroupMessage(state, msg) {
            // 确定消息的群组ID - 处理各种格式
            let groupId;
            if (msg.to) {
                // 处理可能的格式: "group_2" 或 "2" 或 2
                if (typeof msg.to === 'string' && msg.to.startsWith('group_')) {
                    groupId = msg.to.replace('group_', '');
                } else {
                    groupId = msg.to;
                }
            } else {
                groupId = '1'; // 默认群组
            }

            const groupKey = `群聊_${groupId}`;

            if (state.debugMode) {
                console.group('[Store] 添加群组消息');
                console.log('群组键:', groupKey);
                console.log('原始to:', msg.to);
                console.log('消息内容:', msg.content);
            }

            // 确保 state.sessions 存在
            if (!state.sessions) {
                Vue.set(state, 'sessions', {});
            }

            // 确保该群组的消息数组存在
            if (!state.sessions[groupKey]) {
                Vue.set(state.sessions, groupKey, []);
            }

            // 为消息添加唯一ID（如果没有）
            const messageId = msg.messageId || msg.id || (Date.now() + Math.random().toString(36).substring(2, 9));

            // 检查消息是否已存在（防止重复）
            const existingMessageIndex = state.sessions[groupKey].findIndex(m =>
                (m.id === messageId || m.messageId === messageId || (m.content === msg.content &&
                    Math.abs(new Date(m.createTime) - new Date(msg.createTime || new Date())) < 3000))
            );

            if (existingMessageIndex !== -1) {
                if (state.debugMode) {
                    console.log(`群聊消息已存在，不再添加:`, messageId);
                }
                console.groupEnd();
                return;
            }

            // 添加消息，并确保正确标记 self 属性
            const newMessage = {
                id: messageId,
                messageId: messageId, // 同时设置两个ID，确保一致性
                content: msg.content,
                createTime: msg.createTime || new Date(),
                fromId: msg.fromId || msg.from,
                from: msg.from,
                to: msg.to,
                fromName: msg.fromNickname || msg.fromName || msg.from,
                fromProfile: msg.fromProfile,
                self: msg.self === true || msg.fromId === state.currentUser.id || msg.from === state.currentUser.username
            };

            // 添加消息到列表
            state.sessions[groupKey].push(newMessage);

            // 限制消息数量，保留最新的 200 条消息
            if (state.sessions[groupKey].length > 200) {
                state.sessions[groupKey] = state.sessions[groupKey].slice(-200);
                if (state.debugMode) {
                    console.log(`群组[${groupKey}]消息数量已限制为最新的200条`);
                }
            }

            if (state.debugMode) {
                console.log(`群组[${groupKey}]消息已添加, 现有消息数:`, state.sessions[groupKey].length);
            }

            // 保存到本地
            try {
                localStorage.setItem('chat-session', JSON.stringify(state.sessions));
            } catch (e) {
                console.error('[Store] 保存消息到本地存储失败:', e);
                // 如果存储失败（可能是存储空间已满），清除一部分旧消息
                try {
                    const oldSessions = JSON.parse(localStorage.getItem('chat-session') || '{}');
                    // 对每个会话保留最新的50条消息
                    Object.keys(oldSessions).forEach(key => {
                        if (Array.isArray(oldSessions[key]) && oldSessions[key].length > 50) {
                            oldSessions[key] = oldSessions[key].slice(-50);
                        }
                    });
                    localStorage.setItem('chat-session', JSON.stringify(oldSessions));
                    console.log('[Store] 已清理部分聊天记录，重新保存成功');
                } catch (e2) {
                    console.error('[Store] 尝试清理后仍无法保存:', e2);
                }
            }

            // 如果不是自己发送的消息，且不是当前活动群组，则设置未读标记
            if (!newMessage.self && state.currentGroupId !== groupId) {
                Vue.set(state.isDot, groupKey, true);

                // 显示通知
                Notification.info({
                    title: '群聊消息',
                    message: `${newMessage.fromName || newMessage.from}：${newMessage.content.length > 30 ? newMessage.content.substring(0, 30) + '...' : newMessage.content}`,
                    position: 'bottom-right'
                });
            }

            // 触发视图更新事件
            Vue.prototype.$bus && Vue.prototype.$bus.$emit('new-message', {
                message: newMessage,
                addToInjected: true
            });

            if (state.debugMode) {
                console.groupEnd();
            }
        },

        // 更新过滤关键字
        updateFilterKey(state, value) {
            state.filterKey = value;
        },

        // 设置好友列表
        setFriendList(state, friendList) {
            state.friendList = friendList;
            if (state.debugMode) {
                console.log('[Store] 设置好友列表, 数量:', friendList.length);
            }
        },

        // 设置群组列表
        setGroups(state, groups) {
            state.groups = groups;
            if (state.debugMode) {
                console.log('[Store] 设置群组列表, 数量:', groups.length);
            }
        },

        // 切换聊天列表显示
        changeCurrentList(state, listName) {
            if (state.debugMode) {
                console.log("[Store] 切换到列表: " + listName);
            }
        }
    },

    actions: {
        // WebSocket连接
        // 创建WebSocket连接
        connect(context) {
            // 已经有连接先断开
            if (context.state.stomp) {
                context.state.stomp.disconnect();
                context.state.stomp = null;
            }

            // 创建一个SockJS实例，指向统一端点 /ws
            const socket = new SockJS('/ws');  // 修改连接URL
            context.state.stomp = Stomp.over(socket);

            // 添加调试日志
            context.state.stomp.debug = function(str) {
                console.log("[WebSocket] " + str);
            };

            // 连接WebSocket
            context.state.stomp.connect({
                // 添加用户认证信息
                userId: context.state.currentUser.id,
                username: context.state.currentUser.username
            }, success => {
                console.log("[WebSocket] 连接成功:", success);

                /**
                 * 订阅系统广播通知消息
                 */
                context.state.stomp.subscribe("/topic/notification", msg => {
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
                context.state.stomp.subscribe("/topic/greetings", msg => {
                    let receiveMsg = JSON.parse(msg.body);
                    if (context.state.currentSession.username != "群聊") {
                        Vue.set(context.state.isDot, context.state.currentUser.username+"#群聊", true);
                    }
                    context.commit('addGroupMessage', receiveMsg);
                });

                /**
                 * 订阅私人消息
                 */
                context.state.stomp.subscribe('/user/queue/chat', msg => {
                    try {
                        let receiveMsg = JSON.parse(msg.body);
                        console.log('[WebSocket] 收到消息:', receiveMsg);

                        // 使用messageId进行去重
                        const msgId = receiveMsg.messageId;
                        if (msgId && !context.state.processedMessages.has(msgId)) {
                            context.state.processedMessages.add(msgId);

                            // 如果消息过多，移除最早的消息ID
                            if (context.state.processedMessages.size > 100) {
                                const oldestId = Array.from(context.state.processedMessages)[0];
                                context.state.processedMessages.delete(oldestId);
                            }

                            // 如果不是当前会话，显示通知
                            if (!context.state.currentSession || receiveMsg.from != context.state.currentSession.username) {
                                Notification.info({
                                    title: '【' + receiveMsg.fromNickname + '】发来一条消息',
                                    message: receiveMsg.content.length < 8 ? receiveMsg.content : receiveMsg.content.substring(0, 8) + "...",
                                    position: "bottom-right"
                                });
                                Vue.set(context.state.isDot, context.state.currentUser.username + "#" + receiveMsg.from, true);
                            }

                            // 根据消息方向设置notSelf属性
                            receiveMsg.notSelf = receiveMsg.direction === "received";

                            // 确保to属性正确
                            if (receiveMsg.direction === "received") {
                                receiveMsg.to = context.state.currentUser.username; // 接收的消息，to是当前用户
                            }

                            // 将消息添加到会话
                            context.commit('addMessage', receiveMsg);
                        } else {
                            console.log('[WebSocket] 跳过重复消息:', msgId);
                        }
                    } catch (error) {
                        console.error('[WebSocket] 消息处理错误:', error);
                    }
                });
            }, error => {
                console.error('[WebSocket] 连接错误:', error);
                Notification.info({
                    title: '系统消息',
                    message: "无法与服务端建立连接，请尝试重新登陆系统~",
                    position: "top-right"
                });
            });
        },

        // WebSocket断开连接
        disconnect(context) {
            if (context.state.debugMode) {
                console.log('[Store] 断开WebSocket连接');
            }

            // 更新连接状态
            context.commit('SET_CONNECTION_STATE', 'disconnecting');

            // 清除心跳定时器
            if (context.state.heartbeatTimer) {
                clearInterval(context.state.heartbeatTimer);
                context.state.heartbeatTimer = null;
            }

            // 清除所有订阅
            if (context.state.subscriptions && context.state.subscriptions.length > 0) {
                context.state.subscriptions.forEach(sub => {
                    try {
                        if (sub && typeof sub.unsubscribe === 'function') {
                            sub.unsubscribe();
                        }
                    } catch (e) {
                        console.error('[Store] 取消订阅失败:', e);
                    }
                });
                context.state.subscriptions = [];
            }

            // 断开连接
            if (context.state.stomp != null) {
                try {
                    context.state.stomp.disconnect(() => {
                        console.log("[Store] WebSocket成功断开连接");
                        context.state.stomp.connected = false;
                        context.commit('SET_CONNECTION_STATE', 'disconnected');
                    });
                } catch (e) {
                    console.error("[Store] 断开WebSocket连接时发生错误:", e);
                    // 强制标记为断开
                    if (context.state.stomp) {
                        context.state.stomp.connected = false;
                    }
                    context.commit('SET_CONNECTION_STATE', 'disconnected');
                }
            } else {
                context.commit('SET_CONNECTION_STATE', 'disconnected');
            }
        }
    }
});

// 如果有会话状态恢复
if (window.sessionStorage.getItem("state")) {
    try {
        let oldState = JSON.parse(window.sessionStorage.getItem("state"));
        // 仅恢复不为 null 的属性
        for (let key in oldState) {
            if (oldState[key] !== null && key !== 'stomp' && key !== 'connectingPromise' && key !== 'heartbeatTimer') {
                store.state[key] = oldState[key];
            }
        }

        // 确保 sessions 是对象而不是 null
        if (!store.state.sessions) {
            store.state.sessions = {};
        }

        // 确保 currentSession 是对象而不是 null
        if (!store.state.currentSession) {
            store.state.currentSession = {};
        }

        // 初始化消息相关的集合
        store.state.sentMessageIds = new Set();
        store.state.processedMessageIds = new Set();

        // 初始化订阅数组
        store.state.subscriptions = [];

        console.log('[Store] 成功恢复会话状态');
    } catch (e) {
        console.error('[Store] 恢复会话状态失败:', e);
    }
}

// 初始化数据
store.commit('initData');

export default store;