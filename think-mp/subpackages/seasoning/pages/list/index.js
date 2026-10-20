import { storePageBehavior, sharePageBehavior } from '../../../../behaviors/index'
import { seasoningListApi, seasoningDeleteApi } from '../../../../apis/index'
import { AD_UNIT_SEASONING_LIST_CUSTOM } from '../../../../common/constants/index'

Page({
  behaviors: [storePageBehavior, sharePageBehavior],
  data: {
    list: [],
    loading: true,
    AD_UNIT_SEASONING_LIST_CUSTOM,
  },

  async onLoad() {
    this.bindStore({ fields: ['kitchen'] })
    await wx.$onKitchenLoaded
    this.getData()
  },

  async getData(showSkeleton = true) {
    if (showSkeleton) this.setData({ loading: true })
    const r = await seasoningListApi()
    this.setData({
      list: r.data || [],
      loading: false,
    })
  },

  onAdd() {
    this.selectComponent('#seasoningPopup').open()
  },
  onEdit(e) {
    const { item } = e.currentTarget.dataset
    this.selectComponent('#seasoningPopup').open(item)
  },
  onConfirm() {
    this.getData(false)
  },

  async onDelete(e) {
    const { item } = e.currentTarget.dataset
    const r = await wx.$confirm(`确认删除调料「${item.name}」？`)
    if (!r) return
    await seasoningDeleteApi({ id: item.id })
    this.getData(false)
  },
})
