import request from '@/utils/request'

// 各分区统计接口；range 形如 { start: 'yyyy-MM-dd', end: 'yyyy-MM-dd' }，留空则后端默认近 30 天
export const getOverview = () => request.get('/stats/overview')

// 今日 / 昨日逐小时新增与活跃用户数（快照类，无参数，服务端自取今昨两天）
export const getTodayHourly = () => request.get('/stats/today-hourly')

// 累计走势：区间内每日各指标累计总量（与日期区间联动）
export const getCumulative = (range) => request.get('/stats/cumulative', { params: range || {} })

export const getUserStats = (range) => request.get('/stats/user', { params: range || {} })
export const getKitchenStats = (range) => request.get('/stats/kitchen', { params: range || {} })
export const getDishStats = (range) => request.get('/stats/dish', { params: range || {} })
export const getOrderStats = (range) => request.get('/stats/order', { params: range || {} })
export const getReviewStats = (range) => request.get('/stats/review', { params: range || {} })
export const getMemberStats = (range) => request.get('/stats/member', { params: range || {} })

// 登录活跃排行（v14）：区间内用户登录天数榜 / 次数榜 / 单日峰值榜；params 可含 limit（取数上限，缺省 10）
export const getLoginRank = (params) => request.get('/stats/login-rank', { params: params || {} })

// 默认区间：近 30 天（含今日），供日期选择器初始化
export function defaultRange() {
  const end = new Date()
  const start = new Date()
  start.setDate(end.getDate() - 29)
  return { start: fmt(start), end: fmt(end) }
}

export function fmt(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}
