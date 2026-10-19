import { storePageBehavior, sharePageBehavior } from '../../../../behaviors/index'
import { cookReviewPageApi } from '../../../../apis/index'
import { DEFAULT_AVATAR } from '../../../../common/constants/index'
import { timeFormat } from '../../../../utils/index'

Page({
  behaviors: [storePageBehavior, sharePageBehavior],

  data: {
    dishId: null,
    list: [],
    page: { current: 0, size: 20, total: 0 },
    loading: false,
    finished: false,
  },

  async onLoad(options) {
    await wx.$onLogined
    if (options.id) {
      this.setData({ dishId: Number(options.id) })
      this.loadPageData()
    }
  },

  async loadPageData() {
    const { list, page, finished, loading, dishId } = this.data
    if (finished || loading) return
    const nextPage = page.current + 1
    this.setData({ loading: true })
    try {
      const r = await cookReviewPageApi({ dishId, current: nextPage, size: page.size })
      const { list: apiList, total } = r.data || {}
      const records = (apiList || []).map(item => ({
        rating: item.rating || 5,
        nickname: item.nickname || '',
        avatar: item.avatar || DEFAULT_AVATAR,
        content: item.content || '',
        time: timeFormat(item.createdAt),
      }))
      const merged = nextPage === 1 ? records : [...list, ...records]
      this.setData({
        list: merged,
        'page.current': nextPage,
        'page.total': total,
        loading: false,
        finished: merged.length >= total,
      })
    } catch (e) {
      wx.$print('加载评价失败', e)
      this.setData({ loading: false })
    }
  },
})
