import { storePageBehavior, sharePageBehavior } from '../../../../behaviors/index'
import { cookOrderInfoApi, cookOrderCancelApi, cookOrderAcceptApi, cookOrderServeApi } from '../../../../apis/index'
import { COOK_ORDER_STATUS_ENUM } from '../../../../common/enums/index'
import { DEFAULT_DISH_COVER, AD_UNIT_COOKORDER_DETAIL_CUSTOM } from '../../../../common/constants/index'
import dayjs from 'dayjs'

const PERIOD_MAP = { 1: '早餐', 2: '午餐', 3: '晚餐', 4: '宵夜' }
const STATUS_LABEL = { 1: '待接单', 2: '备菜中', 3: '已上菜', 4: '已评价', 5: '已取消' }

Page({
  behaviors: [storePageBehavior, sharePageBehavior],

  data: {
    DEFAULT_DISH_COVER,
    AD_UNIT_COOKORDER_DETAIL_CUSTOM,
    loading: true,
    order: null,
    isChef: false,
    pendingAction: null,
  },

  async onLoad(options) {
    this.bindStore({ fields: ['user', 'kitchen'] })
    await wx.$onKitchenLoaded
    wx.nextTick(() => {
      this._updateChefFlag()
      if (options.id) {
        this._loadOrder(options.id)
      }
    })
  },

  async _loadOrder(id) {
    try {
      const r = await cookOrderInfoApi({ id })
      const d = r.data
      if (!d) return
      const userId = this.data.user?.id
      this.setData({
        loading: false,
        order: {
          ...d,
          periodLabel: PERIOD_MAP[d.timePeriod] || '',
          statusLabel: STATUS_LABEL[d.status] || '',
          dateLabel: this._formatDate(d.date),
          createdAtLabel: this._formatTime(d.createdAt),
          servedAtLabel: this._formatTime(d.servedAt),
          reviewedAtLabel: this._formatTime(d.reviewedAt),
          isMine: d.ordererId === userId,
          isPending: d.status === COOK_ORDER_STATUS_ENUM.ORDERED,
          isAccepted: d.status === COOK_ORDER_STATUS_ENUM.ACCEPTED,
          isServed: d.status === COOK_ORDER_STATUS_ENUM.SERVED,
          isReviewed: d.status === COOK_ORDER_STATUS_ENUM.REVIEWED,
          isCancelled: d.status === COOK_ORDER_STATUS_ENUM.CANCELLED,
        },
      })
    } catch (e) {
      wx.$print('加载订单详情失败', e)
      this.setData({ loading: false })
    }
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

  _formatTime(time) {
    if (!time) return ''
    return dayjs(time).format('M月D日 HH:mm')
  },

  _updateChefFlag() {
    const role = this.data.kitchen?.active?.role
    this.setData({ isChef: role === 1 || role === 2 })
  },

  // ── 接单 ──
  onAccept() {
    const { order } = this.data
    this.setData({ pendingAction: { type: 'accept', id: order.id } })
    this.selectComponent('#actionConfirmPopup').open({
      title: '确认接单',
      content: '确认接下这个订单？',
      confirmText: '接单',
    })
  },

  // ── 上菜 ──
  onServe() {
    const { order } = this.data
    this.setData({ pendingAction: { type: 'serve', id: order.id } })
    this.selectComponent('#actionConfirmPopup').open({
      title: '确认上菜',
      content: '确认这道菜已上菜？',
      confirmText: '上菜',
    })
  },

  // ── 取消 ──
  onCancel() {
    const { order } = this.data
    this.setData({ pendingAction: { type: 'cancel', id: order.id } })
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
    this._loadOrder(id)
  },

  onReview() {
    const { order } = this.data
    this.selectComponent('#reviewPopup').open(order.id)
  },

  // 采购清单：当前订单为数据源，份数由后端按订单菜品数量取；kitchenId 由清单页取全局当前厨房
  onGoShopping() {
    const { order } = this.data
    wx.$navigateTo(`/subpackages/dish/pages/shopping/index?cookOrderIds=${order.id}&source=orderDetail`)
  },

  async onReviewSuccess() {
    this._loadOrder(this.data.order.id)
  },
})
