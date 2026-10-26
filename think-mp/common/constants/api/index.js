// 后端业务码常量（与 think-bk ResultCodeEnum 对齐，集中管理，勿散落）

// 已达上限：service.js 对该码不自动 toast，由页面层自行引导
export const LIMIT_REACHED_CODE = 4005

// 登录态（sessionKey）过期：service.js 对该码不自动 toast
export const SESSION_KEY_EXPIRED_CODE = 4006
