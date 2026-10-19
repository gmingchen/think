import { storePageBehavior, sharePageBehavior } from '../../../../behaviors/index'
import { aiRecommendApi, aiAdoptApi, aiQuotaApi, kitchenExpandDishApi } from '../../../../apis/index'
import { playExpandAd, adServiceEnabled, canExpandByAd } from '../../../../utils/index'
import { DEFAULT_DISH_COVER, AI_DAILY_LIMIT, AI_DISCLAIMER_TEXT, AD_UNIT_AI_BANNER, AD_UNIT_AI_INTERSTITIAL, LIMIT_REACHED_CODE } from '../../../../common/constants/index'

// 插屏广告单例（createInterstitialAd 同一 adUnitId 返回单例，模块级缓存避免重复创建）
let interstitialAd = null
const getInterstitialAd = () => {
  if (!interstitialAd && wx.createInterstitialAd) {
    interstitialAd = wx.createInterstitialAd({ adUnitId: AD_UNIT_AI_INTERSTITIAL })
  }
  return interstitialAd
}

// 生成态主/副文案轮播（手账温暖语调，随思考清单阶段推进）
const QUOTES = [
  { main: '正在翻看你的冰箱食材账本…', sub: '手头有这些食材，看看能凑出什么滋味' },
  { main: '检索库内菜谱，匹配食材命中率…', sub: '优先挑出你家常吃、食材最齐的那道' },
  { main: '大厨 AI 正在为您构思新菜灵感…', sub: '避开老花样，用现有调料做一道时令新味' },
  { main: '最后调味润色，两组美味即将出锅！', sub: '马上为您呈上今日开伙手账' },
]

// 3D 便签翻页文案（纯视觉循环）
const FLIP_TEXTS = [
  '📄 匹配食材最佳火候中…',
  '📖 对比家人的口味忌口…',
  '✨ 测算少油少盐健康配比…',
  '🌿 正在手写步骤摘要…',
]

// AI 思考清单（state: pending / active / done）
const LOADING_STEPS = [
  { key: 'step1', label: '1. 盘点厨房现有食材与偏好', stamp: '已清点' },
  { key: 'step2', label: '2. 测算库内菜谱食材命中率', stamp: '已匹配' },
  { key: 'step3', label: '3. 针对盲区构思时令新菜', stamp: '已成谱' },
]

// 后端 RankedDish → dish-card 视图模型（first 由排序首位派生，后端无该字段）
function mapRanked(item, index) {
  return { ...item, first: index === 0 }
}

// 后端 FreshDish → fresh-card 视图模型（hint/文案由 categoryHint 与分字段拼装）
function mapFresh(item) {
  return {
    name: item.name,
    remark: item.remark || '',
    categoryHint: item.categoryHint || '',
    hint: item.categoryHint ? `分类预估：${item.categoryHint}` : '',
    foods: (item.foods || []).map(f => ({
      name: f.name,
      amount: f.amount || '',
      note: f.process || '',
    })),
    seasonings: (item.seasonings || []).map(s => ({
      name: s.name,
      amount: s.amount || '',
    })),
    steps: item.steps || [],
    adopted: false,
  }
}

