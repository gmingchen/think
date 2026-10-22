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

// 冷静期档位（decision.cool_down_hours，单位：小时）
export const COOL_DOWN_ENUM = {
  H12: 12,
  H24: 24,
  H72: 72,
  H168: 168,
}

export const coolDownList = [
  { label: '12 小时', value: COOL_DOWN_ENUM.H12 },
  { label: '24 小时', value: COOL_DOWN_ENUM.H24 },
  { label: '3 天', value: COOL_DOWN_ENUM.H72 },
  { label: '7 天', value: COOL_DOWN_ENUM.H168 },
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

// 评议写作引导（思危/思退/思变 三视角，纯 UI 辅助，不落库）
export const reviewAngleList = [
  { label: '思危', value: '危', hint: '这个选择最大的风险是什么？' },
  { label: '思退', value: '退', hint: '有没有更稳妥的退路或折中？' },
  { label: '思变', value: '变', hint: '换个角度看，还有什么可能？' },
]
