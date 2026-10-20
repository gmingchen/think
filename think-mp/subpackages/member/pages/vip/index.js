import dayjs from 'dayjs'
import { storePageBehavior, sharePageBehavior } from '../../../../behaviors/index'
import { buyVip, canBuyVip } from '../../../../utils/vipPay'
import { requestSubscribeMessage, setClipboardData } from '../../../../utils/wx-api'
import { subscribeSyncApi } from '../../../../apis/index'
import {
  MEMBER_TIER_TEXT,
  MEMBER_TIER_SHORT_TEXT,
  MEMBER_BENEFITS,
  MEMBER_PERIOD_TEXT,
  MEMBER_PLAN_TIER,
  MEMBER_PLAN_SUFFIX_TEXT,
  MEMBER_RECORD_STATUS_TEXT,
  MEMBER_EXPIRE_TMPL_ID,
} from '../../../../common/constants/index'

// 档位顺序 = 畅厨在上（§5.1 定稿理由 2：价格锚定要「先看贵再看便宜」）。轻享那张是低门槛兜底，不是主推位
const TIER_ORDER = [2, 1]

// 档位角标 emoji / 小标签：纯 UI 文案，不涉及任何价格，不进 constants 是为了让它跟着界面稿走
const TIER_EMOJI = { 2: '👑', 1: '🌿' }
const TIER_TAG = { 2: '掌勺无忧', 1: '轻量体验' }

/**
 * 分 → 展示串（只用于 `saved` 这一个**派生**数字）
 * ⚠️ 成交价一律读后端下发的 priceText，不许在这里算：
 *    「省 ¥54.9」是可由原价与售价推出来的事实，格式化它不产生第二份真相——
 *    改价时后端下发什么就跟着变，不需要发版
 */
const yuan = (cents) => String((cents / 100).toFixed(2).replace(/\.?0+$/, ''))

const periodLabel = (plan) => MEMBER_PERIOD_TEXT[plan.months] || plan.name

/**
 * 按档位分组出两张卡（§5.1）：卡内只写「月价起 + 年付低至」两个价格口径，周期价格留给下面的胶囊卡
 * `enabled = 0` 的套餐后端根本不下发（§7.1），所以退一个 SKU 是这里自动少一行，零前端改动
 * ⚠️ 首购优惠本版不在本页展示：成交价直接取后端 priceText，不使用 firstPriceText，也不画划线原价
 */
const buildTiers = (plans) => {
  const groups = []
  TIER_ORDER.forEach((tier) => {
    // 同档内按 months 升序：加半年付时它自然落在季与年之间，不必回来改这段（§3.2）
    const list = plans.filter(item => item.tier === tier).sort((a, b) => a.months - b.months)
    if (!list.length) return
    const month = list[0]
    const longest = list[list.length - 1]
    groups.push({
      tier,
      emoji: TIER_EMOJI[tier],
      tag: TIER_TAG[tier],
      name: MEMBER_TIER_TEXT[tier],
      // 权益摘要取后端按档位给的那句（轻享那句自带「容量仍受限」，不写会被读成「会员都加容量」）
      desc: month.desc,
      recommend: tier === 2,
      fromText: month.priceText,
      // 「年付低至」只有一个周期时没意义（月付卡写「低至」是自我矛盾），所以 list 只有一行就不写
      toText: list.length > 1 ? longest.priceText : '',
      periods: list.map(item => ({
        code: item.code,
        months: item.months,
        label: periodLabel(item),
        priceText: item.priceText,
        savedText: item.saved ? `省¥${yuan(item.saved)}` : '',
      })),
    })
  })
  return groups
}

// 默认选中哪个周期：首购特惠不在本页展示，默认落最低门槛的月付（第一个）即可，不再把人往某个促销档上带
const defaultPeriod = (group) => group.periods[0]

// 顶部状态卡（§5.1 第一块 + §5.2 前四行）：只画「这人现在是什么身份」，不画可买的东西
// kind 驱动右侧装饰：active→续费钮 / expired→重新激活角标 / guest→锅图标
const buildStatus = (member) => {
  const expireText = member.expireAt ? dayjs(member.expireAt).format('YYYY-MM-DD') : ''
  if (member.effective) {
    return {
      kind: 'active',
      gold: true,
      title: `${MEMBER_TIER_TEXT[member.tier]} · 剩余 ${member.remainDays} 天`,
      note: expireText ? `到期：${expireText}` : '',
      action: '立即续费',
    }
  }
  // 已过期与未开通共用一版判据，靠 expireAt 有没有区分，不为「过期」单做一种贵价视觉（§4.1：不该有羞辱感）
  if (expireText) {
    return {
      kind: 'expired',
      gold: false,
      title: '会员已过期',
      note: `已于 ${expireText} 到期`,
    }
  }
  return {
    kind: 'guest',
    gold: false,
    title: '还不是会员',
    note: '免广告 · 自建厨房的分类与菜谱不限',
  }
}

