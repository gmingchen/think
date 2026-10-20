import { storePageBehavior, sharePageBehavior } from '../../behaviors/index'
import { cookOrderTodayApi, cookOrderAcceptApi, cookOrderServeApi, cookOrderCancelApi } from '../../apis/index'
import { DEFAULT_DISH_COVER } from '../../common/constants/index'

const PERIOD_MAP = { 1: '早餐', 2: '午餐', 3: '晚餐', 4: '宵夜' }
const PERIOD_EMOJI = { 1: '☀️', 2: '🌤️', 3: '🌙', 4: '🌃' }
const STATUS_MAP = { 1: '待接单', 2: '备菜中', 3: '已上菜', 4: '已评价', 5: '已取消' }

Page({
  behaviors: [storePageBehavior, sharePageBehavior],
  data: {
    DEFAULT_DISH_COVER,
    loading: true,
    isChef: false,
    showKitchenSwitch: false,
    blocks: [],
    pendingAction: null,
  },
  onLoad() {
    this.bindStore({ fields: ['user', 'kitchen'], actions: ['updateKitchen'] })
  },
  async onShow() {
    await wx.$onKitchenLoaded
    await this._loadData()
  },

  // 当前用户是否为厨师长/管厨
  get isChef() {
    const role = this.data.kitchen?.active?.role
    return role === 1 || role === 2
  },

  // ── 厨房切换 ──
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
    await this._loadData()
  },

  async _loadData() {
    this.setData({ loading: true })
    try {
      const r = await cookOrderTodayApi()
      const userId = this.data.user?.id
      const isChef = this.isChef
      let orders = (r.data || []).map(item => ({
        id: item.id,
        timePeriod: item.timePeriod,
        ordererId: item.ordererId,
        ordererName: item.ordererName || '',
        isMine: item.ordererId === userId,
        dishes: (item.dishes || []).map(d => ({
          id: d.dishId,
          cover: d.dishCover || '',
          name: d.dishName || '',
          quantity: d.quantity || 1,
        })),
        remark: item.remark || '',
        status: item.status,
        statusLabel: STATUS_MAP[item.status] || '',
        rating: item.rating || 0,
        isPending: item.status === 1,
        isAccepted: item.status === 2,
        isServed: item.status === 3,
        isReviewed: item.status === 4,
      }))
      // 食客视角：只显示我点过菜的时段，该时段内显示所有人的订单
      if (!isChef) {
        const myPeriods = [...new Set(orders.filter(o => o.isMine).map(o => o.timePeriod))]
        orders = orders.filter(o => myPeriods.includes(o.timePeriod))
      }
      // 时段 → 人 → 订单，空块/空人不显示
      const blocks = [1, 2, 3, 4].map(period => {
        const periodOrders = orders.filter(o => o.timePeriod === period)
        const personMap = {}
        periodOrders.forEach(o => {
          const key = o.ordererId
          if (!personMap[key]) {
            personMap[key] = {
              ordererId: key,
              name: o.ordererName,
              isMine: o.isMine,
              orders: [],
            }
          }
          personMap[key].orders.push(o)
        })
        return {
          period,
          label: `${PERIOD_EMOJI[period]} ${PERIOD_MAP[period]}`,
          persons: Object.values(personMap),
        }
      }).filter(b => b.persons.length > 0)
      this.setData({ isChef, blocks, loading: false })
    } catch (e) {
      this.setData({ loading: false })
    }
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
    await this._loadData()
  },

  // ── 评价弹窗 ──
  openComment(e) {
    const { id } = e.currentTarget.dataset
    this.selectComponent('#reviewPopup').open(id)
  },

  // ── 评价成功回调 ──
  async onReviewSuccess() {
    await this._loadData()
  },

  // ── 菜谱详情弹窗 ──
  onDishTap(e) {
    const { id } = e.currentTarget.dataset
    this.selectComponent('#dishPopup').open(id, false)
  },

  // ── 工具方法 ──
  _getCurrentPeriod() {
    const hour = new Date().getHours()
    if (hour >= 5 && hour < 10) return 1
    if (hour >= 10 && hour < 15) return 2
    if (hour >= 15 && hour < 22) return 3
    return 4
  },
})
