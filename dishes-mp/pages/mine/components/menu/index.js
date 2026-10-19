import { storeComponentBehavior } from '../../../../behaviors/index'

// 角色：1-厨师长，2-管厨，3-食客
const MANAGER_ROLES = [1, 2]

const ALL_LIST = [
  { icon: '🏠', label: '厨房', url: '/subpackages/kitchen/pages/list/index' },
  // 「厨房设置」进的是**当前厨房**：url 只存基础路径，?id= 由 onTap 点击时现拼（见 methods）
  { icon: '⚙️', label: '厨房设置', url: '/subpackages/kitchen/pages/settings/index', currentKitchen: true },
  { icon: '📁', label: '分类', url: '/subpackages/category/pages/list/index', roles: MANAGER_ROLES },
  { icon: '👥', label: '成员', url: '/subpackages/kitchen/pages/member/index', roles: MANAGER_ROLES },
  { icon: '🥬', label: '食材', url: '/subpackages/food/pages/list/index', roles: MANAGER_ROLES },
  { icon: '🧂', label: '调料', url: '/subpackages/seasoning/pages/list/index', roles: MANAGER_ROLES },
  { icon: '📋', label: '厨房订单', url: '/subpackages/cookorder/pages/list/index' },
  { icon: '💬', label: '客服', type: 'contact' },
]

Component({
  behaviors: [storeComponentBehavior({ fields: ['kitchen'] })],

  data: {
    list: [],
  },

  observers: {
    'kitchen.active': function(kitchen) {
      const role = kitchen?.role
      const list = ALL_LIST.filter(item => !item.roles || item.roles.includes(role))
      this.setData({ list })
    },
  },

  methods: {
    onTap(e) {
      // 整对象往下传（全站 data-row / data-item 都是这个写法）：对象内部的键名与类型原样保留，
      // 不会像单独一个 data-current="{{ item.currentKitchen }}" 那样把 undefined 烘成字符串、把布尔判据洗失真
      const row = e.currentTarget.dataset.row || {}
      if (!row.url) {
        // 常量表漏配 url 的症状同样是“点了没反应”，不得静默
        wx.$toast('入口还没配置好')
        return
      }
      // 普通入口：url 本身就是完整的，直接跳
      if (!row.currentKitchen) {
        wx.$navigateTo(row.url)
        return
      }
      // 进「当前厨房」的入口：id 只在点击这一刻从 store 取，避开「渲染时 active 未就绪」把 url 烘成空
      const kitchenId = this.data.kitchen?.active?.id
      if (!kitchenId) {
        // 不能静默 return：navigateTo 对空 target 是无声无错的 no-op，症状就是“点了没反应”
        wx.$toast('厨房还没准备好，稍后再试')
        return
      }
      wx.$navigateTo(`${row.url}?id=${kitchenId}`)
    },
  },
})
