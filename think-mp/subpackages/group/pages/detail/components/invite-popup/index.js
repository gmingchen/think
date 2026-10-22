import { grpInviteApi } from '../../../../../../apis/index'

Component({
  data: {
    // 显隐由组件内部自控，外部通过 selectComponent('#id').open({ grpId }) 打开
    show: false,
    grpId: null,
    code: '',
  },
  methods: {
    // 供页面通过 id 调用：打开弹窗并向服务端申请邀请码
    async open({ grpId } = {}) {
      this.setData({ show: true, grpId, code: '' })
      const r = await grpInviteApi({ grpId })
      this.setData({ code: r.data || '' })
    },
    close() {
      this.setData({ show: false })
    },
    // 点 X / 遮罩：内部自关
    onClose() {
      this.close()
    },
    // 复制邀请码；「转发邀请」由页面 onShareAppMessage 处理（读取本组件 code）
    onCopy() {
      if (!this.data.code) return
      wx.setClipboardData({ data: this.data.code })
    },
  },
})
