import dayjs from 'dayjs'
import { storeComponentBehavior } from '../../../../behaviors/index'
import { VIP_PAGE, MEMBER_EXPIRE_WARN_DAYS } from '../../../../common/constants/index'

// 「我的」页会员卡（§4.1 / §10.1）：四态由后端 mineCard.tone 驱动，前端只套样式、不拼文案。
// 价格串也来自 mineCard.price——「置灰期→放开期只改后端配置、不发版」这条约束就在这里落地。
// 2026-10-11 已按当初定的四步换源接上 store，../preview 那个占位数据源随之删除，wxml / wxss 一行未改。
Component({
  behaviors: [storeComponentBehavior({ fields: ['member'] })],

  data: {
    loading: true,
    card: null,
    urgent: false,
    actionTone: 'primary',
  },

  observers: {
    // 判据用 mineCard 而不是 loaded：接口失败时 store 会把 loaded 置 true 但 mineCard 仍是 null
    // （§10.1），那一刻必须还停在骨架——已付 ¥39.9 的人一进来看见「开通会员 · ¥1.9 起」，
    // 会认定自己的钱白花了，这比价格闪一下严重得多
    member(val) {
      const card = (val && val.mineCard) || null
      if (!card) {
        this.setData({ loading: true, card: null, urgent: false, actionTone: 'primary' })
        return
      }
      const remainDays = (val && val.remainDays) || 0
      // 临期不占一个 tone：后端只给 gold，「剩 N 天」由这里转 --color-warning，不加横幅（§4.1 口径 3）
      const urgent = card.tone === 'gold' && remainDays > 0 && remainDays <= MEMBER_EXPIRE_WARN_DAYS
      this.setData({
        loading: false,
        // stamp / remainDays / expireAt 都是本页推导的展示位，后端不下发（§4.1 末「不为临期写第二种文案」）；
        // expireAt 取原值格式化——已过期的人那一列仍在库里留着，客服要能对上账（§8.1）
        card: {
          ...card,
          stamp: card.tone === 'gold' ? (urgent ? '即将到期' : '生效中') : '',
          remainDays: card.tone === 'gold' ? remainDays : 0,
          expireAt: val.expireAt ? dayjs(val.expireAt).format('YYYY.MM.DD') : '',
        },
        urgent,
        actionTone: resolveActionTone(card.tone, urgent),
      })
    },
  },

  methods: {
    // 整卡与按钮同一个动作：卡片是大命中区，会员入口不该只有一小块能点
    onActionTap() {
      const { loading, card } = this.data
      // 骨架态不可点（§4.1）：此刻还不知道这人是不是会员，给任何提示都是瞎报
      if (loading) return
      if (card.tone === 'disabled') {
        // 置灰不是 bug，得说清为什么点不动——静默会让用户怀疑自己网络坏了
        wx.$toast('会员还在准备中，放开后这里可以直接开通')
        return
      }
      wx.$navigateTo(VIP_PAGE)
    },
  },
})

// 按钮色调由 tone + 是否临期推导，不额外要求后端下发字段：
// 未开通用主色（唯一带钱的 CTA）；生效中用描边（不抢注意力）；临期转主色（温和提醒，不加横幅）
const resolveActionTone = (tone, urgent) => {
  if (tone === 'disabled') return 'disabled'
  if (tone === 'expired') return 'outline'
  if (tone === 'gold') return urgent ? 'primary' : 'outline'
  return 'primary'
}
