import { storePageBehavior, paginationBehavior, sharePageBehavior } from '../../../../behaviors/index'
import { cookOrderPageApi, cookOrderCancelApi, cookOrderAcceptApi, cookOrderServeApi } from '../../../../apis/index'
import { COOK_ORDER_STATUS_ENUM } from '../../../../common/enums/index'
import { DEFAULT_DISH_COVER, AD_UNIT_COOKORDER_LIST_CUSTOM } from '../../../../common/constants/index'
import dayjs from 'dayjs'

const PERIOD_MAP = { 1: '早餐', 2: '午餐', 3: '晚餐', 4: '宵夜' }
const STATUS_LABEL = { 1: '待接单', 2: '备菜中', 3: '已上菜', 4: '已评价', 5: '已取消' }

Page({
  behaviors: [storePageBehavior, paginationBehavior('getData'), sharePageBehavior],
  data: {
    DEFAULT_DISH_COVER,
    AD_UNIT_COOKORDER_LIST_CUSTOM,
    activeTab: 1,
    loading: true,
    isChef: false,
    pendingAction: null,
  },

  async onLoad() {
    this.bindStore({ fields: ['user', 'kitchen'] })
    await wx.$onKitchenLoaded
    wx.nextTick(() => {
      this._updateChefFlag()
      this.reloadPageData()
    })
  },

  async getData(params) {
    const reqParams = { ...params }
    if (this.data.activeTab === 1) {
      reqParams.type = 1
    }
    const r = await cookOrderPageApi(reqParams)
    const userId = this.data.user?.id
    const list = (r.data?.list || []).map(item => ({
      ...item,
      periodLabel: PERIOD_MAP[item.timePeriod] || '',
      statusLabel: STATUS_LABEL[item.status] || '',
      dateLabel: this._formatDate(item.date),
      isMine: item.ordererId === userId,
      isPending: item.status === COOK_ORDER_STATUS_ENUM.ORDERED,
      isAccepted: item.status === COOK_ORDER_STATUS_ENUM.ACCEPTED,
      isServed: item.status === COOK_ORDER_STATUS_ENUM.SERVED,
      isReviewed: item.status === COOK_ORDER_STATUS_ENUM.REVIEWED,
      isCancelled: item.status === COOK_ORDER_STATUS_ENUM.CANCELLED,
    }))
    this.pageDataHandler({ ...r.data, list })
    this.setData({ loading: false })
  },

  _formatDate(date) {
    const d = dayjs(date)
    const today = dayjs().startOf('day')
    const diff = today.diff(d.startOf('day'), 'day')
    if (diff === 0) return '今天'
    if (diff === 1) return '昨天'
    if (diff === 2) return '前天'
    return d.format('M月D日')
  },

  _updateChefFlag() {
    const role = this.data.kitchen?.active?.role
    this.setData({ isChef: role === 1 || role === 2 })
  },

  onSwitchTab(e) {
    const tab = Number(e.currentTarget.dataset.tab)
    if (tab === this.data.activeTab) return
    this.setData({ activeTab: tab, loading: true })
    this.reloadPageData()
  },

  // ── 接单 ──
  onAccept(e) {
    const { id } = e.currentTarget.dataset
    this.setData({ pendingAction: { type: 'accept', id } })
    this.selectComponent('#actionConfirmPopup').open({
      title: '确认接单',
      content: '确认接下这个订单？',
      confirmText: '接单',
    })
  },

  // ── 上菜 ──
  onServe(e) {
    const { id } = e.currentTarget.dataset
    this.setData({ pendingAction: { type: 'serve', id } })
    this.selectComponent('#actionConfirmPopup').open({
      title: '确认上菜',
      content: '确认这道菜已上菜？',
      confirmText: '上菜',
    })
  },

  // ── 取消 ──
  onCancel(e) {
    const { id } = e.currentTarget.dataset
    this.setData({ pendingAction: { type: 'cancel', id } })
    this.selectComponent('#actionConfirmPopup').open({
      title: '取消订单',
      content: '确认取消这个订单？',
      confirmText: '确认',
    })
  },

  // ── 确认弹窗回调 ──
  async onActionConfirm() {
    const { type, id } = this.data.pendingAction || {}
    if (!type) return
    if (type === 'accept') {
      await cookOrderAcceptApi({ id })
      wx.$toast('已接单 ✓')
    } else if (type === 'serve') {
      await cookOrderServeApi({ id })
      wx.$toast('已上菜 🎉')
    } else if (type === 'cancel') {
      await cookOrderCancelApi({ id })
      wx.$toast('已取消')
    }
    this.setData({ pendingAction: null })
    this.reloadPageData()
  },

  onReview(e) {
    const { id } = e.currentTarget.dataset
    this.selectComponent('#reviewPopup').open(id)
  },

  // 采购清单：当前行为数据源，份数由后端按订单菜品数量取；kitchenId 由清单页取全局当前厨房
  onGoShopping(e) {
    const { id } = e.currentTarget.dataset
    wx.$navigateTo(`/subpackages/dish/pages/shopping/index?cookOrderIds=${id}&source=orderList`)
  },

  onViewDetail(e) {
    const { id } = e.currentTarget.dataset
    wx.$navigateTo(`/subpackages/cookorder/pages/detail/index?id=${id}`)
  },

  async onReviewSuccess() {
    this.reloadPageData()
  },
})
