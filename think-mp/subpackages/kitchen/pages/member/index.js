import { storePageBehavior, sharePageBehavior } from '../../../../behaviors/index'
import { KITCHEN_ROLE_LIST, KITCHEN_ROLE_ENUM } from '../../../../common/enums/index'
import { DEFAULT_AVATAR, AD_UNIT_KITCHEN_MEMBER_BANNER } from '../../../../common/constants/index'
import { kitchenUserListApi, kitchenUserUpdateRoleApi, kitchenUserDeleteApi } from '../../../../apis/index'

Page({
  behaviors: [storePageBehavior, sharePageBehavior],
  data: {
    id: null,
    name: '',
    me: null,
    list: [],
    loading: true,
    KITCHEN_ROLE_LIST,
    KITCHEN_ROLE_ENUM,
    DEFAULT_AVATAR,
    AD_UNIT_KITCHEN_MEMBER_BANNER,
  },
  async onLoad(options) {
    this.bindStore({ fields: ['user', 'kitchen'] })
    await wx.$onKitchenLoaded
    wx.nextTick(async () => {
      const id = options.id || this.data.kitchen?.active?.id
      if (id) {
        const kitchen = this.data.kitchen?.list?.find(k => k.id === Number(id))
        this.setData({
          id: Number(id),
          name: kitchen?.name || this.data.kitchen?.active?.name || '',
        })
        await this.getData()
      }
      this.setData({ loading: false })
    })
  },
  async getData() {
    const r = await kitchenUserListApi({ kitchenId: this.data.id })
    if (r.data) {
      const me = r.data.find(item => item.userId === this.data.user?.id)
      this.setData({ list: r.data, me })
    }
  },
  async onUpdateRole(e) {
    const { userId, role } = e.currentTarget.dataset
    const newRole = Number(role) === KITCHEN_ROLE_ENUM.MANAGER ? KITCHEN_ROLE_ENUM.GUEST : KITCHEN_ROLE_ENUM.MANAGER
    const member = this.data.list.find(item => item.userId === userId)
    const action = newRole === KITCHEN_ROLE_ENUM.MANAGER ? '设为管厨' : '取消管厨'
    const ok = await wx.$confirm(`确认将「${member.nickname}」${action}？`)
    if (!ok) return
    await kitchenUserUpdateRoleApi({ kitchenId: this.data.id, userId, role: newRole })
    await this.getData()
  },
  async onDeleteMember(e) {
    const { userId } = e.currentTarget.dataset
    const member = this.data.list.find(item => item.userId === userId)
    const ok = await wx.$confirm(`确认移除「${member.nickname}」？`)
    if (!ok) return
    await kitchenUserDeleteApi({ kitchenId: this.data.id, userId })
    await this.getData()
  },
})
