import axios from 'axios';
import { Message, Loading } from "element-ui";
import router from '../router';

// 全局配置
let base = '';  // API基础URL
const REQUEST_TIMEOUT = 15000;  // 请求超时时间(ms)
const MAX_RETRIES = 2;  // 最大重试次数
let loadingInstance = null;  // 加载指示器实例
let pendingRequests = new Map();  // 跟踪进行中的请求
let networkErrorCount = 0;  // 网络错误计数器

// 创建一个axios实例，便于配置
const axiosInstance = axios.create({
  timeout: REQUEST_TIMEOUT,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
});

// 用于取消重复请求
const CancelToken = axios.CancelToken;

// 请求拦截器
axiosInstance.interceptors.request.use(config => {
  // 生成请求标识
  const requestId = `${config.method}:${config.url}:${JSON.stringify(config.params || {})}:${JSON.stringify(config.data || {})}`;

  // 检查是否有相同的请求正在进行
  if (pendingRequests.has(requestId)) {
    config.cancelToken = new CancelToken((cancel) => {
      cancel(`重复请求：${requestId}`);
    });
  } else {
    pendingRequests.set(requestId, true);
    config._requestId = requestId;
  }

  // 添加身份验证令牌（如果存在）
  const token = sessionStorage.getItem('token');
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`;
  }

  // 计算请求大小
  const headerSize = JSON.stringify(config.headers).length;
  let dataSize = 0;
  if (config.data) {
    dataSize = typeof config.data === 'string' ? config.data.length : JSON.stringify(config.data).length;
  }
  const totalSize = headerSize + dataSize;

  // 仅在开发环境或启用调试时记录详细信息
  if (process.env.NODE_ENV === 'development' || sessionStorage.getItem('debug') === 'true') {
    console.log(`[API] 请求 ${config.url} - 方法: ${config.method}`);
    console.log(`[API] 头部大小: ${headerSize}字节, 数据大小: ${dataSize}字节, 总计: ${totalSize}字节`);
  }

  // 警告大型请求
  if (totalSize > 8000) {
    console.warn(`[API] 警告: 请求 ${config.url} 总大小 ${totalSize}字节，可能会触发 431 错误`);
  }

  return config;
}, error => {
  console.error('[API] 请求拦截器错误:', error);
  return Promise.reject(error);
});

// 响应拦截器
axiosInstance.interceptors.response.use(response => {
  // 请求完成后，移除请求ID
  if (response.config && response.config._requestId) {
    pendingRequests.delete(response.config._requestId);
  }

  // 重置网络错误计数器，因为请求成功了
  networkErrorCount = 0;

  // 关闭加载指示器
  if (loadingInstance) {
    loadingInstance.close();
    loadingInstance = null;
  }

  // 处理成功响应
  if (response.status && response.status === 200) {
    // 服务端返回的错误状态
    if (response.data.status === 500) {
      Message.error({ message: response.data.msg || '服务器处理出错' });
      return Promise.reject(new Error(response.data.msg || '服务器处理出错'));
    }

    // 成功消息提示（只对明确指定的成功消息进行提示）
    if (response.data.msg && response.config.showSuccessMessage !== false) {
      Message.success({ message: response.data.msg });
    }

    return response.data;
  }

  return response;
}, error => {
  // 取消的请求不显示错误
  if (axios.isCancel(error)) {
    console.log('[API] 请求被取消:', error.message);
    return Promise.reject(error);
  }

  // 关闭加载指示器
  if (loadingInstance) {
    loadingInstance.close();
    loadingInstance = null;
  }

  // 从请求中移除跟踪ID
  if (error.config && error.config._requestId) {
    pendingRequests.delete(error.config._requestId);
  }

  // 详细错误日志
  console.error('[API] 请求失败:', error);

  // 网络错误处理
  if (error.message && (error.message.includes('Network Error') || error.message.includes('timeout'))) {
    networkErrorCount++;
    console.log(`[API] 网络错误计数: ${networkErrorCount}`);

    // 如果连续网络错误太多，提示用户检查网络
    if (networkErrorCount >= 3) {
      Message.error({
        message: '检测到网络连接不稳定，请检查您的网络设置',
        duration: 5000
      });
      networkErrorCount = 0; // 重置计数器以防连续警告
    }
  }

  // 根据错误类型处理
  if (error.response) {
    const status = error.response.status;
    // 记录详细的错误数据，用于调试
    console.error(`[API] ${status} 错误详情:`, error.response.data);

    // 常见HTTP错误处理
    switch (status) {
      case 400:
        Message.error({ message: '请求参数错误' });
        break;
      case 401:
        Message.warning({ message: '会话已过期，请重新登录' });
        sessionStorage.removeItem('user');
        sessionStorage.removeItem('token');
        router.replace('/');
        break;
      case 403:
        Message.error({ message: '没有权限执行此操作' });
        break;
      case 404:
        Message.error({ message: '请求的资源不存在' });
        break;
      case 429:
        Message.error({ message: '请求过于频繁，请稍后再试' });
        break;
      case 431:
        Message.error({ message: '请求头字段太大，请简化请求数据' });
        break;
      case 500:
        Message.error({ message: '服务器内部错误' });
        break;
      case 502:
        Message.error({ message: '网关错误' });
        break;
      case 503:
        Message.error({ message: '服务暂时不可用' });
        break;
      case 504:
        Message.error({ message: '网关超时' });
        break;
      default:
        // 获取并显示服务器的错误消息
        const errorMsg = error.response.data?.msg || error.response.data?.message || `服务器响应错误(${status})`;
        Message.error({ message: errorMsg });
    }
  } else if (error.request) {
    // 请求发送了但没有响应
    Message.error({ message: '服务器没有响应，请检查网络连接' });
  } else {
    // 请求设置错误
    Message.error({ message: `请求错误: ${error.message}` });
  }

  // 如果配置了重试且尚未超过最大重试次数
  if (error.config && !error.config._retryCount) {
    error.config._retryCount = 1;
    return axiosInstance(error.config);
  } else if (error.config && error.config._retryCount < MAX_RETRIES) {
    error.config._retryCount++;
    // 指数退避重试
    const delay = 1000 * Math.pow(2, error.config._retryCount - 1);
    return new Promise(resolve => {
      setTimeout(() => resolve(axiosInstance(error.config)), delay);
    });
  }

  return Promise.reject(error);
});

// 创建请求方法
function createRequest(method) {
  return (url, data = null, config = {}) => {
    const showLoading = config.loading !== false;
    const showSuccessMsg = config.showSuccessMessage !== false;

    // 显示加载指示器
    if (showLoading && !loadingInstance) {
      loadingInstance = Loading.service({
        fullscreen: true,
        text: config.loadingText || '正在加载...',
        background: 'rgba(0, 0, 0, 0.7)'
      });
    }

    // 准备请求配置
    const requestConfig = {
      method,
      url: `${base}${url}`,
      showSuccessMessage: showSuccessMsg,
      ...config
    };

    // 根据HTTP方法设置数据
    if (['post', 'put', 'patch'].includes(method)) {
      requestConfig.data = data;
    } else {
      requestConfig.params = data;
    }

    return axiosInstance(requestConfig);
  };
}

// 导出请求方法
export const getRequest = createRequest('get');
export const postRequest = createRequest('post');
export const putRequest = createRequest('put');
export const deleteRequest = createRequest('delete');
export const patchRequest = createRequest('patch');

/**
 * 用于表单提交的POST请求（使用x-www-form-urlencoded格式）
 */
export const postKeyValueRequest = (url, params, config = {}) => {
  const showLoading = config.loading !== false;

  if (showLoading && !loadingInstance) {
    loadingInstance = Loading.service({
      fullscreen: true,
      text: config.loadingText || '正在处理...',
      background: 'rgba(0, 0, 0, 0.7)'
    });
  }

  // 检查参数大小是否过大
  const dataSize = JSON.stringify(params).length;
  if (dataSize > 8000) {
    console.warn(`[API] 警告: 表单数据大小(${dataSize}字节)可能超过某些服务器限制`);
  }

  return axiosInstance({
    method: 'post',
    url: `${base}${url}`,
    data: params,
    transformRequest: [function (data) {
      let formData = '';
      for (let key in data) {
        if (data[key] !== undefined && data[key] !== null) {
          formData += `${encodeURIComponent(key)}=${encodeURIComponent(data[key])}&`;
        }
      }
      return formData.slice(0, -1);  // 移除最后的'&'
    }],
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    },
    ...config
  });
};

/**
 * 文件上传请求
 */
export const uploadFile = (url, file, params = {}, config = {}) => {
  const formData = new FormData();
  formData.append('file', file);

  // 添加其他参数
  Object.keys(params).forEach(key => {
    formData.append(key, params[key]);
  });

  const uploadConfig = {
    ...config,
    headers: {
      'Content-Type': 'multipart/form-data',
      ...config.headers
    }
  };

  return postRequest(url, formData, uploadConfig);
};

/**
 * 批量请求
 */
export const batchRequests = (requests, config = {}) => {
  const showLoading = config.loading !== false;

  if (showLoading && !loadingInstance) {
    loadingInstance = Loading.service({
      fullscreen: true,
      text: config.loadingText || '正在处理多个请求...',
      background: 'rgba(0, 0, 0, 0.7)'
    });
  }

  return axios.all(requests)
      .then(axios.spread((...responses) => {
        if (loadingInstance) {
          loadingInstance.close();
          loadingInstance = null;
        }
        return responses;
      }))
      .catch(error => {
        if (loadingInstance) {
          loadingInstance.close();
          loadingInstance = null;
        }
        throw error;
      });
};

/**
 * 设置API基础URL
 */
export const setBaseUrl = (newBase) => {
  base = newBase;
};

/**
 * 取消所有未完成的请求
 */
export const cancelAllRequests = () => {
  pendingRequests.forEach((_, requestId) => {
    console.log(`[API] 取消请求: ${requestId}`);
    // 这里可以实现具体的取消逻辑，如果需要
  });
  pendingRequests.clear();
};

/**
 * 重置网络错误计数器
 */
export const resetNetworkErrorCount = () => {
  networkErrorCount = 0;
};

/**
 * 导出axios实例以便可能的扩展
 */
export const axiosClient = axiosInstance;

/**
 * 检测网络连接状态
 */
export const checkNetworkConnection = async () => {
  try {
    await axiosInstance.get(`${base}/ping`, { timeout: 5000 });
    return true;
  } catch (error) {
    console.error('[API] 网络连接检查失败:', error);
    return false;
  }
};