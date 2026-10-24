import { storePageBehavior, paginationBehavior } from '../../behaviors/index'
import { decisionPagePlazaApi, decisionRandomPlazaApi, reviewSubmitApi } from '../../apis/index'
import { ROUTE, decisionDetailUrl } from '../../common/constants/index'

Page({
  behaviors: [storePageBehavior, paginationBehavior('getData')],
  data: {
    ROUTE,
    loading: true,
    // 众思广场显式锁定每页 10 条（覆写共享 paginationBehavior 默认值，仅限本页）
    page: {
      current: 0,
      size: 10,
      total: 0,
    },
    // 填写评议表单弹窗（review-popup 底部滑出）：待评议决策单ID、选项数组、预选倾向下标
    showReview: false,
    reviewId: null,
    reviewOptions: [],
    reviewPreselect: -1,
    // 「去看看」内容浏览弹窗（decision-view-popup 居中）：展示随机决策内容+选项，点选项再弹评议表单
    showView: false,
    viewItem: {},
    viewOptions: [],
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
    const { option, content } = e.detail
    await reviewSubmitApi({ decisionId: this.data.reviewId, option, content })
    this.setData({ showReview: false, showView: false })
    wx.$toast('评议已提交')
    this.reloadPageData()
  },

  // 随机查看一条公开决策（换一面镜子照照别人）：先弹「去看看」内容弹窗，点其选项再弹评议表单
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
  onViewClose() {
    this.setData({ showView: false })
  },

  onCreateTap() {
    wx.$navigateTo(ROUTE.DECISION_CREATE)
  },
})
