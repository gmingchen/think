// 决策单领域枚举（与后端 think-bk common/enums 严格对齐，编码不可随意改动）

// 决策单状态（decision.status）：草稿→评议中→已截止→已复盘/已关闭
export const DECISION_STATUS_ENUM = {
  DRAFT: 0,
  REVIEWING: 1,
  ENDED: 2,
  RECAPTURED: 3,
  CLOSED: 4,
}

export const decisionStatusList = [
  { label: '草稿', value: DECISION_STATUS_ENUM.DRAFT },
  { label: '评议中', value: DECISION_STATUS_ENUM.REVIEWING },
  { label: '已截止', value: DECISION_STATUS_ENUM.ENDED },
  { label: '已复盘', value: DECISION_STATUS_ENUM.RECAPTURED },
  { label: '已关闭', value: DECISION_STATUS_ENUM.CLOSED },
]

// 可见范围（decision.visibility）
export const VISIBILITY_ENUM = {
  SELF: 1,
  GRP: 2,
  PUBLIC: 3,
}

export const visibilityList = [
  { label: '仅自己（草稿）', value: VISIBILITY_ENUM.SELF },
  { label: '群内', value: VISIBILITY_ENUM.GRP },
  { label: '公开', value: VISIBILITY_ENUM.PUBLIC },
]

// 审核闸门状态（decision.check_status / review.check_status），仅公开档使用
export const CHECK_STATUS_ENUM = {
  NONE: 0,
  PENDING: 1,
  PASS: 2,
  REJECT: 3,
}

export const checkStatusList = [
  { label: '无需审核', value: CHECK_STATUS_ENUM.NONE },
  { label: '审核中', value: CHECK_STATUS_ENUM.PENDING },
  { label: '已发布', value: CHECK_STATUS_ENUM.PASS },
  { label: '暂未能发布', value: CHECK_STATUS_ENUM.REJECT },
]

// 评议时限（decision.cool_down_minutes，单位：分钟）：1~1440（≤ 24 小时）；0=不限（靠人数达标或手动结束收口，与人数不限不可同时选）
// 创建页采用两段式 stepper（小时 0~24 + 分钟 0/15/30/45），此处仅提供边界常量与可算参数，不再提供固定档位列表
export const COOL_DOWN_ENUM = {
  UNLIMITED: 0,
}
export const COOL_DOWN_MAX_MINUTES = 1440 // 上限 24 小时
export const COOL_DOWN_MIN_MINUTES = 15 // 非不限时的最小时长（一个分钟档步进）
export const COOL_DOWN_HOUR_MAX = 24 // 小时位上限
export const COOL_DOWN_MINUTE_OPTIONS = [0, 15, 30, 45] // 分钟位可选值

// 评议人数上限档位（decision.review_limit；0=不限，与评议时限不限不可同时选，发布时拦截）
export const REVIEW_LIMIT_ENUM = {
  N5: 5,
  N10: 10,
  N20: 20,
  N50: 50,
  UNLIMITED: 0,
}

export const reviewLimitList = [
  { label: '5 人', value: REVIEW_LIMIT_ENUM.N5 },
  { label: '10 人', value: REVIEW_LIMIT_ENUM.N10 },
  { label: '20 人', value: REVIEW_LIMIT_ENUM.N20 },
  { label: '50 人', value: REVIEW_LIMIT_ENUM.N50 },
  { label: '不限', value: REVIEW_LIMIT_ENUM.UNLIMITED },
]

// 决策单列表场景（subpackages/decision/list ?scene=）
export const DECISION_SCENE_ENUM = {
  GROUP: 'group',
  MINE: 'mine',
}

// group 场景状态筛选 Tab（评议中默认；草稿/待审核归入进行中，不在此列）
export const decisionGrpTabList = [
  { label: '评议中', value: DECISION_STATUS_ENUM.REVIEWING },
  { label: '已截止', value: DECISION_STATUS_ENUM.ENDED },
  { label: '已关闭', value: DECISION_STATUS_ENUM.CLOSED },
  { label: '已复盘', value: DECISION_STATUS_ENUM.RECAPTURED },
]

// mine 场景一级 Tab
export const DECISION_MINE_TAB_ENUM = {
  CREATED: 'created',
  REVIEWED: 'reviewed',
}

export const decisionMineTabList = [
  { label: '我发起的', value: DECISION_MINE_TAB_ENUM.CREATED },
  { label: '我评议过的', value: DECISION_MINE_TAB_ENUM.REVIEWED },
]