const buildCtaText = (member, plan) => {
  if (!member.enabled) return '即将开放'
  // 一档升二档不写价格：剩余天数按 §7.5 折算，最终到账天数依赖点击那一刻的剩余量，
  // 在这里报一个价就是给客服留一条对不上的账（§5.4 第四条「不给公式也不给例子」同理）
  if (member.effective && member.tier === 1 && plan.tier === 2) return '升级 · 补差价折算'
  return `${member.effective ? '续费' : '立即开通'} · ${plan.priceText}`
}

// 记录行：planCode 拆前缀取档位、拆后缀取周期（历史 SKU 可能已下架，不能靠 plans 列表反查）
// 状态色只靠文案里有没有「退」字判，不猜后端 status 数字码，避免枚举对不上时上错色
const buildRecords = (records) => (records || []).map((item) => {
  const [prefix, suffix] = String(item.planCode || '').split('_')
  const tierText = MEMBER_TIER_TEXT[MEMBER_PLAN_TIER[prefix]] || item.planCode
  const statusText = MEMBER_RECORD_STATUS_TEXT[item.status] || ''
  return {
    // 交易单号是发放通知回填的，正常必有；理论上缺值的脏数据用 paidAt+status 兜底唯一性
    key: `${item.transactionId}-${item.paidAt}-${item.status}`,
    // 点行复制的退款凭证：与用户微信账单里的「交易单号」一致，报给客服就能查单
    transactionId: item.transactionId || '',
    label: `${tierText} · ${MEMBER_PLAN_SUFFIX_TEXT[suffix] || suffix || ''}`.trim(),
    priceText: item.amount != null ? `¥${yuan(item.amount)}` : '',
    dateText: item.paidAt ? dayjs(item.paidAt).format('YYYY-MM-DD HH:mm') : '',
    statusText,
    statusClass: statusText.indexOf('退') >= 0 ? 'refund' : 'active',
  }
})

