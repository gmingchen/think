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

/**
 * 经纬度计算距离
 * @param {*} longitude 
 * @param {*} latitude 
 * @param {*} targetLongitude 
 * @param {*} targetLatitude 
 */
export function calcDistance(longitude = 0, latitude = 0, targetLongitude = 0, targetLatitude = 0) {
  var radiu = (latitude * Math.PI) / 180.0
  var targetRadiu = (targetLatitude * Math.PI) / 180.0
  var a = radiu - targetRadiu
  var b = (longitude * Math.PI) / 180.0 - (targetLongitude * Math.PI) / 180.0
  var r = 6378138
  var distance =
    r *
    2 *
    Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) + Math.cos(radiu) * Math.cos(targetRadiu) * Math.pow(Math.sin(b / 2), 2)))
  return distance
}

/**
 * 手机号脱敏
 * @param {*} mobile 
 */
export function encryptMobile(mobile) {
  return mobile.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
}
