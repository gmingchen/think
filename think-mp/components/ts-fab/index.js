Component({
  properties: {
    text: {
      type: String,
      value: '',
    },
  },
  methods: {
    onTap() {
      this.triggerEvent('click')
    },
  },
})
