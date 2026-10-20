import { service } from '../service'

export const seasoningListApi = params => {
  return service({ url: '/seasoning/list', method: 'GET', params })
}

export const seasoningCreateApi = params => {
  return service({ url: '/seasoning/create', method: 'POST', params })
}

export const seasoningUpdateApi = params => {
  return service({ url: '/seasoning/update', method: 'POST', params })
}

export const seasoningDeleteApi = params => {
  return service({ url: '/seasoning/delete', method: 'DELETE', params })
}
