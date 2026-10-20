import { action } from 'mobx-miniprogram'

export default {
  system: {
    appId: '', // APPID
    envVersion: '', // 小程序版本
    version: '',
    scene: 0, // 场景值

    brand: '', // 设备品牌
    model: '', // 设备型号
    system: '', // 操作系统及版本
    platform: '', // 客户端平台
  },
  // 获取小程序系统信息
  getSystemInfo: action(function() {
    const { miniProgram } = wx.getAccountInfoSync()
    const deviceInfo = wx.getDeviceInfo()
    wx.$printDebug('getAccountInfoSync', miniProgram)
    wx.$printDebug('getDeviceInfo', deviceInfo)
    this.system = { ...this.system, ...miniProgram, ...deviceInfo }
  }),
  // 设置场景值
  setScene: action(function(scene) {
    this.system = { ...this.system, scene }
  }),
}
