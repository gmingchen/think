import { service } from '../service'

export const cookReviewPageApi = params => {
  return service({
    url: '/cook-review/page',
    method: 'GET',
    params
  })
}

export const cookReviewCreateApi = params => {
  return service({
    url: '/cook-order/review',
    method: 'POST',
    params
  })
}
