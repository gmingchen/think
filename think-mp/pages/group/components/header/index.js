Component({
  methods: {
    // 点击创建：仅通知页面，建群弹窗/流程由页面处理
    onCreate() {
      this.triggerEvent('create')
    },
    // 点击加入：通知页面打开邀请码入群弹窗
    onJoin() {
      this.triggerEvent('join')
    },
  },
})
