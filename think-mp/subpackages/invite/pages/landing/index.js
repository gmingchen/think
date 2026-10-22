import { storePageBehavior } from '../../../../behaviors/index'
import { grpJoinApi } from '../../../../apis/index'
import { ROUTE } from '../../../../common/constants/index'

Page({
  behaviors: [storePageBehavior],
  data: {
    code: '',
    joining: false,
  },

  onLoad(options) {
    this.bindStore({ fields: ['user', 'system'] })
    this.setData({ code: options.code || '' })
  },

  async onJoinTap() {
    const { code, joining } = this.data
    if (joining) return
    if (!code) return wx.$toast('邀请码缺失')
    this.setData({ joining: true })
    try {
      await wx.$onLogined
      await grpJoinApi({ code })
      wx.$toast('已加入群组')
      wx.switchTab({ url: ROUTE.TAB_GROUP })
    } finally {
      this.setData({ joining: false })
    }
  },

  onBackHome() {
    wx.switchTab({ url: ROUTE.TAB_INDEX })
  },
})
