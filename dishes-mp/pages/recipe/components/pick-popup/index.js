import { DEFAULT_DISH_COVER } from '../../../../common/constants/index'
import { pickDishes } from '../../../../utils/pick'

// 每批抽取道数（池子不足时 pickDishes 按 Math.min 自动降级）
const PICK_COUNT = 3

Component({
  options: {
    multipleSlots: true,
  },
  properties: {
    show: {
      type: Boolean,
      value: false,
    },
    dishes: {
      type: Array,
      value: [],
    },
  },

  data: {
    DEFAULT_DISH_COVER,
    pickedList: [],
    _excludeIds: [],
  },

  observers: {
    show(val) {
      if (val && this.data.dishes.length > 0) {
        this._doPick()
      }
    },
  },

  methods: {
    _doPick() {
      const { dishes } = this.data
      if (!dishes.length) return
      const picked = pickDishes(dishes, {
        count: PICK_COUNT,
        excludeIds: this.data._excludeIds,
      })
      this.setData({
        pickedList: picked,
        _excludeIds: picked.map(d => d.id),
      })
    },

    onClose() {
      this.triggerEvent('close')
    },

    onRefresh() {
      this._doPick()
      wx.vibrateShort({ type: 'medium' })
    },

    onAddSingle(e) {
      const { id } = e.currentTarget.dataset
      const dish = this.data.pickedList.find(d => d.id === id)
      if (dish) {
        this.triggerEvent('add', { id: dish.id, name: dish.name, photo: dish.photo })
      }
    },

    onAddAll() {
      const { pickedList } = this.data
      pickedList.forEach(dish => {
        this.triggerEvent('add', { id: dish.id, name: dish.name, photo: dish.photo })
      })
      this.onClose()
    },

    // 纯 UI 组件不导航：把当前抽取结果交给页面层跳采购清单，弹窗不关，返回时仍在
    onShopping() {
      const dishIds = this.data.pickedList.map(d => d.id)
      if (!dishIds.length) return
      this.triggerEvent('shopping', { dishIds })
    },
  },
})
