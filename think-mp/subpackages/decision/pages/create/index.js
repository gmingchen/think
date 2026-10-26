import { storePageBehavior, fileBehavior, sharePageBehavior } from '../../../../behaviors/index'
import {
  decisionCreateApi,
  decisionUpdateApi,
  decisionPublishApi,
  decisionDetailApi,
  tagListApi,
} from '../../../../apis/index'
import { subscribeSyncApi } from '../../../../apis/index'
import { requestSubscribeMessage } from '../../../../utils/wx-api'
import { DECISION_REMIND_TMPL_ID } from '../../../../common/constants/index'
import {
  VISIBILITY_ENUM,
  COOL_DOWN_ENUM,
  REVIEW_LIMIT_ENUM,
  COOL_DOWN_MAX_MINUTES,
  COOL_DOWN_MIN_MINUTES,
  COOL_DOWN_MINUTE_OPTIONS,
  visibilityList,
  GRP_ROLE_ENUM,
} from '../../../../common/enums/index'
import { decisionDetailUrl } from '../../../../common/constants/index'

// 表单字面量常量化（禁止魔法数字/字符串）
const TITLE_MAX = 30
const CONTENT_MAX = 1000
const OPTION_MIN = 2
const OPTION_MAX = 6
// 单个选项文案上限：10 字
const OPTION_TEXT_MAX = 10
const IMAGE_MAX = 3
const TAG_MAX = 3
// 评议人数上限：stepper 逐数（1~50，支持点击输入），0=不限（另由「不限」开关切换）
const REVIEW_LIMIT_MIN = 1
const REVIEW_LIMIT_MAX = 50
const DEFAULT_REVIEW_LIMIT = 5
// 评议时限：两段式（小时 0~24 + 分钟 0/15/30/45），单位分钟；0=不限。默认 24 小时
const DEFAULT_COOL_DOWN_MINUTES = COOL_DOWN_MAX_MINUTES
// 默认可见范围：群内评议（草稿由 status 区分，不再提供「仅自己」选项）
const DEFAULT_VISIBILITY = VISIBILITY_ENUM.GRP

// uploader 组件透传 uploadMedia 的选项（凭证与背景图：仅图片，存储目录 decision）
const UPLOAD_OPTIONS = { mediaTypes: ['image'], prefix: 'decision' }

