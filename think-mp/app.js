import { store } from './store/index'
import { navigateTo, navigateBack, navigateBackTo, toast, parseQuery } from './utils/index';
import { confirm } from './utils/wx-api';
import { showRewardedAd } from './utils/ad';
import * as print from './utils/print';

// 导航跳转
wx.$navigateTo = navigateTo
wx.$navigateBack = navigateBack
wx.$navigateBackTo = navigateBackTo

// 轻提示
wx.$toast = toast

// 确认弹窗
wx.$confirm = confirm

// 激励广告
wx.$showRewardedAd = showRewardedAd

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

// 厨房-监听阻塞
wx.$onKitchenLoaded = new Promise(resolve => {
  wx.$kitchenLoaded = resolve;
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
    // 获取用户信息（需要 lastKitchenId 来恢复厨房选择）
    await store.getUser()
    store.getKitchen()
    // 会员状态：与厨房并行、不 await。它只决定「少不少一层权益」，不决定页面能不能渲染，
    // 拿它去堵首屏不值；失败时 store 自己按非会员降级（§10.2 / §17 第 10 条），
    // 而「我的」页会员卡靠 mineCard 为空停在骨架，不会闪一张「未开通」卡
    store.getMember()
  },
})
