import { storePageBehavior, sharePageBehavior } from '../../../../behaviors/index'

Page({
  behaviors: [storePageBehavior, sharePageBehavior],
  data: {
  },
  onLoad() {
    this.bindStore({ fields: ['system'] })
  },
  onAgreement() {
    wx.$navigateTo('/subpackages/user/pages/agreement/index')
  },
  onPrivacy() {
    wx.$navigateTo('/subpackages/user/pages/privacy/index')
  },
})
