import dayjs from 'dayjs'
import { storePageBehavior, sharePageBehavior } from '../../../../behaviors/index'
import { dishShoppingListApi } from '../../../../apis/index'
import { DEFAULT_DISH_COVER, AD_UNIT_SHOPPING_LIST_CUSTOM } from '../../../../common/constants/index'
import { aggregateShoppingList, buildSourceKey } from './aggregate'
import shoppingAudio from './audio'

// 勾选缓存统一按条目归属 id 分片：订单类 orderId:{orderId} 长期有效，菜品类 dishId:{dishId} 隔日归零
// 分片组名刻意区别于旧版整块 key 的 dish: 前缀，避免 djb2 哈希撞车真实菜 id 时误读
// 恢复口径为「未勾优先」：条目在其全部归属分片都勾过才算已买（两菜共用食材只买一份不算买齐）
const STORAGE_PREFIX = 'shopping-list:'
const shardKey = (group, id) => `${STORAGE_PREFIX}${group}:${id}`
// 本地缓存回收：超 90 天未写入的勾选分片基本不会再翻，进页时顺手扫掉；扫描标记 key 防同日重复遍历
const CACHE_TTL_DAYS = 90
const SWEEP_FLAG_KEY = `${STORAGE_PREFIX}__swept`

// 可折叠区域映射：开关字段 / 内容测量选择器 / 容器高度字段
const COLLAPSE_AREAS = [
  { flag: 'dishListExpanded', inner: '#dishListInner', hKey: 'dishListH' },
  { flag: 'requiredExpanded', inner: '#requiredInner', hKey: 'reqH' },
  { flag: 'optionalExpanded', inner: '#optionalInner', hKey: 'optH' },
  { flag: 'seasoningExpanded', inner: '#seasoningInner', hKey: 'seaH' },
]

