import { service } from '../service'

export const presetCategoryListApi = params => {
  return service({ url: '/preset/category/list', method: 'GET', params })
}

export const presetFoodListApi = params => {
  return service({ url: '/preset/food/list', method: 'GET', params })
}

export const presetSeasoningListApi = params => {
  return service({ url: '/preset/seasoning/list', method: 'GET', params })
}
