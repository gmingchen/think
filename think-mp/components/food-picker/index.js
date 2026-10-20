import { foodListApi } from '../../apis/index'

Component({
  properties: {
    visible: {
      type: Boolean,
      value: false,
      observer(val) {
        if (val) {
          this._loadList()
          this._initSelected()
        }
      },
    },
    // 已选中的食材ID列表
    selectedIds: {
      type: Array,
      value: [],
    },
  },

  data: {
    list: [],
    tempSelected: [],
  },

  methods: {
    async _loadList() {
      try {
        const r = await foodListApi()
        this.setData({ list: r.data || [] })
      } catch (err) {
        // 失败已由 service 统一 toast，此处兼容观察器内未 await 的调用
      }
    },

    _initSelected() {
      this.setData({ tempSelected: [...this.data.selectedIds] })
    },

    // 供外部调用刷新
    refresh() {
      this._loadList()
    },

    onClose() {
      this.setData({ visible: false })
    },

    onToggleSelect(e) {
      const { id } = e.currentTarget.dataset
      const tempSelected = [...this.data.tempSelected]
      const index = tempSelected.indexOf(id)
      if (index > -1) {
        tempSelected.splice(index, 1)
      } else {
        tempSelected.push(id)
      }
      this.setData({ tempSelected })
    },

    onConfirm() {
      // 列表未返回就确认会把未加载误判为未勾选，导致已填项被清空
      if (!this.data.list.length && this.data.tempSelected.length) {
        wx.$toast('食材列表加载中，请稍候')
        return
      }
      // 按点选顺序返回选中项，保证新增项排在列表末尾
      // 食材库已删除的项 list 里查不到，保留 id 以免误删表单项
      const selectedItems = this.data.tempSelected.map(id => {
        const food = this.data.list.find(item => item.id === id)
        return { id, name: food ? food.name : '' }
      })
      this.triggerEvent('confirm', { items: selectedItems })
      this.setData({ visible: false })
    },

    onOpenCreate() {
      this.selectComponent('#foodPopup').open()
    },

    async onFoodCreated(e) {
      const { id, created } = (e && e.detail) || {}
      // 新增成功后自动选中，不需用户再点一次
      if (created && id && !this.data.tempSelected.includes(id)) {
        this.setData({ tempSelected: [...this.data.tempSelected, id] })
      }
      await this._loadList()
    },
  },
})
