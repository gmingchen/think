import { showRewardedAd } from './ad'
import {
  AD_UNIT_KITCHEN_EXPAND_DISH,
  AD_UNIT_KITCHEN_EXPAND_CATEGORY,
} from '../common/constants/index'

// 激励视频扩容公共逻辑：厨房设置页（入口一）与新增菜谱/分类页（入口二）复用同一套
// createRewardedVideoAd 同一 adUnitId 返回单例，模块级缓存 + onClose 只注册一次，避免回调累加
let dishAd = null
let categoryAd = null
// 本次发起扩容的广告类型：onClose 只处理本次发起的那一端，防止模拟器对所有实例广播 onClose 导致两端同时扩容
let pendingType = null
// 本次发起的结算回调（{ type, ended }) => void；单例广告同一时刻只播放一个，故用模块级即可
let activeSettled = null

// onClose/展示失败统一走这里结清：仅当类型与本次发起一致时才回调，否则视为广播噪声忽略
function settle(type, ended) {
  if (pendingType !== type) return
  pendingType = null
  const cb = activeSettled
  activeSettled = null
  cb && cb({ type, ended })
}

// 首次调用时创建两个激励视频单例并注册回调（onLoad 由微信自动触发；onError 处理无填充/异常调用）
function ensureAds() {
  if (!wx.createRewardedVideoAd) return
  if (!dishAd) {
    dishAd = wx.createRewardedVideoAd({ adUnitId: AD_UNIT_KITCHEN_EXPAND_DISH })
    dishAd.onError((err) => {
      console.warn('[expand-ad] 菜谱广告拉取失败', err && err.errCode, err && err.errMsg)
    })
    dishAd.onClose((res) => settle('dish', !!(res && res.isEnded)))
  }
  if (!categoryAd) {
    categoryAd = wx.createRewardedVideoAd({ adUnitId: AD_UNIT_KITCHEN_EXPAND_CATEGORY })
    categoryAd.onError((err) => {
      console.warn('[expand-ad] 分类广告拉取失败', err && err.errCode, err && err.errMsg)
    })
    categoryAd.onClose((res) => settle('category', !!(res && res.isEnded)))
  }
}

/**
 * 播放扩容激励视频；完整观看后回调 onSettled({ type, ended })
 * ended=false 表示未看完 / 环境不支持 / 广告展示失败，调用方据此复位按钮态，不发奖
 * @param {'dish'|'category'} type 扩容类型
 * @param {(r: { type: string, ended: boolean }) => void} onSettled
 */
export function playExpandAd(type, onSettled) {
  ensureAds()
  const ad = type === 'dish' ? dishAd : categoryAd
  if (!ad) {
    wx.$toast('当前环境暂不支持广告')
    onSettled && onSettled({ type, ended: false })
    return
  }
  pendingType = type
  activeSettled = onSettled
  showRewardedAd(ad, '广告加载失败，请稍后再试', () => {
    // 展示失败不会触发 onClose，这里主动结清，避免 pending 残留导致下次误判
    settle(type, false)
  })
}
