import { storeComponentBehavior } from '../../behaviors/index'

Component({
  behaviors: [
    storeComponentBehavior({
      // member：v12 会员免广告位的唯一 Banner 收口点，本组件被 21 个页面引用，
      // 在这里判一次即可全站生效，不要回到各页面各自叠判据（§10.2）
      fields: ['ad', 'member'],
    })
  ],
  properties: {
    // 广告位 ID（流量主后台创建）
    adUnitId: {
      type: String,
      value: '',
    },
    // 透传给内部原生 <ad> 组件的样式类，方便外部页面控制布局
    cls: {
      type: String,
      value: '',
    },
  },
  data: {
    // 广告实际加载成功后才显示，避免无填充时留白块
    visible: false,
  },
  methods: {
    onLoad(e) {
      this.setData({ visible: true })
      this.triggerEvent('on-load', e.detail)
    },
    onError(e) {
      this.setData({ visible: false })
      this.triggerEvent('on-error', e.detail)
    },
  },
})
