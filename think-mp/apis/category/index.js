import { service } from '../service'

export const categoryListApi = params => {
  return service({
    url: '/category/list',
    method: 'GET',
    params: params
  })
}

export const categoryCreateApi = params => {
  return service({
    url: '/category/create',
    method: 'POST',
    params: params
  })
}

export const categoryUpdateApi = params => {
  return service({
    url: '/category/update',
    method: 'POST',
    params: params
  })
}

export const categorySortApi = params => {
  return service({
    url: '/category/sort',
    method: 'POST',
    params: params
  })
}

export const categoryDeleteApi = params => {
  return service({
    url: '/category/delete',
    method: 'DELETE',
    params: params
  })
}
