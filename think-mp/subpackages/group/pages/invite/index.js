import { storePageBehavior } from '../../../../behaviors/index'
import { grpInviteInfoApi, grpJoinApi } from '../../../../apis/index'
import { ROUTE, groupDetailUrl } from '../../../../common/constants/index'

// 邀请落地页三态：loading（拉取中）/ joinable（待加入）/ success（已入群）/ expired（失效）
Page({
  behaviors: [storePageBehavior],
  data: {
    code: '',
    state: 'loading',
    info: {},
    joining: false,
  },

  async onLoad(options) {
    this.bindStore({ fields: ['user', 'system'] })
    this.setData({ code: options.code || '' })
    await wx.$onLogined
    this._loadInfo()
  },

  // 凭邀请码拉取群/邀请人概览；后端 joinable=false（码失效/过期/群解散）即进失效态
  async _loadInfo() {
    const { code } = this.data
    if (!code) {
      this.setData({ state: 'expired' })
      return
    }
    try {
      const r = await grpInviteInfoApi({ code })
      const info = r.data || {}
      this.setData({ info, state: info.joinable ? 'joinable' : 'expired' })
    } catch (e) {
      this.setData({ state: 'expired' })
    }
  },

  // 加入群组 → 成功后原地切「已入群」态（错误提示由全局 service 统一 toast）
  async onJoinTap() {
    if (this.data.joining) return
    this.setData({ joining: true })
    try {
      await grpJoinApi({ code: this.data.code })
      this.setData({ state: 'success' })
    } finally {
      this.setData({ joining: false })
    }
  },
  // 成功态：进入群详情查阅在评决策
  onEnterGroup() {
    const { grpId } = this.data.info
    if (!grpId) return
    wx.$navigateTo(groupDetailUrl(grpId))
  },
  // 随便看看（跳众思广场）
  onBrowse() {
    wx.switchTab({ url: ROUTE.TAB_SQUARE })
  },
  // 失效态：去众思广场看看
  onGoSquare() {
    wx.switchTab({ url: ROUTE.TAB_SQUARE })
  },
})
