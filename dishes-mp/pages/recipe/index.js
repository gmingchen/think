import { storePageBehavior, sharePageBehavior } from '../../behaviors/index'
import { categoryListApi, dishListApi, dishDeleteApi, dishBatchUpdateCategoryApi } from '../../apis/index'
import { KITCHEN_ROLE_ENUM } from '../../common/enums/index'
import { DEFAULT_DISH_COVER, AD_UNIT_RECIPE_BANNER } from '../../common/constants/index'

// 滚动防抖间隔
const SCROLL_THROTTLE = 80

// 持久化选中状态，防止页面重建丢失
let _selectedIds = new Set()

Page({
  behaviors: [storePageBehavior, sharePageBehavior],

  data: {
    DEFAULT_DISH_COVER,
    AD_UNIT_RECIPE_BANNER,
    loading: true,
    KITCHEN_ROLE_ENUM,
    showKitchenSwitch: false,
    categoryList: [],
    activeCategory: '',
    allDishes: [],
    groupedDishList: [],
    scrollIntoView: '',
    sidebarScrollIntoView: '',
    _programScroll: false,
    _lastScrollTime: 0,
    cartShake: false,
    selectedCount: 0,
    isManageMode: false,
    showCategoryPicker: false,
    showPickPopup: false,
    floatItems: [
      { icon: '⚙️', label: '分类管理', value: 'category', closeOnClick: false },
      { icon: '🗑️', label: '批量删除', value: 'batchDelete', closeOnClick: false },
      { icon: '📁', label: '批量改分类', value: 'batchCategory', closeOnClick: false },
      { icon: '📖', label: '新增菜谱', value: 'add', closeOnClick: false },
    ],
  },

  async onLoad() {
    this.bindStore({ fields: ['user', 'system', 'kitchen', 'cart'], actions: ['updateKitchen', 'addToCart', 'removeFromCart', 'removeDishesFromCart'] })
  },

  async onShow() {
    await wx.$onKitchenLoaded
    await this._loadCategories()
    await this._loadDishes()
    this.setData({ loading: false })
  },

  // ── 分类 ──
  async _loadCategories() {
    const r = await categoryListApi()
    const list = (r.data || []).map(c => ({ ...c, id: Number(c.id) }))
    list.push({ id: null, name: '其它' })
    // 默认选中第一个分类
    const activeCategory = list.length ? list[0].id : ''
    this.setData({ categoryList: list, activeCategory })
  },

  onSelectCategory(e) {
    const id = e.currentTarget.dataset.id
    const normalizedId = id === null ? null : Number(id)
    const sidebarId = normalizedId === null ? 'tab-other' : `tab-${normalizedId}`
    this.setData({ activeCategory: normalizedId, sidebarScrollIntoView: sidebarId })
    this._scrollToActiveCategory()
  },

  onManageCategory() {
    wx.$navigateTo('/subpackages/category/pages/list/index')
  },

  // 进入当前分类的菜谱排序页（categoryId 为空则为"未分类"组）
  onSortCategory(e) {
    const { categoryId, categoryName } = e.currentTarget.dataset
    const cid = (categoryId === null || categoryId === undefined || categoryId === '') ? '' : categoryId
    const name = encodeURIComponent(categoryName || '未分类')
    const url = cid
      ? `/subpackages/dish/pages/sort/index?categoryId=${cid}&categoryName=${name}`
      : `/subpackages/dish/pages/sort/index?categoryName=${name}`
    wx.$navigateTo(url)
  },

  onDishListScroll(e) {
    // 程序触发的滚动不处理
    if (this.data._programScroll) return
    // 节流
    const now = Date.now()
    if (now - this.data._lastScrollTime < SCROLL_THROTTLE) return
    this._lastScrollTime = now
    this._detectActiveCategory()
  },

  _detectActiveCategory() {
    const { groupedDishList } = this.data
    if (!groupedDishList.length) return
    const query = wx.createSelectorQuery().in(this)
    query.select('.recipe__dish-list').boundingClientRect()
    groupedDishList.forEach(g => {
      const id = g.categoryId === null ? 'cat-other' : `cat-${g.categoryId}`
      query.select(`#${id}`).boundingClientRect()
    })
    query.exec(res => {
      if (!res || !res[0]) return
      const containerTop = res[0].top
      let activeIdx = 0
      for (let i = 1; i < res.length; i++) {
        if (res[i] && res[i].top <= containerTop + 10) {
          activeIdx = i - 1
        }
      }
      const newCategory = groupedDishList[activeIdx].categoryId
      if (newCategory !== this.data.activeCategory) {
        const sidebarId = newCategory === null ? 'tab-other' : `tab-${newCategory}`
        this.setData({ activeCategory: newCategory, sidebarScrollIntoView: sidebarId })
      }
    })
  },

  // ── 菜谱 ──
  async _loadDishes() {
    const kitchenId = this.data.kitchen?.active?.id
    if (!kitchenId) return
    const r = await dishListApi({ kitchenId })
    const list = (r.data || []).map(d => {
      const categoryId = d.categoryId != null ? Number(d.categoryId) : null
      return {
        id: Number(d.id),
        name: d.name,
        photo: d.cover || '',
        categoryId,
        categoryName: this._getCategoryName(categoryId),
        cookCount: d.cookCount || 0,
        rating: d.rating || 0,
        tag: '',
        selected: _selectedIds.has(Number(d.id)),
      }
    })
    // 同步选中状态：移除已不存在的菜谱ID
    const validIds = new Set(list.map(d => d.id))
    _selectedIds = new Set([..._selectedIds].filter(id => validIds.has(Number(id))))
    this.setData({ allDishes: list })
    this._filterDishes()
    // 管理模式下恢复选中计数
    if (this.data.isManageMode) {
      this.setData({ selectedCount: _selectedIds.size })
    }
    // 列表渲染后重新触发滚动到选中分类
    this._scrollToActiveCategory()
  },

  _scrollToActiveCategory() {
    const { activeCategory } = this.data
    if (activeCategory === '' || activeCategory === undefined) return
    const scrollId = activeCategory === null ? 'cat-other' : `cat-${activeCategory}`
    // 标记程序滚动，防止联动冲突
    this.setData({ _programScroll: true, scrollIntoView: '' }, () => {
      this.setData({ scrollIntoView: scrollId })
      setTimeout(() => {
        this.setData({ _programScroll: false })
      }, 400)
    })
  },

  _getCategoryName(categoryId) {
    if (!categoryId) return '其它'
    const cat = this.data.categoryList.find(c => c.id === categoryId)
    return cat ? cat.name : ''
  },

  _filterDishes() {
    const { allDishes, categoryList } = this.data
    // 按分类分组（展示全部）
    const grouped = categoryList
      .map(cat => {
        const dishes = cat.id === null
          ? allDishes.filter(d => !d.categoryId)
          : allDishes.filter(d => d.categoryId === cat.id)
        return dishes.length ? { categoryId: cat.id, categoryName: cat.name, dishes } : null
      })
      .filter(Boolean)
    this.setData({ groupedDishList: grouped })
  },

  // ── 编辑模式 ──
  onFabChange(e) {
    const isManageMode = e.detail.expanded
    // 退出编辑模式时重置选中状态
    if (!isManageMode) {
      _selectedIds.clear()
      const groupedDishList = this.data.groupedDishList.map(g => ({
        ...g,
        dishes: g.dishes.map(d => ({ ...d, selected: false })),
      }))
      this.setData({ groupedDishList, selectedCount: 0 })
    }
    this.setData({ isManageMode })
  },

  onToggleSelect(e) {
    const id = Number(e.currentTarget.dataset.id)
    const groupedDishList = this.data.groupedDishList.map(g => ({
      ...g,
      dishes: g.dishes.map(d => d.id === id ? { ...d, selected: !d.selected } : d),
    }))
    // 同步持久化选中ID
    let wasSelected = false
    this.data.groupedDishList.forEach(g => {
      const found = g.dishes.find(d => d.id === id)
      if (found) wasSelected = found.selected
    })
    if (wasSelected) {
      _selectedIds.delete(id)
    } else {
      _selectedIds.add(id)
    }
    let selectedCount = 0
    groupedDishList.forEach(g => g.dishes.forEach(d => { if (d.selected) selectedCount++ }))
    this.setData({ groupedDishList, selectedCount })
  },

  onBatchCategory() {
    if (this.data.selectedCount === 0) {
      wx.$toast('请先选择菜谱')
      return
    }
    this.setData({ showCategoryPicker: true })
  },

  onCloseCategoryPicker() {
    this.setData({ showCategoryPicker: false })
  },

  async onConfirmCategory(e) {
    const categoryId = e.detail.id
    const ids = this._getSelectedIds()
    try {
      await dishBatchUpdateCategoryApi({ ids, categoryId })
      wx.$toast('修改分类成功')
      this.setData({ showCategoryPicker: false })
      this._clearSelection()
      this._loadDishes()
    } catch (err) {
      console.error('batch update category failed:', err)
    }
  },

  onBatchDelete() {
    if (this.data.selectedCount === 0) {
      wx.$toast('请先选择菜谱')
      return
    }
    this.selectComponent('#deleteConfirmPopup').open({
      title: '确认删除',
      content: `确定删除选中的 ${this.data.selectedCount} 道菜谱吗？`,
      confirmText: '删除',
    })
  },

  async onConfirmDelete() {
    const ids = this._getSelectedIds()
    try {
      await dishDeleteApi({ ids })
      wx.$toast('删除成功')
      this.removeDishesFromCart(ids)
      this._clearSelection()
      this._loadDishes()
    } catch (err) {
      console.error('batch delete failed:', err)
    }
  },

  _getSelectedIds() {
    const ids = []
    this.data.groupedDishList.forEach(g => {
      g.dishes.forEach(d => {
        if (d.selected) ids.push(d.id)
      })
    })
    return ids
  },

  _clearSelection() {
    _selectedIds.clear()
    const groupedDishList = this.data.groupedDishList.map(g => ({
      ...g,
      dishes: g.dishes.map(d => ({ ...d, selected: false })),
    }))
    this.setData({ groupedDishList, selectedCount: 0 })
  },

  // ── 菜谱操作 ──
  onDishTap(e) {
    const { id } = e.currentTarget.dataset
    this.selectComponent('#dishPopup').open(id, true)
  },

  onAddDishFromPopup(e) {
    const popup = this.selectComponent('#dishPopup')
    const dish = popup?.data?.dish
    if (dish?.id) {
      this.addToCart({ id: dish.id, name: dish.name, photo: dish.photo })
      wx.vibrateShort({ type: 'medium' })
      this._shakeCartBasket()
    }
    popup?.close()
  },

  onDetailDish(e) {
    const { id } = e.detail
    this.selectComponent('#dishPopup').close()
    if (id) {
      wx.$navigateTo(`/subpackages/dish/pages/detail/index?id=${id}`)
    }
  },

  onAddDish(e) {
    const { id } = e.currentTarget.dataset
    // 从分组中找到菜谱信息
    let dish = null
    this.data.groupedDishList.forEach(g => {
      const found = g.dishes.find(d => d.id === id)
      if (found) dish = found
    })
    if (dish) {
      wx.vibrateShort({ type: 'medium' })
      this.addToCart({ id: dish.id, name: dish.name, photo: dish.photo })
      this._shakeCartBasket()
    }
  },

  onStepperChange(e) {
    const id = Number(e.currentTarget.dataset.id)
    const { value } = e.detail
    const currentQty = this._getCartQuantity(id)
    
    wx.vibrateShort({ type: 'medium' })
    
    if (value > currentQty) {
      // 增加 - 找到菜谱信息并添加
      let dish = null
      this.data.groupedDishList.forEach(g => {
        const found = g.dishes.find(d => d.id === id)
        if (found) dish = found
      })
      if (dish) {
        this.addToCart({ id: dish.id, name: dish.name, photo: dish.photo })
      }
    } else if (value < currentQty) {
      // 减少
      this.removeFromCart(id)
    }
    
    this._shakeCartBasket()
  },

  // 竹篮摇晃动画
  _shakeCartBasket() {
    this.setData({ cartShake: true })
    setTimeout(() => {
      this.setData({ cartShake: false })
    }, 500)
  },

  // 获取菜谱在购物车中的数量
  _getCartQuantity(dishId) {
    const cart = this.data.cart
    if (!cart || !cart.list) return 0
    const item = cart.list.find(i => i.dishId === Number(dishId))
    return item ? item.quantity : 0
  },

  onAddNewDish() {
    wx.$navigateTo('/subpackages/dish/pages/add-edit/index')
  },

  onFloatAction(e) {
    const { value } = e.detail
    switch (value) {
      case 'add':
        this.onAddNewDish()
        break
      case 'category':
        this.onManageCategory()
        break
      case 'batchCategory':
        this.onBatchCategory()
        break
      case 'batchDelete':
        this.onBatchDelete()
        break
    }
  },

  onEditDish(e) {
    const { id } = e.currentTarget.dataset
    wx.$navigateTo(`/subpackages/dish/pages/add-edit/index?id=${id}`)
  },

  // ── 购物车 ──
  onPickDish() {
    this.setData({ showPickPopup: true })
  },

  onClosePickPopup() {
    this.setData({ showPickPopup: false })
  },

  onPickAddToCart(e) {
    const { id, name, photo } = e.detail
    this.addToCart({ id, name, photo })
    wx.vibrateShort({ type: 'medium' })
    this._shakeCartBasket()
  },

  // 帮我选弹窗内点「清单」：抽菜结果为数据源跳采购清单，弹窗不关返回仍在
  onPickShopping(e) {
    const dishIds = (e.detail.dishIds || []).join(',')
    if (!dishIds) return
    wx.$navigateTo(`/subpackages/dish/pages/shopping/index?dishIds=${dishIds}&source=pick`)
  },

  onOpenCartPopup() {
    this.selectComponent('#cartPopup').open()
  },

  onGoOrder() {
    const cart = this.data.cart
    if (!cart || cart.quantity === 0) {
      wx.$toast('请先选择菜谱')
      return
    }
    wx.$navigateTo('/subpackages/cookorder/pages/confirm/index')
  },

  // ── 厨房切换弹窗 ──
  onOpenKitchenSwitch() {
    this.setData({ showKitchenSwitch: true })
  },
  onCloseKitchenSwitch() {
    this.setData({ showKitchenSwitch: false })
  },
  async onSwitchKitchenConfirm(e) {
    const { item } = e.detail
    this.onCloseKitchenSwitch()
    await this.updateKitchen(item)
    await this._loadCategories()
    this._loadDishes()
  },
})
