import { storePageBehavior, sharePageBehavior } from '../../../../behaviors/index'
import { KITCHEN_ROLE_LIST } from '../../../../common/enums/index'
import { AD_UNIT_KITCHEN_LIST_BANNER } from '../../../../common/constants/index'

Page({
  behaviors: [storePageBehavior, sharePageBehavior],
  data: {
    loading: true,
    KITCHEN_ROLE_LIST,
    AD_UNIT_KITCHEN_LIST_BANNER,
  },
  async onLoad() {
    await wx.$onLogined
    this.bindStore({ fields: ['kitchen'] })
    // 等待数据加载
    setTimeout(() => {
      this.setData({ loading: false })
    }, 300)
  },
  onTap(e) {
    const { id } = e.currentTarget.dataset
    wx.$navigateTo(`/subpackages/kitchen/pages/settings/index?id=${id}`)
  },
})
