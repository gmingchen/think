import { decisionListUrl } from '../../../../common/constants/index'
import { DECISION_SCENE_ENUM } from '../../../../common/enums/index'

Component({
  methods: {
    // 入口固定跳「我的」决策单列表，不依赖页面状态，跳转逻辑组件自管
    onTap() {
      wx.$navigateTo(decisionListUrl(DECISION_SCENE_ENUM.MINE))
    },
  },
})
