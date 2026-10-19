import { storeComponentBehavior } from '../../behaviors/index'

Component({
  behaviors: [storeComponentBehavior({ fields: ['kitchen'] })],

  methods: {
    onTap() {
      this.triggerEvent('tap')
    },
  },
})
