import { storePageBehavior, sharePageBehavior } from '../../behaviors/index'
import { decisionStatsMineApi } from '../../apis/index'
import { ROUTE, decisionListUrl } from '../../common/constants/index'
import { DECISION_SCENE_ENUM, DECISION_MINE_TAB_ENUM, DECISION_STATUS_ENUM } from '../../common/enums/index'
import dayjs from 'dayjs'

const DEFAULT_STATS = { createdCount: 0, reviewedCount: 0, receivedCount: 0 }

Page({
  behaviors: [storePageBehavior, sharePageBehavior],
  data: {
    ROUTE,
    stats: { ...DEFAULT_STATS },
    registerDays: '',
  },

  onLoad() {
    this.bindStore({
      fields: {
        user: 'user',
        system: 'system',
        tabbar: 'tabbar',
        // 加入天数依赖 store.user：wx.$onLogined 在 token 到达时就 resolve，
        // 而用户信息是其后 getUser() 异步回填，一次性读会拿到空值。
        // 声明为计算字段，由 mobx reaction 在 user 落地时自动重算，首次进入即可显示
        registerDays: store => this._calcRegisterDays(store.user && store.user.createdAt),
      },
    })
  },

  async onShow() {
    await wx.$onLogined
    this.getData()
  },

  // 注册时长（天）：以注册当天为第 1 天，无 createdAt 或异常则不展示
  _calcRegisterDays(createdAt) {
    if (!createdAt) return ''
    const created = dayjs(createdAt)
    if (!created.isValid()) return ''
    const days = dayjs().diff(created, 'day') + 1
    return days > 0 ? days : ''
  },

  async getData() {
    const r = await decisionStatsMineApi()
    if (r.data) {
      this.setData({ stats: r.data })
    }
  },

  onProfileTap() {
    wx.$navigateTo(ROUTE.USER_EDIT)
  },
  onDecisionTap() {
    wx.$navigateTo(decisionListUrl(DECISION_SCENE_ENUM.MINE, `&tab=${ DECISION_MINE_TAB_ENUM.CREATED }`))
  },
  onReviewTap() {
    wx.$navigateTo(decisionListUrl(DECISION_SCENE_ENUM.MINE, `&tab=${ DECISION_MINE_TAB_ENUM.REVIEWED }`))
  },
  onDraftTap() {
    wx.$navigateTo(decisionListUrl(DECISION_SCENE_ENUM.MINE, `&tab=${ DECISION_MINE_TAB_ENUM.CREATED }&status=${ DECISION_STATUS_ENUM.DRAFT }`))
  },
  onRecapTap() {
    wx.$navigateTo(decisionListUrl(DECISION_SCENE_ENUM.MINE, `&tab=${ DECISION_MINE_TAB_ENUM.CREATED }&status=${ DECISION_STATUS_ENUM.ENDED }`))
  },
  onAboutTap() {
    wx.$navigateTo(ROUTE.USER_ABOUT)
  },
})
