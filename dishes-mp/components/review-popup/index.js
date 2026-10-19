import { cookOrderInfoApi, cookReviewCreateApi } from '../../apis/index'
import { DEFAULT_DISH_COVER } from '../../common/constants/index'

// 默认评价内容（按评分匹配）
const DEFAULT_REVIEWS = {
  5: '味道超赞，必须点赞！',
  4: '味道不错，很好吃~',
  3: '味道还行，继续加油！',
  2: '一般般，还有提升空间！',
  1: '需要改进哦~',
}

Component({
  data: {
    DEFAULT_DISH_COVER,
    visible: false,
    loading: false,
    cookOrderId: null,
    dishes: [],
  },

  methods: {
    async open(orderId) {
      this.setData({ visible: true, loading: true, cookOrderId: orderId, dishes: [] })
      try {
        const r = await cookOrderInfoApi({ id: orderId })
        if (r.data) {
          const dishes = (r.data.dishes || []).map(d => ({
            dishId: d.dishId,
            name: d.dishName || '',
            cover: d.dishCover || '',
            quantity: d.quantity || 1,
            rating: 5,
            content: '',
          }))
          this.setData({ dishes, loading: false })
        } else {
          this.setData({ loading: false })
        }
      } catch (e) {
        this.setData({ loading: false })
        this.close()
      }
    },

    close() {
      this.setData({ visible: false, cookOrderId: null, dishes: [] })
    },

    onRating(e) {
      const dishIndex = e.currentTarget.dataset.dishIndex
      this.setData({ [`dishes[${dishIndex}].rating`]: e.detail })
    },

    onContentInput(e) {
      const { dishIndex } = e.currentTarget.dataset
      this.setData({ [`dishes[${dishIndex}].content`]: e.detail.value })
    },

    async onSubmit() {
      const { cookOrderId, dishes } = this.data
      if (!dishes.length) return
      await cookReviewCreateApi({
        cookOrderId,
        dishes: dishes.map(d => ({
          dishId: d.dishId,
          rating: d.rating,
          content: d.content || DEFAULT_REVIEWS[d.rating] || '好评！',
        })),
      })
      wx.$toast('评价成功 🎉')
      this.triggerEvent('success')
      this.close()
    },
  }
})
