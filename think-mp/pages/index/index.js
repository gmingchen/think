import { storePageBehavior, sharePageBehavior } from '../../behaviors/index'
import { kitchenStatsApi } from '../../apis/index'
import { KITCHEN_ROLE_ENUM } from '../../common/enums/index'
import {
  AD_UNIT_INDEX_BANNER,
  AD_UNIT_INDEX_NATIVE,
  OFFICIAL_TOPIC,
  OFFICIAL_LIMIT,
  OFFICIAL_BG_COLOR,
  OFFICIAL_PLACEHOLDER,
  OFFICIAL_RECOMMEND_PATH,
  OFFICIAL_RECOMMEND_TITLE,
} from '../../common/constants/index'

Page({
  behaviors: [storePageBehavior, sharePageBehavior],
  data: {
    loading: true,
    showKitchenSwitch: false,
    showInvite: false,
    KITCHEN_ROLE_ENUM,
    AD_UNIT_INDEX_BANNER,
    AD_UNIT_INDEX_NATIVE,
    invite: {
      kitchenId: null,
      code: '',
    },
    useGuide: false,
    stats: [],
    canShowOfficialPublish: false,
    OFFICIAL_TOPIC,
    OFFICIAL_LIMIT,
    OFFICIAL_BG_COLOR,
    OFFICIAL_PLACEHOLDER,
    OFFICIAL_RECOMMEND_PATH,
    OFFICIAL_RECOMMEND_TITLE,
  },

  async onLoad(options) {
    this.setData({ canShowOfficialPublish: wx.canIUse('official-account-publish') })
    this.bindStore({ fields: ['user', 'system', 'kitchen', 'cart'], actions: ['getKitchen', 'updateKitchen', 'addToCart'] })
    // 检查是否有厨房邀请参数
    await wx.$onKitchenLoaded
    if (options.kitchenId && options.code) {
      this.setData({
        invite: {
          kitchenId: Number(options.kitchenId),
          code: options.code,
        },
      })
    } else {
      this.setData({ useGuide: true })
    }
  },

  async onShow() {
    await wx.$onKitchenLoaded
    this.setData({ showInvite: this.data.kitchen?.active?.role !== KITCHEN_ROLE_ENUM.GUEST })
    await this._loadStats()
    this.setData({ loading: false })
  },

  // ── 功能入口图 ──
  onPickTap() {
    wx.switchTab({ url: '/pages/recipe/index' })
  },
  onDrawTap() {
    wx.$navigateTo('/subpackages/dish/pages/draw/index')
  },
  onAiRecommendTap() {
    wx.$navigateTo('/subpackages/dish/pages/ai-recommend/index')
  },

  // ── 厨房切换弹窗 ──
  onOpenKitchenSwitch() {
    this.setData({ showKitchenSwitch: true })
  },
  onCloseKitchenSwitch() {
    this.setData({ showKitchenSwitch: false })
  },
  async onSwitchKitchenConfirm(e) {
    const { item } = e.detail
    this.onCloseKitchenSwitch()
    await this.updateKitchen(item)
    await this._loadStats()
  },

  // ── 统计 ──
  async _loadStats() {
    const r = await kitchenStatsApi()
    if (r.data) {
      this.setData({ stats: r.data })
    }
  },

  // ── 厨房邀请弹窗 ──
  async onInviteAccept() {
    await this._loadStats()
  },
  onInviteReject() {
    this.setData({ useGuide: true })
  },
  onGuideConfirm() {
    this._loadStats()
  },

  // ── 菜谱弹窗 ──
  onAddDish() {
    const dish = this.selectComponent('#dishPopup').data.dish
    if (dish) {
      this.addToCart({ id: dish.id, name: dish.name, photo: dish.photo })
      wx.vibrateShort({ type: 'medium' })
      wx.$toast('已加入点餐')
    }
    this.selectComponent('#dishPopup').close()
  },
  onViewDishDetail(e) {
    const { id } = e.detail
    this.selectComponent('#dishPopup').close()
    if (id) {
      wx.$navigateTo(`/subpackages/dish/pages/detail/index?id=${id}`)
    }
  },

  // ── 列表跳转 ──
  onLatestDishTap(e) {
    const { id } = e.detail
    this.selectComponent('#dishPopup').open(id, false)
  },
  onBestItemTap(e) {
    const { item } = e.detail
    this.selectComponent('#dishPopup').open(item.id)
  },
})
