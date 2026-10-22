import { GRP_ROLE_ENUM, GRP_STATUS_ENUM } from '../../../../../../common/enums/index'

Component({
  properties: {
    // 群核心资料：{ name, myRole, status, memberCount }（属性名 data，模板中以 data.xxx 访问）
    data: {
      type: Object,
      value: {},
    },
  },
  data: {
    // 角色 / 状态枚举供模板比较（群主 / 成员 / 已解散徽章、按钮显隐）
    GRP_ROLE_ENUM,
    GRP_STATUS_ENUM,
  },
  methods: {
    // 纯 UI，交互统一 triggerEvent 交由页面处理业务
    onEditTap() {
      this.triggerEvent('edit')
    },
    onMembersTap() {
      this.triggerEvent('members')
    },
    onInviteTap() {
      this.triggerEvent('invite')
    },
    onDissolveTap() {
      this.triggerEvent('dissolve')
    },
    onLeaveTap() {
      this.triggerEvent('leave')
    },
  },
})
