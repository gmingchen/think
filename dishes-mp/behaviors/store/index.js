import { createStoreBindings, storeBindingsBehavior } from 'mobx-miniprogram-bindings'
import { store } from '../../store/index'

export const storePageBehavior = Behavior({
  methods: {
    bindStore(options) {
      this.storeBindings = createStoreBindings(this, {
        store, ...options
      })
    }
  },
  onUnload: function () {
    this.storeBindings.destroyStoreBindings();
  },
})

export const storeComponentBehavior = (options) => Behavior({
  behaviors: [storeBindingsBehavior],
  storeBindings: { store, ...options },
})
