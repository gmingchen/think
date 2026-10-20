import { TIMEOUT } from '../config'
import { service } from '../service'
import { HOST_ENUM } from '../../common/enums/index'
import { store } from '../../store/index'
import { getFileType, toast } from '../../utils/index'

// ── 七牛上传凭证（带缓存） ──

let _tokenCache = null

/**
 * 获取七牛上传凭证（缓存有效期内自动复用）
 */
export const getUploadToken = async () => {
  if (_tokenCache && Date.now() < _tokenCache.expireAt) {
    return _tokenCache.data
  }
  const res = await service({ url: '/os/uploadToken', method: 'GET' })
  wx.$print('uploadToken', res.data)
  _tokenCache = {
    data: res.data,
    // 提前 5 分钟过期，避免边界问题
    expireAt: Date.now() + (2 * 60 * 60 - 300) * 1000,
  }
  return res.data
}

// ── 七牛直传 ──

/**
 * 生成文件 key
 * @param {String} path 本地文件路径（用于提取后缀名）
 */
const generateKey = (path, prefix) => {
  const now = Date.now()
  const rand = Math.random().toString(36).substring(2, 8)
  const suffix = path.lastIndexOf('.') !== -1 ? path.slice(path.lastIndexOf('.')) : ''
  const name = `${ now }_${ rand }${ suffix }`
  return prefix ? `${ prefix }/${ name }` : name
}

/**
 * 上传单个文件到七牛
 * @param {String} filePath 本地临时文件路径
 * @param {String} token 上传凭证
 * @param {String} domain 外链域名
 * @returns {String} 文件访问 URL
 */
export const uploadToQiniu = (filePath, token, domain, uploadUrl, prefix) => {
  const key = generateKey(filePath, prefix)
  return new Promise((resolve, reject) => {
    const attempt = (retriesLeft) => {
      wx.uploadFile({
        url: uploadUrl,
        filePath,
        name: 'file',
        formData: { token, key },
        timeout: TIMEOUT,
        success: (response) => {
          if (response.statusCode === 200) {
            const url = `${ domain }/${ key }`
            wx.$print('七牛', url)
            resolve(url)
          } else {
            wx.$print('七牛', { statusCode: response.statusCode, data: response.data })
            reject(response)
          }
        },
        fail: (err) => {
          if (retriesLeft > 0) {
            attempt(retriesLeft - 1)
          } else {
            reject(err)
          }
        },
      })
    }
    attempt(2)
  })
}

/**
 * 批量上传
 * @param {Array} files { path, name, size }
 * @param {Object} tokenData { token, extra: { domain } }
 */
const uploadMultiple = async (files, tokenData, prefix) => {
  const { token, extra } = tokenData
  const domain = extra.domain
  const uploadUrl = extra.uploadUrl
  return Promise.all(files.map(file => uploadToQiniu(file.path, token, domain, uploadUrl, prefix)))
}

/**
 * 上传标准化处理
 * @param {Array} files { path, name, size }
 * @returns {Array} { url, name, path, size, type }
 */
const uploadNormalize = async (files, prefix) => {
  const tokenData = await getUploadToken()
  const urls = await uploadMultiple(files, tokenData, prefix)
  return urls.map((url, index) => {
    const { name, path, size } = files[index]
    return { url, name, path, size, type: getFileType(path) }
  })
}

// ── 文件选择 & 校验 ──

/**
 * 验证文件大小
 * @param {Number} size 单位 B
 * @param {Number} limit 单位 M
 */
const validateSize = (size, limit) => {
  if (!limit || size <= limit * 1024 * 1024) {
    return true
  }
  toast(`大小不能超过${ limit }M`)
  return false
}

const validateSizes = (sizes, limit) => {
  return sizes.every(size => validateSize(size, limit))
}

/**
 * 微信临时文件标准化
 */
const filesNormalize = (tempFiles) => {
  const results = { files: [], sizes: [] }
  tempFiles.forEach(tempFile => {
    const { size, tempFilePath } = tempFile
    results.files.push({ path: tempFilePath, name: '', size })
    results.sizes.push(size)
  })
  return results
}

/**
 * 选择文件成功处理器
 */
const chooseSuccessHandler = async (tempFiles, limit, resolve, reject, prefix) => {
  const { sizes, files } = filesNormalize(tempFiles)
  if (!limit || validateSizes(sizes, limit)) {
    wx.showLoading({ title: '上传中', mask: true })
    try {
      resolve(await uploadNormalize(files, prefix))
    } catch (err) {
      reject(err)
    } finally {
      wx.hideLoading()
    }
  }
}

// ── 对外导出 ──

/**
 * 拍摄或从手机相册中选择图片或视频
 */
export const uploadMedia = (count = 1, limit, options = {}) => {
  const { prefix, ...rest } = options
  return new Promise((resolve, reject) => {
    wx.chooseMedia({
      sizeType: ['compressed'],
      ...rest,
      count,
      fail: reject,
      success: async ({ tempFiles }) => chooseSuccessHandler(tempFiles, limit, resolve, reject, prefix),
    })
  })
}

/**
 * 上传微信头像（微信返回的临时路径）
 * @param {String} avatarPath 微信头像临时路径
 * @returns {String} 上传后的 URL
 */
export const uploadAvatar = async (avatarPath, prefix) => {
  const result = await uploadNormalize([{ path: avatarPath, name: '', size: 0 }], prefix)
  return result[0].url
}

/**
 * 下载文件
 */
export const downloadFile = (url) => {
  return new Promise((resolve, reject) => {
    wx.downloadFile({
      url,
      fail: reject,
      success: ({ tempFilePath }) => {
        resolve(tempFilePath)
      },
    })
  })
}

/**
 * 打开文档
 */
export const openDocument = (path, type) => {
  return new Promise((resolve, reject) => {
    wx.openDocument({
      filePath: path,
      fileType: type,
      success: () => {
        resolve()
      },
      fail: reject,
    })
  })
}

/**
 * 预览文档
 */
export const previewDocument = async (url, type) => {
  const path = await downloadFile(url)
  await openDocument(path, type)
  return { url, type, path }
}
