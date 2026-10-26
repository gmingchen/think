/**
 * 通过路径获取值
 * @param {*} path 路径
 * @param {*} json json对象
 */
export const getValueByPath = (path, json) => {
  const parts = path.split('.')
  let current = json
  for (let i = 0; i < parts.length; i++) {
    const part = parts[i]
    if (part.includes('[')) {
      const arrayParts = part.split('[')
      for (let j = 0; j < arrayParts.length; j++) {
        let indexOrKey = arrayParts[j].replace(']', '')
        if (!isNaN(indexOrKey)) {
          indexOrKey = parseInt(indexOrKey, 10)
        }
        current = current[indexOrKey]
        if (current === undefined) {
          return undefined
        }
      }
    } else {
      current = current[part]
      if (current === undefined) {
        return undefined
      }
    }
  }
  return current
}
