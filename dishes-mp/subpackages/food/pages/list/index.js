import { storePageBehavior, sharePageBehavior } from '../../../../behaviors/index'
import { foodListApi, foodDeleteApi } from '../../../../apis/index'
import { AD_UNIT_FOOD_LIST_CUSTOM } from '../../../../common/constants/index'

Page({
  behaviors: [storePageBehavior, sharePageBehavior],
  data: {
    list: [],
    loading: true,
    AD_UNIT_FOOD_LIST_CUSTOM,
  },

  async onLoad() {
    this.bindStore({ fields: ['kitchen'] })
    await wx.$onKitchenLoaded
    this.getData()
  },

  async getData(showSkeleton = true) {
    if (showSkeleton) this.setData({ loading: true })
    const r = await foodListApi()
    this.setData({
      list: r.data || [],
      loading: false,
    })
  },

  onAdd() {
    this.selectComponent('#foodPopup').open()
  },
  onEdit(e) {
    const { item } = e.currentTarget.dataset
    this.selectComponent('#foodPopup').open(item)
  },
  onConfirm() {
    this.getData(false)
  },

  async onDelete(e) {
    const { item } = e.currentTarget.dataset
    const r = await wx.$confirm(`确认删除食材「${item.name}」？`)
    if (!r) return
    await foodDeleteApi({ id: item.id })
    this.getData(false)
  },
})