Page({
  behaviors: [storePageBehavior, sharePageBehavior],

  data: {
    loading: true,
    // member.enabled 原值（§5.2 最后一行）：置灰只灰 CTA，价格与权益照常展示，不隐藏入口
    open: false,
    // 低版本基础库（§2.1 门槛 2.19.2）：常驻弱提示 + CTA 置灰，点它要说清为什么点不动
    lowVersion: false,
    status: null,
    tiers: [],
    currentTierName: '',
    periods: [],
    tier: 0,
    plan: null,
    ctaText: '',
    ctaDisabled: false,
    purchasing: false,
    noticeOpen: false,
    benefits: MEMBER_BENEFITS,
    benefitHeader: { free: '免费版', lite: MEMBER_TIER_SHORT_TEXT[1], pro: MEMBER_TIER_SHORT_TEXT[2] },
    records: [],
  },

  onLoad() {
    // 选中态放在实例上而不是 data 上：它不是渲染结果而是渲染入参，走 setData 就会多一次中间帧
    this.selection = { tier: 0, code: '' }
    this.setData({ lowVersion: !canBuyVip() })
    // 绑 member 字段只为让 this.data.member 跟着 store 同步（onTierTap/onPeriodTap/onBuy 的 render 入参靠它）；
    // 渲染不走 observer，改由 onShow 在请求返回后手动调 render
    this.bindStore({ fields: ['member'], actions: ['getMember'] })
  },

  async onShow() {
    // 每次进页重拉（§5.3）：从客服、分享卡片、其他页回来时，「剩 N 天」与广告状态要准。
    // ⚠️ 先等登录就绪再拉：/member/info 是登录态接口，冷启直接落进本页（分享卡片）时
    //    onLaunch 的 login() 还没回，不等同步报 4001 —— 而失败会把 loaded 置 true、
    //    plans 为空，页面就画成「无可买套餐 + 即将开放」这张假空态（与置灰版看不出区别）
    await wx.$onLogined
    // getMember 是异步 action，await 到它把 store.member 写完（绑定反应会同步刷新 this.data.member）之后，再手动渲染
    await this.getMember()
    // 用 nextTick 把渲染排到下一个周期：先让这一轮 getMember 触发的 setData（store 绑定回写 this.data.member）落定，
    // 再读 this.data.member 渲染，避免拿到尚未刷新的中间态
    wx.nextTick(() => this.render(this.data.member || {}))
  },

  render(member) {
    if (!member.loaded) {
      // loaded 在成功与失败都置位，所以停在这里只可能是「还没回来」：整页骨架，不画半张卡
      this.setData({ loading: true })
      return
    }
    const tiers = buildTiers(member.plans || [])
    const base = {
      loading: false,
      open: !!member.enabled,
      status: buildStatus(member),
      records: buildRecords(member.records),
    }
    if (!tiers.length) {
      // member_plan 空表或全部下架：无从选起。这不是「未开通」，CTA 只能灰着，
      // 靠 service 那句拒单文案之外的空态由本页兜住（页面照常可截图，审核要看的就是这些）
      this.setData({ ...base, tiers: [], currentTierName: '', periods: [], tier: 0, plan: null, ctaText: '即将开放', ctaDisabled: true })
      return
    }
    // 选中态保持：onShow 重拉不能把人已经点选的档位跳回默认值，他可能正在比两个周期的价差
    // || {} 是兜底：万一 render 在 onLoad 给 selection 赋值之前就被触发，也不至于解构 undefined 抛错
    const { tier: prevTier, code: prevCode } = this.selection || {}
    const group = tiers.find(item => item.tier === prevTier) || tiers[0]
    const plan = group.periods.find(item => item.code === prevCode) || defaultPeriod(group)
    this.selection = { tier: group.tier, code: plan.code }
    // plan 带 tier 是为了 CTA 那句「升级 · 补差价折算」判得出档位（periods 里没有这个字段）
    const selected = { ...plan, tier: group.tier }
    this.setData({
      ...base,
      tiers: tiers.map(item => ({ ...item, selected: item.tier === group.tier })),
      currentTierName: MEMBER_TIER_TEXT[group.tier],
      periods: group.periods.map(period => ({ ...period, selected: period.code === plan.code })),
      tier: group.tier,
      plan: selected,
      ctaText: buildCtaText(member, selected),
      // 置灰的三个来源：功能未放开 / 低版本 / 无可买 SKU。购买中由 wxml 单独画 loading，不改这个判据
      ctaDisabled: !member.enabled || this.data.lowVersion,
    })
  },

  onTierTap(e) {
    const { tier } = e.currentTarget.dataset
    if (!tier || tier === this.selection.tier) return
    // 换档位后周期必须在新档位里重选：畅厨的季付 code 拿到轻享那边是不存在的 SKU（code 清掉交给 render 选默认）
    this.selection = { tier, code: '' }
    this.render(this.data.member || {})
  },

  onPeriodTap(e) {
    const { code } = e.currentTarget.dataset
    if (!code || code === this.selection.code) return
    // 只改选中项，其余渲染模型由 render 统一重算（换周期就换价格，CTA 那句也跟着换）
    this.selection = { ...this.selection, code }
    this.render(this.data.member || {})
  },

  onToggleNotice() {
    this.setData({ noticeOpen: !this.data.noticeOpen })
  },

  onAgreement() {
    wx.$navigateTo('/subpackages/user/pages/agreement/index')
  },

  onPrivacy() {
    wx.$navigateTo('/subpackages/user/pages/privacy/index')
  },

  async onBuy() {
    const { purchasing, ctaDisabled, open, lowVersion, plan } = this.data
    if (purchasing || !plan) return
    if (!open) {
      // 置灰不是 bug，得说清为什么点不动——静默会让人怀疑自己网络坏了（§4.1 同口径）
      wx.$toast('会员功能即将开放')
      return
    }
    if (lowVersion) {
      wx.$toast('请升级微信后再试')
      return
    }
    if (ctaDisabled) return
    // 点「开通」先弹一次订阅授权，为「到期前 3 天」那条提醒攒一次性配额；
    // 无论授不授权都继续购买（必须落在 tap 处理函数调用栈里，微信才允许弹 requestSubscribeMessage）
    await this.requestExpireNotice()
    // 每一步的结果都由 utils/vipPay.js 弹话术（含「钱付了但没确认」那句），这里只负责按钮别再让人点
    this.setData({ purchasing: true })
    try {
      await buyVip(plan.code)
    } finally {
      this.setData({ purchasing: false })
      // 成交时 buyVip 已刷过 store；store 绑定回写 this.data.member 是异步的，与 onShow 同理用 nextTick
      // 等这帧 setData 落定再读，否则刚付完款仍会画成旧的「未开通」（买完不跳转，这是唯一刷新出口）
      wx.nextTick(() => this.render(this.data.member || {}))
    }
  },

  // 订阅「会员到期提醒」：弹授权 → 用户允许则把这次授权同步给后端累加本地配额（v9 记账模型）。
  // 一次性订阅「授权一次=可发一次」且微信不查剩余配额，全靠本地账，故 accept 后必须 sync，否则后端发不出。
  // 成功/失败/拒绝都不影响后续购买，故整体吞掉异常
  async requestExpireNotice() {
    if (!MEMBER_EXPIRE_TMPL_ID) return
    try {
      const res = await requestSubscribeMessage({ tmplIds: [MEMBER_EXPIRE_TMPL_ID] })
      // requestSubscribeMessage 的返回以模板 ID 为键，值为 accept/reject/ban/filter；只有 accept 才记配额
      if (res && res[MEMBER_EXPIRE_TMPL_ID] === 'accept') {
        await subscribeSyncApi({ templateId: MEMBER_EXPIRE_TMPL_ID, count: 1 })
      }
    } catch (e) {
      // 用户拒绝、模板 ID 未配、非用户触发、同步失败等一律忽略：不阻断开通
    }
  },

  // 点记录行复制交易单号（微信账单同款号码，客服据此查单退款）；setClipboardData 自带「内容已复制」提示，不叠 toast
  async onCopyTransactionId(e) {
    const no = e.currentTarget.dataset.no
    if (!no) return
    try {
      await setClipboardData({ data: no })
    } catch (err) {
      // 单号在行上本来就全文可见，复制失败不打断用户
    }
  },
})
