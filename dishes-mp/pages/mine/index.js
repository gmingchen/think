import { storePageBehavior, sharePageBehavior } from '../../behaviors/index'
import { showRewardedAd } from '../../utils/index'
import { AD_UNIT_MINE_BANNER, AD_UNIT_MINE_REWARD } from '../../common/constants/index'

// 激励视频单例（createRewardedVideoAd 同一 adUnitId 返回单例，模块级缓存避免监听重复注册）
let rewardAd = null
const getRewardAd = () => {
  if (!rewardAd && wx.createRewardedVideoAd) {
    rewardAd = wx.createRewardedVideoAd({ adUnitId: AD_UNIT_MINE_REWARD })
    rewardAd.onClose((res) => {
      if (res && res.isEnded) {
        wx.$toast('感谢支持 ❤️')
      }
    })
  } 
  return rewardAd
}

Page({
  behaviors: [storePageBehavior, sharePageBehavior],
  data: {
    loading: true,
    AD_UNIT_MINE_BANNER,
  },
  async onLoad() {
    await wx.$onLogined
    this.bindStore({ fields: ['auth', 'kitchen'] })
    // 等待用户数据加载
    setTimeout(() => {
      this.setData({ loading: false })
    }, 300)
  },

  // 看广告支持一下（用户主动行为，不强制）
  onWatchAdTap() {
    const ad = getRewardAd()
    if (!ad) {
      wx.$toast('当前环境暂不支持广告')
      return
    }
    showRewardedAd(ad, '广告加载失败，请稍后再试')
  },
})
