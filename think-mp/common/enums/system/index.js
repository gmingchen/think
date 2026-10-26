// 小程序版本
export const ENV_VERSION_ENUM = {
  DEVELOP: 'develop',
  TRIAL: 'trial',
  RELEASE: 'release',
}

// 环境变量
export const ENV_ENUM = {
  DEVELOPMENT: 'develop',
  TESTING: 'trial',
  PRODUCTION: 'release'
}

// 接口 host
export const HOST_ENUM = {
  [ENV_ENUM.DEVELOPMENT]: 'https://think.mx-studio.cn',
  // [ENV_ENUM.DEVELOPMENT]: 'http://localhost:8820',
  [ENV_ENUM.TESTING]: 'https://think.mx-studio.cn',
  [ENV_ENUM.PRODUCTION]: 'https://think.mx-studio.cn',
}

// 客户端平台（wx.getDeviceInfo().platform 的返回值，落库于 store.system.platform）
export const PLATFORM_ENUM = {
  IOS: 'ios',
  ANDROID: 'android',
  DEVTOOLS: 'devtools',
  WINDOWS: 'windows',
  MAC: 'mac',
}

// 请求方式
export const METHOD_ENUM = {
  OPTIONS: 'OPTIONS',
  GET: 'GET',
  HEAD: 'HEAD',
  POST: 'POST',
  PUT: 'PUT',
  DELETE: 'DELETE',
  TRACE: 'TRACE',
  CONNECT: 'CONNECT'
}

// 请求头-内容类型
export const CONTENT_TYPE_ENUM = {
  JSON: 'application/json;charset=UTF-8',
  FORM: 'application/x-www-form-urlencoded;charset=UTF-8',
  UPLOAD: 'multipart/form-data',
  STREAM: 'application/octet-stream;charset=UTF-8'
}

// 请求页面参数key
export const QUERY_KEY_ENUM = {
  SHARE_USER_ID: 'sui', // 分享者用户ID
}
