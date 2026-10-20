import { storeComponentBehavior } from '../../behaviors/index'

Component({
  behaviors: [storeComponentBehavior({ fields: ['kitchen'] })],
  properties: {
    visible: {
      type: Boolean,
      value: false
    }
  },
  methods: {
    onClose() {
      this.triggerEvent('close')
    },
    onSelect(e) {
      const { item } = e.currentTarget.dataset
      if (item.id !== this.data.kitchen.active?.id) {
        this.triggerEvent('switch', { item })
      }
    }
  }
})
