import { GRP_ROLE_ENUM } from '../../../../common/enums/index'

Component({
  properties: {
    // 单个群组条目：{ id, name, myRole, memberCount }（属性名 data，模板中以 data.xxx 访问）
    data: {
      type: Object,
      value: {},
    },
  },
  data: {
    // 角色枚举供模板比较（群主 / 成员徽章）
    GRP_ROLE_ENUM,
  },
})
