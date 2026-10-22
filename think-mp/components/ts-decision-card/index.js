import { DECISION_STATUS_ENUM } from '../../common/enums/index'

Component({
  properties: {
    // DecisionListItemVO；后端补充 content摘要/images/tags 时自动渲染（wx:if 防御）
    item: {
      type: Object,
      value: {},
      observer: 'onItem',
    },
    // 是否在卡片底部展示「冷静期截止」倒计时位（广场/群内展示，个人归档可不展）
    showCountdown: {
      type: Boolean,
      value: true,
    },
  },
  data: {
    percent: 0,
    reviewing: false,
  },
  methods: {
    onItem(item) {
      if (!item) return
      const limit = item.reviewLimit || 0
      const count = item.reviewCount || 0
      const percent = limit ? Math.min(100, Math.round((count / limit) * 100)) : 0
      this.setData({
        percent,
        reviewing: item.status === DECISION_STATUS_ENUM.REVIEWING,
      })
    },
    onTap() {
      this.triggerEvent('click', { id: this.data.item.id })
    },
  },
})
