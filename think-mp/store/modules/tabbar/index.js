import { action } from 'mobx-miniprogram'
import { TABBAR_ENUM, TABBAR_LIST } from '../../../common/enums/index'

export default {
  tabbar: {
    active: TABBAR_ENUM.HOME,
    list: [...TABBAR_LIST]
  },
  updateActive: action(function({ value, url }) {
    const tab = this.tabbar.list.find(item => item.value === value || item.url === url)
    if (tab) {
      const { value } = tab
      this.tabbar = {
        ...this.tabbar,
        active: value,
      }
    }
    return tab
  }),
  switchTabbar: action(function({ value, url }) {
    const tab = this.updateActive({ value, url })
    if (tab) {
      wx.switchTab({ url: tab.url })
    }
  }),
}
