import { grpCreateApi } from '../../../../apis/index'

Component({
  data: {
    // 显隐由组件内部自控，外部通过 selectComponent('#id').open() 打开
    show: false,
    name: '',
  },
  methods: {
    // 供页面通过 id 调用：打开弹窗并清空上次输入
    open() {
      this.setData({ show: true, name: '' })
    },
    close() {
      this.setData({ show: false })
    },
    onInput(e) {
      this.setData({ name: e.detail.value })
    },
    // 点 X / 遮罩：内部自关
    onClose() {
      this.close()
    },
    // 新增：成功后自关并向页面抛出 confirm（新增完成），页面据此刷新列表
    async onConfirm() {
      const name = this.data.name.trim()
      if (!name) {
        wx.$toast('请输入群名称')
        return
      }
      await grpCreateApi({ name })
      wx.$toast('群组已创建')
      this.close()
      this.triggerEvent('confirm')
    },
  },
})
