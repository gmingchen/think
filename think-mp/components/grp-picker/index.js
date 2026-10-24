import { grpPageApi } from '../../apis/index'

Component({
  properties: {
    // 当前已选群 ID：列表中命中项高亮打勾
    selected: {
      type: null,
      value: null,
    },
  },
  data: {
    // 显隐由组件内部自控，外部通过 selectComponent('#id').open() 打开
    show: false,
    loading: false,
    grps: [],
  },
  methods: {
    // 供页面通过 id 调用：打开弹窗并拉取我的群组列表
    async open() {
      this.setData({ show: true, loading: true })
      const r = await grpPageApi({ current: 1, size: 50 })
      this.setData({ grps: r.data?.list || [], loading: false })
    },
    close() {
      this.setData({ show: false })
    },
    // 点 X / 遮罩：内部自关
    onClose() {
      this.close()
    },
    onSelect(e) {
      const { id, name } = e.currentTarget.dataset
      this.close()
      this.triggerEvent('select', { id, name })
    },
  },
})
