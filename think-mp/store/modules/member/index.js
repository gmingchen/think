import { action } from 'mobx-miniprogram'
import { memberInfoApi } from '../../../apis/index'

/**
 * 会员状态（v12 §10.1）
 *
 * 全站会员判定的唯一数据源：广告收口（`utils/ad.js`）、容量闸门（`utils/capacity.js`）、
 * 「我的」页会员卡、会员中心页都读它，所以这里**只存后端下发的原值**，不在前端做任何推导
 * （档位归零、首单资格、卡片文案都是服务端算好的，前端算一份就是第二份真相）。
 */
export default {
  // loaded 之前一切按「还不知道」处理：初值全给非会员的保守值，权益判定因此降级而不是抛错
  member: {
    loaded: false,
    // member.enabled 的原值（本版发版态 false）：只用来置灰 CTA，不参与任何权益判定
    // ⚠️ 不写 mineCard.tone === 'disabled' 这种判据：生效中的人在置灰期 tone 仍是 gold（§5.2）
    enabled: false,
    tier: 0,
    effective: false,
    expireAt: null,
    remainDays: 0,
    adFree: false,
    unlimitedCapacity: false,
    firstOrder: false,
    plans: [],
    mineCard: null,
    records: [],
  },

  getMember: action(async function() {
    try {
      const r = await memberInfoApi()
      // r.data 为空（后端异常回 null）也要置 loaded，否则会永远停在骨架且无人再拉
      this.member = { ...(r.data || {}), loaded: true }
    } catch (error) {
      // ⚠️ 失败也必须把 loaded 置位：它只表示「这一轮请求回来了」，不表示「这人有会员」。
      //    会员卡因此仍停在骨架（判据是 mineCard，见 §4.1），而权益判定按非会员降级——
      //    Redis 或后端整体不可用时，用户看到的与现状完全一致，不会因拿不到快照而打断菜谱主链路（§17 第 10 条）。
      //    提示不由这里控制：apis/service 对业务错误必 toast，此处无法静音，与 getUser() 失败时的表现同一档。
      this.member = { ...this.member, loaded: true }
    }
  }),
}
