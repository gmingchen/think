Component({
  properties: {
    // DecisionListItemVO（紧凑卡：仅标题 + 状态徽章 + 进度）
    item: {
      type: Object,
      value: {},
      observer: 'onItem',
    },
    // 是否展示状态徽章（我的列表需区分草稿/已截止/已复盘，默认展示）
    showStatus: {
      type: Boolean,
      value: true,
    },
  },
  data: {
    percent: 0,
    // 人数不限（reviewLimit=0）：进度位改「已评 N 人 · 不限」
    countUnlimited: false,
  },
  methods: {
    onItem(item) {
      if (!item) return
      const limit = item.reviewLimit || 0
      const count = item.reviewCount || 0
      const percent = limit ? Math.min(100, Math.round((count / limit) * 100)) : 0
      this.setData({
        percent,
        countUnlimited: !limit,
      })
    },
    onTap() {
      this.triggerEvent('click', { id: this.data.item.id })
    },
  },
})
