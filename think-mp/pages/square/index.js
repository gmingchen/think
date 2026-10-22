import { storePageBehavior, paginationBehavior } from '../../behaviors/index'
import { decisionPagePlazaApi, decisionRandomPlazaApi, tagListApi } from '../../apis/index'
import { ROUTE, decisionDetailUrl } from '../../common/constants/index'

const TAG_ALL = 0 // 标签筛选「全部」哨兵值

Page({
  behaviors: [storePageBehavior, paginationBehavior('getData')],
  data: {
    ROUTE,
    loading: true,
    TAG_ALL,
    tags: [],
    activeTagId: TAG_ALL,
  },

  async onLoad() {
    this.bindStore({ fields: ['user', 'system', 'tabbar'] })
    await wx.$onLogined
    this.getTags()
    this.reloadPageData()
  },

  async getTags() {
    const r = await tagListApi()
    if (r.data) {
      this.setData({ tags: r.data })
    }
  },

  async getData(params) {
    const reqParams = { ...params }
    if (this.data.activeTagId !== TAG_ALL) {
      reqParams.tagId = this.data.activeTagId
    }
    const r = await decisionPagePlazaApi(reqParams)
    this.pageDataHandler(r.data)
    this.setData({ loading: false })
  },

  onSwitchTag(e) {
    const id = Number(e.currentTarget.dataset.id)
    if (id === this.data.activeTagId) return
    this.setData({ activeTagId: id, loading: true })
    this.reloadPageData()
  },

  onCardTap(e) {
    wx.$navigateTo(decisionDetailUrl(e.detail.id))
  },

  // 随机查看一条公开决策（换一面镜子照照别人）
  async onRandomTap() {
    const r = await decisionRandomPlazaApi()
    if (r.data && r.data.id) {
      wx.$navigateTo(decisionDetailUrl(r.data.id))
    }
  },

  onCreateTap() {
    wx.$navigateTo(ROUTE.DECISION_CREATE)
  },
})
