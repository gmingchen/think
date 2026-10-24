// 页面路由集中管理（禁止散落魔法字符串，遵循 web-common 规范）

export const ROUTE = {
  // 主包 Tab
  TAB_INDEX: '/pages/index/index', // 三思
  TAB_GROUP: '/pages/group/index', // 群组
  TAB_SQUARE: '/pages/square/index', // 众思
  TAB_MINE: '/pages/mine/index', // 我的

  // 决策分包
  DECISION_CREATE: '/subpackages/decision/pages/create/index',
  DECISION_DETAIL: '/subpackages/decision/pages/detail/index',
  DECISION_LIST: '/subpackages/decision/pages/list/index',
  DECISION_REPORT: '/subpackages/decision/pages/report/index',
  DECISION_RECAP: '/subpackages/decision/pages/recap/index',

  // 群组分包
  GROUP_DETAIL: '/subpackages/group/pages/detail/index',
  GROUP_MEMBERS: '/subpackages/group/pages/members/index',

  // 邀请分包
  INVITE_LANDING: '/subpackages/invite/pages/landing/index',

  // 复用现有脚手架页（账号资料/关于/反馈）
  USER_EDIT: '/subpackages/user/pages/edit/index',
  USER_ABOUT: '/subpackages/user/pages/about/index',
  FEEDBACK: '/subpackages/feedback/pages/merchant/index',
}

// 决策单列表带 scene 参数
export const decisionListUrl = (scene, extra = '') => {
  return `${ ROUTE.DECISION_LIST }?scene=${ scene }${ extra }`
}

// 决策单详情
export const decisionDetailUrl = id => {
  return `${ ROUTE.DECISION_DETAIL }?id=${ id }`
}

// 决策单报告
export const decisionReportUrl = id => {
  return `${ ROUTE.DECISION_REPORT }?id=${ id }`
}

// 决策单复盘
export const decisionRecapUrl = id => {
  return `${ ROUTE.DECISION_RECAP }?id=${ id }`
}

// 群详情
export const groupDetailUrl = id => {
  return `${ ROUTE.GROUP_DETAIL }?id=${ id }`
}

// 群成员管理
export const groupMembersUrl = id => {
  return `${ ROUTE.GROUP_MEMBERS }?grpId=${ id }`
}

// 邀请落地页（凭 code 入群）
export const inviteLandingUrl = code => {
  return `${ ROUTE.INVITE_LANDING }?code=${ code }`
}
