import { storePageBehavior, sharePageBehavior } from '../../../../behaviors/index'
import { dishListApi, categoryListApi } from '../../../../apis/index'
import { DEFAULT_DISH_COVER, AD_UNIT_DRAW_BANNER, AD_UNIT_DRAW_INTERSTITIAL } from '../../../../common/constants/index'
import { pickDishes } from '../../../../utils/pick'
import { adServiceEnabled } from '../../../../utils/index'
import slotAudio from './components/slot-machine/slot-audio'

// 插屏广告单例（createInterstitialAd 同一 adUnitId 返回单例，模块级缓存避免重复创建）
let interstitialAd = null
const getInterstitialAd = () => {
  if (!interstitialAd && wx.createInterstitialAd) {
    interstitialAd = wx.createInterstitialAd({ adUnitId: AD_UNIT_DRAW_INTERSTITIAL })
  }
  return interstitialAd
}

Page({
  behaviors: [storePageBehavior, sharePageBehavior],

  data: {
    DEFAULT_DISH_COVER,
    AD_UNIT_DRAW_BANNER,
    loading: true,
    isEmpty: false,
    dishes: [],
    drawCount: 3,
    pickedList: [],
    showResult: false,
    stamped: false,
    cartShake: false,
  },

  async onLoad() {
    this.bindStore({ fields: ['kitchen', 'cart', 'ad'], actions: ['addToCart', 'setPosterDrawData'] })
    await wx.$onKitchenLoaded
    wx.nextTick(() => this._loadDishes())
  },

  onShow() {
    // 空态引导去添加菜谱后返回本页，刷新菜品池退出空态
    if (this.data.isEmpty) this._loadDishes()
  },

  onUnload() {
    this._destroyed = true
  },

  async _loadDishes() {
    const kitchenId = this.data.kitchen?.active?.id
    if (!kitchenId) {
      this.setData({ loading: false, isEmpty: true })
      return
    }
    try {
      const [dishRes, catRes] = await Promise.all([
        dishListApi({ kitchenId }),
        categoryListApi(),
      ])
      const dishes = (dishRes.data || []).map(d => ({
        id: Number(d.id),
        name: d.name,
        photo: d.cover || '',
        rating: d.rating || 0,
        cookCount: d.cookCount || 0,
        categoryId: d.categoryId != null ? Number(d.categoryId) : null,
      }))
      const categoryMap = {}
      ;(catRes.data || []).forEach(c => {
        categoryMap[Number(c.id)] = c.name
      })
      this.setData({ dishes, categoryMap, isEmpty: dishes.length === 0, loading: false, drawCount: Math.min(3, dishes.length) })
    } catch (e) {
      wx.$print('加载菜谱失败', e)
      this.setData({ loading: false })
    }
  },

  // ── 锁定 / 解锁菜品 ──
  onLockToggle(e) {
    const slot = this._getSlotMachine()
    if (slot && slot.data.spinning) {
      wx.$toast('摇奖飞转中，请停机后再锁定~')
      return
    }
    const idx = e.currentTarget.dataset.index
    const pickedList = this.data.pickedList.map((d, i) =>
      i === idx ? { ...d, _locked: !d._locked } : d
    )
    this.setData({ pickedList })
  },

  // ── 抽取（slot-machine 组件 start 事件：页面只做抽菜业务，动画交给组件）──
  onSpinStart(e) {
    this._startSpin(e.detail.count)
  },

  _startSpin(count) {
    const { dishes, pickedList } = this.data
    if (!dishes.length) return
    // 有锁定菜时便签在飞转中仍可见可点，这里必须拦截，
    // 否则未锁定项被提前移除而 spin 静默早退，表现为“重摇不滚动”
    const slot = this._getSlotMachine()
    if (slot && slot.data.spinning) {
      wx.$toast('摇奖飞转中，请停机后再摇~')
      return
    }

    // 重摇时保留已锁定的菜品，只为未锁定槽位抽取新菜
    const locked = pickedList.filter(d => d._locked)
    const needCount = count - locked.length
    const lockedIds = locked.map(d => d.id)
    // 锁定菜永远排除（pickDishes 在池子不足时会跳过排除集，所以它是硬性约束，不能只靠事后去重）
    const excludeIds = [...lockedIds]
    // 容量允许时，额外排除当前批次里未锁定的菜，避免重摇结果原地不变
    const currentUnlockedIds = pickedList.filter(d => !d._locked).map(d => d.id)
    if (needCount > 0 && dishes.length - lockedIds.length - currentUnlockedIds.length >= needCount) {
      excludeIds.push(...currentUnlockedIds)
    }
    
    // 池子被锁定菜占满时没菜可抽；剩余不足时 pickDishes 会允许重复抽，不拦截
    if (needCount > 0 && dishes.length - lockedIds.length <= 0) {
      wx.$toast('没有更多菜可以抽了~')
      return
    }

    let picked
    if (needCount > 0) {
      picked = pickDishes(dishes, { count: needCount, excludeIds })
      // 防御：pickDishes 在池子不足时会跳过排除，可能抽中锁定菜，这里去重强制不重复
      const lockedIdSet = new Set(locked.map(d => d.id))
      picked = picked.filter(d => !lockedIdSet.has(d.id))
    } else {
      picked = []
    }
    const allPicked = [...locked, ...picked]
    if (!allPicked.length) return

    // 每列滚轮对应结果里的一道菜，交给 slot-machine 组件单向飞转
    if (locked.length > 0) {
      // 有锁定菜品：便签保持可见，立即移除未锁定项，摇完后由 onSpinChange 追加新菜
      this.setData({ drawCount: count, stamped: false, pickedList: locked })
    } else {
      // 无锁定：正常隐藏便签，摇完再出
      this.setData({ drawCount: count, showResult: false, stamped: false })
    }
    this._getSlotMachine().spin({ value: allPicked })
  },

  // ── 出结果（slot-machine 组件滚轮停稳后触发 change）──
  onSpinChange(e) {
    const result = e.detail.value || []
    // 保留当前已锁定项（_locked: true），新摇出的菜追加在后面
    const locked = this.data.pickedList.filter(d => d._locked)
    const lockedIds = new Set(locked.map(d => d.id))
    const newPicked = result.filter(d => !lockedIds.has(d.id))
    const pickedList = [...locked, ...newPicked]
    this.setData({
      pickedList,
      showResult: true,
    })
    wx.vibrateShort({ type: 'medium' })
    setTimeout(() => {
      if (this._destroyed) return
      // 印章落下与盖印闷响同步（音效上下文已由组件点击手势 init）
      slotAudio.playStampThud()
      this.setData({ stamped: true })
      // 便签出纸、印章落定后弹出插屏广告（延迟避免刚出结果就被遮挡）
      setTimeout(() => {
        if (this._destroyed) return
        this._showInterstitial()
      }, 600)
    }, 480)
  },

  // ── 插屏广告：受全局开关与会员免广告双重控制，无填充/频次上限/未生效静默忽略 ──
  _showInterstitial() {
    // 判据收在 utils/ad.js 单点：不读 this.data.ad，因为那只带得出广告总开关、带不出会员态（§10.2）
    if (!adServiceEnabled()) return
    const ad = getInterstitialAd()
    if (!ad) return
    ad.show().catch(() => {
      // show 失败（拉不到/超频次）不提示，不打断摇菜体验
    })
  },

  _getSlotMachine() {
    if (!this._slotMachineRef) {
      this._slotMachineRef = this.selectComponent('#slotMachine')
    }
    return this._slotMachineRef
  },

  // 竹篮摇晃动画（同菜谱页）
  _shakeCartBasket() {
    this.setData({ cartShake: true })
    setTimeout(() => {
      this.setData({ cartShake: false })
    }, 500)
  },

  onAddAllToCart() {
    const { pickedList } = this.data
    if (!pickedList.length) return
    pickedList.forEach(dish => {
      this.addToCart({ id: dish.id, name: dish.name, photo: dish.photo })
    })
    wx.vibrateShort({ type: 'medium' })
    wx.$toast(`已加入 ${pickedList.length} 道菜`)
    this._shakeCartBasket()
    // 入篮后收起便签，恢复初始未抽状态，可重新摇菜
    this.setData({ pickedList: [], showResult: false, stamped: false })
  },

  // ── 导航 ──
  // 空态去添加菜谱（无 id 即新增）
  onAddDish() {
    wx.$navigateTo('/subpackages/dish/pages/add-edit/index')
  },

  // 点击便签行：弹出菜谱详情弹窗（纯查看，不显示加购按钮）
  onDishTap(e) {
    const { id } = e.currentTarget.dataset
    if (id) {
      this.selectComponent('#dishPopup').open(id, false)
    }
  },

  onViewDetail(e) {
    const { id } = e.currentTarget.dataset
    if (id) {
      wx.$navigateTo(`/subpackages/dish/pages/detail/index?id=${id}`)
    }
  },

  // 弹窗内「查看完整菜谱」：dish-popup 只抛出 detail 事件（携 id），需页面自己关弹窗 + 路由
  onPopupDetail(e) {
    const { id } = e.detail
    this.selectComponent('#dishPopup').close()
    if (id) {
      wx.$navigateTo(`/subpackages/dish/pages/detail/index?id=${id}`)
    }
  },

  onGoOrder() {
    const cart = this.data.cart
    if (!cart || cart.quantity === 0) {
      wx.$toast('请先选择菜谱')
      return
    }
    wx.$navigateTo('/subpackages/cookorder/pages/confirm/index')
  },

  // 采购清单：数据源为当前抽取结果（每行一道计一份），不关便签，返回时摇奖状态仍在
  onGoShopping() {
    const dishIds = this.data.pickedList.map(d => d.id).join(',')
    if (!dishIds) return
    wx.$navigateTo(`/subpackages/dish/pages/shopping/index?dishIds=${dishIds}&source=draw`)
  },

  // 点击菜篮：打开购物车弹窗，弹窗内确认后仍走 onGoOrder
  onOpenCartPopup() {
    this.selectComponent('#cartPopup').open()
  },

  // 点击分享：弹出全局 share-popup（转发/海报二选一）
  onOpenShareSheet() {
    this.selectComponent('#sharePopup').open()
  },

  // share-popup 选择回调：选「生成海报」携当前抽取结果跳海报页
  onShareSelect(e) {
    if (e.detail.key !== 'poster') return
    const { pickedList } = this.data
    if (!pickedList.length) return
    this.setPosterDrawData({
      dishes: pickedList.map(d => ({
        id: d.id,
        name: d.name,
        photo: d.photo || DEFAULT_DISH_COVER,
        rating: d.rating || 0,
        cookCount: d.cookCount || 0,
      })),
      kitchenName: this.data.kitchen?.active?.name || '',
    })
    wx.$navigateTo('/subpackages/poster/pages/draw/index')
  },

  // 重写分享（摇奖结果卡片：pickedList 传入 behavior 生成分状态文案）
  onShareAppMessage() {
    return this._drawShare(this.data.pickedList)
  },
  // 重写朋友圈分享（标题复用分状态文案）
  onShareTimeline() {
    return this._drawTimeline(this.data.pickedList)
  },
})
