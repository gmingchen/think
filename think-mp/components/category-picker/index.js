import { categoryListApi } from '../../apis/index'

Component({
  properties: {
    visible: {
      type: Boolean,
      value: false,
      observer(val) {
        // 每次打开都刷新，保证从分类管理页返回后能拿到最新分类
        if (val) {
          this._loadList()
        }
      },
    },
    value: {
      type: String,
      value: '',
    },
  },

  data: {
    list: [],
    // 是否已加载过，避免首开异步加载期间误闪空态
    loaded: false,
  },

  methods: {
    async _loadList() {
      const r = await categoryListApi()
      this.setData({ list: r.data || [], loaded: true })
    },
    onClose() {
      this.triggerEvent('close')
    },
    onSelect(e) {
      this.triggerEvent('confirm', e.currentTarget.dataset)
    },
    // 空态引导：先关闭弹窗再跳转分类管理页，返回后重新打开即拉最新
    onGoManage() {
      this.triggerEvent('close')
      wx.$navigateTo('/subpackages/category/pages/list/index')
    },
  },
})
