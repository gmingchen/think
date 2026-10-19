export const toast = (title) => {
  wx.showToast({ title, icon: 'none', mask: false, duration: 2000 })
}
