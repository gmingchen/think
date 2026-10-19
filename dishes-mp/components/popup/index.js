Component({
  options: {
    multipleSlots: true
  },
  properties: {
    show: {
      type: Boolean,
      value: false
    },
    title: {
      type: String,
      value: ''
    },
    subtitle: {
      type: String,
      value: ''
    },
    // 弹窗位置：bottom（默认）| center
    position: {
      type: String,
      value: 'bottom'
    },
    // 弹窗打开时锁定页面滚动（面板内自带 scroll-view 不受影响）
    lockScroll: {
      type: Boolean,
      value: true
    }
  },

  methods: {
    onClose() {
      this.triggerEvent('close')
    },
    // 空方法，仅用于 catch 阻断 touchmove 冒泡，防止背面页面跟随滚动
    onLockTouchMove() {}
  }
})
