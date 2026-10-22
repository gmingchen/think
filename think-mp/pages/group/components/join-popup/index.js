import { grpJoinApi } from '../../../../apis/index'

Component({
  data: {
    // 显隐由组件内部自控，外部通过 selectComponent('#id').open() 打开
    show: false,
    code: '',
  },
  methods: {
    // 供页面通过 id 调用：打开弹窗并清空上次输入
    open() {
      this.setData({ show: true, code: '' })
    },
    close() {
      this.setData({ show: false })
    },
    onInput(e) {
      this.setData({ code: e.detail.value })
    },
    // 点 X / 遮罩：内部自关
    onClose() {
      this.close()
    },
    // 读取剪贴板一键粘贴邀请码（对应详情页“复制邀请码”后的粘贴入口）
    onPaste() {
      wx.getClipboardData({
        success: ({ data }) => {
          if (data) this.setData({ code: String(data).trim() })
        },
      })
    },
    // 凭码入群：成功后自关并向页面抛出 confirm，页面据此刷新列表
    async onConfirm() {
      const code = this.data.code.trim()
      if (!code) {
        wx.$toast('请输入邀请码')
        return
      }
      await grpJoinApi({ code })
      wx.$toast('已加入群组')
      this.close()
      this.triggerEvent('confirm')
    },
  },
})
