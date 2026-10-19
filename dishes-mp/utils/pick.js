/**
 * 加权随机抽样纯函数
 * 独立页与弹窗共用，不依赖任何外部模块
 *
 * 权重公式：weight = 1 + rating × 0.5 + log(cookCount + 1) × 0.3
 */

// 单道菜的计算权重
function calcWeight(dish) {
  const rating = dish.rating || 0
  const cookCount = dish.cookCount || 0
  return 1 + rating * 0.5 + Math.log(cookCount + 1) * 0.3
}

// 轮盘赌：从 pool 中按权重抽 1 道，抽出后从 pool 移除
function roulettePick(pool) {
  const weights = pool.map(calcWeight)
  const totalWeight = weights.reduce((sum, w) => sum + w, 0)
  let rand = Math.random() * totalWeight

  for (let i = 0; i < pool.length; i++) {
    if (rand < weights[i]) {
      return pool.splice(i, 1)[0]
    }
    rand -= weights[i]
  }
  // 浮点精度兜底
  return pool.splice(pool.length - 1, 1)[0]
}

/**
 * 按模式过滤候选池
 * @param {Array} allDishes  全量菜谱
 * @param {string} mode      模式：balanced | new | expert | category
 * @param {number|null} categoryId  按分类抽时的分类 ID
 * @returns {Array} 过滤后的候选池
 */
function filterByMode(allDishes, mode, categoryId) {
  if (mode === 'new') {
    // 换新花样：只从 cookCount 最低的 30% 里等概率抽
    const sorted = [...allDishes].sort((a, b) => a.cookCount - b.cookCount)
    const threshold = Math.max(2, Math.ceil(sorted.length * 0.3))
    return sorted.slice(0, threshold)
  }
  if (mode === 'expert') {
    // 只看拿手：rating >= 4.5
    const filtered = allDishes.filter(d => d.rating >= 4.5)
    return filtered.length > 0 ? filtered : [...allDishes]
  }
  if (mode === 'category' && categoryId != null) {
    const filtered = allDishes.filter(d => d.categoryId === categoryId)
    return filtered.length > 0 ? filtered : [...allDishes]
  }
  // balanced（默认）
  return [...allDishes]
}

/**
 * 加权随机抽样
 * @param {Array} allDishes    全量菜谱 [{ id, rating, cookCount, categoryId, ... }]
 * @param {Object} options
 * @param {string} options.mode          抽取模式（默认 balanced）
 * @param {number} options.count         抽取数量（默认 1）
 * @param {Array} options.excludeIds     上一批抽中的 ID，排除避免连续重复
 * @param {number|null} options.categoryId  按分类抽时的分类 ID
 * @returns {Array} 抽中的菜谱列表
 */
export function pickDishes(allDishes, options = {}) {
  const { mode = 'balanced', count = 1, excludeIds = [], categoryId = null } = options

  if (!allDishes || allDishes.length === 0) return []

  // 1. 按模式过滤候选池
  let candidates = filterByMode(allDishes, mode, categoryId)

  // 2. 排除上一批（候选池 > 抽取数量时才排除，否则跳过）
  if (candidates.length > count && excludeIds.length > 0) {
    const filtered = candidates.filter(d => !excludeIds.includes(d.id))
    // 排除后数量仍够才使用排除后的池子
    if (filtered.length >= count) {
      candidates = filtered
    }
  }

  // 3. 轮盘赌加权不重复抽取
  const pool = [...candidates]
  const targetCount = Math.min(count, pool.length)
  const result = []

  for (let i = 0; i < targetCount; i++) {
    result.push(roulettePick(pool))
  }

  return result
}
