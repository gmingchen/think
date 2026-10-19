import { QUERY_KEY_ENUM } from '../common/enums/index'

/**
 * 解析 Query 参数
 * @param {Object} query 页面 Query
 */
export const parseQuery = (query = {}) => {
  const result = {}
  for(const key in query) {
    if (key === 'scene' && query.scene) {
      const scene = decodeURIComponent(query.scene)
      const scenes = scene.split('&')
      scenes.forEach(item => {
        const arr = item.split('=')
        if (arr.length === 2) {
          result[arr[0]] = arr[1]
        }
      });
    } else {
      result[key] = query[key]
    }
  }
  console.log('~~🚀：参数解析结果=>', result)
  return result;
}

/**
 * 解析 url 参数
 * @param {Object} url URL
 */
export const parseUrl = url =>{
  var result = {};
  var arr = url.substr(url.indexOf('?') + 1).split('&');
  arr.forEach(item=>{
    var temp = item.split('=');
    result[temp[0]] = temp[1];
  });
 return result;
}

/**
 * 解析 url 参数
 * @param {Object} url URL
 */
export const parsePathScene = path =>{
  const scene = parseUrl(path)
  return parseQuery(scene);
}

/**
 * 转 json 对象 为 URL 参数
 * @param {*} json Json 参数
 */
export const parseJsonToUrlParam = (json) => {
  return Object.keys(json).map(key => key + '=' + json[key]).join('&');
}
