<template>
  <el-card id="sec-hero" shadow="never" class="hero">
    <div v-loading="loading">
      <el-alert
        v-if="failed"
        type="error"
        :closable="false"
        title="数据加载失败，请确认后端服务（8810）是否正常"
      />
      <div v-else class="hero__grid">
        <div v-for="m in metrics" :key="m.label" class="hero__item">
          <div class="hero__value" :style="{ color: m.color }">
            {{ m.display }}<span v-if="m.suffix" class="hero__suffix">{{ m.suffix }}</span>
          </div>
          <div class="hero__label">{{ m.label }}</div>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getOverview } from '@/api/stats'
import { fmtNum } from '@/utils/format'
import { useLoad } from '@/utils/load'

// 块级自治：自请求 overview 快照，loading / 失败态不连带其他块
const raw = ref({})
const { loading, failed, load } = useLoad(async () => {
  raw.value = await getOverview()
})
onMounted(load)
defineExpose({ load })

const metrics = computed(() => {
  const d = raw.value
  return [
    { label: '累计用户', display: fmtNum(d.totalUsers), color: '#409EFF' },
    { label: '累计厨房', display: fmtNum(d.totalKitchens), color: '#67C23A' },
    { label: '累计菜谱', display: fmtNum(d.totalDishes), color: '#E6A23C' },
    { label: '累计订单', display: fmtNum(d.totalOrders), color: '#409EFF' },
    { label: '累计评价', display: fmtNum(d.totalReviews), color: '#67C23A' },
    { label: '当前有效会员', display: fmtNum(d.validMembers), color: '#E6A23C' },
    { label: '会员总收入', display: fmtNum(d.memberRevenue, 2), suffix: ' 元', color: '#F56C6C' }
  ]
})
</script>

<style scoped>
.hero {
  border-radius: 10px;
  background: linear-gradient(180deg, var(--hero-bg-1), var(--hero-bg-2));
}
.hero__grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
}
.hero__item {
  text-align: center;
  padding: 8px 4px;
}
.hero__value {
  font-size: 34px;
  font-weight: 800;
  line-height: 1.15;
}
.hero__suffix {
  font-size: 14px;
  font-weight: 400;
  margin-left: 2px;
  color: var(--text-2);
}
.hero__label {
  font-size: 13px;
  color: var(--text-2);
  margin-top: 6px;
}
</style>
