export const fieldBehavior = Behavior({
  methods: {
    onFieldChange(e) {
      const { field } = e.currentTarget.dataset
      if (field) {
        this.setData({ [field]: e.detail?.value ?? e.detail })
      }
    }
  },
})
