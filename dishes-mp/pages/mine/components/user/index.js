import { DEFAULT_AVATAR, MEMBER_TIER_SHORT_TEXT } from '../../../../common/constants/index'
import { storeComponentBehavior } from '../../../../behaviors/index'
import dayjs from 'dayjs'

Component({
  behaviors: [storeComponentBehavior({ fields: ['user', 'member'] })],

  data: {
    defaultAvatar: DEFAULT_AVATAR,
    joinLabel: '',
    // 会员角标（§4 入口 2）：档位短名，空串即不渲染。判据取 effective，不拿 tier > 0 反推：
    // 后端的归零与生效判定同源（§7.1），但「有档位」与「在生效期」不是一回事，写错一个符号就变永久角标
    memberBadge: '',
  },

  observers: {
    // 角标只在生效期出现，未开通 / 已过期 / 置灰都不画：一个点不动的 👑 比没有更让人怀疑自己掉了权益
    member(val) {
      const badge = val && val.effective ? (MEMBER_TIER_SHORT_TEXT[val.tier] || '') : ''
      if (badge !== this.data.memberBadge) this.setData({ memberBadge: badge })
    },
  },

  observers: {
    // 「加入 · 第 N 天」：首日算第 1 天；createdAt 缺失或非法（旧登录快照无此字段）时整行不渲染
    user(val) {
      const createdAt = val && val.createdAt
      if (!createdAt || !dayjs(createdAt).isValid()) {
        if (this.data.joinLabel) this.setData({ joinLabel: '' })
        return
      }
      const joined = dayjs(createdAt).startOf('day')
      const days = Math.max(dayjs().startOf('day').diff(joined, 'day') + 1, 1)
      this.setData({ joinLabel: `${joined.format('YYYY-MM-DD')} 加入 · 第 ${days} 天` })
    },
  },

  methods: {
    onTap() {
      wx.$navigateTo('/subpackages/user/pages/edit/index')
    },
  },
})
