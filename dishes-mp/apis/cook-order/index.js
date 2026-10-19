import { service } from '../service'

export const cookOrderPageApi = params => {
  return service({ url: '/cook-order/page', method: 'GET', params })
}

export const cookOrderTodayApi = params => {
  return service({
    url: '/cook-order/today',
    method: 'GET',
    params: params
  })
}

export const cookOrderLatestApi = params => {
  return service({
    url: '/cook-order/latest',
    method: 'GET',
    params: params
  })
}

export const cookOrderCreateApi = params => {
  return service({
    url: '/cook-order/create',
    method: 'POST',
    params
  })
}

export const cookOrderAcceptApi = params => {
  return service({
    url: '/cook-order/accept',
    method: 'POST',
    params
  })
}

export const cookOrderServeApi = params => {
  return service({
    url: '/cook-order/serve',
    method: 'POST',
    params
  })
}

export const cookOrderCancelApi = params => {
  return service({
    url: '/cook-order/cancel',
    method: 'POST',
    params
  })
}

export const cookOrderInfoApi = params => {
  return service({
    url: '/cook-order/info',
    method: 'GET',
    params
  })
}
