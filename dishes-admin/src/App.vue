<template>
  <div class="app-root">
    <router-view />
  </div>
</template>

<script setup></script>

<style scoped>
/* 不在内部建滚动容器（overflow-y: auto）：页面滚动统一交给 window，
否则 sticky 工具栏与锚点滚动的坐标计算都会错位 */
.app-root {
  min-height: 100%;
}
</style>
