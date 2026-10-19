import { aiParseApi, aiParseCommitApi } from '../../../../../../apis/index'
import { AI_DISCLAIMER_TEXT, AD_UNIT_AI_PARSE_INTERSTITIAL } from '../../../../../../common/constants/index'
import { adServiceEnabled } from '../../../../../../utils/index'

// 插屏广告单例（createInterstitialAd 同一 adUnitId 返回单例，模块级缓存避免重复创建）
let interstitialAd = null
const getInterstitialAd = () => {
  if (!interstitialAd && wx.createInterstitialAd) {
    interstitialAd = wx.createInterstitialAd({ adUnitId: AD_UNIT_AI_PARSE_INTERSTITIAL })
  }
  return interstitialAd
}

Component({
  properties: {
    // 厨房ID：由页面层传入，组件保持纯 UI、不自行摸 store
    kitchenId: {
      type: null,
      value: '',
    },
    // 今日剩余识别次数：页面层拉后端后以属性传入，输入态副标题展示与前置守卫使用
    remainCount: {
      type: Number,
      value: 0,
    },
  },

  data: {
    AI_DISCLAIMER_TEXT,
    visible: false,
    // 两态：input（输入文案）↔ preview（展示识别结果，预览即确认）
    mode: 'input',
    text: '',
    maxLen: 1000,
    // 识别/确认进行中标记，配合 mask loading 防重复点击与悬空请求
    loading: false,
    // parse 返回的预览结构（仅名字，无 id），供预览态渲染 + commit 原样回传
    result: { name: '', categoryHint: '', remark: '', foods: [], seasonings: [], steps: [] },
  },

  methods: {
    open() {
      this.setData({ visible: true, mode: 'input', text: '', loading: false })
    },

    onClose() {
      // 识别/确认中禁止关闭，避免请求悬空
      if (this.data.loading) return
      this.setData({ visible: false })
    },

    onInput(e) {
      this.setData({ text: e.detail.value })
    },

    // 输入态「开始识别」：调 parse 拿预览，成功切预览态并 emit parsed 供页面层额度 -1；失败 toast 不切态
    async onStart() {
      if (this.data.loading) return
      const text = this.data.text.trim()
      if (!text) {
        wx.$toast('请先粘贴文案')
        return
      }
      if (this.data.remainCount <= 0) {
        wx.$toast('今日 AI 识别次数已用完，明天再来')
        return
      }
      this.setData({ loading: true })
      wx.showLoading({ title: 'AI 识别中...', mask: true })
      try {
        const r = await aiParseApi({ kitchenId: this.data.kitchenId, text })
        this.setData({ mode: 'preview', result: this._normalize(r.data) })
        this.triggerEvent('parsed')
        // 识别出结果后短暂停留再弹插屏（不打断刚看到结果的瞬间）
        setTimeout(() => this._showInterstitial(), 600)
      } catch (err) {
        wx.$print('识别失败', err)
      } finally {
        wx.hideLoading()
        this.setData({ loading: false })
      }
    },

    // ── 插屏广告：受全局开关与会员免广告双重控制，无填充/频次上限/未生效静默忽略 ──
    _showInterstitial() {
      if (!adServiceEnabled()) return
      const ad = getInterstitialAd()
      if (!ad) return
      ad.show().catch(() => {
        // show 失败（拉不到/超频次）不提示，不打断识别体验
      })
    },

    // 预览态「重新输入」：退回输入态并保留原文案
    onReInput() {
      this.setData({ mode: 'input' })
    },

    // 预览态「确认使用」：调 commit 才建库拿 id，成功回传回填结构并关闭；失败 toast 不关闭
    async onConfirm() {
      if (this.data.loading) return
      const { result, kitchenId } = this.data
      this.setData({ loading: true })
      wx.showLoading({ title: '处理中...', mask: true })
      try {
        const r = await aiParseCommitApi({
          kitchenId,
          name: result.name,
          categoryHint: result.categoryHint,
          remark: result.remark,
          foods: result.foods,
          seasonings: result.seasonings,
          steps: result.steps,
        })
        this.triggerEvent('confirm', { data: this._normalize(r.data) })
        this.setData({ visible: false })
      } catch (err) {
        wx.$print('确认失败', err)
      } finally {
        wx.hideLoading()
        this.setData({ loading: false })
      }
    },

    // 兜底：LLM 某数组缺省时归一为空数组，避免预览渲染报错
    _normalize(data) {
      const d = data || {}
      return {
        name: d.name || '',
        categoryHint: d.categoryHint || '',
        categoryId: d.categoryId || '',
        categoryName: d.categoryName || '',
        remark: d.remark || '',
        foods: d.foods || [],
        seasonings: d.seasonings || [],
        steps: d.steps || [],
      }
    },
  },
})