Page({
  behaviors: [storePageBehavior, sharePageBehavior],

  data: {
    DEFAULT_DISH_COVER,
    AI_DISCLAIMER_TEXT,
    AD_UNIT_AI_BANNER,
    // 初始加载骨架屏（进页面等厨房/额度就绪）
    loading: true,
    // 状态流转：select 选择态 / loading 生成中 / result 结果态
    status: 'select',
    remainCount: AI_DAILY_LIMIT,
    // 采纳凭证（后端 Redis key）
    recommendId: '',
    // 选择态
    selectedFoods: [],
    selectedFoodsIds: [],
    showFoodPicker: false,
    peopleOptions: [
      { label: '1人', value: 1 },
      { label: '2-3人', value: 3 },
      { label: '4人+', value: 4 },
    ],
    peopleValue: 3,
    tasteOptions: [
      { label: '不限', value: '' },
      { label: '清淡鲜甜', value: '清淡鲜甜' },
      { label: '微辣', value: '微辣' },
      { label: '中辣', value: '中辣' },
      { label: '麻辣', value: '麻辣' },
      { label: '香辣', value: '香辣' },
      { label: '酸甜', value: '酸甜开胃' },
      { label: '咸鲜', value: '咸鲜下饭' },
    ],
    tasteValue: '',
    avoid: '',
    // 生成态
    loadingText: QUOTES[0].main,
    loadingSubText: QUOTES[0].sub,
    flipText: FLIP_TEXTS[0],
    loadingSteps: LOADING_STEPS.map((s, i) => ({ ...s, state: i === 0 ? 'active' : 'pending' })),
    // 结果态
    ranked: [],
    fresh: [],
    // 换一批去重：本会话已展示过的第一组菜 ID / 已生成的新菜名
    shownDishIds: [],
    shownFreshNames: [],
    // 菜篮入篮抽拽动效
    cartShake: false,
    // 扩容中（看广告防抖）
    expanding: false,
  },

  async onLoad() {
    this.bindStore({ fields: ['kitchen', 'cart', 'ad'], actions: ['addToCart', 'getKitchen'] })
    // 荐菜/采纳均为登录态接口，须等登录完成
    await wx.$onLogined
    // 回显以后端为准：进页面拉真实剩余次数，厨房与额度就绪后收起骨架屏
    await wx.$onKitchenLoaded
    await this._loadQuota()
    this.setData({ loading: false })
  },

  async _loadQuota() {
    try {
      const r = await aiQuotaApi()
      if (r.data != null) this.setData({ remainCount: r.data })
    } catch (e) {
      // 额度拉取失败不阻塞页面（service 已统一 toast），保持当前展示
    }
  },

  onUnload() {
    this._destroyed = true
    this._stopLoading()
  },

  // ── 选择态交互 ──
  onOpenFoodPicker() {
    this.setData({ showFoodPicker: true })
  },

  onCloseFoodPicker() {
    this.setData({ showFoodPicker: false })
  },

  onFoodConfirm(e) {
    const items = (e.detail && e.detail.items) || []
    this.setData({ selectedFoods: items, selectedFoodsIds: items.map(f => f.id), showFoodPicker: false })
  },

  onRemoveFood(e) {
    const { id } = e.currentTarget.dataset
    const selectedFoods = this.data.selectedFoods.filter(f => f.id !== id)
    this.setData({ selectedFoods, selectedFoodsIds: selectedFoods.map(f => f.id) })
  },

  onSelectPeople(e) {
    this.setData({ peopleValue: e.currentTarget.dataset.value })
  },

  onSelectTaste(e) {
    this.setData({ tasteValue: e.currentTarget.dataset.value })
  },

  onAvoidInput(e) {
    this.setData({ avoid: e.detail.value })
  },

  // ── 组装请求参数 ──
  _buildParams() {
    return {
      kitchenId: this.data.kitchen?.active?.id,
      foodIds: this.data.selectedFoods.map(f => f.id),
      peopleCount: this.data.peopleValue,
      taste: this.data.tasteValue || undefined,
      avoid: this.data.avoid || undefined,
    }
  },

  // ── 生成（第一组 + 第二组）──
  async onGenerate() {
    if (!this.data.selectedFoods.length) {
      wx.$toast('先告诉 AI 你有什么食材吧')
      return
    }
    if (!this.data.kitchen?.active?.id) {
      wx.$toast('请先创建或选择厨房')
      return
    }
    if (this.data.remainCount <= 0) {
      wx.$toast('今日 AI 荐菜次数已用完，明天再来')
      return
    }
    // 首次/重新生成：清空已展示历史，换一批在此基础上叠加排除
    this.setData({ shownDishIds: [], shownFreshNames: [] })
    await this._runRecommend(this._buildParams())
  },

  // ── 换一批（排除本会话已展示过的菜，重出全新两组）──
  async onRefreshFresh() {
    if (this.data.remainCount <= 0) {
      wx.$toast('今日 AI 荐菜次数已用完，明天再来')
      return
    }
    const params = {
      ...this._buildParams(),
      excludeDishIds: this.data.shownDishIds,
      excludeFreshNames: this.data.shownFreshNames,
    }
    await this._runRecommend(params)
  },

  async _runRecommend(params) {
    this._startLoading()
    try {
      const r = await aiRecommendApi(params)
      const data = r.data || {}
      const ranked = (data.ranked || []).map(mapRanked)
      const fresh = (data.fresh || []).map(mapFresh)
      this.setData({
        status: 'result',
        recommendId: data.recommendId || '',
        ranked,
        fresh,
        remainCount: data.remainCount != null ? data.remainCount : Math.max(0, this.data.remainCount - 1),
        // 累积已展示，供下次换一批排除
        shownDishIds: Array.from(new Set(this.data.shownDishIds.concat(ranked.map(d => d.dishId)))),
        shownFreshNames: Array.from(new Set(this.data.shownFreshNames.concat(fresh.map(d => d.name)))),
      })
      // 出结果后短暂停留再弹插屏（不打断刚看到结果的瞬间）
      setTimeout(() => {
        if (this._destroyed) return
        this._showInterstitial()
      }, 600)
    } catch (e) {
      // 失败已由 service 统一 toast；退回选择态（输入已保留，可重试）
      // 后端先自增后调 LLM，失败也已扣额，重拉额度校正回显
      this.setData({ status: 'select' })
      this._loadQuota()
    } finally {
      this._stopLoading()
    }
  },

  _startLoading() {
    this._stopLoading()
    this._loadingTimers = []
    this.setData({
      status: 'loading',
      loadingText: QUOTES[0].main,
      loadingSubText: QUOTES[0].sub,
      flipText: FLIP_TEXTS[0],
      loadingSteps: LOADING_STEPS.map((s, i) => ({ ...s, state: i === 0 ? 'active' : 'pending' })),
    })

    // 翻页文案循环（2.4s 一次，与 CSS flip 动画同拍）
    let flipIdx = 0
    this._loadingTimers.push(setInterval(() => {
      flipIdx = (flipIdx + 1) % FLIP_TEXTS.length
      this.setData({ flipText: FLIP_TEXTS[flipIdx] })
    }, 2400))

    // 思考清单时间轴：逐项 done 并盖红章，同步推进主/副文案
    const timeline = [
      { t: 1600, states: ['done', 'active', 'pending'], quote: 1 },
      { t: 3800, states: ['done', 'done', 'active'], quote: 2 },
      { t: 6200, states: ['done', 'done', 'done'], quote: 3 },
    ]
    timeline.forEach(({ t, states, quote }) => {
      this._loadingTimers.push(setTimeout(() => {
        this.setData({
          loadingSteps: LOADING_STEPS.map((s, i) => ({ ...s, state: states[i] })),
          loadingText: QUOTES[quote].main,
          loadingSubText: QUOTES[quote].sub,
        })
      }, t))
    })

    // 6.2s 后若仍在生成（LLM 未回），文案循环保持“活着”，清单保持全部已完成
    this._loadingTimers.push(setTimeout(() => {
      let qi = 3
      this._loadingTimers.push(setInterval(() => {
        qi = (qi + 1) % QUOTES.length
        this.setData({ loadingText: QUOTES[qi].main, loadingSubText: QUOTES[qi].sub })
      }, 3000))
    }, 6400))
  },

  _stopLoading() {
    // clearTimeout / clearInterval 同受数字 id，统一遍历清理
    ;(this._loadingTimers || []).forEach(id => {
      clearTimeout(id)
      clearInterval(id)
    })
    this._loadingTimers = []
  },

  // ── 插屏广告：受全局开关与会员免广告双重控制，无填充/频次上限/未生效静默忽略 ──
  _showInterstitial() {
    if (!adServiceEnabled()) return
    const ad = getInterstitialAd()
    if (!ad) return
    ad.show().catch(() => {
      // show 失败（拉不到/超频次）不提示，不打断荐菜体验
    })
  },

  // ── 结果态交互 ──
  onReset() {
    this.setData({ status: 'select' })
  },

  // 点卡片：弹出菜谱详情弹窗（含「点这道」入篮按钮）
  onDishDetail(e) {
    const { id } = e.detail
    if (id) this.selectComponent('#dishPopup').open(id, true)
  },

  // 已采纳的新菜：点「✓ 已采纳」展示入库后的菜谱详情弹窗
  onFreshDetail(e) {
    const { dishId } = e.detail
    if (dishId) this.selectComponent('#dishPopup').open(dishId, true)
  },

  // 弹窗内「查看完整菜谱」：dish-popup 只抛出 detail 事件（携 id），需页面自己关弹窗 + 路由
  onPopupDetail(e) {
    const { id } = e.detail
    this.selectComponent('#dishPopup').close()
    if (id) wx.$navigateTo(`/subpackages/dish/pages/detail/index?id=${id}`)
  },

  // 弹窗内「点这道」：从弹窗已加载的 dish 取 name/photo 入篮（add 事件只回传 id），入篮后关闭弹窗
  onPopupAddCart() {
    const popup = this.selectComponent('#dishPopup')
    const dish = popup?.data?.dish
    if (!dish?.id) return
    this.addToCart({ id: dish.id, name: dish.name, photo: dish.photo || '' })
    popup.close()
    wx.vibrateShort({ type: 'medium' })
    wx.$toast('已加入点餐篮')
    this._shakeCartBasket()
  },

  // 入篮：走现有点菜篮 store action
  onDishAddCart(e) {
    const { dish } = e.detail
    if (!dish) return
    this.addToCart({ id: dish.dishId, name: dish.dishName, photo: dish.dishCover || '' })
    wx.vibrateShort({ type: 'medium' })
    wx.$toast('已加入点餐篮')
    this._shakeCartBasket()
  },

  // 菜篮抽动：入篮后底部 🧺 抽一下（同 draw 页）
  _shakeCartBasket() {
    this.setData({ cartShake: true })
    setTimeout(() => {
      this.setData({ cartShake: false })
    }, 500)
  },

  // 点击菜篮：打开购物车弹窗，弹窗内确认后仍走 onGoOrder
  onOpenCartPopup() {
    this.selectComponent('#cartPopup').open()
  },

  // 去下单：购物车非空才能进确认页
  onGoOrder() {
    const cart = this.data.cart
    if (!cart || cart.quantity === 0) {
      wx.$toast('点餐篮还是空的，先加几道菜吧')
      return
    }
    wx.$navigateTo('/subpackages/cookorder/pages/confirm/index')
  },

  // 点「采纳」：二次确认弹窗（confirm 事件不带数据，暂存待采纳 index，同 recipe 页删除确认模式）
  onAdopt(e) {
    const { index } = e.detail
    if (!this.data.recommendId) {
      wx.$toast('推荐已过期，重新生成一份吧')
      return
    }
    const dish = this.data.fresh[index]
    this._pendingAdoptIndex = index
    this.selectComponent('#adoptConfirmPopup').open({
      title: '采纳菜谱',
      content: `确定将「${dish ? dish.name : ''}」存入你家菜谱库吗？`,
      confirmText: '采纳',
    })
  },

  // 确认采纳：只传凭证 recommendId + index，内容由后端从 Redis 取
  async onAdoptConfirm() {
    const index = this._pendingAdoptIndex
    if (index == null) return
    this._pendingAdoptIndex = null
    if (!this.data.recommendId) {
      wx.$toast('推荐已过期，重新生成一份吧')
      return
    }
    // 记住本次采纳参数：达上限看完视频扩容后自动重试采纳（同 recommendId + index）
    this._pendingAdopt = { recommendId: this.data.recommendId, index }
    await this._doAdopt(this._pendingAdopt.recommendId, index)
  },

  // 执行采纳：命中菜谱数上限（4005）且允许扩容时弹「看视频 +1」，其余失败由 service 统一 toast
  async _doAdopt(recommendId, index) {
    try {
      const r = await aiAdoptApi({ recommendId, index })
      const fresh = this.data.fresh.map((f, i) => (i === index ? { ...f, adopted: true, dishId: r.data } : f))
      this.setData({ fresh })
      wx.vibrateShort({ type: 'medium' })
      wx.$toast('已存入菜谱库')
    } catch (err) {
      // 命中菜谱数上限（4005）：允许扩容弹「看视频 +1」，否则兜底 toast（service 对 4005 不自动提示）
      if (err && err.code === LIMIT_REACHED_CODE) {
        if (this._canExpand()) {
          this.selectComponent('#limitPopup').open({
            title: '菜谱数已达上限',
            content: '看个小视频即可 +1，不限次数～或删除部分旧内容后可继续添加',
            confirmText: '📺 看视频 +1',
            cancelText: '以后再说',
            autoClose: false,
          })
        } else {
          wx.$toast(err.message || '菜谱数已达上限')
        }
        return
      }
      // 其余失败（推荐已过期等）service 已统一 toast
    }
  },

  // 仅厨师长/管厨且可扩容时才可看视频（采纳写入当前 active 厨房，与后端 checkPermission 对齐）
  // ⚠️ 用 canExpandByAd 而不是 adServiceEnabled：轻享会员免的是打扰型广告，
  //    把 adFree 混进来会让付了 ¥1.9 的人容量变成硬墙（§17 第 19 条）
  // ⚠️ 入参取 active 厨房的 KitchenVO.unlimitedCapacity（厨房级）而不是 store.member 的同名位（人级）
  _canExpand() {
    const active = this.data.kitchen?.active
    return (active?.role === 1 || active?.role === 2) && canExpandByAd(active?.unlimitedCapacity)
  },

  // 达上限弹窗确认：拉起激励视频，完整观看后为当前厨房菜谱上限 +1，成功后自动重试采纳
  onExpandConfirm() {
    if (!this._pendingAdopt || this.data.expanding) return
    this.setData({ expanding: true })
    playExpandAd('dish', async ({ ended }) => {
      this.setData({ expanding: false })
      // 广告结束（看完/中途关/加载失败）后才关闭达上限引导弹窗（点击确认时不立即关）
      const limitPopup = this.selectComponent('#limitPopup')
      limitPopup && limitPopup.close()
      if (!ended) return
      try {
        await kitchenExpandDishApi({ kitchenId: this.data.kitchen.active.id })
        await this.getKitchen()
        wx.$toast('已扩容 +1')
        // 扩容成功后重试采纳
        const { recommendId, index } = this._pendingAdopt
        await this._doAdopt(recommendId, index)
      } catch (e) {
        // 扩容接口异常（非4005）service 已提示，此处静默
      }
    })
  },

  // 重写分享（专属文案 + 落地荐菜页，同 draw 页接法）
  onShareAppMessage() {
    return this._aiRecommendShare()
  },
  // 重写朋友圈分享
  onShareTimeline() {
    return this._aiRecommendTimeline()
  },
})
