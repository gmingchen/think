import { service } from '../service'

// 回填/修改复盘（决策单转「已复盘」）
export const recapSaveApi = params => {
  return service({ url: '/recap/save', method: 'POST', params })
}

// 按决策单查询复盘
export const recapDetailApi = params => {
  return service({ url: '/recap/detail', method: 'GET', params })
}
