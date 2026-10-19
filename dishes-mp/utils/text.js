/**
 * 步骤单条最大字数（与录入框 maxlength 保持一致）
 */
export const STEP_MAX_LENGTH = 200

// 步骤序号前缀：1. / 1、 / 1） / （1） / 第一步 / 第一步：
// 序号后跟数字时用守卫跳过，避开小数（如 1.5 斤）被误判为序号
const ORDER_MARK = '(?:\\d{1,3}\\s*[.、)）．][\\s　]*(?![\\d一二三四五六七八九十])|[（(]\\d{1,3}[)）][\\s　]*|第\\s*[一二三四五六七八九十百\\d]{1,4}\\s*步[\\s　]*[：:.、-]?[\\s　]*)'
const ORDER_PREFIX = new RegExp('^[\\s　]*' + ORDER_MARK)
// 步骤序号切点：用于无换行的整段文本
const ORDER_SPLITTER = new RegExp('(?=' + ORDER_MARK + ')')
// 拆分后遗留的连接符号（如“第一步…，第二步…”句尾的逗号）
const TRAILING_JOINER = /[，,、；;：:\s]+$/

const SENTENCE_ENDS = '。！？；;'

/**
 * 按句末标点拆句（保留标点）
 */
const splitSentences = (text) => {
  const sentences = []
  let start = 0
  for (let i = 0; i < text.length; i++) {
    if (SENTENCE_ENDS.indexOf(text[i]) > -1) {
      sentences.push(text.slice(start, i + 1))
      start = i + 1
    }
  }
  if (start < text.length) sentences.push(text.slice(start))
  return sentences
}

/**
 * 超长文本二次拆分：按句合并到上限，单句仍超长则硬切
 */
const splitLongText = (text) => {
  if (text.length <= STEP_MAX_LENGTH) return [text]
  const result = []
  let chunk = ''
  splitSentences(text).forEach(sentence => {
    let rest = sentence
    while (rest.length > STEP_MAX_LENGTH) {
      if (chunk) { result.push(chunk); chunk = '' }
      result.push(rest.slice(0, STEP_MAX_LENGTH))
      rest = rest.slice(STEP_MAX_LENGTH)
    }
    if (chunk.length + rest.length > STEP_MAX_LENGTH) {
      if (chunk) result.push(chunk)
      chunk = rest
    } else {
      chunk += rest
    }
  })
  if (chunk) result.push(chunk)
  return result.filter(item => !!item.trim())
}

/**
 * 将整段做法文本拆分为步骤数组
 * 规则：优先按换行拆；若只有一段则按序号标记拆；
 * 每段去掉序号前缀，超过 STEP_MAX_LENGTH 再按句末标点二次拆分
 * @param {*} text 整段做法文本
 */
export const splitSteps = (text) => {
  if (!text || !text.trim()) return []
  let lines = text.split(/\r?\n/)
  // 无换行可用时，退化为按序号标记切分
  if (lines.filter(line => !!line.trim()).length <= 1) {
    lines = text.split(ORDER_SPLITTER)
  }
  const steps = []
  lines
    .map(line => line.replace(ORDER_PREFIX, '').replace(TRAILING_JOINER, '').trim())
    .filter(line => !!line)
    .forEach(line => splitLongText(line).forEach(step => steps.push(step)))
  return steps
}
