import axios from 'axios'
import { ElMessage } from 'element-plus'

// dev 走 vite 代理，生产由部署侧反代同域；统一以 /dishes 为前缀（后端 context-path）
const request = axios.create({
  baseURL: '/dishes',
  timeout: 20000
})

// 后端统一返回体 Result：{ code, message, data }，成功 code = 0
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res && typeof res === 'object' && 'code' in res) {
      if (res.code === 0) {
        return res.data
      }
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
    ElMessage.error(error.message || '网络异常')
    return Promise.reject(error)
  }
)

export default request
