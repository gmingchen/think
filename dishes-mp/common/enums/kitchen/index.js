// 厨房角色
export const KITCHEN_ROLE_ENUM = {
  OWNER: 1,
  MANAGER: 2,
  GUEST: 3,
}

export const KITCHEN_ROLE_LIST = [
  { label: '厨师长', value: KITCHEN_ROLE_ENUM.OWNER },
  { label: '管厨', value: KITCHEN_ROLE_ENUM.MANAGER },
  { label: '食客', value: KITCHEN_ROLE_ENUM.GUEST },
]

// 烹饪订单状态
export const COOK_ORDER_STATUS_ENUM = {
  ORDERED: 1,
  ACCEPTED: 2,
  SERVED: 3,
  REVIEWED: 4,
  CANCELLED: 5,
}

export const COOK_ORDER_STATUS_LIST = [
  { label: '已下单', value: COOK_ORDER_STATUS_ENUM.ORDERED },
  { label: '已接单', value: COOK_ORDER_STATUS_ENUM.ACCEPTED },
  { label: '已上菜', value: COOK_ORDER_STATUS_ENUM.SERVED },
  { label: '已评价', value: COOK_ORDER_STATUS_ENUM.REVIEWED },
  { label: '已取消', value: COOK_ORDER_STATUS_ENUM.CANCELLED },
]

// 时段
export const TIME_PERIOD_ENUM = {
  BREAKFAST: 1,
  LUNCH: 2,
  DINNER: 3,
  SNACK: 4,
}

export const TIME_PERIOD_LIST = [
  { label: '早餐', value: TIME_PERIOD_ENUM.BREAKFAST },
  { label: '午餐', value: TIME_PERIOD_ENUM.LUNCH },
  { label: '晚餐', value: TIME_PERIOD_ENUM.DINNER },
  { label: '宵夜', value: TIME_PERIOD_ENUM.SNACK },
]
