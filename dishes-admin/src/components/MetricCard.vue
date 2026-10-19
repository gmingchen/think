<template>
  <el-card shadow="never" class="metric-card">
    <div class="metric-card__label">
      {{ label }}
      <span v-if="scope" class="metric-card__scope" :class="`metric-card__scope--${scope}`">
        {{ scope === 'all' ? '全量' : '区间' }}
      </span>
    </div>
    <div class="metric-card__value" :style="{ color: color }">
      {{ value }}<span v-if="suffix" class="metric-card__suffix">{{ suffix }}</span>
    </div>
    <div v-if="sub" class="metric-card__sub">{{ sub }}</div>
  </el-card>
</template>

<script setup>
defineProps({
  label: String,
  value: [Number, String],
  suffix: String,
  // 副行：口径说明或原始值，如「累计 1,234」「= 区间活跃 ÷ 累计用户」
  sub: String,
  color: { type: String, default: 'var(--text-main)' },
  // 口径徽标：all = 全量快照，range = 随日期区间联动
  scope: { type: String, default: '' }
})
</script>

<style scoped>
.metric-card {
  border-radius: 10px;
}
.metric-card__label {
  font-size: 13px;
  color: var(--text-2);
  margin-bottom: 8px;
}
.metric-card__scope {
  font-size: 11px;
  padding: 0 5px;
  margin-left: 4px;
  border-radius: 4px;
  vertical-align: 1px;
}
.metric-card__scope--all {
  color: var(--text-3);
  border: 1px solid var(--panel-border);
}
.metric-card__scope--range {
  color: #409eff;
  border: 1px solid rgba(64, 158, 255, 0.4);
}
.metric-card__value {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.2;
}
.metric-card__suffix {
  font-size: 13px;
  font-weight: 400;
  margin-left: 2px;
  color: var(--text-2);
}
.metric-card__sub {
  font-size: 12px;
  color: var(--text-3);
  margin-top: 6px;
}
</style>
