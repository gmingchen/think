import { action } from 'mobx-miniprogram'
import { TABBAR_ENUM, TABBAR_LIST } from '../../../common/enums/index'
import { KITCHEN_ROLE_ENUM } from '../../../common/enums/index'

const CHEF_TABS = {
  [TABBAR_ENUM.RECIPE]: { label: '菜谱', emoji: '📖' },
  [TABBAR_ENUM.COOK]: { label: '下厨', emoji: '🍳' },
}
const GUEST_TABS = {
  [TABBAR_ENUM.RECIPE]: { label: '点餐', emoji: '🍖' },
  [TABBAR_ENUM.COOK]: { label: '上桌', emoji: '🔔' },
}

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
  updateRoleTabs: action(function(role) {
    const isGuest = role === KITCHEN_ROLE_ENUM.GUEST
    const roleTabs = isGuest ? GUEST_TABS : CHEF_TABS
    const list = TABBAR_LIST.map(item => {
      const override = roleTabs[item.value]
      return override ? { ...item, label: override.label, emoji: override.emoji } : { ...item }
    })
    this.tabbar = {
      ...this.tabbar,
      list,
    }
  }),
}