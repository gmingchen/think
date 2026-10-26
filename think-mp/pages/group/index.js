import { storePageBehavior, paginationBehavior, sharePageBehavior } from '../../behaviors/index'
import { grpPageApi } from '../../apis/index'
import { ROUTE, groupDetailUrl } from '../../common/constants/index'

Page({
  behaviors: [storePageBehavior, paginationBehavior('getData'), sharePageBehavior],
  data: {
    ROUTE,
  },

  onLoad() {
    this.bindStore({ fields: ['user', 'system', 'tabbar'] })
  },

  async onShow() {
    // 每次页面显示都重拉首页数据，保证从其它 tab 切回时列表为最新
    await wx.$onLogined
    this.reloadPageData()
  },

  async getData(params) {
    const r = await grpPageApi(params)
    this.pageDataHandler(r.data)
  },

  onTap(e) {
    const { id } = e.currentTarget.dataset
    wx.$navigateTo(groupDetailUrl(id))
  },

  // ── 创建群组（弹窗显隐与创建流程内聚于 create-popup 组件，新增完成后刷新列表）──
  onOpenCreate() {
    this.selectComponent('#createPopup').open()
  },
  // ── 邀请码入群（弹窗显隐与入群流程内聚于 join-popup 组件，成功后刷新列表）──
  onOpenJoin() {
    this.selectComponent('#joinPopup').open()
  },
  onConfirm() {
    this.reloadPageData()
  },
})
