import { service } from '../service'

export const foodListApi = params => {
  return service({ url: '/food/list', method: 'GET', params })
}

export const foodCreateApi = params => {
  return service({ url: '/food/create', method: 'POST', params })
}

export const foodUpdateApi = params => {
  return service({ url: '/food/update', method: 'POST', params })
}

export const foodDeleteApi = params => {
  return service({ url: '/food/delete', method: 'DELETE', params })
}
