/**
 * 采购清单聚合纯函数（docs/v6 §四）
 * 输入 /dish/shopping/list 返回的按菜分组明细，输出「必买 / 可不买 / 调料」三档清单
 * 合并规则：同菜多行行内拼接、跨菜多段菜名锚点、×N份挂段后；条件求和从严，认不出即拼接
 */

// 可求和单位白名单：只认「前缀数字 + 白名单单位」；斤/两等一律放弃求和（换算表属过度设计）
const SUM_UNIT_WHITELIST = ['克', 'g', '千克', 'kg', '毫升', 'ml', '个', '只', '勺', '汤匙', '茶匙', '段', '颗', '头', '根', '片', '块', '条', '包', '袋', '盒', '碗']

// 解析「前缀数字(含小数) + 单位」，适量/半斤等认不出返回 null；g 与 克不互并（单位字符串完全相等才算同单位）
const parseAmount = (amount, unit) => {
  const m = /^(\d+(?:\.\d+)?)(.*)$/.exec((amount || '').trim())
  if (!m) return null
  const u = m[2].trim() || (unit || '').trim()
  if (!u || !SUM_UNIT_WHITELIST.includes(u)) return null
  return { value: parseFloat(m[1]), unit: u }
}

// 求和值展示：保留至多两位小数并去掉尾零（0.5 → "0.5"，600.00 → "600"）
const fmtNum = n => String(Math.round(n * 100) / 100)

// 单个食材/调料桶 → 展示项 { key, name, dishIds, orderIds, summable, amountDisplay, amountSegs, sources, process }
const buildItem = bucket => {
  const occs = [...bucket.occMap.values()]
  // 条目归属菜 id = 所有来源 occurrence 的 dishId；勾选缓存按菜分片回写用
  const dishIds = [...bucket.occMap.keys()]
  // 条目归属订单集 = 所有来源菜的订单并集，勾选缓存按订单分片回写用
  const orderIds = [...new Set(occs.flatMap(o => o.orderIds))]
  const base = { key: bucket.key, name: bucket.name, dishIds, orderIds, amountSegs: [] }

  // amount 全为空：只显名称不显占位符（形态⑥）
  const hasAmount = occs.some(o => o.rows.some(r => (r.amount || '').trim()))
  if (!hasAmount) return { ...base, summable: false, amountDisplay: '', sources: [], process: '' }

  // 处理方式去重合并（去皮/切块等次要信息，有则展示）
  const process = [...new Set(occs.flatMap(o => o.rows.map(r => r.process)).filter(Boolean))].join(' · ')
  const occLabel = o => (o.quantity > 1 ? `${o.dishName}×${o.quantity}份` : o.dishName)

  // 条件求和：所有段单位完全相同且数值均可解析才求"共X"（×份数直接乘进总量），任一段不满足整行退回拼接
  const parsed = []
  let summable = true
  occs.forEach(o => {
    o.rows.forEach(r => {
      const p = parseAmount(r.amount, r.unit)
      if (p) parsed.push({ ...p, quantity: o.quantity })
      else summable = false
    })
  })
  if (summable && parsed.length) {
    // 同单位 = 单位字符串完全相等（g 与 克不互并），任一段单位不同即降级拼接
    summable = parsed.every(p => p.unit === parsed[0].unit)
  }
  if (summable && parsed.length) {
    const total = parsed.reduce((sum, p) => sum + p.value * p.quantity, 0)
    const display = `共${fmtNum(total)}${parsed[0].unit}` + (occs.length > 1 ? `（${occs.length}道菜）` : '')
    return { ...base, summable: true, amountDisplay: display, sources: occs.map(occLabel), process }
  }

  // 退回拼接：菜名做锚点，同菜内多行行内拼接（不同 process 分别保留），份数挂段后
  // 长拼接串不挤在主行右侧，拆为逐段数组由 meta 区整行分段展示
  const amountSegs = occs.map(o => {
    const suffix = o.quantity > 1 ? `×${o.quantity}份` : ''
    const text = o.rows
      .map(r => [((r.amount || '').trim()), r.process].filter(Boolean).join('·'))
      .filter(Boolean)
      .join(' + ')
    return text ? `${text}（${o.dishName}${suffix}）` : `${o.dishName}${suffix}`
  })
  // 分段串已含菜名与处理信息，不再重复挂来源/处理小字
  return { ...base, summable: false, amountDisplay: '', amountSegs, sources: [], process: '' }
}

/**
 * 聚合入口
 * @param {Array} dishes 接口返回的按菜分组明细 DishShoppingListVO[]
 * @returns {{ required: Array, optional: Array, seasonings: Array }} 三档清单（组内保持首次出现顺序）
 */
export const aggregateShoppingList = (dishes = []) => {
  const foodBuckets = new Map()
  const seasoningBuckets = new Map()

  // 按食材/调料 id 归桶；桶内按菜品归段（场景 A 同菜多行合入 rows，场景 C 份数记在段上）
  const push = (map, key, name, dish, row, optional) => {
    if (!map.has(key)) map.set(key, { key, name, occMap: new Map(), isOptional: !!optional })
    const bucket = map.get(key)
    if (!bucket.occMap.has(dish.dishId)) {
      bucket.occMap.set(dish.dishId, { dishName: dish.dishName, quantity: dish.quantity || 1, orderIds: dish.cookOrderIds || [], rows: [] })
    }
    bucket.occMap.get(dish.dishId).rows.push(row)
  }

  dishes.forEach(dish => {
    (dish.foods || []).forEach(f => {
      push(foodBuckets, `f:${f.foodId}`, f.foodName, dish, { amount: f.amount, unit: f.unit, process: f.process }, f.isOptional === 1)
    })
    ;(dish.seasonings || []).forEach(s => {
      push(seasoningBuckets, `s:${s.seasoningId}`, s.name, dish, { amount: s.amount, unit: s.unit }, s.isOptional === 1)
    })
  })

  const foodItems = [...foodBuckets.values()].map(b => ({ bucket: b, optional: !!b.isOptional }))
  return {
    required: foodItems.filter(i => !i.optional).map(i => buildItem(i.bucket)),
    optional: foodItems.filter(i => i.optional).map(i => buildItem(i.bucket)),
    seasonings: [...seasoningBuckets.values()].map(buildItem),
  }
}

// id 集合哈希：排序后 djb2，结果与传入顺序无关（同组菜共享缓存 key）
const hashIds = ids => {
  const s = [...ids].sort((a, b) => a - b).join(',')
  let h = 5381
  for (let i = 0; i < s.length; i++) {
    h = ((h << 5) + h + s.charCodeAt(i)) >>> 0
  }
  return h.toString(36)
}

/**
 * 旧版整块缓存的 sourceKey（仅作升级兼容回退读取，新写入一律走 id 分片）
 * 篮子 → cart；抽菜/帮我选 → dish:{ids哈希}
 * 现行规则：订单类按 orderId:{orderId}、菜品类按 dishId:{dishId} 分片存储（组名不用 dish:/order: 以避开本函数的哈希 key 空间）
 */
export const buildSourceKey = (source, dishIds = []) => {
  if (source === 'cart') return 'cart'
  if (dishIds.length) return `dish:${hashIds(dishIds)}`
  return source || 'unknown'
}
