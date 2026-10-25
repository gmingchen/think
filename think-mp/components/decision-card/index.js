import dayjs from 'dayjs'
import { DECISION_STATUS_ENUM, decisionStatusList } from '../../common/enums/index'

Component({
  properties: {
    // DecisionListItemVO（列表卡：徽章+倒计时 / 标题 / 摘要 / 缩略图 / 进度）
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
    // 徽章有可显示文案时才占位（与 status-badge 内部判空一致，避免头部空徽章间距）
    badgeLabel: '',
    isDraft: false,
    // 评议中剩余倒计时（渲染时静态计算，HH:mm:ss）
    countdownText: '',
    // 缩略图（至多 3 张）
    thumbs: [],
  },
  methods: {
    onItem(item) {
      if (!item) return
      const limit = item.reviewLimit || 0
      const count = item.reviewCount || 0
      const percent = limit ? Math.min(100, Math.round((count / limit) * 100)) : 0
      const statusItem = decisionStatusList.find(i => i.value === item.status)
      this.setData({
        percent,
        countUnlimited: !limit,
        badgeLabel: statusItem ? statusItem.label : '',
        isDraft: item.status === DECISION_STATUS_ENUM.DRAFT,
        countdownText: this._buildCountdown(item),
        thumbs: (item.images || []).slice(0, 3),
      })
    },
    // 倒计时：仅评议中且有截止时间时显示；已到点显示「即将截止」
    _buildCountdown(item) {
      if (item.status !== DECISION_STATUS_ENUM.REVIEWING || !item.coolDownEndAt) return ''
      const end = dayjs(item.coolDownEndAt)
      if (!end.isValid()) return ''
      const diff = end.diff(dayjs(), 'second')
      if (diff <= 0) return '即将截止'
      const h = Math.floor(diff / 3600)
      const m = Math.floor((diff % 3600) / 60)
      const s = diff % 60
      const pad = n => (n < 10 ? `0${n}` : `${n}`)
      return `倒计时 ${ pad(h) }:${ pad(m) }:${ pad(s) }`
    },
    // 缩略图点击就地预览（catchtap 阻断冒泡，不误触进详情；可滑看全部图片）
    onPreviewImage(e) {
      const { url } = e.currentTarget.dataset
      const urls = this.data.item.images || []
      if (!urls.length) return
      wx.previewImage({ current: url, urls })
    },
    onTap() {
      this.triggerEvent('click', { id: this.data.item.id })
    },
  },
})
