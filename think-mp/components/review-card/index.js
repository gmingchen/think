import dayjs from 'dayjs'

Component({
  properties: {
    // DecisionListItemVO（评议中列表项）：依赖 content/images/options/initiator/publishedAt
    item: {
      type: Object,
      value: {},
      observer: 'onItem',
    },
  },
  data: {
    percent: 0,
    // 人数不限（reviewLimit=0）：进度位改「已评 N 人 · 不限」
    countUnlimited: false,
    // 友好发布时间、详情是否需折叠、当前是否已展开
    timeText: '',
    contentLong: false,
    expanded: false,
    // 无头像时回退显示的昵称首字
    avatarChar: '',
    // 结束条件文案：满 N 人 / 还剩 相对剩余时间（任一满足即结束，用“或”连接）
    endText: '',
  },
  methods: {
    onItem(item) {
      if (!item) return
      const limit = item.reviewLimit || 0
      const count = item.reviewCount || 0
      const percent = limit ? Math.min(100, Math.round((count / limit) * 100)) : 0
      const nickname = (item.initiator && item.initiator.nickname) || ''
      this.setData({
        percent,
        countUnlimited: !limit,
        timeText: this._formatPublishTime(item.publishedAt || item.createdAt),
        contentLong: false,
        expanded: false,
        avatarChar: Array.from(nickname.trim())[0] || '',
        endText: this._buildEndText(limit, item.coolDownEndAt),
      }, () => this._measureContent())
    },
    // 折叠规则：不靠字数估算，而是渲染后量真实高度——
    // .e-2 钳制下 scrollHeight 拿不到全文高（部分基础库不支持该字段），改用 boundingClientRect 对比：
    // 隐藏的等宽不钳制克隆高度 > 被钳为两行的实际高度，才真的溢出，才显示「展开」。
    _measureContent() {
      const q = this.createSelectorQuery()
      q.select('.review-card__content').boundingClientRect()
      q.select('.review-card__measure').boundingClientRect()
      q.exec((res) => {
        const shown = res && res[0]
        const full = res && res[1]
        if (!shown || !full) return
        this.setData({ contentLong: full.height > shown.height + 1 })
      })
    },
    // 详情折叠/展开（catchtap 阻断冒泡，避免误触进入详情）
    onToggleExpand() {
      this.setData({ expanded: !this.data.expanded })
    },
    // 选项按钮 → 回抛评议事件（附完整 item，供居中弹层展示与卡片一致的内容）
    onOptionTap(e) {
      const { item } = this.data
      this.triggerEvent('review', {
        id: item.id,
        option: Number(e.currentTarget.dataset.index),
        options: item.options || [],
        item,
      })
    },
    // 缩略图点击就地预览（catchtap 已阻断冒泡，不会误触进入详情）
    onPreviewImage(e) {
      const { url } = e.currentTarget.dataset
      const urls = this.data.item.images || []
      if (!urls.length) return
      wx.previewImage({ current: url, urls })
    },
    // 相对时间：刚刚 / N分钟前 / N小时前 / 昨天 HH:mm / N天前 / YYYY年M月D日
    _formatPublishTime(time) {
      if (!time) return ''
      const target = dayjs(time)
      if (!target.isValid()) return ''
      const minutes = dayjs().diff(target, 'minute')
      if (minutes < 1) return '刚刚'
      if (minutes < 60) return `${minutes}分钟前`
      const hours = dayjs().diff(target, 'hour')
      if (hours < 24) return `${hours}小时前`
      const days = dayjs().diff(target, 'day')
      if (days === 1) return `昨天 ${target.format('HH:mm')}`
      if (days < 30) return `${days}天前`
      return target.format('YYYY年M月D日')
    },
    onTap() {
      this.triggerEvent('click', { id: this.data.item.id })
    },
    // 举报按钮 → 回抛 report 事件，由上层弹举报弹层
    onReportTap() {
      this.triggerEvent('report', { id: this.data.item.id })
    },
    // 结束条件：人数上限 + 相对剩余时长，缺哪个省哪个（均不限时为空），结尾接“后结束”
    _buildEndText(limit, coolDownEndAt) {
      const parts = []
      if (limit > 0) parts.push(`满 ${limit} 人`)
      const remain = this._formatRemaining(coolDownEndAt)
      if (remain) parts.push(remain)
      return parts.join(' 或 ')
    },
    // 相对剩余时长（不带“还剩”）：N分钟 / N小时N分 / N天N小时；无或已过截止返回空串
    _formatRemaining(time) {
      if (!time) return ''
      const end = dayjs(time)
      if (!end.isValid()) return ''
      const diff = end.diff(dayjs(), 'minute')
      if (diff <= 0) return ''
      if (diff < 60) return `${diff}分钟`
      const hours = Math.floor(diff / 60)
      const mins = diff % 60
      if (hours < 24) return mins ? `${hours}小时${mins}分` : `${hours}小时`
      const days = Math.floor(hours / 24)
      const remH = hours % 24
      return remH ? `${days}天${remH}小时` : `${days}天`
    },
  },
})
