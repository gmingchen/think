/**
 * 查找枚举
 * @param {*} value 
 * @param {*} list 
 */
export const findEnum = (value, list) => {
  return list.find(item => item.value === value)
}
/**
 * 查找枚举label
 * @param {*} value 
 * @param {*} list 
 */
export const findEnumLabel = (value, list) => {
  const item = findEnum(value, list)
  return item ? item.label : ''
}
