import { service } from '../service'

/**
 * 会员（v12 §7.1 / §7.2 / §7.3）
 *
 * 四个方法对应后端四个接口，其中下单是**两步**（2026-10-11 拆分）：
 *   memberOrderCreateApi 落一条待支付快照拿单号 → memberPaySignApi 按单号换双签名。
 * 拆开的唯一意义就是：**签名失败重签同一单号**，不再像拆前那样每次重试都多留一条待支付单，
 * 所以这两个方法之间不得在内存里传任何价格或套餐信息（签名一律由服务端回读它自己那行快照）。
 */

// 会员状态聚合：档位 + 在售套餐 + 「我的」页卡片文案 + 最近开通记录，一次请求全给
export const memberInfoApi = params => {
  return service({
    url: '/member/info',
    method: 'GET',
    params
  })
}

// 创建订单：入参 { planCode }，出参 MemberOrderVO（单号、金额快照、expireAt 支付截止时刻）
// ⚠️ 出参里的 expireAt 是服务端定的，前端画倒计时用它，不要拿「我点了多久」自己算
export const memberOrderCreateApi = params => {
  return service({
    url: '/member/order/create',
    method: 'POST',
    params
  })
}

// 取支付签名：入参 { outTradeNo }，出参 XpayPrepayVO（signData / paySig / signature / mode）
// ⚠️ URL 挂在 /xpay 前缀下（它产出的是通道参数、与卖什么无关），但类内实现仍留在会员侧，
//    所以这一行签出来的只有会员的单；出参整包透给 wx.requestVirtualPayment，前端一个字段都不要碰
export const memberPaySignApi = params => {
  return service({
    url: '/xpay/sign',
    method: 'POST',
    params
  })
}

// 查单兜底 + 主动发放：入参 { outTradeNo }，出参 { delivered, status }
// 支付 success 回调只是「微信说付完了」，权益落没落库只认本接口的 delivered
export const memberConfirmApi = params => {
  return service({
    url: '/member/confirm',
    method: 'POST',
    params
  })
}
