import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// 后端 dishes-bk：dev 端口 8810、上下文 /dishes（见 application-dev.yml）
const BACKEND = 'http://localhost:8810'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5200,
    proxy: {
      // 前端统一以 /dishes 前缀请求，dev 代理到后端，规避跨域
      '/dishes': {
        target: BACKEND,
        changeOrigin: true
      }
    }
  }
})
