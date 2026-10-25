import { abuseSubmitApi } from '../../apis/index'

const REASONS = ['不友善内容', '违法违规', '广告/垃圾信息', '涉及他人隐私', '其他']

Component({
  properties: {
    show: {
      type: Boolean,
      value: false,
    },
    // 举报对象类型：1-决策单 2-评议
    targetType: {
      type: Number,
      value: 1,
    },
    // 被举报对象ID
    targetId: {
      type: null,
      value: null,
    },
  },
  data: {
    reasons: REASONS,
    activeIndex: -1,
    detail: '',
    submitting: false,
  },
  observers: {
    // 每次打开重置状态
    'show': function (show) {
      if (show) {
        this.setData({ activeIndex: -1, detail: '', submitting: false })
      }
    },
  },
  methods: {
    onSelectReason(e) {
      this.setData({ activeIndex: Number(e.currentTarget.dataset.index) })
    },
    onDetailInput(e) {
      this.setData({ detail: e.detail.value })
    },
    async onSubmit() {
      const { activeIndex, detail, reasons, targetType, targetId } = this.data
      if (activeIndex < 0) {
        wx.$toast('请选择举报理由')
        return
      }
      const reason = reasons[activeIndex] + (detail ? `：${detail}` : '')
      if (this.data.submitting) return
      this.setData({ submitting: true })
      try {
        await abuseSubmitApi({ targetType, targetId, reason })
        wx.$toast('举报已提交')
        this.triggerEvent('success')
        this.onClose()
      } finally {
        this.setData({ submitting: false })
      }
    },
    onClose() {
      this.triggerEvent('close')
    },
  },
})
