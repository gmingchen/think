import { service } from '../service'

// 我的群列表（分页）
export const grpPageApi = params => {
  return service({ url: '/grp/page', method: 'POST', params })
}

// 建群
export const grpCreateApi = params => {
  return service({ url: '/grp/create', method: 'POST', params })
}

// 群详情
export const grpDetailApi = params => {
  return service({ url: '/grp/detail', method: 'GET', params })
}

// 修改群信息（仅群主）
export const grpUpdateApi = params => {
  return service({ url: '/grp/update', method: 'POST', params })
}

// 解散群（仅群主）
export const grpDissolveApi = params => {
  return service({ url: '/grp/dissolve', method: 'POST', params })
}

// 生成邀请码
export const grpInviteApi = params => {
  return service({ url: '/grp/invite', method: 'POST', params })
}

// 凭邀请码入群
export const grpJoinApi = params => {
  return service({ url: '/grp/join', method: 'POST', params })
}

// 退出群
export const grpLeaveApi = params => {
  return service({ url: '/grp/leave', method: 'POST', params })
}

// 移除群成员（仅群主）
export const grpRemoveMemberApi = params => {
  return service({ url: '/grp/removeMember', method: 'POST', params })
}

// 群成员列表（分页）
export const grpMembersPageApi = params => {
  return service({ url: '/grp/members/page', method: 'POST', params })
}
