import { action } from 'mobx-miniprogram'

export default {
  select: {
    stores: [],
  },
  updateStores: action(function(stores) {
    this.select.stores = stores
  }),
  clearStores: action(function() {
    this.select.stores = []
  })
}
