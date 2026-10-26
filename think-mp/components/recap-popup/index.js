const OTHER = -1 // 最终选择「其他/暂缓」哨兵
const RESULT_MAX = 2000

Component({
  properties: {
    show: {
      type: Boolean,
      value: false,
      observer: 'onShowChange',
    },
    // 备选方案数组：[string]（与决策单一致）
    options: {
      type: Array,
      value: [],
    },
    // 已复盘回显：最终选择下标（0..n-1）或 OTHER(-1)；新增传 null
    choice: {
      type: null,
      value: null,
    },
    // 已复盘回显：实际结局文本；新增传 ''
    result: {
      type: String,
      value: '',
    },
    // 提交中（页面控制）：防连点/重复提交，提交中禁用按钮
    submitting: {
      type: Boolean,
      value: false,
    },
  },
  data: {
    OTHER,
    RESULT_MAX,
    selected: null,
    content: '',
  },
  methods: {
    onShowChange(show) {
      if (show) {
        // 打开时以当前 props 回显，避免上次输入残留
        this.setData({
          selected: this.data.choice === undefined ? null : this.data.choice,
          content: this.data.result || '',
        })
      }
    },
    onSelectChoice(e) {
      this.setData({ selected: Number(e.currentTarget.dataset.index) })
    },
    onInputResult(e) {
      this.setData({ content: e.detail.value })
    },
    onClose() {
      this.triggerEvent('close')
    },
    onSubmit() {
      if (this.data.submitting) {
        return
      }
      const { selected, content } = this.data
      if (selected === null) {
        wx.$toast('请选择最终选择')
        return
      }
      this.triggerEvent('submit', { finalChoice: selected, actualResult: content.trim() })
    },
  },
})
