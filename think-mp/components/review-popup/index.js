Component({
  properties: {
    show: {
      type: Boolean,
      value: false,
      observer: 'onShowChange',
    },
    // 备选方案数组：[string]（方案文案）
    options: {
      type: Array,
      value: [],
    },
    // 本人已评议时回显：倾向下标 + 观点（新增时分别传 -1 / ''）
    myOption: {
      type: Number,
      value: -1,
    },
    myContent: {
      type: String,
      value: '',
    },
  },
  data: {
    selected: -1,
    content: '',
  },
  methods: {
    onShowChange(show) {
      if (show) {
        // 打开时以当前 props 回显，避免上次输入残留
        this.setData({ selected: this.data.myOption, content: this.data.myContent || '' })
      }
    },
    onSelectOption(e) {
      this.setData({ selected: Number(e.currentTarget.dataset.index) })
    },
    onInputContent(e) {
      this.setData({ content: e.detail.value })
    },
    onClose() {
      this.triggerEvent('close')
    },
    onSubmit() {
      const { selected, content } = this.data
      if (selected < 0) {
        wx.$toast('请选择倾向的备选项')
        return
      }
      if (!content.trim()) {
        wx.$toast('请填写评议观点')
        return
      }
      this.triggerEvent('submit', { option: selected, content: content.trim() })
    },
  },
})
