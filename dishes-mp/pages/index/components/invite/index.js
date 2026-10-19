import { kitchenUserInviteInfoApi, kitchenUserJoinApi } from '../../../../apis/index'
import { KITCHEN_ROLE_LIST } from '../../../../common/enums/index'
import { storeComponentBehavior } from '../../../../behaviors/index'
import { store } from '../../../../store/index'

Component({
  behaviors: [storeComponentBehavior({ fields: ['kitchen'], actions: ['getKitchen', 'updateKitchen'] })],
  properties: {
    invite: {
      type: Object,
      value: {
        kitchenId: null,
        code: '',
      },
    },
  },

  data: {
    show: false,
    loading: true,
    info: null,
    KITCHEN_ROLE_LIST,
  },

  observers: {
    'invite.kitchenId, invite.code'(kitchenId, code) {
      if (kitchenId && code) {
        // 检查是否已在该厨房（直接读同步的 store，而非异步镜像 this.data.kitchen）
        const inKitchen = store.kitchen.list.some(k => k.id === kitchenId)
        if (inKitchen) {
          // wx.$toast('你已在该厨房')
          return
        }
        this.setData({ show: true, info: null, loading: true })
        this._loadInviteInfo()
      }
    },
  },

  methods: {
    async _loadInviteInfo() {
      this.setData({ loading: true })
      try {
        const r = await kitchenUserInviteInfoApi({
          kitchenId: this.data.invite.kitchenId,
          code: this.data.invite.code,
        })
        if (r.data) {
          this.setData({ info: r.data })
        }
      } catch (e) {
        wx.$toast(e.message || '邀请信息加载失败')
      } finally {
        this.setData({ loading: false })
      }
    },

    onClose() {
      this.setData({ show: false })
      this.triggerEvent('reject')
    },

    async onAccept() {
      try {
        await kitchenUserJoinApi({
          kitchenId: this.data.invite.kitchenId,
          code: this.data.invite.code,
        })
        wx.$toast('已加入厨房')
        this.setData({ show: false })
        // 刷新厨房列表并切换到目标厨房
        // 直接读同步更新的 mobx store，而非异步镜像 this.data.kitchen
        // （绑定值靠 setData 同步，await getKitchen() 后不保证已刷新，会漏切厨房）
        await this.getKitchen()
        const kitchen = store.kitchen.list.find(k => k.id === this.data.invite.kitchenId)
        if (kitchen) {
          await this.updateKitchen(kitchen)
        }
        this.triggerEvent('accept')
      } catch (e) {
        wx.$toast(e.message || '加入失败')
      }
    },

    onReject() {
      this.setData({ show: false })
      this.triggerEvent('reject')
    },

    stopPropagation() {},
  },
})
