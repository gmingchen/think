<template>
  <el-card id="sec-user" shadow="never" class="section">
    <template #header>
      <SectionHeader title="用户与活跃" :insight="insight" :level="insightLevel" />
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
          <MetricCard label="区间活跃用户" :value="fmtNum(summary.activeUsers)" scope="range" color="#409EFF" />
          <MetricCard label="活跃率" :value="fmtPct(summary.activeUsers, summary.totalUsers)" sub="= 区间活跃 ÷ 累计用户" scope="range" color="#67C23A" />
          <MetricCard label="新增回访率" :value="fmtPct(summary.newReturnedUsers, summary.newUsers)" sub="= 区间新用户再次登录 ÷ 区间新增" scope="range" color="#E6A23C" />
          <MetricCard label="区间新增(邀请)" :value="fmtNum(summary.newInvitedUsers)" :sub="`占区间新增 ${fmtPct(summary.newInvitedUsers, summary.newUsers)}`" scope="range" color="#409EFF" />
          <MetricCard label="区间新增(非邀请)" :value="fmtNum(summary.newNormalUsers)" sub="自然新增" scope="range" color="#909399" />
          <MetricCard label="区间活跃(邀请)" :value="fmtNum(summary.activeInvitedUsers)" :sub="`占区间活跃 ${fmtPct(summary.activeInvitedUsers, summary.activeUsers)}`" scope="range" color="#409EFF" />
          <MetricCard label="区间活跃(非邀请)" :value="fmtNum(summary.activeNormalUsers)" sub="自然活跃" scope="range" color="#909399" />
          <MetricCard label="沉默用户占比" :value="fmtPct(summary.silentUsers, summary.totalUsers)" sub="沉默 = 14 天未登录" scope="all" color="#F56C6C" />
          <MetricCard label="邀请 TOP1" :value="top1 ? top1.nickname : '—'" :sub="top1 ? `带来 ${top1.inviteCount} 人` : ''" scope="all" color="#E6A23C" />
        </div>

        <el-card class="mb16" shadow="never" header="每日新增 / 活跃趋势">
          <EChart :option="trendOption" height="320px" />
        </el-card>

        <el-row :gutter="16">
          <el-col :span="10">
            <el-card shadow="never" header="性别分布">
              <EChart :option="genderOption" height="280px" />
            </el-card>
          </el-col>
          <el-col :span="14">
            <el-card shadow="never">
              <template #header>
                <div class="rank-header">
                  <span>邀请排行 · TOP{{ top }}</span>
                  <el-radio-group v-model="top" size="small" @change="load">
                    <el-radio-button v-for="n in TOP_OPTIONS" :key="n" :value="n">{{ n }}</el-radio-button>
                  </el-radio-group>
                </div>
              </template>
              <el-table :data="topInviter" size="small" max-height="280px">
                <el-table-column type="index" label="#" width="50" />
                <el-table-column prop="nickname" label="昵称" show-overflow-tooltip />
                <el-table-column prop="inviteCount" label="邀请人数" width="120" sortable />
              </el-table>
            </el-card>
          </el-col>
        </el-row>

        <div class="sec-note">口径：活跃 = 区间内登录流水去重（v14 精确 DAU，上线前历史日期无流水记为 0）；沉默 = 连续 14 天未登录（累计口径，仍取 last_login_at）；新增回访 = 注册后再次登录过（last_login &gt; created_at，非严格留存）；被邀请 = inviter_id 非空（经分享 / 邀请码归因注册），非邀请 = 自然新增；区间活跃按邀请来源拆分 = 活跃用户（登录流水去重）关联注册时的 inviter_id 是否为空（邀请 + 非邀请 = 区间活跃总）</div>
      </template>
    </div>
  </el-card>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { getUserStats } from '@/api/stats'
import SectionHeader from '@/components/SectionHeader.vue'
import MetricCard from '@/components/MetricCard.vue'
import EChart from '@/components/EChart.vue'
import { baseGrid, axisDataZoom, xAxisCategory } from '@/utils/chart'
import { fmtNum, fmtPct } from '@/utils/format'
import { useLoad } from '@/utils/load'

const props = defineProps({ range: { type: Array, default: () => [] } })

const TOP_OPTIONS = [10, 20, 50, 100]
const top = ref(10)

const raw = ref({})
const summary = computed(() => raw.value.summary || {})
const topInviter = computed(() => raw.value.topInviter || [])
const top1 = computed(() => topInviter.value[0])

// 沉默占比超 30% 视为用户流失风险，洞察转警示色
const insightLevel = computed(() => {
  const s = summary.value
  const t = Number(s.totalUsers) || 0
  const silent = Number(s.silentUsers) || 0
  return t && silent / t > 0.3 ? 'warning' : 'info'
})

const insight = computed(() => {
  const s = summary.value
  const silentRate = fmtPct(s.silentUsers, s.totalUsers)
  const warn = insightLevel.value === 'warning' ? '，需留意流失' : ''
  return `区间活跃率 ${fmtPct(s.activeUsers, s.totalUsers)}，沉默用户占 ${silentRate}${warn}`
})

const trendOption = computed(() => {
  const t = raw.value.trend || []
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['被邀请新增', '非邀请新增', '活跃'] },
    grid: baseGrid,
    dataZoom: axisDataZoom(),
    xAxis: xAxisCategory(t.map((r) => r.date)),
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      // 新增拆邀请来源：堆叠柱总高即原「新增」总量，活跃仍为折线
      { name: '被邀请新增', type: 'bar', stack: 'new', barMaxWidth: 18, data: t.map((r) => r.newInvitedUsers) },
      { name: '非邀请新增', type: 'bar', stack: 'new', barMaxWidth: 18, data: t.map((r) => r.newNormalUsers) },
      { name: '活跃', type: 'line', smooth: true, showSymbol: false, data: t.map((r) => r.activeUsers) }
    ]
  }
})

const genderOption = computed(() => {
  const g = raw.value.gender || []
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: ['45%', '70%'],
        data: g.map((r) => ({ name: r.label, value: r.num })),
        label: { formatter: '{b} {d}%' }
      }
    ]
  }
})

const { loading, failed, load } = useLoad(async () => {
  raw.value = await getUserStats({ start: props.range[0], end: props.range[1], limit: top.value })
})

watch(() => props.range, load, { deep: true })
onMounted(load)
defineExpose({ load })
</script>

<style scoped>
.rank-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.sec-note {
  font-size: 12px;
  color: var(--text-3);
  margin-top: 12px;
}
</style>
