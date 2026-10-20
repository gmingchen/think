import { storePageBehavior, sharePageBehavior } from '../../../../behaviors/index'
import { playExpandAd, buildCapacity } from '../../../../utils/index'
import { KITCHEN_ROLE_LIST } from '../../../../common/enums/index'
import { kitchenInfoApi, kitchenStatsApi, kitchenLeaveApi, kitchenExpandDishApi, kitchenExpandCategoryApi } from '../../../../apis/index'
import { AD_UNIT_KITCHEN_SETTINGS_CUSTOM, VIP_PAGE } from '../../../../common/constants/index'
import dayjs from 'dayjs'

// 六格资产：厨房维度统计，全部来自 /kitchen/stats
// 刻意不带上限（不显示 x/max）——「离上限还有多远」由下方扩容卡独占，避免同一页两处讲同一个数字
const STATS = [
  { key: 'dishCount', label: '记录菜谱' },
  { key: 'categoryCount', label: '菜谱分类' },
  { key: 'memberCount', label: '围坐亲朋' },
  { key: 'cookOrderCount', label: '温馨下厨' },
  { key: 'foodCount', label: '储备食材' },
  { key: 'seasoningCount', label: '秘制调料' },
]

Page({
  behaviors: [storePageBehavior, sharePageBehavior],
  data: {
    id: null,
    KITCHEN_ROLE_LIST,
    target: null,
    stats: null,
    statsList: STATS.map(item => ({ label: item.label, value: 0 })),
    plateCount: 0,
    memberCount: 0,
    createdDate: '',
    dish: null,
    category: null,
    loading: true,
    expanding: false,
    AD_UNIT_KITCHEN_SETTINGS_CUSTOM,
  },
  async onLoad(options) {
    this.bindStore({ fields: ['user', 'kitchen', 'ad'], actions: ['getKitchen'] })
    await wx.$onLogined
    const { id } = options
    if (id) {
      this.setData({ id: Number(id) })
      // 两个数据源并行拉取，任一先回都能渲染出完整卡片（render 内做了空值兜底）
      await Promise.all([this.getInfo(), this.getStats()]).catch(() => {})
    }
    this.setData({ loading: false })
  },
  async getInfo() {
    const r = await kitchenInfoApi({ kitchenId: this.data.id })
    if (r.data) {
      this.setData({ target: r.data })
      this.render()
    }
  },
  async getStats() {
    const r = await kitchenStatsApi({ kitchenId: this.data.id })
    if (r.data) {
      this.setData({ stats: r.data })
      this.render()
    }
  },
  // 统一渲染：target（名称/角色/上限）与 stats（六格/当前数）任一到位都刷新本页展示
  // 容量行的「当前数」优先取 stats，与六格同源；stats 未回时退回 target 同名字段（后端两处统计口径一致）
  render() {
    const { target, stats } = this.data
    if (!target) return
    const cur = key => (stats && stats[key] != null ? stats[key] : (target[key] || 0))
    this.setData({
      statsList: STATS.map(item => ({ label: item.label, value: (stats && stats[item.key]) || 0 })),
      plateCount: (stats && stats.cookOrderDishCount) || 0,
      memberCount: (stats && stats.memberCount) || target.memberCount || 0,
      createdDate: target.createdAt ? dayjs(target.createdAt).format('YYYY-MM-DD') : '',
      // unlimited 取 target（KitchenVO）的厨房级标记，它已含 owner 判定，前端零推导（§6.2）
      dish: buildCapacity(cur('dishCount'), target.maxDishCount, target.unlimitedCapacity),
      category: buildCapacity(cur('categoryCount'), target.maxCategoryCount, target.unlimitedCapacity),
    })
  },
  onKitchenName() {
    this.selectComponent('#namePopup').open({
      id: this.data.target.id,
      name: this.data.target.name,
    })
  },
  async onConfirm() {
    await this.getInfo()
    await this.getKitchen()
  },
  onMember() {
    wx.$navigateTo(`/subpackages/kitchen/pages/member/index?id=${this.data.id}`)
  },
  async onExitKitchen() {
    const ok = await wx.$confirm('确认退出该厨房？', '退出厨房')
    if (!ok) return
    await kitchenLeaveApi({ kitchenId: this.data.id })
    wx.$toast('已退出厨房')
    await this.getKitchen()
    wx.$navigateBack()
  },
  // 会员引导行（§4 入口 4：二档转化位）
  onGoVip() {
    wx.$navigateTo(VIP_PAGE)
  },
  // 菜谱扩容：用户主动拉起激励视频，完整观看后调 /kitchen/expand-dish
  onExpandDish() {
    if (this.data.expanding) return
    this.setData({ expanding: true })
    playExpandAd('dish', ({ ended }) => this.onExpandSettled('dish', ended))
  },
  // 分类扩容：用户主动拉起激励视频，完整观看后调 /kitchen/expand-category
  onExpandCategory() {
    if (this.data.expanding) return
    this.setData({ expanding: true })
    playExpandAd('category', ({ ended }) => this.onExpandSettled('category', ended))
  },
  // 扩容广告结算：仅完整观看(ended)才真正调接口 +1，并刷新本页与全局厨房上限
  async onExpandSettled(type, ended) {
    this.setData({ expanding: false })
    if (!ended) return
    const api = type === 'dish' ? kitchenExpandDishApi : kitchenExpandCategoryApi
    try {
      const r = await api({ kitchenId: this.data.id })
      // 返回的是上限 +1 后的厨房主数据；六格数字不随扩容变化，无需重取 stats
      if (r.data) {
        this.setData({ target: r.data })
        this.render()
      }
      this.getKitchen()
      wx.$toast('已扩容 +1')
    } catch (e) {
      // service 层已统一提示，此处静默
    }
  },
})
