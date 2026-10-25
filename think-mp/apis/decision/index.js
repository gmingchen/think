import { service } from '../service'

// 我发起的决策单列表（分页）
export const decisionPageMineApi = params => {
  return service({ url: '/decision/pageMine', method: 'POST', params })
}

// 我评议过的决策单列表（分页）
export const decisionPageReviewedApi = params => {
  return service({ url: '/decision/pageReviewed', method: 'POST', params })
}

// 群内决策单列表（分页）
export const decisionPageGrpApi = params => {
  return service({ url: '/decision/pageGrp', method: 'POST', params })
}

// 公开广场列表（分页，可按标签筛选）
export const decisionPagePlazaApi = params => {
  return service({ url: '/decision/pagePlaza', method: 'POST', params })
}

// 随机查看一条公开决策单（首次「去看看」）
export const decisionRandomPlazaApi = () => {
  return service({ url: '/decision/randomPlaza', method: 'GET' })
}

// 换一个：随机取一条且保证不与 params.excludeId（上一条）重复
export const decisionChangePlazaApi = params => {
  return service({ url: '/decision/changePlaza', method: 'GET', params })
}

// 决策单详情
export const decisionDetailApi = params => {
  return service({ url: '/decision/detail', method: 'GET', params })
}

// 我的统计（发起/参与/收获）
export const decisionStatsMineApi = () => {
  return service({ url: '/decision/statsMine', method: 'GET' })
}

// 创建决策单（存草稿）
export const decisionCreateApi = params => {
  return service({ url: '/decision/create', method: 'POST', params })
}

// 编辑决策单（草稿/审核退回态）
export const decisionUpdateApi = params => {
  return service({ url: '/decision/update', method: 'POST', params })
}

// 发布决策单
export const decisionPublishApi = params => {
  return service({ url: '/decision/publish', method: 'POST', params })
}

// 提前结束评议
export const decisionEndEarlyApi = params => {
  return service({ url: '/decision/endEarly', method: 'POST', params })
}

// 关闭评议
export const decisionCloseApi = params => {
  return service({ url: '/decision/close', method: 'POST', params })
}

// 删除决策单
export const decisionDeleteApi = params => {
  return service({ url: '/decision/delete', method: 'POST', params })
}
