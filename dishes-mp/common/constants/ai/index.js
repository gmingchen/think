// AI 每日次数上限的三个常量只作「额度接口回显前的初始占位」（页面 data 初值），**真正判定与回显都在后端**。
// 后端未就绪或接口失败时，弹窗副标题会拿这个数亮一下（test 环境后端是 999，更会对不上），
// 因此它们只要求「与 base 同量级、不致于亮出一个比真实额度高的数」，不要求逐字准确。
// 改后端 application.yml 的 ai.llm.daily-limit / ai.llm.parse-daily-limit / ai.image.daily-limit 时同步这里。

// AI 荐菜每日次数上限（后端 base daily-limit = 3）
export const AI_DAILY_LIMIT = 3

// AI 文案识别每日次数上限（后端 base parse-daily-limit = 5）
export const AI_PARSE_DAILY_LIMIT = 5

// AI 封面生图每日次数上限（后端 base image.daily-limit = 2）
export const AI_IMAGE_DAILY_LIMIT = 2

// AI 封面上传前的压缩宽度（混元原图固定 1024x1024，而封面实际展示最宽约 750px）
export const AI_COVER_COMPRESS_WIDTH = 750

// AI 封面上传前的压缩质量（仅对 jpg 有效，实测混元返图为 jpeg；80 在观感与体积间取衷）
export const AI_COVER_COMPRESS_QUALITY = 80

// 结果页底部 AIGC 声明（不可关闭）
export const AI_DISCLAIMER_TEXT = '推荐与灵感菜谱由 AI 生成，仅供参考'
