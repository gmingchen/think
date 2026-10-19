import { storePageBehavior, sharePageBehavior } from '../../../../behaviors/index'
import { categoryListApi, categorySortApi, categoryDeleteApi, kitchenExpandCategoryApi } from '../../../../apis/index'
import { playExpandAd, buildCapacity, canExpandByAd } from '../../../../utils/index'
import { LIMIT_REACHED_CODE, AD_UNIT_CATEGORY_BANNER, VIP_PAGE } from '../../../../common/constants/index'

Page({
  behaviors: [storePageBehavior, sharePageBehavior],
  data: {
    list: [],
    loading: true,
    expanding: false,
    category: null,
    AD_UNIT_CATEGORY_BANNER,
    dragIndex: -1,
    dragOffsetY: 0,
    dragStartY: 0,
    itemHeight: 0,
    targetIndex: -1,
    dragOffsets: [],
  },

  async onLoad() {
    this.bindStore({ fields: ['kitchen', 'ad'], actions: ['getKitchen'] })
    await wx.$onKitchenLoaded
    this.getData()
  },

  async getData(showSkeleton = true) {
    if (showSkeleton) this.setData({ loading: true })
    const r = await categoryListApi()
    const list = r.data || []
    this.setData({
      list,
      loading: false,
      // 当前分类数以已加载列表为准（列表接口不返回 categoryCount），上限与「不限」标记都取当前厨房
      // unlimited 传的是 KitchenVO.unlimitedCapacity（已含 owner 判定），前端不再自己拼档位（§6.2）
      category: buildCapacity(list.length, this.data.kitchen?.active?.maxCategoryCount, this.data.kitchen?.active?.unlimitedCapacity),
    })
  },

  onAdd() {
    this.selectComponent('#categoryPopup').open()
  },
  onEdit(e) {
    const { item } = e.currentTarget.dataset
    this.selectComponent('#categoryPopup').open(item)
  },
  onConfirm() {
    this.getData(false)
  },

  // 分类弹窗 emit limit（4005）→ 弹引导 or 普通 toast
  onCategoryLimit(e) {
    if (this._canExpand()) {
      this.selectComponent('#limitPopup').open({
        title: '分类数已达上限',
        content: '看个小视频即可 +1，不限次数～或删除部分旧内容后可继续添加',
        confirmText: '📺 看视频 +1',
        cancelText: '以后再说',
        autoClose: false,
      })
      return
    }
    const msg = (e && e.detail && e.detail.message) || '分类数已达上限'
    wx.$toast(msg)
  },

  // 仅厨师长/管厨且可扩容时才可看视频（畅厨在自己的厨房已不限数，入口隐藏；轻享仍可用）
  _canExpand() {
    const active = this.data.kitchen?.active
    return (active?.role === 1 || active?.role === 2) && canExpandByAd(active?.unlimitedCapacity)
  },

  // 容量条下的会员引导行（§4 入口 4：二档转化位）
  onGoVip() {
    wx.$navigateTo(VIP_PAGE)
  },

  // 顶栏「📺 +1」：主动拉起激励视频为分类上限 +1
  onExpandCapacity() {
    this._expandCategoryByAd()
  },

  // confirm-popup 确认（入口二达上限）→ 看广告 → 扩容 → 重试 onSave
  onExpandConfirm() {
    this._expandCategoryByAd(() => {
      const popup = this.selectComponent('#categoryPopup')
      popup && popup.onSave()
    })
  },

  // 看激励视频为分类上限 +1 的公共流程：完整观看后调接口、刷新全局与本页容量；onAfterExpand 为扩容成功后额外动作
  _expandCategoryByAd(onAfterExpand) {
    if (this.data.expanding) return
    this.setData({ expanding: true })
    playExpandAd('category', async ({ ended }) => {
      this.setData({ expanding: false })
      // 广告结束（看完/中途关/加载失败）后才关闭达上限引导弹窗（点击确认时不立即关）
      const limitPopup = this.selectComponent('#limitPopup')
      limitPopup && limitPopup.close()
      if (!ended) return
      try {
        await kitchenExpandCategoryApi({ kitchenId: this.data.kitchen.active.id })
        await this.getKitchen()
        wx.$toast('已扩容 +1')
        // 重拉列表以刷新顶部容量条（上限取 getKitchen 后的 active.maxCategoryCount）
        this.getData(false)
        onAfterExpand && onAfterExpand()
      } catch (e) {
        // 扩容接口异常（非4005）service 已提示，此处静默
      }
    })
  },

  async onDelete(e) {
    const { item } = e.currentTarget.dataset
    const r = await wx.$confirm(`确认删除分类「${item.name}」？`)
    if (!r) return
    await categoryDeleteApi({ id: item.id })
    this.getData(false)
  },

  // ── 拖拽排序 ──
  onDragStart(e) {
    const index = e.currentTarget.dataset.index
    const query = wx.createSelectorQuery()
    query.selectAll('.category__cat-item').boundingClientRect()
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
    if (dragIndex !== targetIndex) {
      const newList = [...list]
      const [item] = newList.splice(dragIndex, 1)
      newList.splice(targetIndex, 0, item)
      this.setData({ list: newList })
      await categorySortApi({ ids: newList.map(i => i.id) })
    }
  },
})
