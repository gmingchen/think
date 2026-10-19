import { action } from 'mobx-miniprogram'
import { login } from '../../../utils/index'
import { loginApi } from '../../../apis/index'

export default {
  auth: {
    token: '',
  },
  login: action(async function(inviter) {
    try {
      const { code } = await login()
      if (!code) {
        throw new Error('微信登录凭证获取失败')
      }
      const params = { code, inviter }
      const r = await loginApi(params)
      if (r?.data?.token) {
        this.auth.token = r.data.token
        wx.nextTick(() => wx.$logined())
      } else {
        throw new Error('Token获取异常')
      }
    } catch ({ errMsg, errno, message }) {
      message = `登录失败：${ errno || '' } ${ errMsg || message || '' }`
      wx.$toast(message)
      // 循环重试登录
      setTimeout(() => this.login(), 5000)
    }
  })
}
