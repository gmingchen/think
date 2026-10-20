import { aiImageGenApi, uploadAvatar } from '../../../../../../apis/index'
import { AI_DISCLAIMER_TEXT, AI_COVER_COMPRESS_WIDTH, AI_COVER_COMPRESS_QUALITY, AD_UNIT_AI_IMAGE_INTERSTITIAL } from '../../../../../../common/constants/index'
import { adServiceEnabled } from '../../../../../../utils/index'

// 插屏广告单例（createInterstitialAd 同一 adUnitId 返回单例，模块级缓存避免重复创建）
let interstitialAd = null
const getInterstitialAd = () => {
  if (!interstitialAd && wx.createInterstitialAd) {
    interstitialAd = wx.createInterstitialAd({ adUnitId: AD_UNIT_AI_IMAGE_INTERSTITIAL })
  }
  return interstitialAd
}

Component({
  properties: {
    // 厨房ID：由页面层传入，组件保持纯 UI、不自行摸 store
    kitchenId: {
      type: null,
      value: '',
    },
    // 今日剩余生图次数：页面层拉后端后以属性传入，副标题展示与前置守卫使用
    remainCount: {
      type: Number,
      value: 0,
    },
  },

  data: {
    AI_DISCLAIMER_TEXT,
    visible: false,
    // 三态：loading（生图中）→ ready（预览可确认）/ error（失败可重试，额度已后端返还）
    status: 'loading',
    // 生成图写入用户目录后的本地临时路径：<image> 展示与上传均用它（规避 setData 1MB 上限）
    tempPath: '',
    // 生图请求进行中标记：禁止关闭/重复点击，避免悬空请求
    busy: false,
    // 上传进行中标记：「使用这张」防重复点击
    uploading: false,
    // 上次生图入参（name/categoryHint/foods/seasonings），供「重生成」原样复用
    params: null,
  },

  methods: {
    // 页面层收集当前表单数据后调用：open({ name, categoryHint, foods, seasonings })
    open(params) {
      this.setData({ visible: true, params: params || null, tempPath: '', uploading: false })
      this._generate()
    },

    onClose() {
      // 生图/上传中禁止关闭，避免请求悬空
      if (this.data.busy || this.data.uploading) return
      this.setData({ visible: false })
    },

    // 「重生成 / 重试」：复用上次入参再调一次（成功则扣新额度）
    onRegenerate() {
      if (this.data.busy) return
      this._generate()
    },

    async _generate() {
      if (this.data.remainCount <= 0) {
        wx.$toast('今日 AI 生图次数已用完，明天再来')
        this.setData({ status: 'error' })
        return
      }
      const p = this.data.params || {}
      this.setData({ busy: true, status: 'loading', tempPath: '' })
      try {
        const r = await aiImageGenApi({
          kitchenId: this.data.kitchenId,
          name: p.name,
          categoryHint: p.categoryHint,
          foods: p.foods,
          seasonings: p.seasonings,
          steps: p.steps,
        })
        const base64 = r.data && r.data.imageBase64
        if (!base64) throw new Error('生图返回为空')
        const tempPath = await this._writeFile(base64)
        this.setData({ status: 'ready', tempPath })
        // 成功出图才通知页面额度 -1（后端失败已回退计数，前端同步不扣）
        this.triggerEvent('consumed')
        // 出图后短暂停留再弹插屏（不打断刚看到结果的瞬间）
        setTimeout(() => this._showInterstitial(), 600)
      } catch (err) {
        wx.$print('AI 生图失败', err)
        this.setData({ status: 'error' })
      } finally {
        this.setData({ busy: false })
      }
    },

    // ── 插屏广告：受全局开关与会员免广告双重控制，无填充/频次上限/未生效静默忽略 ──
    _showInterstitial() {
      if (!adServiceEnabled()) return
      const ad = getInterstitialAd()
      if (!ad) return
      ad.show().catch(() => {
        // show 失败（拉不到/超频次）不提示，不打断生图体验
      })
    },

    // base64 dataURL 写入用户目录，返回本地临时路径（后缀跟真实编码，避免七牛 key 后缀与内容不一致）
    _writeFile(base64) {
      const mime = (base64.match(/^data:([^;]+);base64,/) || [])[1] || 'image/jpeg'
      const ext = mime === 'image/png' ? 'png' : mime === 'image/gif' ? 'gif' : mime === 'image/webp' ? 'webp' : 'jpg'
      return new Promise((resolve, reject) => {
        const filePath = `${wx.env.USER_DATA_PATH}/ai-cover-${Date.now()}.${ext}`
        wx.getFileSystemManager().writeFile({
          filePath,
          data: base64.replace(/^data:image\/\w+;base64,/, ''),
          encoding: 'base64',
          success: () => resolve(filePath),
          fail: reject,
        })
      })
    },

    // 「使用这张」：先压缩再上传（混元无体积/质量参数，只能生成后自己压），
    // 复用现有七牛直传链路（同头像上传），成功后回传 URL 由页面回填 form.photo
    async onUse() {
      if (this.data.uploading || this.data.status !== 'ready') return
      this.setData({ uploading: true })
      wx.showLoading({ title: '上传中...', mask: true })
      try {
        const filePath = await this._compress(this.data.tempPath)
        const url = await uploadAvatar(filePath, 'dish')
        this.triggerEvent('confirm', { url })
        this.setData({ visible: false })
      } catch (err) {
        wx.$print('AI 封面上传失败', err)
        wx.$toast('上传失败，请重试')
      } finally {
        wx.hideLoading()
        this.setData({ uploading: false })
      }
    },

    // 上传前压到封面实际展示宽度：原图 1024x1024 实测 270~758KB，是全站最大的图片来源（实拍走 chooseMedia 已被微信压过）
    // 压缩失败不阻断主流程，退回原图继续上传
    _compress(path) {
      return new Promise((resolve) => {
        wx.compressImage({
          src: path,
          quality: AI_COVER_COMPRESS_QUALITY,
          compressedWidth: AI_COVER_COMPRESS_WIDTH,
          success: ({ tempFilePath }) => {
            wx.$print('AI 封面压缩后', tempFilePath)
            resolve(tempFilePath)
          },
          fail: () => resolve(path),
        })
      })
    },
  },
})
