import { storeComponentBehavior } from '../../../../behaviors/index'

Component({
  behaviors: [
    storeComponentBehavior({
      fields: ['ad'],
    })
  ],
  data: {
    list: [
      // 看广告支持一下：暂时下线，保留注释不删除
      // { label: '📺 看广告支持一下', type: 'reward' },
      // 「商户合作」为 v13 需求验证入口（非家庭场景能力），放「推荐给朋友」上方：
      // 与菜单区常驻功能（厨房/分类/成员等）不同，它不占业务菜单坑位，更适合归入这一组低频入口
      // 文案故意不用「商户合作」这种 B 端说法，直接点名身份，让没意识到自己是目标用户的开店/摆摊用户也能对号入座
      { label: '💼 开店/摆摊？点这里提需求', url: '/subpackages/feedback/pages/merchant/index' },
      { label: '🌿 推荐给朋友', type: 'share' },
      { label: '📖 关于厨菜记', url: '/subpackages/user/pages/about/index' },
    ],
  },

  methods: {
    onTap(e) {
      const { url } = e.currentTarget.dataset
      wx.$navigateTo(url)
    },
    // 激励视频入口：逻辑在页面侧，组件只抛出事件
    onReward() {
      this.triggerEvent('reward')
    },
  },
})
