<template>
  <div class="range-bar">
    <el-date-picker
      v-model="inner"
      type="daterange"
      range-separator="至"
      start-placeholder="开始日期"
      end-placeholder="结束日期"
      value-format="YYYY-MM-DD"
      :clearable="false"
      unlink-panels
      @change="onPick"
    />
    <el-button-group class="presets">
      <el-button size="small" @click="applyToday">今日</el-button>
      <el-button size="small" @click="applyYesterday">昨日</el-button>
      <el-button v-for="p in presets" :key="p.days" size="small" @click="applyPreset(p.days)">
        近{{ p.days }}日
      </el-button>
    </el-button-group>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  // [start, end]，均为 YYYY-MM-DD
  modelValue: { type: Array, default: () => [] }
})
const emit = defineEmits(['update:modelValue', 'change'])

const inner = ref(props.modelValue)
const presets = [{ days: 7 }, { days: 30 }, { days: 90 }]

const fmt = (d) => {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const emitRange = (range) => {
  inner.value = range
  emit('update:modelValue', range)
  emit('change', range)
}

const applyPreset = (days) => {
  const end = new Date()
  const start = new Date()
  start.setDate(end.getDate() - (days - 1))
  emitRange([fmt(start), fmt(end)])
}

const applyToday = () => {
  const d = new Date()
  emitRange([fmt(d), fmt(d)])
}

const applyYesterday = () => {
  const d = new Date()
  d.setDate(d.getDate() - 1)
  emitRange([fmt(d), fmt(d)])
}

const onPick = (val) => {
  if (val) emitRange(val)
}

watch(
  () => props.modelValue,
  (val) => {
    if (val && val.join() !== (inner.value || []).join()) inner.value = val
  }
)
</script>

<style scoped>
.range-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
</style>
