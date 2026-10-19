// 数字格式化：千分位 / 金额 / 百分比，空值占位 '-'

export const fmtNum = (v, precision = -1) => {
  if (v === null || v === undefined || v === '') return '-'
  if (precision >= 0 && typeof v === 'number') return v.toFixed(precision)
  if (typeof v === 'number') return v.toLocaleString()
  return v
}

// 占比：part / total 百分比；总数为 0 或非法时返回占位 '—'
export const fmtPct = (part, total, digits = 1) => {
  const p = Number(part) || 0
  const t = Number(total) || 0
  if (!t) return '—'
  return `${((p / t) * 100).toFixed(digits)}%`
}
