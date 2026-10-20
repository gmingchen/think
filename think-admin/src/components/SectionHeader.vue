<template>
  <div class="sec-header">
    <span class="sec-header__title">{{ title }}</span>
    <span
      v-if="insight"
      class="sec-header__insight"
      :class="`sec-header__insight--${level}`"
    >{{ insight }}</span>
  </div>
</template>

<script setup>
defineProps({
  title: String,
  // 数据实时算出的结论文案
  insight: String,
  // info 常态 / warning 需留意 / danger 需关注，由各分区按阈值判定
  level: { type: String, default: 'info' }
})
</script>

<style scoped>
.sec-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}
.sec-header__title {
  font-size: 15px;
  font-weight: 600;
}
.sec-header__insight {
  font-size: 13px;
  border-radius: 6px;
  padding: 2px 10px;
}
.sec-header__insight--info {
  color: #409eff;
  background: var(--insight-bg);
}
.sec-header__insight--warning {
  color: #e6a23c;
  background: rgba(230, 162, 60, 0.12);
}
.sec-header__insight--danger {
  color: #f56c6c;
  background: rgba(245, 108, 108, 0.12);
}
</style>
