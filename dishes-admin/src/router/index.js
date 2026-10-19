import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  { path: '/', name: 'dashboard', component: () => import('@/views/Dashboard.vue'), meta: { title: '运营数据看板' } }
]

export default createRouter({
  history: createWebHashHistory(),
  routes
})
