import { print } from './print'

/**
 * 统一微信 API Promise 化回调处理器
 * @param {Function} resolve Promise 的 resolve 函数
 * @param {Function} reject Promise 的 reject 函数
 * @param {string} title 日志标题，用于打印
 * @returns {Object} 包含 success, fail, complete 方法的回调对象
 */
const normalize = (resolve, reject, title) => {
  return {
    success: resolve,
    fail: reject,
    complete: r => {
      print(title, r)
    },
  }
}

/**
 * 创建安全的 normalize，防止 options 覆盖回调
 * @param {Function} resolve Promise resolve
 * @param {Function} reject Promise reject
 * @param {string} title 日志标题
 * @param {Object} options 微信 API 选项
 */
const safeNormalize = (resolve, reject, title, options = {}) => {
  const { success, fail, complete, ...restOptions } = options
  return {
    ...restOptions,
    ...normalize(resolve, reject, title),
  }
}

/**
 * 确认弹窗
 * @param {string} content 提示内容
 * @param {string} [title='提示'] 标题
 * @returns {Promise<boolean>} 用户是否点击确认
 */
export const confirm = (content, title = '提示') => {
  return new Promise((resolve) => {
    wx.showModal({
      title,
      content,
      success: (res) => {
        resolve(res.confirm)
      },
    })
  })
}

/**
 * 登录
 */
export const login = () => {
  return new Promise((resolve, reject) => {
    print('login')
    wx.login({
      ...normalize(resolve, reject, 'login'),
    })
  })
}

/**
 * 用户授权设置信息 key 列表
 * 这些 key 会被转换为 scope.xxx 格式查询 authSetting
 */
const authSettingKeys = [
  'userInfo', 'address',
  'invoiceTitle', 'invoice', 'werun', 'record', 'writePhotosAlbum',
  'camera', 'bluetooth', 'addPhoneContact', 'addPhoneCalendar',
]
/**
 * 获取设置
 * @param {string|undefined} key 某一个设置项，若没有该字段直接返回完整结果
 * @returns {Promise<*>} 返回设置结果，如果传入 key 则返回对应的设置值
 */
export const getSetting = (key) => {
  return new Promise((resolve, reject) => {
    print('getSetting')
    wx.getSetting({
      withSubscriptions: true,
      fail: reject,
      complete: r => {
        print('getSetting', r)
      },
      success: r => {
        if (!key) {
          resolve(r)
          return
        }
        const { authSetting = {}, subscriptionsSetting = {} } = r
        if (authSettingKeys.includes(key)) {
          const scopeKey = `scope.${ key }`
          resolve(authSetting[scopeKey])
        } else {
          const { mainSwitch, itemSettings = {} } = subscriptionsSetting
          let result = subscriptionsSetting[key]
          if (!result && itemSettings) {
            result = { mainSwitch, [key]: itemSettings[key] }
          }
          resolve(result)
        }
      }
    })
  })
}

/**
 * 获取网络类型
 * @returns {Promise<{networkType: string}>} 返回网络类型信息
 */
export const getNetwork = () => {
  return new Promise((resolve, reject) => {
    print('getNetwork')
    wx.getNetworkType({
      ...normalize(resolve, reject, 'getNetwork'),
    })
  })
}

/**
 * 拉起微信授权弹窗
 * @param {Object} options 授权选项（scope 必填，形如 scope.writePhotosAlbum）
 */
export const authorize = (options = {}) => {
  return new Promise((resolve, reject) => {
    print('authorize', options)
    wx.authorize(
      safeNormalize(resolve, reject, 'authorize', options)
    )
  })
}

/**
 * 微信支付
 * @param {Object} options 支付选项
 */
export const requestPayment = (options = {}) => {
  return new Promise((resolve, reject) => {
    print('requestPayment')
    wx.requestPayment(
      safeNormalize(resolve, reject, 'requestPayment', options)
    )
  })
}

/**
 * 微信虚拟支付（道具直购，v12 §2.1）
 * @param {Object} options { signData, paySig, signature, mode }
 *
 * ⚠️ 四个字段一律**原样透传**，前端不许拼、不许改、不许重新序列化：
 *    paySig 签的是 `requestVirtualPayment&{signData}` 这一个串，改一个字符就报 -15006，
 *    而报回来的只有码没有「哪里变了」，排查会先怀疑签名算法（§2.3 那条铁律在前端的落点）。
 */
export const requestVirtualPayment = (options = {}) => {
  return new Promise((resolve, reject) => {
    print('requestVirtualPayment')
    wx.requestVirtualPayment(
      safeNormalize(resolve, reject, 'requestVirtualPayment', options)
    )
  })
}

/**
 * 扫码
 * @param {Object} options 扫码选项
 */
export const scanCode = (options = {}) => {
  return new Promise((resolve, reject) => {
    print('scanCode')
    wx.scanCode(
      safeNormalize(resolve, reject, 'scanCode', options)
    )
  })
}

/**
 * 复制内容到剪贴板（微信自带「内容已复制」提示，调用方不必再叠 toast）
 * @param {Object} options { data } 要复制的文本
 */
export const setClipboardData = (options = {}) => {
  return new Promise((resolve, reject) => {
    print('setClipboardData', options)
    wx.setClipboardData(
      safeNormalize(resolve, reject, 'setClipboardData', options)
    )
  })
}

/**
 * 消息订阅
 * @param {Object} options 订阅选项
 */
export const requestSubscribeMessage = (options = {}) => {
  return new Promise((resolve, reject) => {
    print('requestSubscribeMessage')
    wx.requestSubscribeMessage(
      safeNormalize(resolve, reject, 'requestSubscribeMessage', options)
    )
  })
}

/**
 * 保存图片到系统相册
 * @param {Object} options 保存选项（filePath 等）
 */
export const saveImageToPhotosAlbum = (options = {}) => {
  return new Promise((resolve, reject) => {
    print('saveImageToPhotosAlbum', options)
    wx.saveImageToPhotosAlbum(
      safeNormalize(resolve, reject, 'saveImageToPhotosAlbum', options)
    )
  })
}

/**
 * 拉起微信图片分享面板（发送给朋友 / 朋友圈 / 存相册）
 * @param {Object} options 分享选项（path 必填）
 */
export const showShareImageMenu = (options = {}) => {
  return new Promise((resolve, reject) => {
    print('showShareImageMenu', options)
    wx.showShareImageMenu(
      safeNormalize(resolve, reject, 'showShareImageMenu', options)
    )
  })
}
