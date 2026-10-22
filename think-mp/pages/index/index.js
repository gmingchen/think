import { decisionStatsMineApi } from '../../apis/index'

// reviewAvgRate：结束评议（已截止+已复盘）单据的平均评议完成率，后端无此类单据时返 null，由 wxml 兜 0
const DEFAULT_STATS = { createdCount: 0, reviewedCount: 0, receivedCount: 0, recapCount: 0, reviewAvgRate: 0 }

Page({
  data: {
    stats: { ...DEFAULT_STATS },
  },

  async onShow() {
    await wx.$onLogined
    this.getData()
  },

  async getData() {
    const r = await decisionStatsMineApi()
    if (r.data) {
      this.setData({ stats: r.data })
    }
  },
})
