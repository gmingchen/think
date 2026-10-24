import { storePageBehavior } from '../../../../behaviors/index'
import { decisionDetailApi } from '../../../../apis/index'

Page({
  behaviors: [storePageBehavior],
  data: {
    id: null,
    loading: true,
    data: {},
    report: {},
    total: 0,
    rows: [],
  },

  async onLoad(options) {
    this.bindStore({ fields: ['system'] })
    await wx.$onLogined
    this.setData({ id: options.id })
    await this.getData()
  },

  async getData() {
    const r = await decisionDetailApi({ id: this.data.id })
    const data = r.data || {}
    const report = data.report || {}
    const total = report.totalReview || 0
    const counts = report.optionCounts || {}
    // optionCounts 为下标->计数（Map key 可能是字符串），统一按 options 顺序对齐
    const rows = (data.options || []).map((o, i) => {
      const count = counts[i] != null ? counts[i] : (counts[String(i)] || 0)
      return {
        label: o,
        count,
        percent: total ? Math.round((count / total) * 100) : 0,
      }
    })
    // 最高票选项高亮（印章红），供报告页对齐 UI「最高建议」标记
    let topIndex = -1
    rows.forEach((row, i) => {
      if (row.count > 0 && (topIndex === -1 || row.count > rows[topIndex].count)) topIndex = i
    })
    rows.forEach((row, i) => { row.isTop = i === topIndex })
    this.setData({ loading: false, data, report, total, rows })
  },

  onBackToDetail() {
    wx.$navigateBack()
  },
})