Page({
  // 只取 behavior 里的基础分享（无 dataset.type 就走 _baseShare）：卡片为小程序通用文案 + 落地首页
  behaviors: [storePageBehavior, sharePageBehavior],

  data: {
    DEFAULT_DISH_COVER,
    AD_UNIT_SHOPPING_LIST_CUSTOM,
    loading: true,
    loadFailed: false,
    notFound: false,
    noItems: false,

    source: '',
    canAddToCart: false,

    required: [],
    optional: [],
    seasonings: [],
    // 折叠状态：可不买/调料默认折叠，需要买默认展开
    requiredExpanded: true,
    optionalExpanded: false,
    seasoningExpanded: false,

    // 顶部主进度：三档全统计（需要买 + 可不买 + 调料），全部勾完才触发印章
    itemTotal: 0,
    itemChecked: 0,
    progressPct: 0,
    allChecked: false,
    reqChecked: 0,
    optChecked: 0,
    seaChecked: 0,

    // 来源菜列表（封面+名称+数量）：默认折叠，点订单行展开；收起态只显示汇总句不重复菜名
    dishCount: 0,
    dishList: [],
    dishListExpanded: false,

    // 展开/折叠过渡：容器高度显式值（px，量取内容真实高度），配合 CSS height transition
    dishListH: 0,
    reqH: 0,
    optH: 0,
    seaH: 0,
  },

  async onLoad(options) {
    this.sweepStaleCache()
    this.setData({
      source: options.source || '',
      // 篮子/抽菜/帮我选来源提供「入篮」；订单来源已下单不显示
      canAddToCart: ['cart', 'draw', 'pick'].includes(options.source),
    })
    // query 为逗号分隔 id 串；kitchenId 不走入口透传，统一取全局 store 当前厨房
    this.dishIds = (options.dishIds || '').split(',').filter(Boolean).map(Number)
    this.cookOrderIds = (options.cookOrderIds || '').split(',').filter(Boolean).map(Number)
    // 旧版整块缓存 key，仅用于升级时回退读取历史进度
    this.sourceKey = buildSourceKey(options.source || '', this.dishIds)
    // 订单类勾选属于订单本身（订单唯一，反复进入应延续），不受跨天重置影响；
    // cart/draw/pick 是菜品类入口，勾选按菜 id 分片互通、隔日归零（「今天的菜」场景）
    this.orderBased = ['orderList', 'orderDetail', 'latest'].includes(options.source)
    this.shardGroup = this.orderBased ? 'orderId' : 'dishId'
    this.checked = new Set()
    // 调料默认全勾，unchecked 记录用户显式取消的调料 tombstone，重拉/重进时不被勾回去
    this.unchecked = new Set()

    // 朋友圈分享只能落在当前页且只带 inviterId，无来源 id 时不发请求（后端会报「两者不能同时为空」），直接给空态
    if (!this.dishIds.length && !this.cookOrderIds.length) {
      this.setData({ loading: false, notFound: true })
      return
    }

    this.bindStore({ fields: ['kitchen'], actions: ['addToCart'] })
    await wx.$onKitchenLoaded
    this.getData()
  },

  // 回收过期勾选缓存：同一自然日最多扫一次（遍历全量 key 是同步开销），只动本页面前缀的 key
  sweepStaleCache() {
    try {
      const today = dayjs().format('YYYY-MM-DD')
      if (wx.getStorageSync(SWEEP_FLAG_KEY) === today) return
      wx.setStorageSync(SWEEP_FLAG_KEY, today)
      const deadline = Date.now() - CACHE_TTL_DAYS * 24 * 60 * 60 * 1000
      const keys = (wx.getStorageInfoSync().keys || []).filter(k => k.startsWith(STORAGE_PREFIX) && k !== SWEEP_FLAG_KEY)
      keys.forEach(key => {
        try {
          const cache = wx.getStorageSync(key)
          // 缺 updatedAt 的是更早版本或损坏数据，无法判龄，一并回收
          if (!cache || typeof cache.updatedAt !== 'number' || cache.updatedAt < deadline) {
            wx.removeStorageSync(key)
          }
        } catch (e) {
          try { wx.removeStorageSync(key) } catch (err) { /* 忽略 */ }
        }
      })
    } catch (e) {
      // 存储异常不阻塞页面加载
    }
  },

  onShow() {
    // 篮子/订单可能在别处被改动，返回时静默重拉对账（首进由 onLoad 触发）
    if (this.loaded) this.getData(true)
  },

  onUnload() {
    // 音效内的 resume 轮询是页面级定时器，离开必须清掉，否则后台空转
    shoppingAudio.destroy()
  },

  // silent=true 时保留上次渲染，仅完成后刷新（弱网失败不清屏）
  async getData(silent = false) {
    if (!silent) this.setData({ loading: true, loadFailed: false })
    try {
      const kitchenId = this.data.kitchen?.active?.id
      const r = await dishShoppingListApi({
        kitchenId,
        dishIds: this.dishIds,
        cookOrderIds: this.cookOrderIds,
      })
      this.dishes = r.data || []
      if (!this.dishes.length) {
        this.setData({ loading: false, loadFailed: false, notFound: true })
        return
      }
      this.buildAgg()
      this.loaded = true
    } catch (e) {
      // service 已统一 toast；首拉失败给重试按钮，刷新失败保留上次渲染
      if (!this.data.dishCount) this.setData({ loading: false, loadFailed: true })
    }
  },

  // 聚合 + 恢复勾选：条目按归属分片做「未勾优先」判定；调料默认全勾（tombstone 除外）
  buildAgg() {
    const { required, optional, seasonings } = aggregateShoppingList(this.dishes)
    this.items = { required, optional, seasonings }
    const allKeys = [...required, ...optional, ...seasonings].map(i => i.key)
    // 恢复勾选：逐条目比对它归属的全部分片（订单/菜），片片都勾过才算已买；任一分片缺失即未勾
    // 幽灵 key 天然不参与（只遍历当前条目），无需再和缓存做交集对账
    const shards = this.loadShards()
    this.checked = new Set()
    this.unchecked = new Set()
    const fillState = item => {
      const parts = this.itemShardIds(item).map(id => shards.get(id)).filter(Boolean)
      if (parts.length && parts.every(p => p.checked.includes(item.key))) this.checked.add(item.key)
      // tombstone（调料显式取消）只要命中任一分片就生效，不被「默认全勾」补回去
      if (parts.some(p => p.unchecked.includes(item.key))) this.unchecked.add(item.key)
    }
    ;[...required, ...optional, ...seasonings].forEach(fillState)
    seasonings.forEach(i => {
      if (!this.unchecked.has(i.key)) this.checked.add(i.key)
    })

    this.setData({
      loading: false,
      loadFailed: false,
      notFound: false,
      noItems: !allKeys.length,
      dishCount: this.dishes.length,
      dishList: this.dishes.map(d => ({ dishId: d.dishId, dishName: d.dishName, dishCover: d.dishCover, quantity: d.quantity })),
    })
    this.renderGroups()
  },

  // 勾选标记 + 已买沉底 + 各组计数与主进度
  renderGroups() {
    const sink = list => {
      const flagged = list.map(i => ({ ...i, checked: this.checked.has(i.key) }))
      return [...flagged.filter(i => !i.checked), ...flagged.filter(i => i.checked)]
    }
    const required = sink(this.items.required)
    const optional = sink(this.items.optional)
    const seasonings = sink(this.items.seasonings)
    const itemTotal = required.length + optional.length + seasonings.length
    const itemChecked = required.filter(i => i.checked).length + optional.filter(i => i.checked).length + seasonings.filter(i => i.checked).length
    this.setData({
      required,
      optional,
      seasonings,
      reqChecked: required.filter(i => i.checked).length,
      optChecked: optional.filter(i => i.checked).length,
      seaChecked: seasonings.filter(i => i.checked).length,
      itemTotal,
      itemChecked,
      progressPct: itemTotal ? Math.round((itemChecked / itemTotal) * 100) : 0,
      allChecked: itemTotal > 0 && itemChecked === itemTotal,
    }, () => {
      // 勾选沉底后行数/折行可能变化，重新量取已展开区域高度，避免固定高度裁切内容
      this.syncCollapseHeights()
    })
  },

  // 整行 toggle 勾选，即时写缓存；取消勾选调料时记 tombstone，不被「默认全勾」覆盖回去
  onToggle(e) {
    const { group, key } = e.currentTarget.dataset
    const isChecked = this.checked.has(key)
    // 首次出声发生在触摸回调栈内，AudioContext 恰好在此时创建，满足 iOS 的手势解锁要求
    if (isChecked) {
      shoppingAudio.playUncheck()
      this.checked.delete(key)
      if (group === 'seasoning') this.unchecked.add(key)
    } else {
      shoppingAudio.playCheck()
      this.checked.add(key)
      this.unchecked.delete(key)
    }
    this.writeChecked(key)
    // 盖章声只认「未买齐 → 买齐」这一次翻转，静默重拉与清空后的重绘不响
    const wasAllChecked = this.data.allChecked
    this.renderGroups()
    if (!wasAllChecked && this.data.allChecked) {
      shoppingAudio.playStamp()
      shoppingAudio.playChime()
    }
  },

  // 统一展开/折叠入口（横条 + 三分组）：量取内容高度后显式设高，让 height 可过渡
  async onToggleArea(e) {
    const area = COLLAPSE_AREAS.find(a => a.flag === e.currentTarget.dataset.flag)
    if (!area) return
    if (this.data[area.flag]) {
      shoppingAudio.playPaper(false)
      // 收起：当前固定高 → 0，transition 自动接管
      this.setData({ [area.flag]: false, [area.hKey]: 0 })
      return
    }
    shoppingAudio.playPaper(true)
    const height = await this.measureHeight(area.inner)
    this.setData({ [area.flag]: true, [area.hKey]: height })
  },

  // 量取折叠区内容自然高度（overflow hidden 不影响子元素布局，收起态也可测）
  measureHeight(selector) {
    return new Promise(resolve => {
      const query = wx.createSelectorQuery()
      query.select(selector).boundingClientRect(rect => resolve(rect ? rect.height : 0))
      query.exec()
    })
  },

  // 刷新已展开区域的测量高度（沉底重排/静默重拉后同步）
  async syncCollapseHeights() {
    const opened = COLLAPSE_AREAS.filter(a => this.data[a.flag])
    if (!opened.length) return
    const updates = {}
    for (const area of opened) {
      updates[area.hKey] = await this.measureHeight(area.inner)
    }
    this.setData(updates)
  },

  // 本次入口涉及的全部分片（订单/菜）：隔日分片视作空缓存；菜品类某菜今日无分片时回退旧版整块缓存（升级兼容）
  loadShards() {
    const ids = this.orderBased ? this.cookOrderIds : this.dishIds
    const today = dayjs().format('YYYY-MM-DD')
    let legacy
    const caches = ids.map(id => {
      const cache = this.readShard(this.shardGroup, id)
      const untouched = !cache.checked.length && !cache.unchecked.length && !cache.date
      // 菜品类某菜无分片时回退旧版整块缓存（升级前的当日进度不丢）
      if (untouched && !this.orderBased) {
        if (!legacy) legacy = this.readLegacyCache()
        return legacy.date === today ? legacy : { checked: [], unchecked: [] }
      }
      // 菜品类分片看日期（隔日归零）；订单类分片不带日期，长期有效
      if (this.orderBased) return cache
      return cache.date === today ? cache : { checked: [], unchecked: [] }
    })
    const map = new Map()
    ids.forEach((id, i) => map.set(id, caches[i]))
    return map
  },

  // 旧版整块缓存（cart / dish:{ids哈希}），菜品类某菜无当日分片时回退读（lazy，一次会话内最多读一次）
  readLegacyCache() {
    const legacyKey = STORAGE_PREFIX + (this.sourceKey || 'cart')
    try {
      const cache = wx.getStorageSync(legacyKey)
      return {
        checked: Array.isArray(cache && cache.checked) ? cache.checked : [],
        // 旧版缓存无 unchecked 字段，兼容为空数组
        unchecked: Array.isArray(cache && cache.unchecked) ? cache.unchecked : [],
        date: typeof cache?.date === 'string' ? cache.date : '',
      }
    } catch (e) {
      // 缓存损坏/版本变更：重置该 key，不影响清单展示
      try { wx.removeStorageSync(legacyKey) } catch (err) { /* 忽略 */ }
      return { checked: [], unchecked: [], date: '' }
    }
  },

  // 条目的归属分片 id：订单类取归属订单集（老数据无该字段时兜底本次全部订单），菜品类取归属菜集
  itemShardIds(item) {
    if (!item) return []
    if (!this.orderBased) return item.dishIds || []
    return item.orderIds.length ? item.orderIds : this.cookOrderIds
  },

  // 本次勾选/取消写回：按条目归属 id 逐个分片回写（组合视图勾一次，单独进入各归属都认）
  writeChecked(key) {
    if (!key) return
    try {
      const ids = this.itemShardIds(this.findItem(key))
      ids.forEach(id => this.writeShard(this.shardGroup, id, key, !this.orderBased))
    } catch (e) {
      // 存储写入失败不阻塞勾选
    }
  },

  // 按 key 找聚合条目（取条目归属 id 集 dishIds / orderIds）
  findItem(key) {
    const { required = [], optional = [], seasonings = [] } = this.items || {}
    return [...required, ...optional, ...seasonings].find(i => i.key === key)
  },

  readShard(group, id) {
    try {
      const cache = wx.getStorageSync(shardKey(group, id))
      return {
        checked: Array.isArray(cache && cache.checked) ? cache.checked : [],
        unchecked: Array.isArray(cache && cache.unchecked) ? cache.unchecked : [],
        date: typeof cache?.date === 'string' ? cache.date : '',
      }
    } catch (e) {
      try { wx.removeStorageSync(shardKey(group, id)) } catch (err) { /* 忽略 */ }
      return { checked: [], unchecked: [], date: '' }
    }
  },

  // 增量更新单个分片：只动本次变更的 key，其余保持该订单/菜已有进度；菜品类带 date 供隔日判新鲜度
  writeShard(group, id, key, withDate) {
    const cache = this.readShard(group, id)
    const checked = new Set(cache.checked)
    const unchecked = new Set(cache.unchecked)
    if (this.checked.has(key)) checked.add(key)
    else checked.delete(key)
    if (this.unchecked.has(key)) unchecked.add(key)
    else unchecked.delete(key)
    const payload = { checked: [...checked], unchecked: [...unchecked], updatedAt: Date.now() }
    if (withDate) payload.date = dayjs().format('YYYY-MM-DD')
    wx.setStorageSync(shardKey(group, id), payload)
  },

  onClearChecked() {
    wx.$confirm('清除后需要重新核对勾选，确定清空当前清单的勾选进度？', '清空勾选').then(ok => {
      if (!ok) return
      // 回到初始态：食材全部未勾，调料恢复默认全勾
      this.checked = new Set(this.items.seasonings.map(i => i.key))
      this.unchecked = new Set()
      if (this.orderBased) {
        // 勾选分散在各归属分片里，逐个删除；调料默认态下次进入自动补，无需写入
        this.cookOrderIds.forEach(oid => {
          try { wx.removeStorageSync(shardKey(this.shardGroup, oid)) } catch (e) { /* 忽略 */ }
        })
      } else {
        this.dishIds.forEach(did => {
          try { wx.removeStorageSync(shardKey(this.shardGroup, did)) } catch (e) { /* 忽略 */ }
        })
        // 菜品类隔日本就归零，此处同步删除旧版整块 key，防止分片全空时被回退读出
        try { wx.removeStorageSync(STORAGE_PREFIX + (this.sourceKey || 'cart')) } catch (e) { /* 忽略 */ }
      }
      this.renderGroups()
      wx.$toast('已清空勾选')
      shoppingAudio.playClear()
    })
  },

  // 把清单来源菜按份数入篮（篮子/抽菜/帮我选来源）
  onAddToCart() {
    this.dishes.forEach(d => {
      for (let i = 0; i < (d.quantity || 1); i++) {
        this.addToCart({ id: d.dishId, name: d.dishName, photo: '' })
      }
    })
    shoppingAudio.playChime()
    wx.vibrateShort({ type: 'medium' })
    wx.$toast('已把这几道菜放入点菜篮 🧺')
    setTimeout(() => {
      wx.$navigateBack()
    }, 800)
  },

  onBack() {
    wx.$navigateBack()
  },

  onRetry() {
    this.getData()
  },
})
