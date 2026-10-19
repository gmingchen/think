Component({
  data: {
    visible: false,
    title: '',
    content: '',
    confirmText: '确认',
    cancelText: '取消',
    // 点击确认后是否立即关闭：默认 true；广告类弹窗传 false，由页面在广告结算后手动 close
    autoClose: true,
  },

  methods: {
    open(options = {}) {
      this.setData({
        visible: true,
        title: options.title || '提示',
        content: options.content || '',
        confirmText: options.confirmText || '确认',
        cancelText: options.cancelText || '取消',
        autoClose: options.autoClose !== false,
      })
    },

    close() {
      this.setData({ visible: false })
    },

    onConfirm() {
      this.triggerEvent('confirm')
      // autoClose=false 时不立即关闭（如看广告弹窗，保留至广告结束后由页面 close）
      if (this.data.autoClose) this.close()
    },

    onCancel() {
      this.triggerEvent('cancel')
      this.close()
    }
  }
})
