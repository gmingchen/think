import { storePageBehavior, getPosterCopy } from '../../../../behaviors/index'
import { authorize, getSetting, saveImageToPhotosAlbum, showShareImageMenu } from '../../../../utils/index'
import { generateQrcodeApi } from '../../../../apis/index'

// 小程序码扫码落地页（对应后端 getUnlimited 的 page，不带前导斜杠）
const QRCODE_PAGE = 'subpackages/dish/pages/draw/index'

// 相册写入权限：getSetting 传不带前缀的 key，authorize 需带 scope. 前缀
const ALBUM_SCOPE_KEY = 'writePhotosAlbum'
const ALBUM_SCOPE = `scope.${ALBUM_SCOPE_KEY}`

// 导航条内容高，与 components/navbar 的 data.height 保持一致（px）
const NAV_BAR_H = 40
// 底部按钮栏实测失败时的兜底高度（px）
const FOOTER_FALLBACK_H = 56
// 舞台上下内边距，须与 wxss .poster-draw__stage 的 padding 保持一致（rpx）
const STAGE_PAD_RPX = 32

Page({
  behaviors: [storePageBehavior],

  data: {
    loading: true,
    stageH: 0,
    dishes: [],
    title: '',
    subTitle: '',
    qrCodeUrl: '',
  },

  onLoad() {
    this.bindStore({ fields: ['poster'] })
    // bindStore 绑定需等一帧才落到 data，统一在 nextTick 内取数
    wx.nextTick(() => this._init())
  },

  async _init() {
    const { drawData } = this.data.poster || {}
    if (!drawData || !drawData.dishes || !drawData.dishes.length) {
      this.setData({ loading: false })
      wx.$toast('没有可生成的抽菜结果')
      setTimeout(() => wx.$navigateBack(), 800)
      return
    }
    // 分状态文案复用 behavior 纯函数；副标题拼厨房名
    const { title, subTitle } = getPosterCopy(drawData.dishes)
    const sub = drawData.kitchenName ? `${drawData.kitchenName} · ${subTitle}` : subTitle
    this.setData({ dishes: drawData.dishes, title, subTitle: sub })

    // 带参小程序码接口需登录态拿 userId，等登录完成再出图
    await wx.$onLogined
    const { stageH, maxHeight } = await this._measureStage()
    this.setData({ stageH })
    this._renderPoster(drawData.dishes, title, sub, maxHeight)
  },

  // 舞台可用高度 = 窗口高 - 导航栏 - 底部按钮栏：既用于垂直居中撑高，也用于回传画布做等比缩放
  _measureStage() {
    const { windowHeight, windowWidth, statusBarHeight, safeArea } = wx.getSystemInfoSync()
    // 部分机型（glass-easel）查不到 slot 内容，兜底值补上安全区高度
    const bottomInset = safeArea ? Math.max(windowHeight - safeArea.bottom, 0) : 0
    return new Promise((resolve) => {
      wx.createSelectorQuery()
        .select('.poster-draw__footer')
        .boundingClientRect((rect) => {
          const footerH = rect && rect.height ? rect.height : FOOTER_FALLBACK_H + bottomInset
          const stageH = windowHeight - (statusBarHeight + NAV_BAR_H) - footerH
          const pad = (windowWidth / 750) * STAGE_PAD_RPX * 2
          resolve({ stageH: Math.floor(stageH), maxHeight: Math.floor(stageH - pad) })
        })
        .exec()
    })
  },

  // 拉带参码（失败降级占位，不阻断出图）→ 交给 canvas-poster 渲染
  async _renderPoster(dishes, title, subTitle, maxHeight) {
    let qrCodeUrl = ''
    try {
      const r = await generateQrcodeApi({ page: QRCODE_PAGE })
      qrCodeUrl = r.data || ''
    } catch (e) {
      wx.$print('邀请小程序码获取失败，降级占位码', e)
    }
    this.setData({ qrCodeUrl, loading: true })
    const canvas = this.selectComponent('#posterCanvas')
    try {
      await canvas.render({ dishes, title, subTitle, qrCodeUrl, maxHeight })
    } catch (e) {
      wx.$print('海报渲染失败', e)
    } finally {
      this.setData({ loading: false })
    }
  },

  // 导出画布 → 存相册（授权被拒降级提示）
  async onSave() {
    const path = await this._export()
    if (!path) return
    try {
      await this._authorizeAlbum()
      await saveImageToPhotosAlbum({ filePath: path })
      wx.$toast('已保存到相册')
    } catch (e) {
      wx.$print('保存相册失败', e)
      wx.$toast('相册授权被拒，可直接点分享')
    }
  },

  // 导出画布 → 拉起微信图片面板（发送给朋友 / 朋友圈 / 存相册）；发出的是纯图片，靠海报上的小程序码完成引流
  async onShare() {
    const path = await this._export()
    if (!path) return
    try {
      await showShareImageMenu({ path })
      wx.$toast('感谢分享')
    } catch (e) {
      wx.$print('分享海报取消', e)
    }
  },

  async _export() {
    const canvas = this.selectComponent('#posterCanvas')
    try {
      return await canvas.exportImage()
    } catch (e) {
      wx.$toast('海报还没生成好，稍等一下~')
      return ''
    }
  },

  // 相册授权预检：已授权直接过，未授权才拉微信授权框（拒绝时由 onSave 的 catch 降级提示）
  async _authorizeAlbum() {
    const granted = await getSetting(ALBUM_SCOPE_KEY)
    if (granted) return
    await authorize({ scope: ALBUM_SCOPE })
  },
})
