// 商户需求反馈（v13，需求验证用）
// ⚠️ 选项值必须与后端 FeedbackBusinessTypeEnum / FeedbackCurrentToolEnum / FeedbackDishScaleEnum /
//    FeedbackWillingnessEnum 的 code 逐一对齐，改一侧必须同步改另一侧，否则提交的选项会被后端拒收或统计口径错位。

// 经营类型
export const BUSINESS_TYPE_ENUM = {
  HOME_RESTAURANT: 1, // 私房菜
  SMALL_SHOP: 2,      // 小餐饮门店
  STREET_STALL: 3,    // 摆摊
  BNB_GROUP_MEAL: 4,  // 民宿或团体餐
  CANTEEN: 5,         // 员工食堂
  OTHER: 6,           // 其他
}

export const BUSINESS_TYPE_LIST = [
  { label: '私房菜', value: BUSINESS_TYPE_ENUM.HOME_RESTAURANT },
  { label: '小餐饮门店', value: BUSINESS_TYPE_ENUM.SMALL_SHOP },
  { label: '摆摊', value: BUSINESS_TYPE_ENUM.STREET_STALL },
  { label: '民宿或团体餐', value: BUSINESS_TYPE_ENUM.BNB_GROUP_MEAL },
  { label: '员工食堂', value: BUSINESS_TYPE_ENUM.CANTEEN },
  { label: '其他', value: BUSINESS_TYPE_ENUM.OTHER },
]

// 现用管理方式（多选）
export const CURRENT_TOOL_ENUM = {
  WECHAT_GROUP: 1,       // 微信群口头沟通
  HANDWRITTEN: 2,        // 手写小票或白板
  EXCEL: 3,              // Excel或备忘录
  DELIVERY_PLATFORM: 4,  // 外卖平台后台
  POS_SAAS: 5,           // 专业收银SaaS
  NONE: 6,               // 没有固定方式
}

export const CURRENT_TOOL_LIST = [
  { label: '微信群口头沟通', value: CURRENT_TOOL_ENUM.WECHAT_GROUP },
  { label: '手写小票或白板', value: CURRENT_TOOL_ENUM.HANDWRITTEN },
  { label: 'Excel或备忘录', value: CURRENT_TOOL_ENUM.EXCEL },
  { label: '外卖平台后台', value: CURRENT_TOOL_ENUM.DELIVERY_PLATFORM },
  { label: '专业收银SaaS', value: CURRENT_TOOL_ENUM.POS_SAAS },
  { label: '没有固定方式', value: CURRENT_TOOL_ENUM.NONE },
]

// 菜品规模
export const DISH_SCALE_ENUM = {
  TINY: 1,   // 20以内
  SMALL: 2,  // 20至50
  MEDIUM: 3, // 50至100
  LARGE: 4,  // 100以上
}

export const DISH_SCALE_LIST = [
  { label: '20以内', value: DISH_SCALE_ENUM.TINY },
  { label: '20~50', value: DISH_SCALE_ENUM.SMALL },
  { label: '50~100', value: DISH_SCALE_ENUM.MEDIUM },
  { label: '100以上', value: DISH_SCALE_ENUM.LARGE },
]

// 付费意愿
export const WILLINGNESS_ENUM = {
  FREE: 1,   // 免费才用
  LOW: 2,    // 20元以内每月
  MEDIUM: 3, // 20至50元每月
  HIGH: 4,   // 50元以上或一次性买断
}

export const WILLINGNESS_LIST = [
  { label: '免费才用', value: WILLINGNESS_ENUM.FREE },
  { label: '20元以内每月', value: WILLINGNESS_ENUM.LOW },
  { label: '20~50元每月', value: WILLINGNESS_ENUM.MEDIUM },
  { label: '50元以上或买断', value: WILLINGNESS_ENUM.HIGH },
]
