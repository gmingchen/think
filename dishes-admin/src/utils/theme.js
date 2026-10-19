// 深色模式：单例状态 + localStorage 持久化 + html.dark 切换
import { ref, watch } from 'vue'

export const isDark = ref(localStorage.getItem('admin-theme') === 'dark')

const apply = () => document.documentElement.classList.toggle('dark', isDark.value)

export const toggleDark = () => {
  isDark.value = !isDark.value
  localStorage.setItem('admin-theme', isDark.value ? 'dark' : 'light')
}

watch(isDark, apply)
apply()
