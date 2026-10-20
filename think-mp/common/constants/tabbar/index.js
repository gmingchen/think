import { TABBAR_ENUM } from '../../enums/index'

export const TABBAR_LIST = [
  { label: '小舍', value: TABBAR_ENUM.HOME, emoji: '🏡', url: '/pages/index/index' },
  { label: '菜谱', value: TABBAR_ENUM.RECIPE, emoji: '📖', url: '/pages/recipe/index' },
  { label: '下厨', value: TABBAR_ENUM.COOK, emoji: '🍳', url: '/pages/cook/index' },
  { label: '我的', value: TABBAR_ENUM.MINE, emoji: '📇', url: '/pages/mine/index' },
]
