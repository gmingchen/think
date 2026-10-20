import { service } from '../service'

// AI 荐菜：一次调用产出两组推荐（现有菜排序 + 新菜谱），LLM 耗时较长，超时放宽到 60s
export const aiRecommendApi = params => {
  return service({
    url: '/ai/dish/recommend',
    method: 'POST',
    params,
    timeout: 60000,
  })
}

// 采纳新菜谱：从 Redis 暂存取 fresh[index] 转菜谱落库，只传凭证不传内容
export const aiAdoptApi = params => {
  return service({
    url: '/ai/dish/adopt',
    method: 'POST',
    params,
  })
}

// 今日剩余荐菜次数：只读回显，进页面从后端拉真实值（后端为准）
export const aiQuotaApi = () => {
  return service({
    url: '/ai/dish/quota',
    method: 'GET',
  })
}

// 今日剩余文案识别次数：同荐菜额度回显，后端为准
export const aiParseQuotaApi = () => {
  return service({
    url: '/ai/dish/parse/quota',
    method: 'GET',
  })
}

// AI 文案识别（预览）：LLM 解析耗时，超时对齐荐菜 60s；只回名字/分类提示，不建库不落库
export const aiParseApi = params => {
  return service({
    url: '/ai/dish/parse',
    method: 'POST',
    params,
    timeout: 60000,
  })
}

// AI 文案识别确认：用户确认后才把名字建入食材/调料库拿 id，返回带 id 回填结构（纯 DB，普通超时）
export const aiParseCommitApi = params => {
  return service({
    url: '/ai/dish/commit',
    method: 'POST',
    params,
  })
}

// 今日剩余 AI 生图次数：同荐菜/识别额度回显，后端为准
export const aiImageQuotaApi = () => {
  return service({
    url: '/ai/dish/image/quota',
    method: 'GET',
  })
}

// AI 封面生图（预览）：经云函数 image-proxy 中转混元生图，耗时 15–60s，超时对齐后端 130s；只返 base64，不上传存储不写库
export const aiImageGenApi = params => {
  return service({
    url: '/ai/dish/image',
    method: 'POST',
    params,
    timeout: 130000,
  })
}
