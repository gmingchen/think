import { action } from 'mobx-miniprogram'
import { authUserApi, userUpdateLastKitchenApi } from '../../../apis/index'

export default {
  user: {},
  getUser: action(async function() {
    const r = await authUserApi()
    if (r.data) {
      this.user = r.data
    }
  }),
  updateLastKitchenId: action(async function(kitchenId) {
    await userUpdateLastKitchenApi({ kitchenId })
    this.user = { ...this.user, lastKitchenId: kitchenId }
  })
}