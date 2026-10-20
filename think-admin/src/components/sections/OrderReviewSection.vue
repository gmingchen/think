<template>
  <el-card id="sec-order" shadow="never" class="section">
    <template #header>
      <SectionHeader title="做饭体验" :insight="insight" :level="insightLevel" />
    </template>

    <div v-loading="loading">
      <el-alert
        v-if="failed"
        type="error"
        :closable="false"
        title="数据加载失败，请确认后端服务（8810）是否正常"
      />
      <template v-else>
        <div class="grid grid-cards mb16">
          <MetricCard label="订单完成率" :value="pctText(orderSummary.completionRate)" sub="= 已完成 ÷ 未取消订单" scope="all" color="#67C23A" />
          <MetricCard label="评价率" :value="fmtPct(reviewSummary.reviewCount, orderSummary.doneInRange)" sub="= 区间评价 ÷ 区间已完成" scope="range" color="#409EFF" />
          <MetricCard label="取消率" :value="fmtPct(orderSummary.canceledOrders, orderSummary.totalOrders)" sub="= 已取消 ÷ 累计订单" scope="all" color="#F56C6C" />
          <MetricCard label="区间平均评分" :value="fmtNum(reviewSummary.avgRating, 2)" suffix=" 分" :sub="`区间评价 ${fmtNum(reviewSummary.reviewCount)} 条`" scope="range" color="#E6A23C" />
        </div>

        <el-row class="mb16" :gutter="16">
          <el-col :span="12">
            <el-card shadow="never" header="各时段订单分布（何时做饭最多）">
              <EChart :option="periodOption" height="280px" />
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="never" header="评分分布（区间）">
              <EChart :option="ratingDistOption" height="280px" />
            </el-card>
          </el-col>
        </el-row>

        <el-card shadow="never" header="平均评分走势">
          <EChart :option="ratingTrendOption" height="300px" />
        </el-card>

        <div class="sec-note">口径：完成率 / 取消率为全量状态构成；评价率、时段分布、评分分布与均分走势随日期区间联动（评价按评价时间、订单按下单时间，略有错位）</div>
      </template>
    </div>
  </el-card>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { getOrderStats, getReviewStats } from '@/api/stats'
import SectionHeader from '@/components/SectionHeader.vue'
import MetricCard from '@/components/MetricCard.vue'
import EChart from '@/components/EChart.vue'
import { baseGrid, axisDataZoom, xAxisCategory } from '@/utils/chart'
import { fmtNum, fmtPct } from '@/utils/format'
import { useLoad } from '@/utils/load'

const props = defineProps({ range: { type: Array, default: () => [] } })

const orderRaw = ref({})
const reviewRaw = ref({})

const orderSummary = computed(() => orderRaw.value.summary || {})
const reviewSummary = computed(() => reviewRaw.value.summary || {})
const ratingDist = computed(() => reviewRaw.value.ratingDist || [])

const pctText = (v) => (v === null || v === undefined || v === '' ? '—' : `${Number(v).toFixed(1)}%`)

const lowRateNum = computed(() => {
  const r = reviewSummary.value
  const total = Number(r.reviewCount) || 0
  return total ? (Number(r.low) || 0) / total : 0
})

// 完成率 < 80% 或差评率 > 5% 视为体验风险，洞察转红色
const insightLevel = computed(() => {
  const completion = Number(orderSummary.value.completionRate)
  const completionBad = !Number.isNaN(completion) && completion < 80
  return completionBad || lowRateNum.value > 0.05 ? 'danger' : 'info'
})

const insight = computed(() => {
  const o = orderSummary.value
  const r = reviewSummary.value
  const warn = insightLevel.value === 'danger' ? '，需关注' : ''
  return `完成率 ${pctText(o.completionRate)}，平均评分 ${fmtNum(r.avgRating, 2)}，差评率 ${fmtPct(r.low, r.reviewCount)}${warn}`
})

const periodOption = computed(() => {
  const d = orderRaw.value.periodDist || []
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: ['40%', '68%'],
        data: d.map((r) => ({ name: r.label, value: r.num })),
        label: { formatter: '{b} {d}%' }
      }
    ]
  }
})

// 评分 1~5 星补齐缺位星为 0，柱色按星级由冷到暖
const ratingDistOption = computed(() => {
  const idx = new Map(ratingDist.value.map((r) => [Number(r.rating), Number(r.num) || 0]))
  const stars = [1, 2, 3, 4, 5]
  const colors = ['#F56C6C', '#F56C6C', '#E6A23C', '#67C23A', '#67C23A']
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 60, right: 24, top: 30, bottom: 40 },
    xAxis: { type: 'category', data: stars.map((s) => `${s} 星`) },
    yAxis: { type: 'value', name: '评价数', minInterval: 1 },
    series: [
      {
        type: 'bar',
        barMaxWidth: 48,
        data: stars.map((s, i) => ({ value: idx.get(s) || 0, itemStyle: { color: colors[i] } }))
      }
    ]
  }
})

const ratingTrendOption = computed(() => {
  const t = reviewRaw.value.trend || []
  return {
    tooltip: { trigger: 'axis', valueFormatter: (v) => Number(v).toFixed(1) },
    grid: baseGrid,
    dataZoom: axisDataZoom(),
    xAxis: xAxisCategory(t.map((r) => r.date)),
    yAxis: { type: 'value', name: '均分', min: 0, max: 5 },
    series: [
      {
        name: '平均评分',
        type: 'line',
        smooth: true,
        showSymbol: false,
        lineStyle: { width: 2, color: '#E6A23C' },
        areaStyle: { color: '#E6A23C', opacity: 0.1 },
        data: t.map((r) => r.avgRating)
      }
    ]
  }
})

const { loading, failed, load } = useLoad(async () => {
  const params = { start: props.range[0], end: props.range[1] }
  const [o, r] = await Promise.all([getOrderStats(params), getReviewStats(params)])
  orderRaw.value = o
  reviewRaw.value = r
})

watch(() => props.range, load, { deep: true })
onMounted(load)
defineExpose({ load })
</script>

<style scoped>
.sec-note {
  font-size: 12px;
  color: var(--text-3);
  margin-top: 12px;
}
</style>
