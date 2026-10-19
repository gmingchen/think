import { STEP_MAX_LENGTH } from '../../../../../../utils/index'

Component({
  data: {
    visible: false,
    title: '',
    // index >= 0 为编辑该条，-1 为新增
    index: -1,
    text: '',
    focus: false,
    maxLength: STEP_MAX_LENGTH,
  },

  methods: {
    open(text, index = -1) {
      this.setData({
        title: index > -1 ? '编辑步骤' : '新增步骤',
        index,
        text: text || '',
        visible: true,
        focus: false,
      }, () => this.setData({ focus: true }))
    },

    onClose() {
      this.setData({ visible: false, focus: false })
    },

    onInput(e) {
      this.setData({ text: e.detail.value })
    },

    // 直接确认：提交后关闭弹窗（新增追加 / 编辑更新）
    onConfirm() {
      if (!this._valid()) return
      this.triggerEvent('confirm', { text: this.data.text.trim(), index: this.data.index })
      this.onClose()
    },

    // 确认并继续：提交后清空并保持弹窗打开，焦点回到输入框，可连续录入下一条
    onConfirmAndNext() {
      if (!this._valid()) return
      this.triggerEvent('confirm', { text: this.data.text.trim(), index: -1 })
      this.setData({ text: '', focus: false }, () => this.setData({ focus: true }))
    },

    _valid() {
      if (!this.data.text.trim()) {
        wx.$toast('请输入步骤内容')
        return false
      }
      return true
    },
  },
})
