import { service } from '../service'

/**
 * 订阅消息（决策到期提醒等一次性订阅授权同步）
 *
 * 前端只用到一个 `sync`：{@code requestSubscribeMessage} 拿到 accept 后，把「这次授权」同步给后端，
 * 让 {@code subscribe} 表给当前用户该模板累加一次可发配额。发送是服务端定时任务的内部行为，不在此开接口。
 * ⚠️ 一次性订阅「授权一次=可发一次」，微信不查剩余配额、全靠本地记账，所以这一步不能省：
 *    不同步 → 后端 consume 永远扣不到 → 到期提醒发不出。
 */

// 同步订阅授权次数：入参 { templateId, count }（count 通常为 1，与后端 SubscribeSyncDTO 对齐）
export const subscribeSyncApi = params => {
  return service({
    url: '/subscribe/sync',
    method: 'POST',
    params
  })
}
