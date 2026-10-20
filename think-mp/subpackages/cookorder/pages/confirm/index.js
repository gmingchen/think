import { storePageBehavior, sharePageBehavior } from '../../../../behaviors/index'
import { cookOrderCreateApi } from '../../../../apis/index'
import { DEFAULT_DISH_COVER, AD_UNIT_CONFIRM_BANNER } from '../../../../common/constants/index'

const MEAL_MAP = { '早餐': 1, '午餐': 2, '晚餐': 3, '宵夜': 4 }
const MEAL_NAMES = ['早餐', '午餐', '晚餐', '宵夜']

Page({
  behaviors: [storePageBehavior, sharePageBehavior],

  data: {
    DEFAULT_DISH_COVER,
    AD_UNIT_CONFIRM_BANNER,
    // 日期
    selectedDate: '',
    dateLabel: '',
    minDate: '',
    showCalendar: false,
    // 时段
    mealList: MEAL_NAMES,
    activeMeal: '',
    disabledMealMap: {},
    // 备注
    remark: '',
    // 提交中
    submitting: false,
  },

  onLoad(options) {
    this.bindStore({ fields: ['cart', 'kitchen'], actions: ['clearCart', 'removeFromCart', 'addToCart'] })
    this._initFromOptions(options)
  },

  // ── 初始化 ──
  _initFromOptions(options) {
    const { date, meal } = options
    const today = this._formatDate(new Date())
    const selectedDate = date || today
    const [y, m, d] = selectedDate.split('-').map(Number)
    const isToday = selectedDate === today
    const activeMeal = meal || this._getCurrentMeal()
    const disabledMealMap = isToday ? this._getDisabledMealMap() : {}

    this.setData({
      selectedDate,
      dateLabel: `${m}月${d}日`,
      minDate: today,
      activeMeal,
      disabledMealMap,
    })
  },

  // ── 日期 ──
  _formatDate(d) {
    const y = d.getFullYear()
    const m = String(d.getMonth() + 1).padStart(2, '0')
    const dd = String(d.getDate()).padStart(2, '0')
    return `${y}-${m}-${dd}`
  },

  _getCurrentMeal() {
    const h = new Date().getHours()
    if (h >= 5 && h < 10) return '早餐'
    if (h >= 10 && h < 15) return '午餐'
    if (h >= 15 && h < 22) return '晚餐'
    return '宵夜'
  },

  _getDisabledMealMap() {
    const h = new Date().getHours()
    const map = {}
    if (h >= 22 || h < 5) { map['早餐'] = true; map['午餐'] = true; map['晚餐'] = true }
    else if (h >= 15) { map['早餐'] = true; map['午餐'] = true }
    else if (h >= 10) { map['早餐'] = true }
    return map
  },

  onOpenCalendar() {
    this.setData({ showCalendar: true })
  },

  onCloseCalendar() {
    this.setData({ showCalendar: false })
  },

  onDateConfirm(e) {
    const { date } = e.detail
    const [y, m, d] = date.split('-').map(Number)
    const today = this.data.minDate
    const isToday = date === today
    this.setData({
      selectedDate: date,
      dateLabel: `${m}月${d}日`,
      showCalendar: false,
      isToday,
      disabledMealMap: isToday ? this._getDisabledMealMap() : {},
    })
  },

  // ── 时段 ──
  onSelectMeal(e) {
    const meal = e.currentTarget.dataset.meal
    if (this.data.disabledMealMap[meal]) return
    this.setData({ activeMeal: meal })
  },

  // ── 备注 ──
  onRemarkInput(e) {
    this.setData({ remark: e.detail.value })
  },

  // ── 购物车操作 ──
  onStepperChange(e) {
    const { id } = e.currentTarget.dataset
    const { value } = e.detail
    const item = this.data.cart.list.find(i => i.dishId === id)
    if (!item) return
    if (value > item.quantity) {
      this.addToCart({ id })
    } else if (value < item.quantity) {
      this.removeFromCart(id)
    }
  },

  // ── 提交 ──
  async onSubmit() {
    const { cart, selectedDate, activeMeal, remark, submitting } = this.data
    if (submitting) return
    if (!cart.list.length) {
      wx.$toast('请先选择菜谱')
      return
    }

    this.setData({ submitting: true })
    try {
      await cookOrderCreateApi({
        date: selectedDate,
        timePeriod: MEAL_MAP[activeMeal],
        remark,
        dishes: cart.list.map(item => ({
          dishId: item.dishId,
          quantity: item.quantity,
        })),
      })
      this.clearCart()
      wx.$toast('下单成功')
      setTimeout(() => {
        wx.$navigateBack()
      }, 500)
    } catch (err) {
      wx.$print('下单失败', err)
    } finally {
      this.setData({ submitting: false })
    }
  },
})
