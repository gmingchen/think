import { DECISION_STATUS_ENUM, decisionStatusList } from '../../common/enums/index'

Component({
  properties: {
    status: {
      type: Number,
      value: -1,
      observer: 'update',
    },
  },
  data: {
    label: '',
    // 徽章形态：seal 印章红实心 / brand 品牌深蓝实心 / tag 浅蓝常规标签
    variant: 'tag',
  },
  methods: {
    update(status) {
      const item = decisionStatusList.find(i => i.value === status)
      let variant = 'tag'
      // 评议中=印章红实心；已关闭=品牌深蓝实心；其余（草稿/待审/已截止/已复盘）=浅蓝常规标签
      if (status === DECISION_STATUS_ENUM.REVIEWING) {
        variant = 'seal'
      } else if (status === DECISION_STATUS_ENUM.CLOSED) {
        variant = 'brand'
      }
      this.setData({
        label: item ? item.label : '',
        variant,
      })
    },
  },
})
