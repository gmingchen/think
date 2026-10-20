import { storePageBehavior, sharePageBehavior } from '../../../../behaviors/index'
import { dishInfoApi, categoryListApi, cookReviewPageApi } from '../../../../apis/index'
import { DEFAULT_AVATAR, DEFAULT_DISH_COVER, AD_UNIT_DETAIL_BANNER } from '../../../../common/constants/index'
import { KITCHEN_ROLE_ENUM } from '../../../../common/enums/index'
import { timeFormat } from '../../../../utils/index'

Page({
  behaviors: [storePageBehavior, sharePageBehavior],

  data: {
    DEFAULT_DISH_COVER,
    AD_UNIT_DETAIL_BANNER,
    loading: true,
    KITCHEN_ROLE_ENUM,
    dish: null,
    gallery: [],
    categoryName: '',
    rating: 0,
    foods: [],
    seasonings: [],
    steps: [],
    reviews: [],
    reviewTotal: 0,
    canPostOfficial: false,
  },

  async onLoad(options) {
    this.bindStore({ fields: ['kitchen', 'cart'], actions: ['addToCart'] })
    this.setData({ canPostOfficial: wx.canIUse('shareToOfficialAccount') })
    await wx.$onKitchenLoaded
    if (options.id) {
      await this._loadDishInfo(options.id)
      this._loadReviews(options.id)
      this._loadedId = options.id
    }
  },

  onShow() {
    // 首次进入由 onLoad 加载，从编辑页返回后只刷新菜谱主体数据
    if (this._loadedId && this.data.dish) {
      this._loadDishInfo(this._loadedId)
    }
  },

  async _loadDishInfo(id) {
    try {
      const r = await dishInfoApi({ id })
      const d = r.data
      if (!d) return

      // 查分类名
      let categoryName = ''
      if (d.categoryId) {
        const catRes = await categoryListApi()
        const cat = (catRes.data || []).find(c => c.id === d.categoryId)
        categoryName = cat ? cat.name : ''
      }

      const foods = (d.foods || []).map(f => ({
        name: f.foodName,
        amount: [f.amount, f.unit, f.process].filter(Boolean).join(' '),
      }))

      const seasonings = (d.seasonings || []).map(s => ({
        name: s.name,
        amount: [s.amount, s.unit].filter(Boolean).join(' '),
      }))

      const steps = (d.steps || [])
        .sort((a, b) => a.sort - b.sort)
        .map(s => s.description)

      // 轮播图：首图为封面，其后接详情图（过滤空值）；一张都没有则回退默认图
      const gallery = [d.cover, ...(d.images || [])].filter(Boolean)

      this.setData({
        loading: false,
        dish: {
          id: d.id,
          name: d.name,
          photo: d.cover || '',
          remark: d.remark || '',
          cookCount: d.cookCount || 0,
        },
        gallery: gallery.length ? gallery : [DEFAULT_DISH_COVER],
        categoryName,
        rating: d.rating || 0,
        foods,
        seasonings,
        steps,
      })
    } catch (e) {
      wx.$print('加载菜谱详情失败', e)
      this.setData({ loading: false })
    }
  },

  async _loadReviews(dishId) {
    try {
      const r = await cookReviewPageApi({ dishId, size: 3 })
      const reviews = (r.data?.list || []).map(item => ({
        rating: item.rating || 5,
        nickname: item.nickname || '',
        avatar: item.avatar || DEFAULT_AVATAR,
        content: item.content || '',
        time: timeFormat(item.createdAt),
      }))
      this.setData({ reviews, reviewTotal: r.data?.total || 0 })
    } catch (e) {
      wx.$print('加载评价失败', e)
    }
  },

  onViewAllReviews() {
    const { dish } = this.data
    wx.$navigateTo(`/subpackages/dish/pages/review/index?id=${ dish.id }`)
  },

  // 点击轮播图全屏预览（微信原生，整组可左右翻）；仅默认占位图时不触发
  onPreviewImage(e) {
    const urls = this.data.gallery
    // gallery 为单张且就是本地占位图时不预览（无封面但有详情图时仍可正常预览）
    if (!urls.length || (urls.length === 1 && urls[0] === DEFAULT_DISH_COVER)) return
    const url = e.currentTarget.dataset.url || urls[0]
    wx.previewImage({ urls, current: url })
  },

  onAddToCart() {
    const { dish } = this.data
    this.addToCart({ id: dish.id, name: dish.name, photo: dish.photo })
    wx.vibrateShort({ type: 'medium' })
    wx.$toast('已加入点餐')
    setTimeout(() => {
      wx.$navigateBack()
    }, 500)
  },

  onEditDish() {
    const { dish } = this.data
    wx.$navigateTo(`/subpackages/dish/pages/add-edit/index?id=${ dish.id }`)
  },

  // 晒到公众号贴图
  onPostToOfficial() {
    const { dish, foods, seasonings, steps } = this.data
    if (!dish) return
    this._postToOfficial({
      name: dish.name,
      foods,
      seasonings,
      steps,
      remark: dish.remark,
      cover: dish.photo,
    })
  },

  // 重写分享（菜谱专属卡片）
  onShareAppMessage() {
    const { dish } = this.data
    if (dish) {
      return this._recipeShare({
        id: dish.id,
        name: dish.name,
        cover: dish.photo,
      })
    }
    return this._baseShare()
  },
})
