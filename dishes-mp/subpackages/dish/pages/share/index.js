import { storePageBehavior, sharePageBehavior } from '../../../../behaviors/index'
import { dishShareInfoApi, dishCollectApi, kitchenExpandDishApi } from '../../../../apis/index'
import { playExpandAd, canExpandByAd } from '../../../../utils/index'
import { DEFAULT_AVATAR, DEFAULT_DISH_COVER, AD_UNIT_SHARE_BANNER, LIMIT_REACHED_CODE } from '../../../../common/constants/index'
import { KITCHEN_ROLE_ENUM, KITCHEN_ROLE_LIST } from '../../../../common/enums/index'

Page({
  behaviors: [storePageBehavior, sharePageBehavior],

  data: {
    DEFAULT_DISH_COVER,
    AD_UNIT_SHARE_BANNER,
    loading: true,
    notFound: false,
    dishId: null,
    dish: null,
    foods: [],
    seasonings: [],
    steps: [],
    tips: [],
    sharer: null,
    dishCount: 0,
    KITCHEN_ROLE_LIST,
    // 收藏抽屉
    collectVisible: false,
    // 当前用户有收藏权限的厨房（带 collected 标记）
    ownKitchens: [],
    // 该菜谱已被收藏到的厨房ID
    collectedKitchens: [],
    // 抽屉中选中的厨房ID
    selectedKitchenId: null,
    // 提交中
    submitting: false,
    // 扩容中（看广告防抖）
    expanding: false,
    // 有权限的厨房是否都已收藏过
    allCollected: false,
    // 底部主按钮文案
    collectText: '📖 收藏到我的厨房',
    collectTag: '',
  },

  async onLoad(options) {
    this.bindStore({ fields: ['kitchen', 'user', 'ad'], actions: ['getKitchen'] })
    await wx.$onKitchenLoaded
    if (options.id) {
      this.setData({ dishId: Number(options.id) })
      await this._loadShareInfo(options.id)
    } else {
      this.setData({ loading: false, notFound: true })
    }
  },

  async _loadShareInfo(id) {
    try {
      const r = await dishShareInfoApi({ id })
      const d = r.data
      if (!d) {
        this.setData({ loading: false, notFound: true })
        return
      }
      const foods = (d.foods || []).map(f => ({
        name: f.foodName,
        amount: [f.amount, f.unit].filter(Boolean).join(''),
        process: f.process || '',
      }))
      const seasonings = (d.seasonings || []).map(s => ({
        name: s.name,
        amount: [s.amount, s.unit].filter(Boolean).join(''),
      }))
      const steps = (d.steps || [])
        .slice()
        .sort((a, b) => (a.sort || 0) - (b.sort || 0))
        .map(s => ({ text: s.description }))
      // 小贴士按换行拆成便签条目
      const tips = (d.remark || '')
        .split(/\r?\n/)
        .map(item => item.trim())
        .filter(Boolean)
      this.setData({
        loading: false,
        dish: {
          id: d.id,
          name: d.name,
          cover: d.cover || '',
          categoryName: d.categoryName || '',
          rating: Number(d.rating) || 0,
          cookCount: d.cookCount || 0,
          collectCount: d.collectCount || 0,
        },
        foods,
        seasonings,
        steps,
        tips,
        sharer: d.sharer
          ? {
              nickname: d.sharer.nickname || '',
              avatar: d.sharer.avatar || DEFAULT_AVATAR,
              kitchenName: d.sharer.kitchenName || '',
              role: d.sharer.role || null,
            }
          : null,
        dishCount: d.dishCount || 0,
        collectedKitchens: d.collectedKitchens || [],
      })
      this._refreshCollectState()
    } catch (e) {
      wx.$printError('加载分享菜谱失败', e)
      this.setData({ loading: false, notFound: true })
    }
  },

  // 点击「收藏到我的厨房」
  onCollect() {
    if (this.data.submitting) return
    if (this.data.allCollected) {
      wx.$toast('这道菜已收藏在你的厨房中')
      return
    }
    const { ownKitchens } = this.data
    if (ownKitchens.length === 0) {
      wx.$confirm('收藏需要拥有自己的厨房（厨师长/管厨身份）。是否前往创建？', '暂无可收藏的厨房').then(ok => {
        if (ok) {
          wx.$navigateTo('/subpackages/kitchen/pages/list/index')
        }
      })
      return
    }
    // 打开抽屉，默认选中第一个未收藏的厨房
    const target = ownKitchens.find(k => !k.collected)
    this.setData({
      collectVisible: true,
      selectedKitchenId: target ? target.id : null,
    })
  },

  onCloseCollect() {
    this.setData({ collectVisible: false })
  },

  onGoHome() {
    wx.$navigateTo('/pages/index/index')
  },

  // 抽屉内单选厨房（已收藏的不可选）
  onSelectKitchen(e) {
    const { id } = e.currentTarget.dataset
    const target = this.data.ownKitchens.find(k => String(k.id) === String(id))
    if (!target || target.collected) return
    this.setData({ selectedKitchenId: target.id })
  },

  // 确认收藏到选中的厨房
  onConfirmCollect() {
    const { selectedKitchenId, submitting } = this.data
    if (!selectedKitchenId || submitting) return
    this.setData({ collectVisible: false })
    this._doCollect(selectedKitchenId)
  },

  async _doCollect(kitchenId) {
    if (this.data.submitting) return
    // 记住本次收藏的目标厨房，达上限看完视频扩容后重试收藏到同一个厨房
    this._pendingCollectKitchenId = kitchenId
    this.setData({ submitting: true })
    try {
      await dishCollectApi({ dishId: this.data.dishId, kitchenId })
      wx.vibrateShort({ type: 'medium' })
      wx.$toast('收藏成功')
      // 重新拉取，用后端真实数据刷新收藏数与已收藏厨房
      // （收藏进源菜谱所在厨房时后端不计 collect_count，前端不便自行推算）
      await this._loadShareInfo(this.data.dishId)
    } catch (e) {
      // 命中菜谱数上限（4005）：允许扩容弹「看视频 +1」，否则兜底 toast（service 对 4005 不自动提示）；其余按普通错误处理
      if (e && e.code === LIMIT_REACHED_CODE) {
        if (this._canExpand()) {
          this.selectComponent('#limitPopup').open({
            title: '菜谱数已达上限',
            content: '看个小视频即可 +1，不限次数～或删除部分旧内容后可继续添加',
            confirmText: '📺 看视频 +1',
            cancelText: '以后再说',
            autoClose: false,
          })
        } else {
          wx.$toast(e.message || '菜谱数已达上限')
        }
      } else {
        wx.$printError('收藏失败', e)
      }
    } finally {
      this.setData({ submitting: false })
    }
  },

  // 收藏的目标厨房已在 ownKitchens 中过滤为厨师长/管厨，权限天然满足；入口只看「广告开关 ∧ 该厨房对我还没取消上限」
  // 取的是本次收藏那个目标厨房（不是 active），它才跟后端 4005 的判定口径对得上（§1.4 决策 1）
  _canExpand() {
    const target = (this.data.kitchen?.list || [])
      .find(k => String(k.id) === String(this._pendingCollectKitchenId))
    return canExpandByAd(target?.unlimitedCapacity)
  },

  // 达上限弹窗确认：拉起激励视频，完整观看后为该厨房菜谱上限 +1，成功后自动重试收藏
  onExpandConfirm() {
    const kitchenId = this._pendingCollectKitchenId
    if (!kitchenId || this.data.expanding) return
    this.setData({ expanding: true })
    playExpandAd('dish', async ({ ended }) => {
      this.setData({ expanding: false })
      // 广告结束（看完/中途关/加载失败）后才关闭达上限引导弹窗（点击确认时不立即关）
      const limitPopup = this.selectComponent('#limitPopup')
      limitPopup && limitPopup.close()
      if (!ended) return
      try {
        await kitchenExpandDishApi({ kitchenId })
        await this.getKitchen()
        wx.$toast('已扩容 +1')
        // 扩容成功后重试收藏到同一厨房
        this._doCollect(kitchenId)
      } catch (e) {
        // 扩容接口异常（非4005）service 已提示，此处静默
      }
    })
  },

  // 计算可收藏厨房、整体收藏状态与按钮文案
  _refreshCollectState() {
    const collected = this.data.collectedKitchens || []
    const ownKitchens = (this.data.kitchen?.list || [])
      .filter(k => k.role === KITCHEN_ROLE_ENUM.OWNER || k.role === KITCHEN_ROLE_ENUM.MANAGER)
      .map(k => ({ ...k, collected: collected.some(id => String(id) === String(k.id)) }))
    const allCollected = ownKitchens.length > 0 && ownKitchens.every(k => k.collected)
    const done = ownKitchens.find(k => k.collected)
    let collectText = '📖 收藏到我的厨房'
    if (allCollected) {
      collectText = done ? `✓ 已收藏到「${done.name}」` : '✓ 已收藏'
    }
    // 收藏数为 0 时不展示胶囊
    const collectCount = (this.data.dish && this.data.dish.collectCount) || 0
    this.setData({
      ownKitchens,
      allCollected,
      collectText,
      collectTag: collectCount > 0 ? `${collectCount}人已藏` : '',
    })
  },

  // 落地页不刻意引导二次传播，但保留自然转发能力
  onShareAppMessage() {
    const { dish } = this.data
    if (dish) {
      return this._recipeShare({
        id: dish.id,
        name: dish.name,
        cover: dish.cover,
      })
    }
    return this._baseShare()
  },
})
