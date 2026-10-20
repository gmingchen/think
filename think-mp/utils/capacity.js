// 容量使用率达到该比例即视为「即将满载」（进度条转红提示）
const CAPACITY_WARN_RATIO = 0.9

// 由「当前数 / 上限」推导进度展示数据：余量、百分比、是否告警、是否已满（厨房容量类展示统一复用）
// unlimited：畅厨会员在**本人创建的厨房**内不限数（§1.4 决策 1）。本参数由调用方传后端算好的
// **厨房级** 标记 KitchenVO.unlimitedCapacity（= 畅厨在期 ∧ isOwner）：前端不自己拼档位，也不拿
// store.member.unlimitedCapacity（那是人级，不含归属判定）——两者对管厨在别人的厨房里会分叉（§6.2）
export const buildCapacity = (cur, max, unlimited = false) => {
  const c = cur || 0
  const m = max || 0
  if (unlimited) {
    // 不限时不算百分比、不判告警与已满：拿 50 当分母去画一条「用了 100 道」的进度条会撑破，
    // 而 full 一旦为 true 就会把「已达上限」那句话递给一个根本没上限的人（§4 入口 4 的展示口径）
    return { cur: c, max: m, unlimited: true, remain: 0, percent: 0, warn: false, full: false, over: 0 }
  }
  return {
    cur: c,
    max: m,
    unlimited: false,
    remain: Math.max(m - c, 0),
    percent: m > 0 ? Math.min(100, Math.round((c / m) * 100)) : 0,
    // warn 为「需要警示」（转红）；full 为「已达上限」，用于区分「已满」与「即将满载」文案
    warn: m > 0 && c / m >= CAPACITY_WARN_RATIO,
    full: m > 0 && c >= m,
    // over：会员到期后超出回落线的存量（§12.2），给容量条那句「已超出 N」用；
    // 它不是错误也不是欠费，所以本行不连带把 warn/full 翻成 true（超额只限制新增，不锁已有内容）
    over: Math.max(c - m, 0),
  }
}
