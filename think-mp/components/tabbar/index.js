import { storeComponentBehavior } from '../../behaviors/index'
import { TABBAR_ENUM } from '../../common/enums/index'

Component({
  behaviors: [
    storeComponentBehavior({
      fields: ['tabbar', 'kitchen'],
      actions: ['updateActive', 'switchTabbar', 'updateRoleTabs']
    })
  ],
  observers: {
    'kitchen.active'(active) {
      if (!active) return
      this.updateRoleTabs(active.role)
    }
  },
  pageLifetimes: {
    show: function () {
      const pages = getCurrentPages();
      const page = pages[pages.length - 1]
      const url = `/${ page.route }`
      this.updateActive({ url })
    }
  },
  methods: {
    onTap(event) {
      const { value, url } = event.currentTarget.dataset.row
      this.switchTabbar({ value, url })
    }
  }
})
