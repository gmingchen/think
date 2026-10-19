import { observable } from 'mobx-miniprogram'

import system from './modules/system/index'
import auth from './modules/auth/index'
import user from './modules/user/index'
import kitchen from './modules/kitchen/index'
import member from './modules/member/index'
import tabbar from './modules/tabbar/index'
import cart from './modules/cart/index'
import poster from './modules/poster/index'
import ad from './modules/ad/index'

export const store = observable({
  ...system,
  ...auth,
  ...user,
  ...kitchen,
  ...member,
  ...tabbar,
  ...cart,
  ...poster,
  ...ad,
})