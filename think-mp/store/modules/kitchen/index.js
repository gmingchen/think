import { action } from 'mobx-miniprogram'
import { kitchenListApi } from '../../../apis/index'

export default {
  kitchen: {
    active: null,
    list: [],
  },
  getKitchen: action(async function() {
    const r = await kitchenListApi()
    if (r.data) {
      this.kitchen = {
        ...this.kitchen,
        list: r.data,
      }
      const lastId = this.user?.lastKitchenId
      const active = this.kitchen.list.find(item => item.id === lastId) || this.kitchen.list[0]
      this.updateKitchen(active)
    }
    wx.$kitchenLoaded()
  }),
  updateKitchen: action(async function(active) {
    const changed = this.kitchen.active?.id !== active?.id
    this.kitchen = { 
      ...this.kitchen,
      active,
    }
    if (changed) {
      this.clearCart()
      this.updateLastKitchenId(active.id)
    }
  })
}
