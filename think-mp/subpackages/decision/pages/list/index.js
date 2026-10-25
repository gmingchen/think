import { storePageBehavior, paginationBehavior } from '../../../../behaviors/index'
import {
  decisionPageMineApi,
  decisionPageReviewedApi,
  decisionPageGrpApi,
} from '../../../../apis/index'
import {
  DECISION_SCENE_ENUM,
  DECISION_MINE_TAB_ENUM,
  DECISION_STATUS_ENUM,
  decisionStatusList,
  decisionMineTabList,
  decisionGrpTabList,
} from '../../../../common/enums/index'
import { decisionDetailUrl, ROUTE } from '../../../../common/constants/index'
import dayjs from 'dayjs'

const STATUS_ALL = '' // 状态筛选「全部」哨兵（空串，避免 null 在 data 属性中渲染为 "null"）

// 列表卡倒计时（HH:mm:ss）：仅评议中且有截止时间；到点显「即将截止」，与卡片旧值相同则复用引用避免重绘
function withCountdown(list) {
  return (list || []).map(it => {
    if (it.status !== DECISION_STATUS_ENUM.REVIEWING || !it.coolDownEndAt) return it
    const diff = dayjs(it.coolDownEndAt).diff(dayjs(), 'second')
    let text = '即将截止'
    if (diff > 0) {
      const pad = n => (n < 10 ? `0${ n }` : `${ n }`)
      text = `倒计时 ${ pad(Math.floor(diff / 3600)) }:${ pad(Math.floor((diff % 3600) / 60)) }:${ pad(diff % 60) }`
    }
    return (it.countdownText === text) ? it : { ...it, countdownText: text }
  })
}

Page({
  behaviors: [storePageBehavior, paginationBehavior('getData')],
  data: {
    loading: true,
    scene: DECISION_SCENE_ENUM.MINE,
    grpId: null,
    grpName: '',
    tab: DECISION_MINE_TAB_ENUM.CREATED,
    activeStatus: STATUS_ALL,
    tabs: decisionMineTabList,
    statusChips: [{ label: '全部', value: STATUS_ALL }].concat(decisionStatusList),
    DECISION_SCENE_ENUM,
    DECISION_MINE_TAB_ENUM,
    STATUS_ALL,
  },

  async onLoad(options) {
    this.bindStore({ fields: ['user', 'system'] })
    await wx.$onLogined
    // 群场景：从群详情入口带 scene=group&grpId 进入，一级 Tab 直接按状态筛选（tab 承载状态值）
    if (options.scene === DECISION_SCENE_ENUM.GROUP) {
      this.setData({
        scene: DECISION_SCENE_ENUM.GROUP,
        grpId: options.grpId,
        // 导航标题「群名 · 决策单」（入口 encodeURIComponent 传入，缺省回退「群内决策单」）
        grpName: options.grpName ? decodeURIComponent(options.grpName) : '',
        tabs: decisionGrpTabList,
        tab: decisionGrpTabList[0].value,
      })
      this.reloadPageData()
      return
    }
    const tab = options.tab || DECISION_MINE_TAB_ENUM.CREATED
    // 首页指标卡可携 status 落地（如「完成复盘」直达已复盘筛选）；query 值为字符串，需转回数字才能与 statusChips 的数值命中对比
    let activeStatus = options.status === undefined || options.status === '' ? STATUS_ALL : Number(options.status)
    // 「我评议过的」不展示也不响应状态筛选，归零避免筛选态与实际查询条件不一致
    if (tab === DECISION_MINE_TAB_ENUM.REVIEWED) activeStatus = STATUS_ALL
    this.setData({ tab, activeStatus })
    this.reloadPageData()
  },

  // 每次回到本页重算倒计时（列表数据不重拉，仅刷新剩余时长）
  onShow() {
    if (this.data.list && this.data.list.length) {
      this.setData({ list: withCountdown(this.data.list) })
    }
  },

  async getData(params) {
    const reqParams = { ...params }
    let r
    if (this.data.scene === DECISION_SCENE_ENUM.GROUP) {
      // 群内决策单：tab 即状态值，按 grpId + status 查询
      r = await decisionPageGrpApi({ ...reqParams, grpId: this.data.grpId, status: this.data.tab })
    } else if (this.data.tab === DECISION_MINE_TAB_ENUM.REVIEWED) {
      r = await decisionPageReviewedApi(reqParams)
    } else {
      if (this.data.activeStatus !== STATUS_ALL) {
        reqParams.status = this.data.activeStatus
      }
      r = await decisionPageMineApi(reqParams)
    }
    // 统一注入列表卡倒计时（就地附加，不影响分页游标）
    if (r.data) r.data.list = withCountdown(r.data.list)
    this.pageDataHandler(r.data)
    this.setData({ loading: false })
  },

  onSwitchTab(e) {
    // 群场景 Tab 值是状态数字，需转回 Number 才能与 tab 命中对比
    const tab = this.data.scene === DECISION_SCENE_ENUM.GROUP
      ? Number(e.currentTarget.dataset.tab)
      : e.currentTarget.dataset.tab
    if (tab === this.data.tab) return
    const patch = { tab, loading: true }
    // mine 切到「我评议过的」时状态筛选无意义，重置为全部
    if (this.data.scene !== DECISION_SCENE_ENUM.GROUP && tab === DECISION_MINE_TAB_ENUM.REVIEWED) {
      patch.activeStatus = STATUS_ALL
    }
    this.setData(patch)
    this.reloadPageData()
  },

  onSwitchStatus(e) {
    const raw = e.currentTarget.dataset.value
    const value = raw === '' ? STATUS_ALL : Number(raw)
    if (value === this.data.activeStatus) return
    this.setData({ activeStatus: value, loading: true })
    this.reloadPageData()
  },

  onCardTap(e) {
    wx.$navigateTo(decisionDetailUrl(e.detail.id))
  },

  // 空态引导：发起决策单（「我发起的」场景）
  onCreateTap() {
    wx.$navigateTo(ROUTE.DECISION_CREATE)
  },

  // 空态引导：前往众思广场（tabbar 页，$navigateTo 内部转 switchTab）
  onGoSquare() {
    wx.$navigateTo(ROUTE.TAB_SQUARE)
  },
})
