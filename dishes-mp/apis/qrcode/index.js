import { service } from '../service'

// 生成带当前用户 inviterId 参数的小程序码，返回图片 base64 dataURL
export const generateQrcodeApi = params => {
  return service({
    url: '/user/generateQrcode',
    method: 'POST',
    params
  })
}
