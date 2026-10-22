import { action } from 'mobx-miniprogram'
import { authUserApi } from '../../../apis/index'

export default {
  user: {},
  getUser: action(async function() {
    const r = await authUserApi()
    if (r.data) {
      this.user = r.data
    }
  }),
}