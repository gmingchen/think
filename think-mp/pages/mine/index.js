import { storePageBehavior } from '../../behaviors/index'
import { decisionStatsMineApi } from '../../apis/index'
import { ROUTE, decisionListUrl } from '../../common/constants/index'
import { DECISION_SCENE_ENUM } from '../../common/enums/index'

const DEFAULT_STATS = { createdCount: 0, reviewedCount: 0, receivedCount: 0 }

Page({
  behaviors: [storePageBehavior],
  data: {
    ROUTE,
    stats: { ...DEFAULT_STATS },
  },

  async onLoad() {
    this.bindStore({ fields: ['user', 'system', 'tabbar'] })
  },

  async onShow() {
    await wx.$onLogined
    this.getData()
  },

  async getData() {
    const r = await decisionStatsMineApi()
    if (r.data) {
      this.setData({ stats: r.data })
    }
  },

  onCreateTap() {
    wx.$navigateTo(ROUTE.DECISION_CREATE)
  },
  onProfileTap() {
    wx.$navigateTo(ROUTE.USER_EDIT)
  },
  onDecisionTap() {
    wx.$navigateTo(decisionListUrl(DECISION_SCENE_ENUM.MINE))
  },
  // 群组为 Tab 页，P2 完成 tabBar 切换后生效
  onGroupTap() {
    wx.switchTab({ url: ROUTE.TAB_GROUP })
  },
  onFeedbackTap() {
    wx.$navigateTo(ROUTE.FEEDBACK)
  },
  onAboutTap() {
    wx.$navigateTo(ROUTE.USER_ABOUT)
  },
})
