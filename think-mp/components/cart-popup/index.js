import { storeComponentBehavior } from '../../behaviors/store/index'
import { DEFAULT_DISH_COVER } from '../../common/constants/index'

Component({
  behaviors: [storeComponentBehavior({ fields: ['cart'], actions: ['addToCart', 'removeFromCart'] })],

  data: {
    DEFAULT_DISH_COVER,
    visible: false,
  },

  methods: {
    open() {
      this.setData({ visible: true })
    },

    close() {
      this.setData({ visible: false })
    },

    onStepperChange(e) {
      const { id } = e.currentTarget.dataset
      const { value } = e.detail
      const item = this.data.cart.list.find(i => i.dishId === id)
      if (!item) return
      
      if (value > item.quantity) {
        this.addToCart({ id })
      } else if (value < item.quantity) {
        this.removeFromCart(id)
      }
    },

    onConfirm() {
      this.triggerEvent('confirm')
      this.close()
    },

    // 采购清单：篮子菜按 quantity 展开成重复 dishId（重复次数即份数），弹窗不关，返回时仍在
    onGoShopping() {
      const dishIds = []
      this.data.cart.list.forEach(item => {
        for (let i = 0; i < item.quantity; i++) {
          dishIds.push(item.dishId)
        }
      })
      if (!dishIds.length) return
      wx.$navigateTo(`/subpackages/dish/pages/shopping/index?dishIds=${dishIds.join(',')}&source=cart`)
    },

    onClose() {
      this.close()
    }
  }
})