Page({
  behaviors: [storePageBehavior, fileBehavior, sharePageBehavior],
  data: {
    id: null,
    loading: true,
    saving: false,
    TITLE_MAX,
    CONTENT_MAX,
    OPTION_MAX,
    OPTION_TEXT_MAX,
    IMAGE_MAX,
    TAG_MAX,
    form: {
      title: '',
      content: '',
      options: ['同意', '反对'],
      images: [],
      visibility: DEFAULT_VISIBILITY,
      grpId: null,
      grpName: '',
      coolDownMinutes: DEFAULT_COOL_DOWN_MINUTES,
      reviewLimit: DEFAULT_REVIEW_LIMIT,
      tagIds: [],
    },
    // 时长两段式展示位（由 form.coolDownMinutes 派生；form.coolDownMinutes 为唯一真值）
    cdHours: Math.floor(DEFAULT_COOL_DOWN_MINUTES / 60),
    cdMins: DEFAULT_COOL_DOWN_MINUTES % 60,
    COOL_DOWN_MINUTE_OPTIONS,
    visibilityList,
    tags: [],
    VISIBILITY_ENUM,
    COOL_DOWN_ENUM,
    REVIEW_LIMIT_ENUM: REVIEW_LIMIT_ENUM,
    GRP_ROLE_ENUM,
    uploadOptions: UPLOAD_OPTIONS,
  },

  async onLoad(options) {
    this.bindStore({ fields: ['user', 'system'] })
    await wx.$onLogined
    this.getTags()
    if (options.id) {
      this.setData({ id: Number(options.id) })
      await this.getData()
    } else {
      this.setData({ loading: false })
    }
  },

  async getData() {
    const r = await decisionDetailApi({ id: this.data.id })
    const d = r.data
    if (d) {
      const tags = this.data.tags.length ? this.data.tags : (await this.getTags()) || []
      const tagIds = (d.tags || []).map(name => {
        const t = tags.find(x => x.name === name)
        return t ? t.id : null
      }).filter(Boolean)
      // ?? 而非 ||：不限档存 0，回显时不能被默认值吞掉
      const cdMin = d.coolDownMinutes ?? DEFAULT_COOL_DOWN_MINUTES
      this.setData({
        loading: false,
        cdHours: Math.floor(cdMin / 60),
        cdMins: cdMin % 60,
        form: {
          title: d.title || '',
          content: d.content || '',
          options: d.options && d.options.length ? d.options : ['同意', '反对'],
          images: d.images || [],
          visibility: d.visibility || DEFAULT_VISIBILITY,
          grpId: d.grpId || null,
          grpName: '',
          coolDownMinutes: cdMin,
          reviewLimit: d.reviewLimit ?? DEFAULT_REVIEW_LIMIT,
          tagIds,
        },
      })
    } else {
      this.setData({ loading: false })
    }
  },

  async getTags() {
    const r = await tagListApi()
    const tags = r.data || []
    this.setData({ tags })
    return tags
  },

  // ── 基础字段绑定 ──
  onTitleInput(e) {
    this.setData({ 'form.title': e.detail.value })
  },
  onContentInput(e) {
    this.setData({ 'form.content': e.detail.value })
  },
  onOptionInput(e) {
    const { index } = e.currentTarget.dataset
    this.setData({ [`form.options[${ index }]`]: e.detail.value })
  },
  onOptionAdd() {
    const { options } = this.data.form
    if (options.length >= OPTION_MAX) {
      return wx.$toast(`最多 ${ OPTION_MAX } 个方案`)
    }
    this.setData({ 'form.options': [...options, ''] })
  },
  onOptionDelete(e) {
    const { index } = e.currentTarget.dataset
    const options = this.data.form.options.filter((_, i) => i !== index)
    this.setData({ 'form.options': options })
  },
  onReviewLimitMinus() {
    const cur = this.data.form.reviewLimit
    // 不限(0) 时回到最小值 1；否则递减，最低到 1（再低交由「不限」开关）
    if (cur <= REVIEW_LIMIT_ENUM.UNLIMITED) {
      this.setData({ 'form.reviewLimit': REVIEW_LIMIT_MIN })
      return
    }
    this.setData({ 'form.reviewLimit': cur - 1 < REVIEW_LIMIT_MIN ? REVIEW_LIMIT_MIN : cur - 1 })
  },
  onReviewLimitPlus() {
    const cur = this.data.form.reviewLimit
    const base = cur <= REVIEW_LIMIT_ENUM.UNLIMITED ? REVIEW_LIMIT_MIN - 1 : cur
    this.setData({ 'form.reviewLimit': base + 1 > REVIEW_LIMIT_MAX ? REVIEW_LIMIT_MAX : base + 1 })
  },
  onReviewLimitUnlimited() {
    const cur = this.data.form.reviewLimit
    if (cur === REVIEW_LIMIT_ENUM.UNLIMITED) {
      this.setData({ 'form.reviewLimit': DEFAULT_REVIEW_LIMIT }) // 关不限：回落默认
      return
    }
    // 开不限：若时限已不限，先把时限回落默认 24 小时，保证不同时不限
    if (this.data.form.coolDownMinutes === COOL_DOWN_ENUM.UNLIMITED) {
      this._applyCoolDown(DEFAULT_COOL_DOWN_MINUTES)
      wx.$toast('评议时限与人数不能同时不限，已为时限设上限')
    }
    this.setData({ 'form.reviewLimit': REVIEW_LIMIT_ENUM.UNLIMITED })
  },
  // 点击数字直接输入：失焦夹取到 1~50（非法/空回落最小值）
  onReviewLimitBlur(e) {
    const n = parseInt(e.detail.value, 10)
    const clamped = isNaN(n) ? REVIEW_LIMIT_MIN : Math.min(REVIEW_LIMIT_MAX, Math.max(REVIEW_LIMIT_MIN, n))
    this.setData({ 'form.reviewLimit': clamped })
  },

  // ── 评议时限：两段式（小时 ± + 分钟档），form.coolDownMinutes 为唯一真值 ──
  _applyCoolDown(total) {
    let t = total
    if (t < COOL_DOWN_MIN_MINUTES) t = COOL_DOWN_MIN_MINUTES // 非不限时的下限（0 交由「不限」开关）
    if (t > COOL_DOWN_MAX_MINUTES) t = COOL_DOWN_MAX_MINUTES
    this.setData({ 'form.coolDownMinutes': t, cdHours: Math.floor(t / 60), cdMins: t % 60 })
  },
  onHoursMinus() {
    this._applyCoolDown(this.data.form.coolDownMinutes - 60)
  },
  onHoursPlus() {
    this._applyCoolDown(this.data.form.coolDownMinutes + 60)
  },
  onMinuteSelect(e) {
    const m = Number(e.currentTarget.dataset.value)
    this._applyCoolDown(this.data.cdHours * 60 + m)
  },
  onCoolDownUnlimited() {
    const cur = this.data.form.coolDownMinutes
    if (cur === COOL_DOWN_ENUM.UNLIMITED) {
      this._applyCoolDown(DEFAULT_COOL_DOWN_MINUTES) // 关不限：回落默认 24 小时
      return
    }
    // 开不限：若人数已不限，先把人数回落默认，保证不同时不限
    if (this.data.form.reviewLimit === REVIEW_LIMIT_ENUM.UNLIMITED) {
      this.setData({ 'form.reviewLimit': DEFAULT_REVIEW_LIMIT })
      wx.$toast('评议时限与人数不能同时不限，已为人数设上限')
    }
    this.setData({ 'form.coolDownMinutes': COOL_DOWN_ENUM.UNLIMITED, cdHours: 0, cdMins: 0 })
  },

  // ── 可见范围 ──
  onVisibilitySelect(e) {
    const value = Number(e.currentTarget.dataset.value)
    const patch = { 'form.visibility': value }
    if (value !== VISIBILITY_ENUM.GRP) {
      patch['form.grpId'] = null
      patch['form.grpName'] = ''
    }
    if (value !== VISIBILITY_ENUM.PUBLIC) {
      patch['form.tagIds'] = []
    }
    this.setData(patch)
  },

  // ── 图片上传（可选 ≤3）：选择/删除经 uploader 回抛，fileBehavior 按 data-field 自动同步 form.images ──

  // ── 标签多选（仅公开） ──
  onTagToggle(e) {
    const id = Number(e.currentTarget.dataset.id)
    const tagIds = [...this.data.form.tagIds]
    const idx = tagIds.indexOf(id)
    if (idx > -1) {
      tagIds.splice(idx, 1)
    } else {
      if (tagIds.length >= TAG_MAX) {
        return wx.$toast(`最多选 ${ TAG_MAX } 个标签`)
      }
      tagIds.push(id)
    }
    this.setData({ 'form.tagIds': tagIds })
  },

  // ── 选群（弹窗显隐、群组列表拉取内聚于 grp-picker 组件） ──
  onOpenGrpPicker() {
    this.selectComponent('#grpPicker').open()
  },
  onGrpSelect(e) {
    const { id, name } = e.detail
    this.setData({ 'form.grpId': id, 'form.grpName': name })
  },

  // ── 校验 ──
  _validate(publish) {
    const { form } = this.data
    if (!form.title.trim()) return '请填写标题'
    if (!form.content.trim()) return '请填写决策内容'
    const options = form.options.filter(o => o && o.trim())
    if (options.length < OPTION_MIN) return `至少填写 ${ OPTION_MIN } 个选择`
    // 任何场景（含存草稿）都禁止双不限：时间不限与人数不限不可同时选
    if (form.coolDownMinutes === COOL_DOWN_ENUM.UNLIMITED && form.reviewLimit === REVIEW_LIMIT_ENUM.UNLIMITED) {
      return '评议时限与评议人数不能同时不限，请至少给一个设上限'
    }
    if (publish) {
      if (form.visibility === VISIBILITY_ENUM.SELF) return '仅自己可见无法发布，请选择群内或公开'
      if (form.visibility === VISIBILITY_ENUM.GRP && !form.grpId) return '请选择目标群组'
      // 公开档另需人数上限（后端 ≤100 封顶）
      if (form.visibility === VISIBILITY_ENUM.PUBLIC && form.reviewLimit === REVIEW_LIMIT_ENUM.UNLIMITED) {
        return '公开档请设置评议人数上限'
      }
    }
    return null
  },

  _buildPayload() {
    const { form } = this.data
    return {
      title: form.title.trim(),
      content: form.content.trim(),
      options: form.options.filter(o => o && o.trim()).map(o => o.trim()),
      images: form.images,
      visibility: form.visibility,
      grpId: form.visibility === VISIBILITY_ENUM.GRP ? form.grpId : null,
      coolDownMinutes: form.coolDownMinutes,
      reviewLimit: form.reviewLimit,
      tagIds: form.visibility === VISIBILITY_ENUM.PUBLIC ? form.tagIds : [],
    }
  },

  // ── 存草稿 ──
  async onSave() {
    if (this.data.saving) return
    const err = this._validate(false)
    if (err) return wx.$toast(err)
    this.setData({ saving: true })
    try {
      const payload = this._buildPayload()
      if (this.data.id) {
        await decisionUpdateApi({ ...payload, id: this.data.id })
      } else {
        const r = await decisionCreateApi(payload)
        if (r.data) this.setData({ id: r.data })
      }
      wx.$toast('已保存草稿')
      wx.$navigateBack()
    } finally {
      this.setData({ saving: false })
    }
  },

  // ── 发布 ──
  async onPublish() {
    if (this.data.saving) return
    // 一次性订阅提醒必须先于 await 落在 tap 调用栈内，否则微信不弹窗（留空则不弹）
    this._requestRemind()
    const err = this._validate(true)
    if (err) return wx.$toast(err)
    this.setData({ saving: true })
    try {
      const payload = this._buildPayload()
      let id = this.data.id
      if (id) {
        await decisionUpdateApi({ ...payload, id })
      } else {
        const r = await decisionCreateApi(payload)
        id = r.data
      }
      if (id) {
        await decisionPublishApi({ id })
      }
      wx.$toast('已发起评议')
      // 发布后跳详情（群内直生效、公开进审核）；用 redirectTo 关闭创建页，不留历史栈
      wx.$redirectTo(decisionDetailUrl(id))
    } finally {
      this.setData({ saving: false })
    }
  },

  // 评议时限到期/报告生成提醒订阅：授权一次=可发一次，accept 后 sync 累加配额；任何异常均不阻断发布
  async _requestRemind() {
    if (!DECISION_REMIND_TMPL_ID) return
    try {
      const res = await requestSubscribeMessage({ tmplIds: [DECISION_REMIND_TMPL_ID] })
      if (res && res[DECISION_REMIND_TMPL_ID] === 'accept') {
        await subscribeSyncApi({ templateId: DECISION_REMIND_TMPL_ID, count: 1 })
      }
    } catch (e) {
      // 拒绝/模板未配/非用户触发/同步失败一律忽略
    }
  },
})
