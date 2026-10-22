import { service } from '../service'

// 某决策单下的评议列表（分页）
export const reviewPageApi = params => {
  return service({ url: '/review/page', method: 'POST', params })
}

// 提交/修改评议（禁自评+满员拒绝+先评后看+审核闸门）
export const reviewSubmitApi = params => {
  return service({ url: '/review/submit', method: 'POST', params })
}
