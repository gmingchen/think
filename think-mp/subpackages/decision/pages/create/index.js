import { storePageBehavior } from '../../../../behaviors/index'
import {
  decisionCreateApi,
  decisionUpdateApi,
  decisionPublishApi,
  decisionDetailApi,
  grpPageApi,
  tagListApi,
} from '../../../../apis/index'
import { uploadMedia } from '../../../../apis/index'
import { subscribeSyncApi } from '../../../../apis/index'
import { requestSubscribeMessage } from '../../../../utils/wx-api'
import { DECISION_REMIND_TMPL_ID } from '../../../../common/constants/index'
import {
  VISIBILITY_ENUM,
  COOL_DOWN_ENUM,
  coolDownList,
  visibilityList,
  GRP_ROLE_ENUM,
} from '../../../../common/enums/index'
import { decisionDetailUrl } from '../../../../common/constants/index'

// 表单字面量常量化（禁止魔法数字/字符串）
const TITLE_MAX = 30
const CONTENT_MAX = 1000
const OPTION_MIN = 2
const OPTION_MAX = 6
const IMAGE_MAX = 3
const TAG_MAX = 5
const REVIEW_LIMIT_MIN = 1
const REVIEW_LIMIT_MAX = 50
const DEFAULT_REVIEW_LIMIT = 5
const DEFAULT_COOL_DOWN = COOL_DOWN_ENUM.H24
const DEFAULT_VISIBILITY = VISIBILITY_ENUM.SELF

Page({
  behaviors: [storePageBehavior],
  data: {
    id: null,
    loading: true,
    saving: false,
    TITLE_MAX,
    CONTENT_MAX,
    OPTION_MAX,
    IMAGE_MAX,
    TAG_MAX,
    form: {
      title: '',
      content: '',
      options: [{ label: '' }, { label: '' }],
      images: [],
      visibility: DEFAULT_VISIBILITY,
      grpId: null,
      grpName: '',
      coolDownHours: DEFAULT_COOL_DOWN,
      reviewLimit: DEFAULT_REVIEW_LIMIT,
      tagIds: [],
    },
    visibilityList,
    coolDownList,
    tags: [],
    showGrpPicker: false,
    grps: [],
    VISIBILITY_ENUM,
    GRP_ROLE_ENUM,
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
      this.setData({
        loading: false,
        form: {
          title: d.title || '',
          content: d.content || '',
          options: d.options && d.options.length ? d.options : [{ label: '' }, { label: '' }],
          images: d.images || [],
          visibility: d.visibility || DEFAULT_VISIBILITY,
          grpId: d.grpId || null,
          grpName: '',
          coolDownHours: d.coolDownHours || DEFAULT_COOL_DOWN,
          reviewLimit: d.reviewLimit || DEFAULT_REVIEW_LIMIT,
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
    this.setData({ [`form.options[${ index }].label`]: e.detail.value })
  },
  onOptionAdd() {
    const { options } = this.data.form
    if (options.length >= OPTION_MAX) {
      return wx.$toast(`最多 ${ OPTION_MAX } 个方案`)
    }
    this.setData({ 'form.options': [...options, { label: '' }] })
  },
  onOptionDelete(e) {
    const { index } = e.currentTarget.dataset
    const options = this.data.form.options.filter((_, i) => i !== index)
    this.setData({ 'form.options': options })
  },
  onReviewLimitInput(e) {
    let v = Number(e.detail.value) || REVIEW_LIMIT_MIN
    if (v < REVIEW_LIMIT_MIN) v = REVIEW_LIMIT_MIN
    if (v > REVIEW_LIMIT_MAX) v = REVIEW_LIMIT_MAX
    this.setData({ 'form.reviewLimit': v })
  },
  onReviewLimitMinus() {
    const v = this.data.form.reviewLimit - 1
    this.setData({ 'form.reviewLimit': v < REVIEW_LIMIT_MIN ? REVIEW_LIMIT_MIN : v })
  },
  onReviewLimitPlus() {
    const v = this.data.form.reviewLimit + 1
    this.setData({ 'form.reviewLimit': v > REVIEW_LIMIT_MAX ? REVIEW_LIMIT_MAX : v })
  },
  onCoolDownSelect(e) {
    this.setData({ 'form.coolDownHours': Number(e.currentTarget.dataset.value) })
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

  // ── 图片上传（可选 ≤3） ──
  async onImageUpload() {
    const { images } = this.data.form
    const remain = IMAGE_MAX - images.length
    if (remain <= 0) {
      return wx.$toast(`最多 ${ IMAGE_MAX } 张图片`)
    }
    const r = await uploadMedia(remain, 0, { mediaTypes: ['image'], prefix: 'decision' })
    this.setData({ 'form.images': [...images, ...r.map(x => x.url)] })
  },
  onImageDelete(e) {
    const { index } = e.currentTarget.dataset
    const images = this.data.form.images.filter((_, i) => i !== index)
    this.setData({ 'form.images': images })
  },

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

  // ── 群选择 ──
  async onOpenGrpPicker() {
    const r = await grpPageApi({ current: 1, size: 50 })
    this.setData({ grps: r.data?.list || [], showGrpPicker: true })
  },
  onCloseGrpPicker() {
    this.setData({ showGrpPicker: false })
  },
  onGrpSelect(e) {
    const { id, name } = e.currentTarget.dataset
    this.setData({ 'form.grpId': id, 'form.grpName': name, showGrpPicker: false })
  },

  // ── 校验 ──
  _validate(publish) {
    const { form } = this.data
    if (!form.title.trim()) return '请填写标题'
    if (!form.content.trim()) return '请填写决策内容'
    const options = form.options.filter(o => o.label && o.label.trim())
    if (options.length < OPTION_MIN) return `至少填写 ${ OPTION_MIN } 个备选方案`
    if (publish) {
      if (form.visibility === VISIBILITY_ENUM.SELF) return '仅自己可见无法发布，请选择群内或公开'
      if (form.visibility === VISIBILITY_ENUM.GRP && !form.grpId) return '请选择目标群组'
    }
    return null
  },

  _buildPayload() {
    const { form } = this.data
    return {
      title: form.title.trim(),
      content: form.content.trim(),
      options: form.options.filter(o => o.label && o.label.trim()).map(o => ({ label: o.label.trim() })),
      images: form.images,
      visibility: form.visibility,
      grpId: form.visibility === VISIBILITY_ENUM.GRP ? form.grpId : null,
      coolDownHours: form.coolDownHours,
      reviewLimit: form.reviewLimit,
      tagIds: form.visibility === VISIBILITY_ENUM.PUBLIC ? form.tagIds : [],
    }
  },

  // ── 存草稿 ──
  async onSave() {
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
      // 发布后跳详情（群内直生效、公开进审核）
      wx.$navigateTo(decisionDetailUrl(id))
    } finally {
      this.setData({ saving: false })
    }
  },

  // 冷静期到期/报告生成提醒订阅：授权一次=可发一次，accept 后 sync 累加配额；任何异常均不阻断发布
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
