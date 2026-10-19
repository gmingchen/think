<template>
  <el-card id="sec-member" shadow="never" class="section">
    <template #header>
      <SectionHeader title="会员与收入" :insight="insight" :level="insightLevel" />
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
          <MetricCard label="付费转化率" :value="fmtPct(summary.totalOpened, totalUsers)" sub="= 累计开通人次 ÷ 累计用户" scope="all" color="#E6A23C" />
          <MetricCard label="客单价" :value="fmtNum(income.avgOrder, 2)" suffix=" 元" sub="= 总收入 ÷ 已付订单" scope="all" color="#409EFF" />
          <MetricCard label="净收入" :value="fmtNum(netRevenue, 2)" suffix=" 元" sub="= 总收入 − 退款金额" scope="all" color="#67C23A" />
          <MetricCard label="退款率" :value="fmtPct(refund.refundOrders, income.paidOrders)" sub="= 退款订单 ÷ 已付订单" scope="all" color="#F56C6C" />
          <MetricCard label="7 日内到期" :value="fmtNum(summary.expiringSoon)" suffix=" 人" sub="续费预警，提前触达" scope="all" color="#F56C6C" />
        </div>

        <el-row :gutter="16">
          <el-col :span="10">
            <el-card shadow="never" header="当前有效会员构成">
              <EChart :option="tierOption" height="280px" />
            </el-card>
          </el-col>
          <el-col :span="14">
            <el-card shadow="never" header="各套餐购买分布">
              <el-table :data="planRows" size="small" max-height="280px">
                <el-table-column prop="planCode" label="套餐编码" />
                <el-table-column prop="tier" label="档位" width="90">
                  <template #default="{ row }">{{ row.tier === 1 ? '轻享' : row.tier === 2 ? '畅厨' : row.tier }}</template>
                </el-table-column>
                <el-table-column prop="num" label="购买次数" width="110" sortable />
                <el-table-column prop="share" label="占比" width="90" />
                <el-table-column prop="revenue" label="收入（元）" width="130" sortable />
              </el-table>
            </el-card>
          </el-col>
        </el-row>

        <el-divider content-position="left">会员订单统计</el-divider>
        <p class="order-insight" :class="'order-insight--' + orderInsightLevel">{{ orderInsight }}</p>
        <div class="grid grid-cards mb16">
          <MetricCard label="区间建单" :value="fmtNum(orders.createdOrders)" suffix=" 笔" sub="= 区间内创建的会员订单" scope="range" color="#409EFF" />
          <MetricCard label="支付成功率" :value="fmtPct(orders.paidOrders, orders.createdOrders)" sub="= 已发放 ÷ 区间建单" scope="range" color="#67C23A" />
          <MetricCard label="超时/异常关闭" :value="fmtNum(orders.closedOrders)" suffix=" 笔" sub="15 分钟未付或校验失败" scope="range" color="#E6A23C" />
          <MetricCard label="退款单" :value="fmtNum(orders.refundedOrders)" suffix=" 笔" sub="建单口径，非退款发生时间" scope="range" color="#F56C6C" />
          <MetricCard label="待支付" :value="fmtNum(orders.pendingOrders)" suffix=" 笔" sub="未到期的在途单" scope="range" color="#909399" />
        </div>
        <el-row :gutter="16">
          <el-col :span="10">
            <el-card shadow="never" header="区间订单状态分布">
              <EChart :option="orderStatusOption" height="260px" />
            </el-card>
          </el-col>
          <el-col :span="14">
            <el-card shadow="never" header="支付周期偏好（已发放）">
              <el-table :data="monthsRows" size="small" max-height="260px">
                <el-table-column prop="name" label="周期" />
                <el-table-column prop="num" label="订单数" width="110" sortable />
                <el-table-column prop="share" label="占比" width="90" />
                <el-table-column prop="revenue" label="收入（元）" width="130" sortable />
              </el-table>
            </el-card>
          </el-col>
        </el-row>

        <div class="sec-note">口径：上方收入/退款卡为全量快照（按支付时间）；会员订单统计为区间建单口径（区间内创建的订单，无论后续何时支付/退款），漏斗内部各状态同口径可比；支付周期偏好仅统计已发放订单</div>
      </template>
    </div>
  </el-card>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { getMemberStats, getOverview } from '@/api/stats'
import SectionHeader from '@/components/SectionHeader.vue'
import MetricCard from '@/components/MetricCard.vue'
import EChart from '@/components/EChart.vue'
import { fmtNum, fmtPct } from '@/utils/format'
import { useLoad } from '@/utils/load'

const props = defineProps({
  range: { type: Array, default: () => [] }
})

// 累计用户（overview 快照）：块内自取，用于算付费转化率
const totalUsers = ref(0)

