import { HOST_ENUM, CONTENT_TYPE_ENUM } from '../common/enums/index'
import { AUTH_KEY, APP_KEY, API_PREFIX, LIMIT_REACHED_CODE, SESSION_KEY_EXPIRED_CODE } from '../common/constants/index'
import { TIMEOUT } from './config'
import { store } from '../store/index'
import { print, toast } from '../utils/index'

export const service = (options) => {
  const { url, method, params, headers, timeout } = options
  const host = HOST_ENUM[store.system.envVersion]
  const data = { ...params }
  return new Promise((resolve, reject) => {
    const title = `API: ${ url }`
    print(title, data)
    wx.request({
      url: `${ host }${ API_PREFIX }${ url }`,
      method,
      data,
      header: {
        'Content-Type': CONTENT_TYPE_ENUM.JSON,
        [AUTH_KEY]: store.auth.token || '',
        [APP_KEY]: store.system.appId || '',
        ...headers
      },
      timeout: timeout || TIMEOUT,
      success: response => {
        const { statusCode, data } = response

        if (statusCode !== 200) {
          const message = `服务器异常：${ statusCode }`
          toast(message)
          reject({ message })
          return
        }

        // 后端统一返回格式：{ code: 0 表示成功, 非0 表示业务异常 }
        if (data.code !== 0) {
          const message = data.message || '服务器异常'
          // 4005 已达上限：交由页面层弹「看视频扩容」引导，此处不自动 toast，避免「toast + 弹窗」双反馈
          // 4006 sessionKey 过期：交由 utils/vipPay.js 静默重登后**重签同一单号**，此处弹一句只会
          //   让人以为出了大事，而下一步重试通常就成功了（§12.1 的 -15007）
          const silent = data.code === LIMIT_REACHED_CODE || data.code === SESSION_KEY_EXPIRED_CODE
          if (!silent) toast(message)
          reject({ message, code: data.code })
          return
        }
        
        resolve(data)
      },
      fail: error => {
        let { errno, errMsg } = error
        const message = `网络异常：${ errno || '' } ${ errMsg }`
        toast(message)
        reject({ message })
      },
      complete: response => print(title, response)
    })
  })
}