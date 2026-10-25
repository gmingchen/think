import { service } from '../service'

// 提交举报（仅公开档内容：targetType 1-决策单 2-评议）
export const abuseSubmitApi = params => {
  return service({ url: '/abuse/submit', method: 'POST', params })
}
