import { service } from '../service'

export const kitchenListApi = params => {
  return service({
    url: '/kitchen/list',
    method: 'GET',
    params: params
  })
}

export const kitchenInfoApi = params => {
  return service({
    url: '/kitchen/info',
    method: 'GET',
    params: params
  })
}

export const kitchenStatsApi = params => {
  return service({
    url: '/kitchen/stats',
    method: 'GET',
    params: params
  })
}

export const kitchenUpdateApi = params => {
  return service({
    url: '/kitchen/update',
    method: 'POST',
    params: params
  })
}

export const kitchenLeaveApi = params => {
  return service({
    url: '/kitchen/user/leave',
    method: 'POST',
    params: params
  })
}

// 看激励视频扩容菜谱上限（+1，仅厨师长/管厨）
export const kitchenExpandDishApi = params => {
  return service({
    url: '/kitchen/expand-dish',
    method: 'POST',
    params: params
  })
}

// 看激励视频扩容分类上限（+1，仅厨师长/管厨）
export const kitchenExpandCategoryApi = params => {
  return service({
    url: '/kitchen/expand-category',
    method: 'POST',
    params: params
  })
}

export const kitchenUserListApi = params => {
  return service({
    url: '/kitchen/user/list',
    method: 'GET',
    params: params
  })
}

export const kitchenUserInviteApi = params => {
  return service({
    url: '/kitchen/user/invite',
    method: 'POST',
    params: params
  })
}

export const kitchenUserInviteInfoApi = params => {
  return service({
    url: '/kitchen/user/invite/info',
    method: 'GET',
    params: params
  })
}

export const kitchenUserJoinApi = params => {
  return service({
    url: '/kitchen/user/join',
    method: 'POST',
    params: params
  })
}

export const kitchenUserUpdateRoleApi = params => {
  return service({
    url: '/kitchen/user/update/role',
    method: 'POST',
    params: params
  })
}

export const kitchenUserDeleteApi = params => {
  return service({
    url: '/kitchen/user/delete',
    method: 'DELETE',
    params: params
  })
}
