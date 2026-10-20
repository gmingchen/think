import { splitSteps } from '../../../../../../utils/index'

Component({
  data: {
    visible: false,
    text: '',
    count: 0,
  },

  methods: {
    open() {
      this.setData({ visible: true, text: '', count: 0 })
    },

    onClose() {
      this.setData({ visible: false })
    },

    onInput(e) {
      this.setData({ text: e.detail.value, count: splitSteps(e.detail.value).length })
    },

    onConfirm() {
      const steps = splitSteps(this.data.text)
      if (!steps.length) {
        wx.$toast('没有识别到步骤内容')
        return
      }
      this.triggerEvent('confirm', { steps })
      this.setData({ visible: false, text: '', count: 0 })
    },
  },
})
