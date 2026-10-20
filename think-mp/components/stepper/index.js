Component({
  properties: {
    value: {
      type: Number,
      value: 0
    },
    min: {
      type: Number,
      value: 0
    },
    max: {
      type: Number,
      value: 99
    }
  },

  methods: {
    onDecrement() {
      if (this.properties.value <= this.properties.min) return
      const newVal = this.properties.value - 1
      this.triggerEvent('change', { value: newVal })
    },

    onIncrement() {
      if (this.properties.value >= this.properties.max) return
      const newVal = this.properties.value + 1
      this.triggerEvent('change', { value: newVal })
    }
  }
})
