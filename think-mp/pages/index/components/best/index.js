import { storeComponentBehavior } from '../../../../behaviors/index'
import { dishBestApi } from '../../../../apis/index'
import { DEFAULT_DISH_COVER } from '../../../../common/constants/index'

Component({
  behaviors: [storeComponentBehavior({ fields: ['kitchen'] })],
  data: {
    DEFAULT_DISH_COVER,
    list: []
  },
  observers: {
    'kitchen.active'() {
      this.getData()
    }
  },
  pageLifetimes: {
    show() {
      this.getData()
    }
  },
  methods: {
    async getData() {
      const r = await dishBestApi()
      if (r.data) {
        this.setData({ list: r.data })
      }
    },
    onTapItem(e) {
      this.triggerEvent('itemTap', { item: e.currentTarget.dataset.item })
    }
  }
})
