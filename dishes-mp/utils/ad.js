import { store } from '../store/index'

/**
 * 广告侧的权益判据（v12 §10.2）：**三类广告各自一条判据，不要合成一条**
 *
 * 为什么必须分开（§6.1 三分法）：三类广告与会员权益的关系本来就不一样——
 *   • Banner 与插屏是「打扰型」，买会员就是为了不看它 → 归 `adFree` 管（下面的 adServiceEnabled，
 *     Banner 那一支在 `components/ad-unit/index.wxml` 里以 wxml 内联形式直读 `member.adFree`）；
 *   • 激励视频是「用观看换容量」的交易，**轻享会员（¥1.9）也完整可用** → 归厨房级的
 *     `KitchenVO.unlimitedCapacity` 管，判据里故意不含 `adFree`：把它混进去，付了钱的人容量反而
 *     变成一堵硬墙（净损失，比权益不生效更严重）；
 *   • 「看广告支持一下」是给予行为 → **只判总开关 `ad.enabled`**，所以它根本不需要一条会员判据：
 *     这一条留在 `pages/mine/components/link/index.wxml` 的内联写法里不动（§6.1 表末行「改动面：无」），
 *     本文件不为它提供一个无人调用的函数 —— 任何档位都不该关掉它，写进判据反而是错的。
 *
 * ⚠️ 本文件 import store 与 `apis/service.js` 是同一种形状（store ↔ utils 构成 require 环，
 *    但命名导入在微信编译产物里是属性访问，调用时才解析）；判据只在函数体内读 store，
 *    不要在模块顶层读（顶层拿到的会是尚未填完的半个 store）。
 */

// Banner 与插屏：广告总开关 ∧ 非会员（轻享与畅厨都免）
export const adServiceEnabled = () => !!store.ad?.enabled && !store.member?.adFree

/**
 * 看视频换容量：广告总开关 ∧「这个厨房还没对我取消上限」
 * @param {boolean} unlimited 传 **厨房级** 的 KitchenVO.unlimitedCapacity（= 畅厨在期 ∧ 我是厨师长）
 *
 * ⚠️ 不要传 store.member.unlimitedCapacity：那是**人级**标记（MemberInfoVO 只按「tier >= 2」算，
 *    不含归属判定），而畅厨的「不限」只作用于本人创建的厨房（§1.4 决策 1）。一个畅厨在别人的
 *    厨房里当管厨时，后端在那个厨房**不会**短路、照样报 4005；用人级判据就会把他的 📺 入口一起
 *    关掉 → 既看不了视频扩容又拿不到弹窗，是 §17 第 19 条那个「付了钱反而少一条路径」的同类错误。
 *    后端已把正确的厨房级值放进 KitchenVO，调用点直接取即可，前端零推导。
 *
 * ⚠️ 判据里同样不得出现 adFree（§6.1 三分法）：轻享不免激励视频。
 */
export const canExpandByAd = (unlimited = false) => !!store.ad?.enabled && !unlimited

/**
 * 激励广告加载（带重试）
 * @param {Object} ad 广告实例
 * @param {number} retries 最大重试次数，默认 3
 * @returns {Promise}
 */
export const loadAdWithRetry = (ad, retries = 3) => {
  return new Promise((resolve, reject) => {
    const attempt = (n) => {
      ad.load()
        .then(resolve)
        .catch((err) => {
          if (n < retries) {
            attempt(n + 1)
          } else {
            reject(err)
          }
        })
    }
    attempt(1)
  })
}

/**
 * 展示激励广告（show 失败时自动 load + 重试）
 * @param {Object} ad 广告实例
 * @param {string} failMsg 全部失败后的提示语
 * @param {Function} onFail 失败回调（重置状态等）
 */
export const showRewardedAd = (ad, failMsg, onFail) => {
  ad.show().catch(() => {
    loadAdWithRetry(ad)
      .then(() => ad.show())
      .catch(() => {
        wx.$toast(failMsg)
        onFail && onFail()
      })
  })
}
