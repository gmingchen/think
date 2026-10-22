import { storeComponentBehavior } from '../../behaviors/index'
import { ROUTE } from '../../common/constants/index'

Component({
  behaviors: [
    storeComponentBehavior({
      fields: ['tabbar'],
      actions: ['updateActive', 'switchTabbar']
    })
  ],
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
    },
    // 中间凸起按钮：跳转创建决策单
    onCreateTap() {
      wx.$navigateTo(ROUTE.DECISION_CREATE)
    }
  }
})
