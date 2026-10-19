import dayjs from 'dayjs'
import { store } from '../store/index'
import { memberOrderCreateApi, memberPaySignApi, memberConfirmApi } from '../apis/index'
import { SESSION_KEY_EXPIRED_CODE, MEMBER_QUERY_RETRY } from '../common/constants/index'
import { PLATFORM_ENUM } from '../common/enums/index'
import { requestVirtualPayment } from './wx-api'
import { toast } from './toast'

/**
 * 会员购买（v12 §10.1）：整条支付链路的唯一收口点
 *
 * 链路是**五步**，其中下单是两步（2026-10-11 拆分）：
 *   order/create 落一条待支付单拿单号 → /xpay/sign 按单号换双签名 →
 *   wx.requestVirtualPayment 拉微信收银台 → confirm 轮询查单 → getMember + getUser 双刷新
 *
 * ⚠️ 本文件**故意不登记进 utils/index.js**（不是遗漏，别“好心”给它补上）：
 *    一登记就多出 `utils/index → vipPay → apis/index → service.js → utils/index` 这条 require 环。
 *    说清楚一个曾被本文件夸大的事实：`apis/service.js` 顶部就 import 了 `store`，而 store 又 import
 *    `apis/index` —— **同形状的环早已存在且跑得好好的**，所以“成环 = 启动即崩”是错的断言。
 *    真正的未知量是编译器把命名导入产物化成属性访问（惰性，以环安全）还是快照（求值时取，就会拿到
 *    undefined），这一层我没有取证到。留外的理由因此不是“会崩”，而是：少一条环不需要任何辩护，
 *    多一条环要拿启动去赌一个未取证的编译器行为。调用方请直接 `import { buyVip } from '.../utils/vipPay'`。
 *
 * ⚠️ 这里不出现任何价格：金额、首单资格、划线价都由后端在订单行快照里定好（§7.1「前端不硬编码价格」），
 *    本模块只把 `expectedPriceText` 之类的原值透出去给页面画。
 *
 * @returns {Promise<'delivered'|'paid'|'cancel'|'fail'>}
 *   delivered 权益已到账 / paid 钱付了但 10s 内没确认（交给发货通知）/ cancel 用户自己取消 / fail 未成交
 */
export const buyVip = async (planCode) => {
  // 基础库门槛 ≥ 2.19.2（§2.1）：低版本连收银台都拉不起来，得在**建单之前**就说清楚，
  // 否则每次点都留下一条 15 分钟后才被关掉的死单
  if (!canBuyVip()) {
    toast('请升级微信后再试')
    return 'fail'
  }

  let order
  try {
    const r = await memberOrderCreateApi({ planCode })
    order = r.data
  } catch (error) {
    // 置灰「会员功能即将开放」、套餐已下架、product_id 缺失这些拒单原因，
    // 文案都在后端那句 message 里且已由 service.js 弹出，这里不重复加工也不往下走
    return 'fail'
  }

  // sessionKey 问题的重试只给一次：重登都刷不回 sessionKey 的话，再循环也只是让那条单多挂一会儿（§12.1）
  for (let attempt = 0; ; attempt++) {
    const result = await payOnce(order)
    if (result.done) return result.status
    if (attempt >= 1) {
      toast('登录态已过期，请重试')
      return 'fail'
    }
    // 拆两步的全部意义就在下面这行之后：**重签同一单号**，不像拆前那样只能换新单号、
    // 每重试一次多留一条待支付单。代价是重登得在这里做，不能交给页面
    await store.login()
  }
}

/**
 * 签一次名 + 拉一次收银台
 *
 * done=false 表示「是 sessionKey 的问题，重登之后可以拿同一个单号再来一遍」，
 * 这个出口有两个来源，必须都接住：
 *   ① 后端 4006 —— Redis 里那份 token 值没有 sessionKey（旧版本登录留下的，签不出名）；
 *   ② 客户端 -15007 —— 签名算出来了，但用的 sessionKey 已经不是当前这个（微信侧才发现）。
 * @returns {Promise<{ done: boolean, status?: string }>}
 */
const payOnce = async (order) => {
  const { outTradeNo } = order
  let sign
  try {
    const r = await memberPaySignApi({ outTradeNo })
    sign = r.data
  } catch (error) {
    const { code } = error || {}
    if (code === SESSION_KEY_EXPIRED_CODE) return sessionKeyLost(order)
    // 其余拒签（已过 expire_at / 状态不是待支付 / env 与当前环境不符）不能在这里自动重下单：
    // 重下单要重新判首单资格、重新取价，那是**用户的决定**，不是重试的细节（§12.1 第 2 行）
    return { done: true, status: 'fail' }
  }

  try {
    await requestVirtualPayment({
      signData: sign.signData,
      paySig: sign.paySig,
      signature: sign.signature,
      mode: sign.mode,
    })
  } catch (error) {
    const errNo = resolveErrNo(error)
    if (errNo === ERR_SESSION_KEY) return sessionKeyLost(order)
    // -2 是用户主动取消：订单留在 status=0，15 分钟后由签名路径或定时作业关掉（§12.1）。
    // 这里必须静默——弹「支付失败」会把一个自己点了取消的人劝去反复重试
    if (errNo === ERR_CANCEL) return { done: true, status: 'cancel' }
    toast(ERROR_TEXT[errNo] || defaultFailText())
    return { done: true, status: 'fail' }
  }

  return { done: true, status: await confirmDelivery(outTradeNo) }
}

