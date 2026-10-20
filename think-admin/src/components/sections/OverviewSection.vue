<template>
  <el-card id="sec-overview" shadow="never" class="section">
    <template #header>
      <span class="section-title">今日数据统计 · {{ todayStr }}</span>
    </template>

    <div v-loading="loading">
      <el-alert
        v-if="failed"
        type="error"
        :closable="false"
        title="数据加载失败，请确认后端服务（8810）是否正常"
      />
      <div v-else class="grid grid-cards grid-cards--4col">
        <el-card v-for="c in cards" :key="c.label" shadow="never" class="today-cell">
          <div class="today-cell__top">
            <span class="today-cell__label">{{ c.label }}</span>
            <span class="today-cell__delta" :class="`today-cell__delta--${c.delta.type}`">{{ c.delta.text }}</span>
          </div>
          <div class="today-cell__value" :style="{ color: c.color }">
            {{ c.display }}<span v-if="c.suffix" class="today-cell__suffix">{{ c.suffix }}</span>
          </div>
          <div class="today-cell__yesterday">昨日 {{ c.yesterdayDisplay }}</div>
        </el-card>
      </div>

      <div v-if="!failed && hourlyList.length" class="hourly">
        <div class="hourly__title">逐小时走势 · {{ hourlyTitle }}</div>
        <EChart :option="hourlyOption" height="320px" />
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getOverview, getTodayHourly, fmt } from '@/api/stats'
import { fmtNum } from '@/utils/format'
import { useLoad } from '@/utils/load'
import { baseGrid } from '@/utils/chart'
import EChart from '@/components/EChart.vue'

// 块级自治：自请求 overview 快照（今日/昨日对比）+ 逐小时走势，与其他块互不连带
const raw = ref({})
const hourly = ref({})
const { loading, failed, load } = useLoad(async () => {
  const [overview, hourlyTrend] = await Promise.all([getOverview(), getTodayHourly()])
  raw.value = overview
  hourly.value = hourlyTrend
})
onMounted(load)
defineExpose({ load })

const data = computed(() => raw.value)

const todayStr = fmt(new Date())

// 环比昨日：昨日为 0 时不显示百分比（今日有量记「新」，两日皆 0 记「—」）
const delta = (today, yesterday) => {
  const t = Number(today) || 0
  const y = Number(yesterday) || 0
  if (y === 0) {
    if (t === 0) return { type: 'flat', text: '—' }
    return { type: 'up', text: '▲ 新增' }
  }
  const pct = ((t - y) / y) * 100
  if (pct > 0) return { type: 'up', text: `▲ ${pct.toFixed(1)}%` }
  if (pct < 0) return { type: 'down', text: `▼ ${Math.abs(pct).toFixed(1)}%` }
  return { type: 'flat', text: '= 0%' }
}

const cards = computed(() => {
  const d = data.value
  return [
    { label: '今日新增用户', today: d.todayNewUsers, yesterday: d.yesterdayNewUsers, color: '#409EFF' },
    { label: '今日活跃用户', today: d.activeToday, yesterday: d.yesterdayActive, color: '#409EFF' },
    { label: '今日新建厨房', today: d.todayNewKitchens, yesterday: d.yesterdayNewKitchens, color: '#67C23A' },
    { label: '今日新增菜谱', today: d.todayNewDishes, yesterday: d.yesterdayNewDishes, color: '#E6A23C' },
    { label: '今日订单', today: d.todayOrders, yesterday: d.yesterdayOrders, color: '#409EFF' },
    { label: '今日新增评价', today: d.todayReviews, yesterday: d.yesterdayReviews, color: '#67C23A' },
    { label: '今日会员开通', today: d.todayMemberOpens, yesterday: d.yesterdayMemberOpens, color: '#E6A23C' },
    { label: '今日会员收入', today: d.todayRevenue, yesterday: d.yesterdayRevenue, suffix: ' 元', precision: 2, color: '#F56C6C' }
  ].map((c) => {
    const p = c.precision == null ? -1 : c.precision
    return { ...c, display: fmtNum(c.today, p), yesterdayDisplay: fmtNum(c.yesterday, p), delta: delta(c.today, c.yesterday) }
  })
})

// ── 逐小时走势：今日 / 昨日的新增与活跃 ──
const COLOR_ACTIVE = '#409EFF'
const COLOR_NEW = '#67C23A'
const LEGEND = ['今日活跃', '昨日活跃', '今日新增', '昨日新增']

const hourlyList = computed(() => hourly.value.series || [])

const hourlyTitle = computed(() => {
  const { today, yesterday } = hourly.value
  return today && yesterday ? `今日 ${today} / 昨日 ${yesterday}` : ''
})

// 按指标分色、按日期分虚实：今日实线、昨日虚线，同指标的今昨差距一眼可辨
const buildLine = (name, key, color, dashed) => ({
  name,
  type: 'line',
  smooth: true,
  showSymbol: false,
  lineStyle: { width: dashed ? 1.5 : 2, color, type: dashed ? 'dashed' : 'solid', opacity: dashed ? 0.55 : 1 },
  itemStyle: { color },
  data: hourlyList.value.map((r) => r[key])
})

const hourlyOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    // 今日未到的小时后端返回 null（折线断开），提示框里直接不列这一项
    formatter: (ps) => {
      const rows = ps.filter((p) => p.value !== null && p.value !== undefined)
      return `${ps[0].axisValue}<br/>${rows.map((p) => `${p.marker}${p.seriesName}：${fmtNum(p.value)}`).join('<br/>')}`
    }
  },
  legend: { top: 0, data: LEGEND },
  grid: { ...baseGrid, top: 42, bottom: 24 },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: hourlyList.value.map((r) => `${String(r.hour).padStart(2, '0')}:00`),
    axisLabel: { hideOverlap: true }
  },
  yAxis: { type: 'value', name: '人数', minInterval: 1 },
  series: [
    buildLine('今日活跃', 'todayActive', COLOR_ACTIVE, false),
    buildLine('昨日活跃', 'yesterdayActive', COLOR_ACTIVE, true),
    buildLine('今日新增', 'todayNew', COLOR_NEW, false),
    buildLine('昨日新增', 'yesterdayNew', COLOR_NEW, true)
  ]
}))
</script>

<style scoped>
.section-title {
  font-size: 15px;
  font-weight: 600;
}
.today-cell {
  border-radius: 10px;
}
.today-cell__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.today-cell__label {
  font-size: 13px;
  color: var(--text-2);
}
.today-cell__delta {
  font-size: 12px;
  font-weight: 600;
}
.today-cell__delta--up {
  color: #67c23a;
}
.today-cell__delta--down {
  color: #f56c6c;
}
.today-cell__delta--flat {
  color: var(--text-2);
}
.today-cell__value {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.2;
}
.today-cell__suffix {
  font-size: 13px;
  font-weight: 400;
  margin-left: 2px;
  color: var(--text-2);
}
.today-cell__yesterday {
  font-size: 12px;
  color: var(--text-3);
  margin-top: 6px;
}
.hourly {
  margin-top: 20px;
}
.hourly__title {
  font-size: 13px;
  color: var(--text-2);
  margin-bottom: 8px;
}
</style>
