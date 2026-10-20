// 后端业务码常量（与 dishes-bk ResultCodeEnum 对齐，集中管理，勿散落）

// 已达上限（厨房菜谱/分类容量）：页面层据此弹「看视频扩容」引导，service.js 对该码不自动 toast
export const LIMIT_REACHED_CODE = 4005

// 登录态（sessionKey）过期：utils/vipPay.js 据此静默重登后**重签同一单号**，所以 service.js 也不自动 toast
// —— 此刻弹「登录态已过期」只会让人以为出了大事，而下一步重试通常就成功了（§12.1 的 -15007）
export const SESSION_KEY_EXPIRED_CODE = 4006
