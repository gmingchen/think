import { action } from 'mobx-miniprogram'

export default {
  cart: {
    list: [], // [{ dishId, name, photo, quantity }]
    quantity: 0, // 总数量
  },
  _calcQuantity() {
    this.cart.quantity = this.cart.list.reduce((sum, item) => sum + item.quantity, 0)
  },
  addToCart: action(function(dish) {
    const existing = this.cart.list.find(item => item.dishId === dish.id)
    if (existing) {
      existing.quantity += 1
    } else {
      this.cart.list.push({
        dishId: dish.id,
        name: dish.name,
        photo: dish.photo,
        quantity: 1,
      })
    }
    this._calcQuantity()
    // 触发响应式更新
    this.cart = { ...this.cart }
  }),
  removeFromCart: action(function(dishId) {
    const idx = this.cart.list.findIndex(item => item.dishId === dishId)
    if (idx > -1) {
      const item = this.cart.list[idx]
      if (item.quantity > 1) {
        item.quantity -= 1
      } else {
        this.cart.list.splice(idx, 1)
      }
      this._calcQuantity()
      this.cart = { ...this.cart }
    }
  }),
  removeDishesFromCart: action(function(dishIds) {
    this.cart.list = this.cart.list.filter(item => !dishIds.includes(item.dishId))
    this._calcQuantity()
    this.cart = { ...this.cart }
  }),
  clearCart: action(function() {
    this.cart.list = []
    this.cart.quantity = 0
    this.cart = { ...this.cart }
  }),
}
