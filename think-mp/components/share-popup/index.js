Component({
  properties: {},

  data: {
    show: false,
  },

  methods: {
    // 供父组件通过 selectComponent('#sharePopup').open() 调用
    open() {
      this.setData({ show: true })
    },
    close() {
      this.setData({ show: false })
    },
    onClose() {
      this.setData({ show: false })
    },
    onPosterSelect() {
      this.setData({ show: false })
      this.triggerEvent('select', { key: 'poster' })
    },
  },
})
