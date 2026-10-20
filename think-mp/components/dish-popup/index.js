import { dishInfoApi } from '../../apis/index'
import { sharePageBehavior } from '../../behaviors/index'
import { DEFAULT_DISH_COVER } from '../../common/constants/index'

Component({
  // 混入分享行为，复用 _postToOfficial 贴图能力
  behaviors: [sharePageBehavior],

  data: {
    DEFAULT_DISH_COVER,
    visible: false,
    loading: false,
    dish: null,
    gallery: [],
    isDefaultCover: false,
    useAdd: true,
    canPostOfficial: false,
  },

  methods: {
    async open(id, useAdd = true) {
      this.setData({ useAdd, canPostOfficial: wx.canIUse('shareToOfficialAccount') })
      await this._loadDishInfo(id)
      this.setData({ visible: true })
    },

    close() {
      this.setData({ visible: false })
    },

    async _loadDishInfo(id) {
      this.setData({ loading: true, dish: null })
      try {
        const r = await dishInfoApi({ id })
        if (r.data) {
          const d = r.data
          // 合并食材和调料为用料列表
          const ingredients = [
            ...(d.foods || []).map(f => `${f.foodName} ${f.amount || ''}${f.unit || ''}`.trim()),
            ...(d.seasonings || []).map(s => `${s.name} ${s.amount || ''}${s.unit || ''}`.trim()),
          ]
          // 步骤描述列表
          const steps = (d.steps || []).sort((a, b) => a.sort - b.sort).map(s => s.description)
          // 预览图集：首图=封面，其后接详情图（过滤空值）；一张都没有则回退默认图
          const gallery = [d.cover, ...(d.images || [])].filter(Boolean)

          this.setData({
            loading: false,
            gallery: gallery.length ? gallery : [DEFAULT_DISH_COVER],
            // 一张真实图都没有（无封面且无详情图）：缩略图显示默认图且不可预览
            isDefaultCover: gallery.length === 0,
            dish: {
              id: d.id,
              name: d.name,
              photo: d.cover,
              remark: d.remark || '',
              cookCount: d.cookCount || 0,
              rating: d.rating || 0,
              ingredients,
              steps,
            }
          })
        } else {
          this.setData({ loading: false })
        }
      } catch (e) {
        console.error('load dish info failed:', e)
        this.setData({ loading: false })
      }
    },

    onClose() {
      this.close()
    },

    onAdd() {
      this.triggerEvent('add', { id: this.data.dish?.id })
    },

    onDetail() {
      this.triggerEvent('detail', { id: this.data.dish?.id })
    },

    // 点击封面缩略图：全屏预览整组图（封面+详情图，原生可左右翻）；仅默认占位图时不触发
    onPreviewCover() {
      if (this.data.isDefaultCover) return
      const urls = this.data.gallery
      wx.previewImage({ urls, current: urls[0] })
    },

    // 贴图：弹窗的 ingredients 已合并为整段文字，直接作为食材行预填
    onSticker() {
      const { dish } = this.data
      if (!dish) return
      this._postToOfficial({
        name: dish.name,
        foods: dish.ingredients.map(name => ({ name, amount: '' })),
        steps: dish.steps,
        remark: dish.remark,
        cover: dish.photo,
      })
    },
  }
})
