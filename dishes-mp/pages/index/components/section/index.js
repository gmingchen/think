Component({
  properties: {
    title: {
      type: String,
      value: ''
    },
    link: {
      type: String,
      value: ''
    }
  },
  methods: {
    onTap() {
      this.triggerEvent('tap')
    }
  }
})
