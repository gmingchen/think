import { TABBAR_LIST } from '../common/enums/index'

/**
 * 页面返回
 */
export const navigateBack = () => {
  const pages = getCurrentPages();
  if (pages.length > 1) {
    wx.navigateBack()
  } else {
    const { url } = TABBAR_LIST[0]
    wx.switchTab({ url })
    // wx.redirectTo({ url })
  }
}

/**
 * 页面跳转
 * @param {*} target 
 * @returns 
 */
export const navigateTo = (target) => {
  if (!target) {
    return
  }
  const exist = TABBAR_LIST.some(item => item.url === target)
  if (exist) {
    wx.switchTab({ url: target })
  } else {
    wx.navigateTo({ url: target })
  }
}

/**
 * 返回目标页
 * @param {*} target 目标页路径
 */
export const navigateBackTo = (target) => {
  if(!target) {
    navigateBack()
    return
  }

  const pages = getCurrentPages();
  if (pages.length < 2) {
    navigateTo(target)
    return
  }

  const url = `/${ pages[pages.length - 2].route }`;
  if (url === target) {
    navigateBack()
  } else {
    navigateTo(target)
  }
}
