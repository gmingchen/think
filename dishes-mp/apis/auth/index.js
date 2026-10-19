import { service } from '../service'

export const loginApi = params => {
  return service({
    url: '/auth/login',
    method: 'POST',
    params: params
  })
}

export const authUserApi = params => {
  return service({
    url: '/auth/info',
    method: 'GET',
    params: params
  })
}
