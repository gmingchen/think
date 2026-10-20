// 分区加载：统一 loading + 失败态（请求层已 toast，这里只负责展示失败占位）
import { ref } from 'vue'

export function useLoad(fetcher) {
  const loading = ref(false)
  const failed = ref(false)

  const load = async () => {
    loading.value = true
    failed.value = false
    try {
      await fetcher()
    } catch (e) {
      failed.value = true
    } finally {
      loading.value = false
    }
  }

  return { loading, failed, load }
}
