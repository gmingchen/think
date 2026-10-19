import { storePageBehavior, sharePageBehavior } from '../../../../behaviors/index'
import { AD_UNIT_ABOUT_BANNER } from '../../../../common/constants/index'

Page({
  behaviors: [storePageBehavior, sharePageBehavior],
  data: {
    AD_UNIT_ABOUT_BANNER,
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
