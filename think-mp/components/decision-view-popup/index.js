import dayjs from 'dayjs'

Component({
  properties: {
    // 弹窗标题：默认「随便看看」
    title: {
      type: String,
      value: '随便看看',
    },
    show: {
      type: Boolean,
      value: false,
      observer: 'onShowChange',
    },
    // 决策单完整信息：标题/详情/图片/进度/发起人，供居中弹层展示
    item: {
      type: Object,
      value: {},
      observer: 'onItem',
    },
    // 选择项数组：[string]（方案文案）
    options: {
      type: Array,
      value: [],
    },
  },
  data: {
    // 主体区派生字段：进度、人数不限、友好发布时间、无头像回退首字
    percent: 0,
    countUnlimited: false,
    timeText: '',
    avatarChar: '',
    // 挂载与关闭动画：visible 控制是否渲染，closing 控制淡出动画类
    visible: false,
    closing: false,
  },
  lifetimes: {
    detached() {
      if (this._closeTimer) {
        clearTimeout(this._closeTimer)
        this._closeTimer = null
      }
    },
  },
  methods: {
    onShowChange(show) {
      // show 变化：打开立即挂载；关闭先播淡出动画再延迟卸载，避免直接消失
      if (this._closeTimer) {
        clearTimeout(this._closeTimer)
        this._closeTimer = null
      }
      if (show) {
        this.setData({ visible: true, closing: false })
      } else if (this.data.visible) {
        this.setData({ closing: true })
        this._closeTimer = setTimeout(() => {
          this._closeTimer = null
          this.setData({ visible: false, closing: false })
        }, 240)
      }
    },
    onItem(item) {
      if (!item) return
      const limit = item.reviewLimit || 0
      const count = item.reviewCount || 0
      const nickname = (item.initiator && item.initiator.nickname) || ''
      this.setData({
        percent: limit ? Math.min(100, Math.round((count / limit) * 100)) : 0,
        countUnlimited: !limit,
        timeText: this._formatPublishTime(item.publishedAt || item.createdAt),
        avatarChar: Array.from(nickname.trim())[0] || '',
      })
    },
    // 点选项 → 回抛 review（附完整 item/options），由上层打开评议表单弹窗
    onOptionTap(e) {
      const index = Number(e.currentTarget.dataset.index)
      const { item } = this.data
      this.triggerEvent('review', { id: item.id, option: index, options: this.data.options, item })
    },
    // 主体区缩略图点击就地预览（catchtap 已阻断冒泡）
    onPreviewImage(e) {
      const { url } = e.currentTarget.dataset
      const urls = (this.data.item && this.data.item.images) || []
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
    onClose() {
      this.triggerEvent('close')
    },
    // 空方法：面板 catchtap / 遮罩 catch:touchmove 消费手势，防误触关闭与背面滑动
    onNoop() {},
  },
})
