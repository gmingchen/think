Component({
  options: {
    multipleSlots: true,
  },
  properties: {
    useNavbar: {
      type: Boolean,
      value: true
    },
    useBack: {
      type: Boolean,
      value: false
    },
    useCustomBack: {
      type: Boolean,
      value: false
    },
    title: {
      type: String,
      value: ''
    },
    navbarBackgroundColor: {
      type: String,
      value: ''
    },
    useNavbarLeft: {
      type: Boolean,
      value: false
    },
    useNavbarMiddle: {
      type: Boolean,
      value: false
    },
    useNavbarRight: {
      type: Boolean,
      value: false
    },

    useScroll: {
      type: Boolean,
      value: true
    },
    loading: {
      type: Boolean,
      value: false
    },
    finished: {
      type: Boolean,
      value: false
    },
    scrollTop: {
      type: Number,
      value: 0
    },

    useFooter: {
      type: Boolean,
      value: false
    }
  },
  data: {
    isTabbar: false,
  },
  lifetimes: {
    attached: function() {
      // const isTabbar = !!this.getTabBar()
      // this.setData({ isTabbar })
    }
  },
  methods: {
    onScrollLower(e) {
      this.triggerEvent('on-scroll-lower')
    }
  },
})
