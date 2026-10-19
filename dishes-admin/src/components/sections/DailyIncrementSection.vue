<template>
  <el-card id="sec-daily-inc" shadow="never" class="section">
    <template #header>
      <div class="inc-header">
        <span class="section-title">每日增量走势</span>
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

// 块级自治：自请求 cumulative，与累计走势块同接口但互不连带 loading
const raw = ref({})
const { loading, failed, load } = useLoad(async () => {
  raw.value = await getCumulative({ start: props.range[0], end: props.range[1] })
})
watch(() => props.range, load, { deep: true })
onMounted(load)
defineExpose({ load })

// 每日新增序列的后端 key、数值字段与单位（revenueDailyNew 为金额，字段 amount）
const metricOptions = [
  { value: 'userDailyNew', label: '新增用户', field: 'num', color: '#409EFF' },
  { value: 'kitchenDailyNew', label: '新建厨房', field: 'num', color: '#67C23A' },
  { value: 'dishDailyNew', label: '新增菜谱', field: 'num', color: '#E6A23C' },
  { value: 'orderDailyNew', label: '订单', field: 'num', color: '#409EFF' },
  { value: 'reviewDailyNew', label: '新增评价', field: 'num', color: '#67C23A' },
  { value: 'memberOpenDailyNew', label: '会员开通', field: 'num', color: '#E6A23C' },
  { value: 'revenueDailyNew', label: '会员收入', field: 'amount', color: '#F56C6C' }
]

const metric = ref('userDailyNew')

const current = computed(() => metricOptions.find((o) => o.value === metric.value) || metricOptions[0])
const isRevenue = computed(() => metric.value === 'revenueDailyNew')

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
      name: isRevenue.value ? '当日新增(元)' : '当日新增',
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
.inc-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}
</style>
