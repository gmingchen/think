import { storeComponentBehavior } from '../../../../behaviors/index'
import { cookOrderLatestApi } from '../../../../apis/index'
import { DEFAULT_DISH_COVER } from '../../../../common/constants/index'
import dayjs from 'dayjs'

const PERIOD_MAP = { 1: '早餐', 2: '午餐', 3: '晚餐', 4: '宵夜' }

function formatDateLabel(dateStr) {
  const today = dayjs().format('YYYY-MM-DD')
  const tomorrow = dayjs().add(1, 'day').format('YYYY-MM-DD')
  if (dateStr === today) return '今天'
  if (dateStr === tomorrow) return '明天'
  return dateStr
}

Component({
  behaviors: [storeComponentBehavior({ fields: ['kitchen'] })],
  data: {
    DEFAULT_DISH_COVER,
    title: '🍲 最近一餐',
    period: '',
    orders: [],
  },
  observers: {
    'kitchen.active'() {
      this.getData()
    }
  },
  pageLifetimes: {
    show() {
      this.getData()
    }
  },
  methods: {
    async getData() {
      const r = await cookOrderLatestApi()
      if (!r.data || r.data.length === 0) {
        this.setData({ orders: [], period: '' })
        return
      }
      const first = r.data[0]
      const dateLabel = formatDateLabel(first.date)
      const periodLabel = PERIOD_MAP[first.timePeriod] || ''
      const orders = r.data.map(order => ({
        id: order.id,
        ordererName: order.ordererName || '',
        remark: order.remark || '',
        dishes: (order.dishes || []).map(d => ({
          dishId: d.dishId,
          name: d.dishName,
          cover: d.dishCover,
          quantity: d.quantity,
        })),
      }))
      this.setData({
        orders,
        period: `${dateLabel} · ${periodLabel}`,
      })
    },
    onOrderTap(e) {
      const { id } = e.currentTarget.dataset
      wx.$navigateTo(`/subpackages/cookorder/pages/detail/index?id=${id}`)
    },
    // 采购清单：最近一餐全部订单为数据源，份数由后端按订单菜品数量取
    onGoShopping() {
      const cookOrderIds = this.data.orders.map(o => o.id).join(',')
      if (!cookOrderIds) return
      wx.$navigateTo(`/subpackages/dish/pages/shopping/index?cookOrderIds=${cookOrderIds}&source=latest`)
    },
    onDishTap(e) {
      const { dishId } = e.currentTarget.dataset
      this.triggerEvent('dishTap', { id: dishId })
    }
  }
})
