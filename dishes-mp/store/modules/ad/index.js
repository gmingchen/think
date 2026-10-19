import { action } from 'mobx-miniprogram'

export default {
  ad: {
    // 广告总开关：控制所有广告组件是否显示，关闭后广告组件整体不渲染
    enabled: true,
  },
  // 设置广告总开关
  setAdEnabled: action(function(enabled) {
    this.ad = { ...this.ad, enabled }
  }),
}
