<template>
  <div ref="el" :style="{ width: '100%', height: height }"></div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import { isDark } from '@/utils/theme'

const props = defineProps({
  option: { type: Object, required: true },
  height: { type: String, default: '320px' }
})

const el = ref(null)
let chart = null

// 背景统一透明跟随卡片，主题色交给 echarts 内置 dark 主题处理
const render = () => {
  if (!chart) return
  chart.setOption({ backgroundColor: 'transparent', ...props.option }, true)
}

const resize = () => chart && chart.resize()

const init = () => {
  chart = echarts.init(el.value, isDark.value ? 'dark' : undefined)
  render()
}

onMounted(async () => {
  await nextTick()
  init()
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  chart && chart.dispose()
  chart = null
})

watch(isDark, () => {
  if (!el.value) return
  chart && chart.dispose()
  init()
})

watch(() => props.option, render, { deep: true })
</script>
