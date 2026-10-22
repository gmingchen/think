import { grpUpdateApi } from '../../../../../../apis/index'

Component({
  data: {
    // 显隐由组件内部自控，外部通过 selectComponent('#id').open({ grpId, name }) 打开
    show: false,
    grpId: null,
    name: '',
  },
  methods: {
    // 供页面通过 id 调用：打开弹窗并带入当前群 id 与群名
    open({ grpId, name } = {}) {
      this.setData({ show: true, grpId, name: name || '' })
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
    // 保存：成功后自关并向页面抛出 confirm（更新完成），页面据此刷新详情
    async onConfirm() {
      const name = this.data.name.trim()
      if (!name) {
        wx.$toast('请输入群名称')
        return
      }
      await grpUpdateApi({ grpId: this.data.grpId, name })
      wx.$toast('已更新群名称')
      this.close()
      this.triggerEvent('confirm')
    },
  },
})
