import { DEFAULT_DISH_COVER } from '../../../../../../common/constants/index'

Component({
  properties: {
    // RankedDish：dishId / dishName / dishCover / reason / haveFoods / lackFoods / first / meta
    dish: {
      type: Object,
      value: null,
      observer(val) {
        if (!val) return
        // 预拼食材文本，避免模板里写 join 逻辑
        this.setData({
          haveText: (val.haveFoods || []).join('、'),
          lackText: (val.lackFoods || []).join('、'),
        })
      },
    },
  },

  data: {
    DEFAULT_DISH_COVER,
    haveText: '',
    lackText: '',
  },

  methods: {
    onDetail() {
      this.triggerEvent('detail', { id: this.data.dish.dishId })
    },
    onCart() {
      this.triggerEvent('cart', { dish: this.data.dish })
    },
  },
})
