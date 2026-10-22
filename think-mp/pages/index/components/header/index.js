import { storeComponentBehavior } from '../../../../behaviors/index'

Component({
  // 用户信息直接绑定全局 store（mobx 响应式，store.user 更新后自动刷新，无需等登录）
  behaviors: [storeComponentBehavior({ fields: ['user'] })],
  data: {
    greet: '',
  },
  pageLifetimes: {
    // 页面每次显示时重算问候语，保持「下午好」随时间切换（原页面 onShow 逻辑下沉自管）
    show() {
      this.setData({ greet: this._buildGreet() })
    },
  },
  methods: {
    // 按当前小时生成问候语（UI §3.1「下午好」随时间切换）
    _buildGreet() {
      const h = new Date().getHours()
      if (h < 6) return '夜深了'
      if (h < 12) return '早上好'
      if (h < 14) return '中午好'
      if (h < 18) return '下午好'
      return '晚上好'
    },
  },
})
