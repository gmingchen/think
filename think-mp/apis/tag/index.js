import { service } from '../service'

// 启用中的标签列表（公开广场筛选）
export const tagListApi = () => {
  return service({ url: '/tag/list', method: 'GET' })
}
