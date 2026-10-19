Component({
  options: {
    multipleSlots: true
  },
  properties: {
    leftText: {
      type: String,
      value: ''
    },
    middleText: {
      type: String,
      value: ''
    },
    rightText: {
      type: String,
      value: ''
    },
    useBack: {
      type: Boolean,
      value: true
    },
    useCustomBack: {
      type: Boolean,
      value: false
    },
    fixed: {
      type: Boolean,
      value: false
    },
    placeholder: {
      type: Boolean,
      value: false
    },
    backgroundColor: {
      type: String,
      value: ''
    },
    useLeft: {
      type: Boolean,
      value: false
    },
    useMiddle: {
      type: Boolean,
      value: false
    },
    useRight: {
      type: Boolean,
      value: false
    },
  },
  data: {
    statusBarHeight: 0,
    height: 40
  },
  lifetimes: {
    created: function() {
      // const { statusBarHeight } = wx.getWindowInfo()
      // this.setData({ statusBarHeight })
    },
    attached: function() {
      let statusBarHeight = 0
      const systemInfo = wx.getSystemInfoSync()
      statusBarHeight = systemInfo.statusBarHeight
      this.setData({ statusBarHeight })
    },
  },
  methods: {
    onBack() {
      const { useBack, useCustomBack } = this.properties
      if (useBack) {
        if (useCustomBack) {
          this.triggerEvent('on-back', null, { bubbles: true, composed: true })
        } else {
          wx.$navigateBack()
        }
      }
    }
  }
})