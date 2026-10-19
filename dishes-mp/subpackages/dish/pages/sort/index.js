import { storePageBehavior } from '../../../../behaviors/index'
import { dishListByCategoryApi, dishSortApi } from '../../../../apis/index'
import { DEFAULT_DISH_COVER, AD_UNIT_SORT_BANNER } from '../../../../common/constants/index'

Page({
  behaviors: [storePageBehavior],

  data: {
    DEFAULT_DISH_COVER,
    AD_UNIT_SORT_BANNER,
    kitchenId: null,
    categoryId: null,
    categoryName: '未分类',
    list: [],
    loading: true,
    // 拖拽状态（复刻分类管理页）
    dragIndex: -1,
    dragOffsetY: 0,
    dragStartY: 0,
    itemHeight: 0,
    targetIndex: -1,
    dragOffsets: [],
  },

  async onLoad(options) {
    this.bindStore({ fields: ['kitchen'] })
    // 排序读写均依赖登录态与当前厨房，先等两者就绪（对齐 ai-recommend）
    await wx.$onLogined
    await wx.$onKitchenLoaded
    const kitchenId = this.data.kitchen?.active?.id
    // categoryId 为空串/缺省 → null，表示对"未分类"组排序（与后端作用域一致）
    const rawCid = options.categoryId
    const categoryId = (rawCid === '' || rawCid === undefined || rawCid === null) ? null : Number(rawCid)
    this.setData({
      kitchenId,
      categoryId,
      categoryName: options.categoryName ? decodeURIComponent(options.categoryName) : '未分类',
    })
    this._loadList()
  },

  async _loadList() {
    this.setData({ loading: true })
    // 有分类才带 categoryId；"未分类"组省略该键，避免 GET 空串转 Long 报错
    const params = this.data.categoryId != null ? { categoryId: this.data.categoryId } : {}
    const r = await dishListByCategoryApi(params)
    const list = (r.data || []).map(d => ({
      id: Number(d.id),
      name: d.name,
      photo: d.cover || '',
    }))
    this.setData({ list, loading: false })
  },

  // ── 拖拽排序（手柄 catch 触摸 + transform 让位） ──
  onDragStart(e) {
    const index = e.currentTarget.dataset.index
    const query = wx.createSelectorQuery()
    query.selectAll('.sort__item').boundingClientRect()
    query.exec(rects => {
      const items = rects[0]
      if (!items || !items[index]) return
      this.setData({
        dragIndex: index,
        targetIndex: index,
        dragStartY: e.touches[0].clientY,
        dragOffsetY: 0,
        itemHeight: items[index].height,
        dragOffsets: new Array(this.data.list.length).fill(0),
      })
    })
  },

  onDragMove(e) {
    const { dragIndex, dragStartY, itemHeight, list } = this.data
    if (dragIndex < 0 || !itemHeight) return
    const offsetY = e.touches[0].clientY - dragStartY
    const len = list.length
    let shift = offsetY > 0
      ? Math.floor(offsetY / itemHeight)
      : Math.ceil(offsetY / itemHeight)
    const targetIndex = Math.max(0, Math.min(len - 1, dragIndex + shift))
    const offsets = new Array(len).fill(0)
    for (let i = 0; i < len; i++) {
      if (i === dragIndex) continue
      if (dragIndex < targetIndex && i > dragIndex && i <= targetIndex) {
        offsets[i] = -itemHeight
      } else if (dragIndex > targetIndex && i >= targetIndex && i < dragIndex) {
        offsets[i] = itemHeight
      }
    }
    this.setData({ dragOffsetY: offsetY, targetIndex, dragOffsets: offsets })
  },

  async onDragEnd() {
    const { dragIndex, targetIndex, list } = this.data
    if (dragIndex < 0) return
    this.setData({ dragIndex: -1, dragOffsetY: 0, targetIndex: -1, dragOffsets: [] })
    if (dragIndex === targetIndex) return
    const prevList = list
    const newList = [...list]
    const [item] = newList.splice(dragIndex, 1)
    newList.splice(targetIndex, 0, item)
    this.setData({ list: newList })
    // 松手即落库；失败回滚到拖拽前顺序（不静默吞，避免用户以为已存）
    try {
      await dishSortApi({ kitchenId: this.data.kitchenId, categoryId: this.data.categoryId, ids: newList.map(i => i.id) })
      wx.$toast('排序好啦')
    } catch (err) {
      this.setData({ list: prevList })
      console.error('dish sort failed:', err)
    }
  },
})