const raw = ref({})
const summary = computed(() => raw.value.summary || {})
const income = computed(() => raw.value.income || {})
const refund = computed(() => raw.value.refund || {})
const planDist = computed(() => raw.value.planDist || [])
const orders = computed(() => raw.value.orders || {})
const monthsDist = computed(() => raw.value.monthsDist || [])

// 净收入 = 总收入 − 退款金额
const netRevenue = computed(() => (Number(income.value.totalRevenue) || 0) - (Number(refund.value.refundAmount) || 0))

const refundRateNum = computed(() => {
  const paid = Number(income.value.paidOrders) || 0
  return paid ? (Number(refund.value.refundOrders) || 0) / paid : 0
})

// 退款率超 5% 视为变现风险，洞察转红色
const insightLevel = computed(() => (refundRateNum.value > 0.05 ? 'danger' : 'info'))

const insight = computed(() => {
  const s = summary.value
  const warn = insightLevel.value === 'danger' ? '，退款偏高需关注' : ''
  return `付费转化 ${fmtPct(s.totalOpened, totalUsers.value)}，客单价 ${fmtNum(income.value.avgOrder, 2)} 元，7 日内到期 ${fmtNum(s.expiringSoon)} 人${warn}`
})

const tierOption = computed(() => {
  const s = summary.value
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: ['45%', '70%'],
        data: [
          { name: '轻享', value: Number(s.liteValid) || 0, itemStyle: { color: '#E6A23C' } },
          { name: '畅厨', value: Number(s.proValid) || 0, itemStyle: { color: '#409EFF' } }
        ],
        label: { formatter: '{b} {d}%' }
      }
    ]
  }
})

// 套餐表补占比列（购买次数 ÷ 总购买次数）
const planRows = computed(() => {
  const rows = planDist.value
  const total = rows.reduce((s, r) => s + (Number(r.num) || 0), 0)
  return rows.map((r) => ({ ...r, share: fmtPct(r.num, total) }))
})

// 支付周期映射：months → 卡种名（1/3/12 为本期在用值）
const MONTH_NAME = { 1: '月卡', 3: '季卡', 12: '年卡' }

// 订单状态环图：区间建单按四状态分布（与收入口径无关）
const orderStatusOption = computed(() => {
  const o = orders.value
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: ['45%', '70%'],
        data: [
          { name: '已发放', value: Number(o.paidOrders) || 0, itemStyle: { color: '#67C23A' } },
          { name: '待支付', value: Number(o.pendingOrders) || 0, itemStyle: { color: '#909399' } },
          { name: '已退款', value: Number(o.refundedOrders) || 0, itemStyle: { color: '#F56C6C' } },
          { name: '已关闭', value: Number(o.closedOrders) || 0, itemStyle: { color: '#E6A23C' } }
        ],
        label: { formatter: '{b} {d}%' }
      }
    ]
  }
})

const monthsRows = computed(() => {
  const rows = monthsDist.value
  const total = rows.reduce((s, r) => s + (Number(r.num) || 0), 0)
  return rows.map((r) => ({
    name: MONTH_NAME[r.months] || `${r.months} 个月`,
    num: r.num,
    share: fmtPct(r.num, total),
    revenue: fmtNum(r.revenue, 2)
  }))
})

// 支付成功率 <70% 视为转化偏低，订单洞察转黄
const orderInsightLevel = computed(() => {
  const created = Number(orders.value.createdOrders) || 0
  const paid = Number(orders.value.paidOrders) || 0
  return created && paid / created < 0.7 ? 'warning' : 'info'
})

const orderInsight = computed(() => {
  const o = orders.value
  const warn = orderInsightLevel.value === 'warning' ? '，转化偏低需关注' : ''
  return `区间建单 ${fmtNum(o.createdOrders)} 笔，支付成功率 ${fmtPct(o.paidOrders, o.createdOrders)}，关闭 ${fmtNum(o.closedOrders)} 笔，退款 ${fmtNum(o.refundedOrders)} 笔${warn}`
})

const { loading, failed, load } = useLoad(async () => {
  const [data, ov] = await Promise.all([
    getMemberStats({ start: props.range[0], end: props.range[1] }),
    getOverview()
  ])
  raw.value = data
  totalUsers.value = Number(ov.totalUsers) || 0
})

watch(() => props.range, load, { deep: true })
onMounted(load)
defineExpose({ load })
</script>

<style scoped>
.order-insight {
  font-size: 13px;
  color: var(--text-2);
  margin: 0 0 12px;
}
.order-insight--warning {
  color: #e6a23c;
}
.sec-note {
  font-size: 12px;
  color: var(--text-3);
  margin-top: 12px;
}
</style>
