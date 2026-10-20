<template>
  <el-card id="sec-cum-trend" shadow="never" class="section">
    <template #header>
      <div class="cum-header">
        <span class="section-title">每日累计走势</span>
        <el-radio-group v-model="metric" size="small">
          <el-radio-button v-for="o in metricOptions" :key="o.value" :value="o.value">
            {{ o.label }}
          </el-radio-button>
        </el-radio-group>
      </div>
    </template>

    <div v-loading="loading">
      <el-alert
        v-if="failed"
        type="error"
        :closable="false"
        title="数据加载失败，请确认后端服务（8810）是否正常"
      />
      <EChart v-else :option="option" height="360px" />
    </div>
  </el-card>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { getCumulative } from '@/api/stats'
import EChart from '@/components/EChart.vue'
import { baseGrid, axisDataZoom, xAxisCategory } from '@/utils/chart'
import { useLoad } from '@/utils/load'

const props = defineProps({ range: { type: Array, default: () => [] } })

// 块级自治：自请求 cumulative，与每日增量块同接口但互不连带 loading
const raw = ref({})
const { loading, failed, load } = useLoad(async () => {
  raw.value = await getCumulative({ start: props.range[0], end: props.range[1] })
})
watch(() => props.range, load, { deep: true })
onMounted(load)
defineExpose({ load })

// 各指标对应的后端数组 key、数值字段与单位（revenues 为金额，字段 amount）
const metricOptions = [
  { value: 'users', label: '累计用户', field: 'num', color: '#409EFF' },
  { value: 'kitchens', label: '累计厨房', field: 'num', color: '#67C23A' },
  { value: 'dishes', label: '累计菜谱', field: 'num', color: '#E6A23C' },
  { value: 'orders', label: '累计订单', field: 'num', color: '#409EFF' },
  { value: 'reviews', label: '累计评价', field: 'num', color: '#67C23A' },
  { value: 'memberOpens', label: '累计开通人次', field: 'num', color: '#E6A23C' },
  { value: 'revenues', label: '累计收入', field: 'amount', color: '#F56C6C' }
]

const metric = ref('users')

const current = computed(() => metricOptions.find((o) => o.value === metric.value) || metricOptions[0])
const isRevenue = computed(() => metric.value === 'revenues')

const option = computed(() => {
  const list = raw.value[metric.value] || []
  const { field, color, label } = current.value
  return {
    tooltip: {
      trigger: 'axis',
      formatter: (ps) => {
        const p = ps[0]
        const v = isRevenue.value ? Number(p.value).toFixed(2) + ' 元' : Number(p.value).toLocaleString()
        return `${p.axisValue}<br/>${label}：${v}`
      }
    },
    grid: baseGrid,
    dataZoom: axisDataZoom(),
    xAxis: xAxisCategory(list.map((r) => r.date)),
    yAxis: {
      type: 'value',
      name: isRevenue.value ? '累计收入(元)' : '累计数量',
      minInterval: isRevenue.value ? undefined : 1
    },
    series: [
      {
        name: label,
        type: 'line',
        smooth: true,
        showSymbol: false,
        lineStyle: { width: 2, color },
        areaStyle: { color, opacity: 0.1 },
        data: list.map((r) => r[field])
      }
    ]
  }
})
</script>

<style scoped>
.section-title {
  font-size: 15px;
  font-weight: 600;
}
.cum-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}
</style>
