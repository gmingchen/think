import { storePageBehavior, paginationBehavior, sharePageBehavior } from '../../behaviors/index'
import { decisionPagePlazaApi, decisionRandomPlazaApi, decisionChangePlazaApi, reviewSubmitApi } from '../../apis/index'
import { ROUTE, decisionDetailUrl } from '../../common/constants/index'

Page({
  behaviors: [storePageBehavior, paginationBehavior('getData'), sharePageBehavior],
  data: {
    ROUTE,
    loading: true,
    refreshing: false,
    // 众思广场显式锁定每页 10 条（覆写共享 paginationBehavior 默认值，仅限本页）
    page: {
      current: 0,
      size: 10,
      total: 0,
    },
    // 填写评议表单弹窗（review-popup 底部滑出）：待评议决策单ID、选项数组、预选倾向下标
    showReview: false,
    // 评议提交中（防连点重复提交，同步传 review-popup 禁用按钮）
    submitting: false,
    reviewId: null,
    reviewOptions: [],
    reviewPreselect: -1,
    // 「去看看」内容浏览弹窗（decision-view-popup 居中）：展示随机决策内容+选项，点选项再弹评议表单
    showView: false,
    viewItem: {},
    viewOptions: [],
    // 举报弹层
    showReport: false,
    reportTargetId: null,
  },

  async onLoad() {
    this.bindStore({ fields: ['user', 'system', 'tabbar'] })
  },

  async onShow() {
    // 每次页面显示都重拉首页数据（首次进入 onLoad 仅 bindStore，避免重复请求）
    await wx.$onLogined
    this.reloadPageData()
  },

  async getData(params) {
    const r = await decisionPagePlazaApi(params)
    this.pageDataHandler(r.data)
    this.setData({ loading: false })
  },

  // 下拉刷新
  async onPullRefresh() {
    this.setData({ refreshing: true, list: [], 'page.current': 1 })
    await this.getPageData()
    this.setData({ refreshing: false })
  },

  onCardTap(e) {
    wx.$navigateTo(decisionDetailUrl(e.detail.id))
  },

  // 卡片选项 / 去看看内容弹窗选选项 → 打开底部评议表单，预选中的倾向已带下（myOption=点击下标）
  onReviewFromCard(e) {
    const { id, option, options } = e.detail
    this.setData({
      showReview: true,
      reviewId: id,
      reviewOptions: options || [],
      reviewPreselect: option,
    })
  },
  onReviewClose() {
    this.setData({ showReview: false })
  },
  async onReviewSubmit(e) {
    if (this.data.submitting) return
    this.setData({ submitting: true })
    const { option, content } = e.detail
    try {
      await reviewSubmitApi({ decisionId: this.data.reviewId, option, content })
      this.setData({ showReview: false, showView: false })
      wx.$toast('评议已提交')
      this.reloadPageData()
    } finally {
      this.setData({ submitting: false })
    }
  },

  // 随机查看一条公开决策（换一面镜子照照别人）：横幅「去看看」首次触发，弹「去看看」内容弹窗
  async onRandomTap() {
    const r = await decisionRandomPlazaApi()
    if (r.data && r.data.id) {
      this.setData({
        showView: true,
        viewItem: r.data,
        viewOptions: r.data.options || [],
      })
    } else {
      wx.$toast('暂无可看的公开决策')
    }
  },
  // 弹窗内「换一个」：走专用接口，后端硬保证不与上一条重复；无可换时提示
  async onChangeTap() {
    // 防抖：连点时丢弃重叠请求，避免旧响应覆盖新内容
    if (this._randomLoading) return
    this._randomLoading = true
    const excludeId = this.data.viewItem && this.data.viewItem.id
    try {
      const r = await decisionChangePlazaApi(excludeId ? { excludeId } : {})
      if (r.data && r.data.id) {
        this.setData({
          viewItem: r.data,
          viewOptions: r.data.options || [],
        })
      } else {
        wx.$toast('暂时没有更多可换的了')
      }
    } finally {
      this._randomLoading = false
    }
  },
  onViewClose() {
    this.setData({ showView: false })
  },

  onCreateTap() {
    wx.$navigateTo(ROUTE.DECISION_CREATE)
  },

  // 举报：卡片长按 / 随便看看弹窗内点击
  onReportFromCard(e) {
    this.setData({ showReport: true, reportTargetId: e.detail.id })
  },
  onReportClose() {
    this.setData({ showReport: false })
  },
})
