import { store } from './store/index'
import { navigateTo, navigateBack, navigateBackTo, redirectTo, toast, parseQuery } from './utils/index';
import { confirm } from './utils/wx-api';
import * as print from './utils/print';

// 导航跳转
wx.$navigateTo = navigateTo
wx.$navigateBack = navigateBack
wx.$navigateBackTo = navigateBackTo
wx.$redirectTo = redirectTo

// 轻提示
wx.$toast = toast

// 确认弹窗
wx.$confirm = confirm

// 日志打印 (全局注册)
for (const key in print) {
  if (Object.hasOwnProperty.call(print, key)) {
    const element = print[key];
    wx[`$${ key }`] = element
  }
}

// 登录-监听阻塞
wx.$onLogined = new Promise(resolve => {
  wx.$logined = resolve;
});

App({
  async onLaunch(options) {
    console.log('onLaunch options:', options);
    // 获取小程序系统信息
    store.getSystemInfo()
    // 保存场景值
    store.setScene(options?.scene || 0)
    // 朋友圈单页模式（scene=1154）不需要登录
    if (options?.scene === 1154) return
    // 邀请人归因两条入口：分享卡片参数在 query.inviterId，扫小程序码参数在 query.scene（形如 inviterId%3D1，需解码）
    // 必须走 parseQuery：它同时兼容两种入口，直接读 query.inviterId 会漏掉扫码这一路
    const { inviterId = '' } = parseQuery(options?.query || {})
    // 登录
    await store.login(inviterId)
    // 获取用户信息
    await store.getUser()
  },
})
