import { storePageBehavior, sharePageBehavior } from '../../../../behaviors/index'

Page({
  behaviors: [storePageBehavior, sharePageBehavior],
  data: {},
  onLoad() {
    this.bindStore({ fields: ['system'] })
  },
})
