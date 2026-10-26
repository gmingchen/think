import { storePageBehavior, sharePageBehavior } from '../../../../behaviors/index'
import {
  decisionDetailApi,
  decisionEndEarlyApi,
  decisionCloseApi,
  decisionDeleteApi,
  reviewSubmitApi,
  recapDetailApi,
  recapSaveApi,
} from '../../../../apis/index'
import {
  DECISION_STATUS_ENUM,
  CHECK_STATUS_ENUM,
} from '../../../../common/enums/index'
import {
  decisionReportUrl,
  ROUTE,
} from '../../../../common/constants/index'
import dayjs from 'dayjs'

const STATUS = DECISION_STATUS_ENUM
const CHECK = CHECK_STATUS_ENUM
// 到期后补拉间隔（ms）：后端到期扫描每分钟第 0 秒跑一次，10s 一次能较快跟上
const END_POLL_DELAY = 10000
// 补拉次数上限：共约 70s（足够覆盖一个完整扫描周期），超过则停止，避免后端异常时无限轮询
const END_POLL_MAX = 7
const OTHER = -1 // 复盘最终选择「其他/暂缓」哨兵

Page({
  behaviors: [storePageBehavior, sharePageBehavior],
  data: {
    id: null,
    loading: true,
    data: {},
    percent: 0,
    countdown: '',
    // 发起人发表时间的友好相对文案（刚刚 / N分钟前 / …），展示于昵称下方
    timeText: '',
    // 不限时（无 coolDownEndAt）：倒计时位改文案「不限时 · 满员即截止」
    timeUnlimited: false,
    // 人数不限（reviewLimit=0）：计数位改「已评 N 人 · 不限」，进度条失去分母不展
    countUnlimited: false,
    STATUS,
    CHECK,
    showReview: false,
    // 评议提交中（防连点重复提交，同步传 review-popup 禁用按钮）
    submitting: false,
    showReport: false,
    pendingAction: null,
    // 复盘（仅发起人 + 已截止/已复盘）：拉取的复盘记录 + 最终选择回显文案 + 回填弹窗开关
    recap: null,
    recapChoiceText: '',
    showRecap: false,
  },

  async onLoad(options) {
    this.bindStore({ fields: ['user', 'system'] })
    await wx.$onLogined
    this._endPollTries = 0
    this.setData({ id: options.id })
    await this.getData()
  },

  onUnload() {
    this._clearTimer()
  },

  onHide() {
    this._clearTimer()
  },

  onShow() {
    if (this.data.id) this.getData()
  },

  async getData() {
    const r = await decisionDetailApi({ id: this.data.id })
    const data = r.data || {}
    const limit = data.reviewLimit || 0
    const count = data.reviewCount || 0
    // 仅发起人 + 已截止/已复盘：拉取复盘记录供只读结局卡展示（决策详情 VO 不含复盘字段）
    const canRecap = data.owner && (data.status === STATUS.ENDED || data.status === STATUS.RECAPTURED)
    let recap = null
    let recapChoiceText = ''
    if (canRecap) {
      const rr = await recapDetailApi({ decisionId: this.data.id })
      recap = rr.data || null
      if (recap) {
        recapChoiceText = recap.finalChoice === OTHER
          ? '其他 / 暂缓'
          : ((data.options || [])[recap.finalChoice] || '')
      }
    }
    this.setData({
      loading: false,
      data,
      countUnlimited: !limit,
      percent: limit ? Math.min(100, Math.round((count / limit) * 100)) : 0,
      timeText: this._formatPublishTime(data.publishedAt || data.createdAt),
      recap,
      recapChoiceText,
    })
    this._startCountdown()
  },

  // 相对时间：刚刚 / N分钟前 / N小时前 / 昨天 HH:mm / N天前 / YYYY年M月D日（与 review-card 一致）
  _formatPublishTime(time) {
    if (!time) return ''
    const target = dayjs(time)
    if (!target.isValid()) return ''
    const minutes = dayjs().diff(target, 'minute')
    if (minutes < 1) return '刚刚'
    if (minutes < 60) return `${ minutes }分钟前`
    const hours = dayjs().diff(target, 'hour')
    if (hours < 24) return `${ hours }小时前`
    const days = dayjs().diff(target, 'day')
    if (days === 1) return `昨天 ${ target.format('HH:mm') }`
    if (days < 30) return `${ days }天前`
    return target.format('YYYY年M月D日')
  },

  // ── 评议时限倒计时（评议中且未到截止才计时；不限时无 coolDownEndAt，走「不限时」文案） ──
  _startCountdown() {
    this._clearTimer()
    const { status, coolDownEndAt } = this.data.data
    if (status !== STATUS.REVIEWING || !coolDownEndAt) {
      this.setData({ countdown: '', timeUnlimited: status === STATUS.REVIEWING && !coolDownEndAt })
      return
    }
    const tick = () => {
      const end = dayjs(coolDownEndAt)
      const diff = end.diff(dayjs(), 'second')
      if (diff <= 0) {
        this.setData({ countdown: '评议时限已结束' })
        this._clearTimer()
        // 收口由后端每分钟第 0 秒的到期扫描完成，归零瞬间状态大概率仍是「评议中」，
        // 不补拉就会长时间出现「时限已结束 + 评议中」的矛盾展示
        this._pollUntilEnded()
        return
      }
      const d = Math.floor(diff / 86400)
      const h = Math.floor((diff % 86400) / 3600)
      const m = Math.floor((diff % 3600) / 60)
      const s = diff % 60
      const pad = n => (n < 10 ? `0${ n }` : `${ n }`)
      this.setData({ countdown: d > 0 ? `${ d }天${ pad(h) }:${ pad(m) }:${ pad(s) }` : `${ pad(h) }:${ pad(m) }:${ pad(s) }` })
    }
    tick()
    this._timer = setInterval(tick, 1000)
  },

  // 到期后有限轮询补拉：每 END_POLL_DELAY 拉一次，拉到状态不再是评议中即停；
  // 最多 END_POLL_MAX 次（共约 70s，覆盖一个完整扫描周期）后放弃，避免后端异常时无限轮询
  // 下一轮由 getData → _startCountdown → 仍到期归零 时再次进入本方法，形成自终止循环
  _pollUntilEnded() {
    if (this._endPollTimer || this._endPollTries >= END_POLL_MAX) return
    this._endPollTimer = setTimeout(() => {
      this._endPollTimer = null
      if (this.data.data.status !== STATUS.REVIEWING) return
      this._endPollTries += 1
      this.getData()
    }, END_POLL_DELAY)
  },
  _clearTimer() {
    if (this._timer) {
      clearInterval(this._timer)
      this._timer = null
    }
    if (this._endPollTimer) {
      clearTimeout(this._endPollTimer)
      this._endPollTimer = null
    }
  },

  // ── 评议（禁自评/先评后看：canReview 才可撰写，提交后刷新可见他人观点） ──
  onReviewTap() {
    this.setData({ showReview: true })
  },
  onReviewClose() {
    this.setData({ showReview: false })
  },
  async onReviewSubmit(e) {
    if (this.data.submitting) return
    this.setData({ submitting: true })
    const { option, content } = e.detail
    try {
      await reviewSubmitApi({ decisionId: this.data.id, option, content })
      this.setData({ showReview: false })
      wx.$toast('评议已提交')
      this.getData()
    } finally {
      this.setData({ submitting: false })
    }
  },

  // ── 发起人管理动作 ──
  onEdit() {
    wx.$navigateTo(`${ ROUTE.DECISION_CREATE }?id=${ this.data.id }`)
  },
  onViewReport() {
    wx.$navigateTo(decisionReportUrl(this.data.id))
  },
  onRecap() {
    // 不再单开页面：弹出回填表单（新增时 choice/result 为空，已复盘时自动回显）
    this.setData({ showRecap: true })
  },
  onRecapClose() {
    this.setData({ showRecap: false })
  },
  async onRecapSubmit(e) {
    if (this.data.submitting) return
    this.setData({ submitting: true })
    const { finalChoice, actualResult } = e.detail
    try {
      await recapSaveApi({ decisionId: this.data.id, finalChoice, actualResult })
      this.setData({ showRecap: false })
      wx.$toast(this.data.recap ? '复盘已更新' : '复盘已记录')
      this.getData()
    } finally {
      this.setData({ submitting: false })
    }
  },
  onEndEarly() {
    this._openConfirm('endEarly', '提前结束', '结束后将立即生成报告，确认提前结束评议？')
  },
  onClose() {
    this._openConfirm('close', '关闭评议', '关闭后该决策单将不再接收评议，确认关闭？')
  },
  onDelete() {
    this._openConfirm('delete', '删除决策单', '删除后不可恢复，确认删除？')
  },
  _openConfirm(action, title, message) {
    this.setData({ pendingAction: action })
    this.selectComponent('#actionConfirmPopup').open({ title, content: message })
  },
  async onConfirmDone() {
    const action = this.data.pendingAction
    this.setData({ pendingAction: null })
    if (!action) return
    if (action === 'endEarly') {
      await decisionEndEarlyApi({ id: this.data.id })
      wx.$toast('已结束评议')
    } else if (action === 'close') {
      await decisionCloseApi({ id: this.data.id })
      wx.$toast('已关闭')
    } else if (action === 'delete') {
      await decisionDeleteApi({ id: this.data.id })
      wx.$navigateBack()
      return
    }
    this.getData()
  },

  // 举报
  onReportTap() {
    this.setData({ showReport: true })
  },
  onReportClose() {
    this.setData({ showReport: false })
  },
})
