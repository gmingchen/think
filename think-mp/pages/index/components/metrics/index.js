import { decisionListUrl } from '../../../../common/constants/index'
import { DECISION_SCENE_ENUM, DECISION_MINE_TAB_ENUM, DECISION_STATUS_ENUM } from '../../../../common/enums/index'

Component({
  properties: {
    // 首页统计对象：createdCount / recapCount / reviewedCount
    stats: {
      type: Object,
      value: {},
    },
  },
  data: {
    // 三张卡片的跳转参数（枚举常量，非业务状态），点击时随 dataset 取用
    SCENE_MINE: DECISION_SCENE_ENUM.MINE,
    TAB_CREATED: DECISION_MINE_TAB_ENUM.CREATED,
    TAB_REVIEWED: DECISION_MINE_TAB_ENUM.REVIEWED,
    STATUS_RECAPTURED: DECISION_STATUS_ENUM.RECAPTURED,
  },
  methods: {
    // scene/tab/status 均来自各卡自身 dataset，跳转逻辑组件自管
    onItemTap(e) {
      const { scene, tab, status } = e.currentTarget.dataset
      // status 可能为 0（草稿），故用「非 undefined/空串」判断而非真值判断
      const statusQuery = status === undefined || status === '' ? '' : `&status=${ status }`
      wx.$navigateTo(decisionListUrl(scene, `${ tab ? `&tab=${ tab }` : '' }${ statusQuery }`))
    },
  },
})
