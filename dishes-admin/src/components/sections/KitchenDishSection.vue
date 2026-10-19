<template>
  <el-card id="sec-kitchen" shadow="never" class="section">
    <template #header>
      <SectionHeader title="内容生态" :insight="insight" :level="insightLevel" />
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
          <MetricCard label="家庭协作率" :value="familyRate" sub="= 成员数 ≥ 2 的厨房占比" scope="all" color="#E6A23C" />
          <MetricCard label="平均每厨房菜谱" :value="dishPerKitchen" suffix=" 道" sub="= 累计菜谱 ÷ 累计厨房" scope="all" color="#409EFF" />
          <MetricCard label="被做过比例" :value="fmtPct(dishSummary.cookedDishes, dishSummary.totalDishes)" sub="内容激活（按做过）" scope="all" color="#67C23A" />
          <MetricCard label="被收藏比例" :value="fmtPct(dishSummary.collectedDishes, dishSummary.totalDishes)" sub="跨厨房传播（按收藏）" scope="all" color="#409EFF" />
          <MetricCard label="封面覆盖率" :value="fmtPct(cover.withCover, coverTotal)" sub="内容质量参考" scope="all" color="#909399" />
        </div>

        <div class="rank-toolbar">
          <span class="rank-toolbar__label">榜单条数</span>
          <el-radio-group v-model="top" size="small" @change="load">
            <el-radio-button v-for="n in TOP_OPTIONS" :key="n" :value="n">{{ n }}</el-radio-button>
          </el-radio-group>
        </div>

        <el-row class="mb16" :gutter="16">
          <el-col :span="10">
            <el-card shadow="never" header="厨房成员数分布">
              <EChart :option="memberDistOption" height="300px" />
            </el-card>
          </el-col>
          <el-col :span="14">
            <el-card shadow="never" :header="`最活跃厨房 TOP${top}（按菜谱数）`">
              <el-table :data="topKitchens" size="small" max-height="300px">
                <el-table-column type="index" label="#" width="50" />
                <el-table-column prop="name" label="厨房名" show-overflow-tooltip />
                <el-table-column prop="owner" label="厨师长" show-overflow-tooltip />
                <el-table-column prop="dishCount" label="菜谱数" width="120" sortable />
              </el-table>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-card shadow="never" :header="`热门菜谱 TOP${top}（按做过次数）`">
              <el-table :data="topCook" size="small" max-height="320px">
                <el-table-column type="index" label="#" width="45" />
                <el-table-column prop="name" label="菜名" show-overflow-tooltip />
                <el-table-column prop="cookCount" label="做过" width="70" />
                <el-table-column prop="collectCount" label="收藏" width="70" />
                <el-table-column prop="kitchen" label="厨房" width="120" show-overflow-tooltip />
              </el-table>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="never" :header="`被收藏最多 TOP${top}（跨厨房传播力）`">
              <el-table :data="topCollect" size="small" max-height="320px">
                <el-table-column type="index" label="#" width="45" />
                <el-table-column prop="name" label="菜名" show-overflow-tooltip />
                <el-table-column prop="collectCount" label="被收藏" width="90" />
                <el-table-column prop="kitchen" label="原始厨房" width="120" show-overflow-tooltip />
              </el-table>
            </el-card>
          </el-col>
        </el-row>

        <div class="sec-note">口径：被做过 / 被收藏比例 = 对应菜谱数 ÷ 累计菜谱数（全量）；家庭协作率按厨房成员数分布计算</div>
      </template>
    </div>
  </el-card>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { getKitchenStats, getDishStats } from '@/api/stats'
import SectionHeader from '@/components/SectionHeader.vue'
import MetricCard from '@/components/MetricCard.vue'
import EChart from '@/components/EChart.vue'
import { fmtPct } from '@/utils/format'
import { useLoad } from '@/utils/load'

const props = defineProps({ range: { type: Array, default: () => [] } })

const TOP_OPTIONS = [10, 20, 50, 100]
const top = ref(10)

const kitchenRaw = ref({})
const dishRaw = ref({})

const kitchenSummary = computed(() => kitchenRaw.value.summary || {})
const topKitchens = computed(() => kitchenRaw.value.topKitchens || [])
const memberDist = computed(() => kitchenRaw.value.memberDist || [])
const dishSummary = computed(() => dishRaw.value.summary || {})
const cover = computed(() => dishRaw.value.cover || {})
const topCook = computed(() => dishRaw.value.topCook || [])
const topCollect = computed(() => dishRaw.value.topCollect || [])

const coverTotal = computed(() => (Number(cover.value.withCover) || 0) + (Number(cover.value.withoutCover) || 0))

// 家庭协作率 = 成员数 ≥ 2 的厨房 ÷ 全部厨房（无分布数据时返回 '—'）
const familyRate = computed(() => {
  const d = memberDist.value
  if (!d.length) return '—'
  const total = d.reduce((s, r) => s + (Number(r.num) || 0), 0)
  const family = d.filter((r) => Number(r.memberCount) >= 2).reduce((s, r) => s + (Number(r.num) || 0), 0)
  return fmtPct(family, total)
})

const dishPerKitchen = computed(() => {
  const t = Number(dishSummary.value.totalDishes) || 0
  const k = Number(kitchenSummary.value.totalKitchens) || 0
  return k ? (t / k).toFixed(1) : '—'
})

// 被做过比例低于 20% 视为内容激活不足，洞察转警示色
const insightLevel = computed(() => {
  const t = Number(dishSummary.value.totalDishes) || 0
  const c = Number(dishSummary.value.cookedDishes) || 0
  return t && c / t < 0.2 ? 'warning' : 'info'
})

const insight = computed(() => {
  const rate = fmtPct(dishSummary.value.cookedDishes, dishSummary.value.totalDishes)
  const warn = insightLevel.value === 'warning' ? '，内容激活偏低' : ''
  return `${rate} 的菜谱被做过，家庭协作厨房占 ${familyRate.value}${warn}`
})

const memberDistOption = computed(() => {
  const d = memberDist.value
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 60, right: 24, top: 30, bottom: 40 },
    xAxis: { type: 'category', name: '成员数', data: d.map((r) => r.memberCount) },
    yAxis: { type: 'value', name: '厨房数量', minInterval: 1 },
    series: [{ type: 'bar', barMaxWidth: 48, itemStyle: { color: '#409EFF' }, data: d.map((r) => r.num) }]
  }
})

const { loading, failed, load } = useLoad(async () => {
  const params = { start: props.range[0], end: props.range[1], limit: top.value }
  const [k, d] = await Promise.all([getKitchenStats(params), getDishStats(params)])
  kitchenRaw.value = k
  dishRaw.value = d
})

watch(() => props.range, load, { deep: true })
onMounted(load)
defineExpose({ load })
</script>

<style scoped>
.rank-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.rank-toolbar__label {
  font-size: 13px;
  color: var(--text-3);
}

.sec-note {
  font-size: 12px;
  color: var(--text-3);
  margin-top: 12px;
}
</style>
