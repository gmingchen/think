import { FILE_TYPE_LIST } from '../common/enums/index'
/**
 * 根据url获取文件拓展名
 * @param {*} url 
 */
export const getFileSuffix = (url) => {
  const index = url.lastIndexOf('.')
  return index === -1 ? '' : url.slice(index + 1).toLowerCase()
}
/**
 * 根据url获取文件类型
 * @param {*} url 
 */
export const getFileType = (url) => {
  let result = ''
  const suffix = getFileSuffix(url)
  for (let i = 0; i < FILE_TYPE_LIST.length; i++) {
    const { label, value } = FILE_TYPE_LIST[i];
    if (value.includes(suffix)) {
      result = label
      break
    }
  }
  return result
}

/**
 * 标准化文件数据
 * @param {*} value 
 */
export const normalizeFile = (value) => {
  let result = []
  if (value) {
    if (value instanceof Array) {
      value.forEach(item => {
        result.push(...normalizeFile(item))
      })
    } else if (value instanceof Object && value.url) {
      result = [{ ...value, type: value.type || getFileType(value.url) }]
    } else {
      result = [{ url: value, type: getFileType(value) }]
    }
  }
  return result
}

