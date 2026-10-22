// 群组领域枚举（与后端 think-bk 对齐）

// 群成员角色（grp_member.role）
export const GRP_ROLE_ENUM = {
  OWNER: 1,
  MEMBER: 2,
}

export const grpRoleList = [
  { label: '群主', value: GRP_ROLE_ENUM.OWNER },
  { label: '成员', value: GRP_ROLE_ENUM.MEMBER },
]

// 群状态（grp.status）
export const GRP_STATUS_ENUM = {
  NORMAL: 1,
  DISSOLVED: 2,
}