/**
 * sessionKey 不可用时的分流：还来得及重签就标记重试，已经过期就别再碰这个单号
 * @returns {{ done: boolean, status?: string }}
 */
const sessionKeyLost = (order) => {
  // expireAt 是服务端定的截止时刻，前端只读不自算（§7.2.1）。挂着页面十几分钟再回来点，
  // 单已经关了：此时重签必被拒，而拒一次就多留一次日志，不如直接告诉用户重新下单
  if (order.expireAt && dayjs().isAfter(dayjs(order.expireAt))) {
    toast('支付超时，请重新下单')
    return { done: true, status: 'fail' }
  }
  return { done: false }
}

/**
 * 查单轮询：支付 success 后先调 1 次，未发放再按 interval 重试到 max 次（§7.3 前端策略）
 *
 * 微信的 success 回调只是「微信说付完了」，权益落没落库只认 confirm 回的 delivered；
 * 而 success 本身也可能因异常退出丢掉，所以本函数返回 'paid' 不等于失败——发货通知仍会补上。
 */
const confirmDelivery = async (outTradeNo) => {
  const { interval, max } = MEMBER_QUERY_RETRY
  for (let i = 0; i <= max; i++) {
    if (i > 0) await delay(interval)
    let data
    try {
      const r = await memberConfirmApi({ outTradeNo })
      data = r.data
    } catch (error) {
      // 查单本身失败就不再硬轮：service.js 每失败一次弹一个 toast，跑满 5 次是 5 个弹窗。
      // 钱已经扣了，落到下面那句兜底话术，比刷屏更接近事实
      break
    }
    if (data && data.delivered) {
      // 双刷新：本页状态卡与全站广告条、容量条要同帧更新（§5.3）。
      // 任一失败都不改写支付结果，权益已经发出去了，只是这一帧的界面旧一点
      await Promise.all([store.getMember(), store.getUser()]).catch(() => {})
      toast('开通成功')
      return 'delivered'
    }
  }
  // 跑满 10s 仍没确认：绝不弹「失败」，也不要弹「成功」——这句是唯一诚实的说法（§5.3）
  toast('支付已完成，权益将在 1 分钟内自动到账')
  return 'paid'
}

/**
 * 本机是否能拉起虚拟支付收银台（基础库 ≥ 2.19.2，§2.1）
 * 页面拿它决定 CTA 置灰（§5.2「不隐藏入口」），buyVip 拿它做建单前的闸门——
 * 判据只写这一处，否则两边各写一次 canIUse 的参数字符串，改版本门槛必漏一处
 */
export const canBuyVip = () => !!wx.canIUse('requestVirtualPayment')

// 虚拟支付客户端返回码（§12.1）。-2 与 -15007 在上面各有专属分支，不落在文案表里
const ERR_CANCEL = -2
const ERR_SESSION_KEY = -15007
const DEFAULT_FAIL_TEXT = '支付未完成，请稍后再试'
// iOS 走的是 Apple 支付通道，有两类不成交的原因在本机判不出来（系统低于 iOS 15、App Store 账号
// 不是中国大陆地区；“微信版本过旧”那一类由上面的 canBuyVip 门槛间接挡住），而官方不保证为它们
// 回一个固定错误码，全部只能落到这句兜底。光说“稍后再试”会把人留在原地反复重试同一个错（§2.5 结论四）
const IOS_FAIL_TEXT = '支付未完成，请确认系统为 iOS 15 及以上、App Store 为中国大陆账号'
const defaultFailText = () => (store.system.platform === PLATFORM_ENUM.IOS ? IOS_FAIL_TEXT : DEFAULT_FAIL_TEXT)
const ERROR_TEXT = {
  [-15005]: '下单异常，请稍后再试',
  [-15006]: '下单异常，请稍后再试',
  [-15011]: '下单异常，请稍后再试',
  [-15013]: '下单异常，请稍后再试',
  [-15014]: '活动太火爆，请稍后再试',
  [-15017]: '暂时无法开通，请稍后再试',
  [-15019]: '暂时无法开通，请稍后再试',
}

/**
 * 取虚拟支付的错误码
 * 正常给 `errNo`，但部分版本只在 `errMsg` 里塞一个负数（`requestVirtualPayment:fail -15007 ...`），
 * 两个都不认就会把「sessionKey 过期」这类可重试的错当成通用失败，白丢拆两步带来的重试能力
 */
const resolveErrNo = (error = {}) => {
  if (typeof error.errNo === 'number') return error.errNo
  const matched = /(-\d{1,6})/.exec(error.errMsg || '')
  return matched ? Number(matched[1]) : undefined
}

const delay = (ms) => new Promise(resolve => setTimeout(resolve, ms))
