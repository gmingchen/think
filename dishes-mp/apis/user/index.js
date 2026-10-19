import { service } from '../service'

// 获取用户信息
export const userInfoApi = () => {
  return service({
    url: '/user/info',
    method: 'GET',
  })
}

export const userUpdateApi = params => {
  return service({
    url: '/user/update',
    method: 'POST',
    params,
  })
}

export const userPresetDataApi = params => {
  return service({
    url: '/user/presetData',
    method: 'POST',
    params: params
  })
}

export const userUpdateLastKitchenApi = params => {
  return service({
    url: '/user/update/lastKitchen',
    method: 'POST',
    params,
  })
}
