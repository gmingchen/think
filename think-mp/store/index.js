import { observable } from 'mobx-miniprogram'

import system from './modules/system/index'
import auth from './modules/auth/index'
import user from './modules/user/index'
import tabbar from './modules/tabbar/index'

export const store = observable({
  ...system,
  ...auth,
  ...user,
  ...tabbar,
})