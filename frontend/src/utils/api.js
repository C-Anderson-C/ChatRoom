import axios from 'axios';
import {Message} from "element-ui";
import router from '../router'

// 创建一个axios实例以便于统一配置
const instance = axios.create({
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
});

// 移除不必要的默认headers，减少请求头大小
delete instance.defaults.headers.common['X-Requested-With'];
// 如果使用了Authorization头且不在登录时使用，可以按需添加
// instance.defaults.headers.common['Authorization'] = '';

/*axios全局响应拦截*/
instance.interceptors.response.use(success => {
  if (success.status && success.status == 200 && success.data.status == 500) {//请求成功，但处理出现其他错误
    Message.error({message: success.data.msg})
    return;
  }
  //请求成功且服务器处理无错误
  if (success.data.msg) {
    Message.success({message: success.data.msg});
  }
  return success.data;
}, error => {
  if (!error.response) {
    Message.error({message: '网络错误，请检查您的网络连接'});
    return;
  }

  if (error.response.status == 504) {//	充当网关或代理的服务器，未及时从远端服务器获取请求
    Message.error({message: '找不到服务器'})
  } else if (error.response.status == 403) {	//服务器理解请求客户端的请求，但是拒绝执行此请求
    Message.error({message: '权限不足，请联系管理员'})
  } else if (error.response.status == 401) {//请求要求用户的身份认证
    Message.error({message: '尚未登录，请登录'});
    router.replace("/");//跳转到登陆页
  } else if (error.response.status == 404) {
    Message.error({message: '服务器无法根据客户端的请求找到资源'})
  } else if (error.response.status == 431) {
    Message.error({message: '请求头字段太大，请清除浏览器缓存后重试'})
  } else if (error.response.status == 500) {
    Message.error({message: '服务器内部错误，无法完成请求'})
  } else {
    if (error.response.data) {
      Message.error({message: error.response.data.msg})
    } else {
      Message.error({message: '未知错误!'})
    }
  }
  return Promise.reject(error);
});

let base = '';

/*
登录请求方法，与服务端Spring Security的登录接口对接
 */
export const postKeyValueRequest = (url, params) => {
  // 简化登录请求头信息
  const headers = {
    'Content-Type': 'application/x-www-form-urlencoded'
  };

  return instance({
    method: 'post',
    url: `${base}${url}`,
    data: params,
    transformRequest: [function (data) {//处理请求的数据格式
      let ret = '';
      for (let i in data) {
        ret += encodeURIComponent(i) + '=' + encodeURIComponent(data[i]) + '&'
      }
      return ret.slice(0, -1); // 移除最后一个&字符
    }],
    headers
  });
}

/*
封装"增加"请求方法——post
 */
export const postRequest = (url, params) => {
  return instance({
    method: 'post',
    url: `${base}${url}`,
    data: params
  });
}

/*
封装"修改"请求方法——put
 */
export const putRequest = (url, params) => {
  return instance({
    method: 'put',
    url: `${base}${url}`,
    data: params
  });
}

/*
封装"查询"请求方法——get
 */
export const getRequest = (url, params) => {
  return instance({
    method: 'get',
    url: `${base}${url}`,
    params: params // 对于GET请求，参数应该在params中而非data
  });
}

/*
封装"删除"请求方法——get
 */
export const deleteRequest = (url, params) => {
  return instance({
    method: 'delete',
    url: `${base}${url}`,
    params: params // 对于DELETE请求，建议使用params而非data
  });
}

export default instance;