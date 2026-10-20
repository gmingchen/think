import { service } from '../service'

// 提交商户合作意向反馈（v13，需求验证用）
export const feedbackMerchantCreateApi = params => {
  return service({
    url: '/feedback/merchant',
    method: 'POST',
    params,
  })
}
