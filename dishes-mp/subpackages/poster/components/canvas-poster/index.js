import { DEFAULT_DISH_COVER } from '../../../../common/constants/index'

// 手账风抽菜结果海报 Canvas 2D 绘制组件
// 预览即导出：页面展示的 canvas 元素本身就是绘制目标，导出走 canvasToTempFilePath，天然一致
// 关键时序（见高频坑点）：getImageInfo → createImage → onload → drawImage → 全部完成后才 export

// ── 设计坐标系常量（宽固定 750，单位≈rpx，绘制时按 dpr 缩放）──
const DW = 750
const PAPER_INSET = 40
const PAPER_R = 32
const MARGIN = 60
const NOTE_INSET = 44
const COL_GAP = 36
const ROW_GAP = 50
const HEADER_H = 266
const NOTE_H = 78
const FOOTER_H = 178
const CARD_CAP = 182
// 三道为两排卡片，照片比例与文字区各降一档，配合 fitHeight 尽量不缩画布宽
const ROW_GAP_3 = 36
const CARD_CAP_3 = 150
const PHOTO_RATIO = 0.86
const PHOTO_RATIO_3 = 0.74
// 导出分辨率与展示尺寸解耦：画布位图固定按设计宽 2 倍，展示端缩窄只影响排版不影响清晰度
const EXPORT_SCALE = 2
// 卡片内三行基线占文字区高度的比例（菜名 / 做过次数 / 星星）
const LINE_RATIOS = [0.27, 0.5, 0.72]

// ── 色板 ──
const C = {
  // 画布底色：与 container 的 --bg-base 一致，屏幕上看不出画布边界，导出不带透明角
  pageBg: '#F5EFEB',
  bg: '#FAF5EE',
  card: '#FFFFFF',
  ink: '#3D352E',
  pencil: '#8C7E72',
  terracotta: '#C86D51',
  mustard: '#D4A017',
  stampRed: '#B84A39',
  tape: 'rgba(224, 169, 79, 0.7)',
  tapeGreen: 'rgba(150, 185, 148, 0.72)',
  border: '#DDD3C8',
}

// 图片统一转 Canvas Image：dataURL 直接喂 createImage，其余先 getImageInfo 拿本地临时路径，两者都要等 onload
function loadImage(canvas, src) {
  return new Promise((resolve) => {
    if (!src) return resolve(null)
    const bind = (path, onFail) => {
      const img = canvas.createImage()
      img.onload = () => resolve(img)
      img.onerror = () => (onFail ? onFail() : resolve(null))
      img.src = path
    }
    // base64 dataURL：wx.getImageInfo 不认，只能直喂 createImage
    if (src.startsWith('data:')) {
      bind(src)
      return
    }
    // 代码包内路径（如默认封面 /common/images/dish.jpg）：先直喂，失败再退回 getImageInfo 转路径
    if (src.startsWith('/')) {
      bind(src, () => {
        wx.getImageInfo({
          src,
          success: ({ path }) => bind(path),
          fail: () => resolve(null),
        })
      })
      return
    }
    // 网络图（七牛菜品图）：必须先换成本地临时路径
    wx.getImageInfo({
      src,
      success: ({ path }) => bind(path),
      fail: () => resolve(null),
    })
  })
}

Component({
  data: {
    styleW: 0,
    styleH: 0,
  },

  lifetimes: {
    detached() {
      this._canvas = null
    },
  },

  methods: {
    // 计算排布：单道居中放大，两道一行两列，三道上二下一居中
    _layout(count) {
      let colW
      let rows
      let photoRatio
      let cap
      let rowGap
      if (count <= 1) {
        colW = 380
        rows = 1
        photoRatio = PHOTO_RATIO
        cap = CARD_CAP
        rowGap = ROW_GAP
      } else {
        colW = Math.floor((DW - MARGIN * 2 - COL_GAP) / 2)
        rows = count > 2 ? 2 : 1
        photoRatio = count > 2 ? PHOTO_RATIO_3 : PHOTO_RATIO
        cap = count > 2 ? CARD_CAP_3 : CARD_CAP
        rowGap = count > 2 ? ROW_GAP_3 : ROW_GAP
      }
      const photoH = Math.round(colW * photoRatio)
      const cardH = photoH + cap
      const gridH = rows * cardH + (rows - 1) * rowGap
      return { colW, photoH, cardH, rows, gridH, DH: HEADER_H + gridH + NOTE_H + FOOTER_H }
    },

    // 查询 canvas 节点并按当前布局设尺寸，返回 { canvas, ctx }
    // maxHeight 为页面舞台可用高度：超出时反推缩窄画布宽，保证整张海报不滚动
    _initCanvas(DH, maxHeight) {
      const { windowWidth } = wx.getSystemInfoSync()
      let styleW = Math.floor(windowWidth * 0.88)
      if (maxHeight) {
        styleW = Math.min(styleW, Math.floor((maxHeight * DW) / DH))
      }
      const styleH = Math.floor((styleW * DH) / DW)

      return new Promise((resolve) => {
        // 先按目标尺寸 setData，等 DOM 更新后再查节点，否则拿到旧尺寸导致绘制/导出错位
        this.setData({ styleW, styleH }, () => {
          wx.createSelectorQuery()
            .in(this)
            .select('#posterCanvas')
            .fields({ node: true })
            .exec((res) => {
              const node = res[0].node
              // 位图尺寸按设计稿固定倍率，与 CSS 展示尺寸无关，导出清晰度全设备一致
              node.width = DW * EXPORT_SCALE
              node.height = DH * EXPORT_SCALE
              const ctx = node.getContext('2d')
              ctx.setTransform(EXPORT_SCALE, 0, 0, EXPORT_SCALE, 0, 0)
              this._canvas = node
              resolve({ canvas: node, ctx })
            })
        })
      })
    },

    // 主渲染入口：payload = { dishes, title, subTitle, qrCodeUrl, maxHeight }
    async render(payload) {
      const { dishes = [], title = '', subTitle = '', qrCodeUrl = '', maxHeight = 0 } = payload || {}
      // 重绘前复位：导出必须等本次绘制完成，否则可能拿到半张图
      this._rendered = false
      const count = Math.min(dishes.length, 3)
      const { colW, photoH, cardH, gridH, DH } = this._layout(count)

      const { canvas, ctx } = await this._initCanvas(DH, maxHeight)
      // 先并行加载所有图片，全部 onload 后再绘制，避免导出丢图；无封面统一兜底默认菜品图
      const [qrImg, ...dishImgs] = await Promise.all([
        loadImage(canvas, qrCodeUrl),
        ...dishes.slice(0, count).map((d) => loadImage(canvas, d.photo || DEFAULT_DISH_COVER)),
      ])

      this._drawBackground(ctx, DH)
      this._drawTape(ctx)
      this._drawHeader(ctx, title, subTitle)
      this._drawPolaroids(ctx, { dishes, count, colW, photoH, cardH, dishImgs })
      this._drawNoteLine(ctx, HEADER_H + gridH + NOTE_H / 2)
      this._drawFooter(ctx, DH, qrImg)
      this._rendered = true
    },

    // 纸张：先整幅铺满画布底色（不透明），再叠圆角纸面
    // 四角不能留透明：部分安卓相册底色为黑，透明 PNG 会在圆角外露四个黑角，发出去更难看
    _drawBackground(ctx, DH) {
      const pw = DW - PAPER_INSET * 2
      const ph = DH - PAPER_INSET * 2
      ctx.fillStyle = C.pageBg
      ctx.fillRect(0, 0, DW, DH)

      // 纸面带轻投影：画在画布里才能跟圆角走
      // 投影最大外溢（blur/2 + offsetY）须小于 PAPER_INSET，否则会被画布边缘切掉
      ctx.save()
      this._roundRect(ctx, PAPER_INSET, PAPER_INSET, pw, ph, PAPER_R)
      ctx.fillStyle = C.bg
      ctx.shadowColor = 'rgba(70, 50, 35, 0.18)'
      ctx.shadowBlur = 18
      ctx.shadowOffsetY = 8
      ctx.fill()
      ctx.restore()

      ctx.save()
      this._roundRect(ctx, PAPER_INSET, PAPER_INSET, pw, ph, PAPER_R)
      ctx.clip()
      // 淡麻布点阵纹理（限制在纸面内）
      ctx.fillStyle = 'rgba(180, 160, 140, 0.10)'
      for (let x = PAPER_INSET + 16; x < DW - PAPER_INSET; x += 32) {
        for (let y = PAPER_INSET + 16; y < DH - PAPER_INSET; y += 32) {
          ctx.beginPath()
          ctx.arc(x, y, 1.4, 0, Math.PI * 2)
          ctx.fill()
        }
      }
      ctx.restore()

      // 纸张圆角描边
      ctx.strokeStyle = C.border
      ctx.lineWidth = 4
      this._roundRect(ctx, PAPER_INSET, PAPER_INSET, pw, ph, PAPER_R)
      ctx.stroke()
    },

    // 海报顶部左右两角斜胶带（UI 无中央标签条，改为两角各贴一段黄/绿胶带）
    // cy 必须大于胶带旋转后的纵向半径（约 31），否则上角伸出画布被切、导出图也缺这一角
    _drawTape(ctx) {
      this._tapePiece(ctx, 104, 46, -0.24, C.tape)
      this._tapePiece(ctx, DW - 104, 46, 0.22, C.tapeGreen)
    },

    // 单段胶带：半透明底 + 竖向细纹模拟撕口
    _tapePiece(ctx, cx, cy, angle, color, w = 132, h = 32) {
      ctx.save()
      ctx.translate(cx, cy)
      ctx.rotate(angle)
      ctx.globalAlpha = 0.8
      ctx.fillStyle = color
      ctx.fillRect(-w / 2, -h / 2, w, h)
      ctx.strokeStyle = 'rgba(255, 255, 255, 0.35)'
      ctx.lineWidth = 1.5
      for (let i = -w / 2 + 8; i < w / 2; i += 12) {
        ctx.beginPath()
        ctx.moveTo(i, -h / 2 + 4)
        ctx.lineTo(i, h / 2 - 4)
        ctx.stroke()
      }
      ctx.restore()
    },

    _drawHeader(ctx, title, subTitle) {
      ctx.textAlign = 'center'
      ctx.textBaseline = 'alphabetic'
      ctx.fillStyle = C.pencil
      ctx.font = '20px sans-serif'
      ctx.fillText('~ Zakka Kitchen Journal ~', DW / 2, 96)

      ctx.fillStyle = C.ink
      ctx.font = 'bold 40px sans-serif'
      ctx.fillText(this._fit(ctx, title, DW - 96), DW / 2, 162)

      ctx.fillStyle = C.terracotta
      ctx.font = '22px sans-serif'
      ctx.fillText(this._fit(ctx, subTitle, DW - 120), DW / 2, 218)

      // 虚线分割线（UI 为朱色细虚线）
      ctx.save()
      ctx.setLineDash([10, 8])
      ctx.strokeStyle = 'rgba(200, 109, 81, 0.45)'
      ctx.lineWidth = 2
      ctx.beginPath()
      ctx.moveTo(MARGIN + 12, HEADER_H - 28)
      ctx.lineTo(DW - MARGIN - 12, HEADER_H - 28)
      ctx.stroke()
      ctx.restore()
    },

    // 单行文字超宽则截断加省略号
    _fit(ctx, text, maxW) {
      text = text || ''
      if (ctx.measureText(text).width <= maxW) return text
      let s = text
      while (s.length > 1 && ctx.measureText(s + '…').width > maxW) s = s.slice(0, -1)
      return s + '…'
    },

    // 卡片区与便签区之间的手账批注行：左说明 + 右口号
    _drawNoteLine(ctx, y) {
      ctx.textBaseline = 'middle'
      ctx.font = '19px sans-serif'
      ctx.fillStyle = C.pencil
      ctx.textAlign = 'left'
      ctx.fillText('🎲 摇号定餐 · 免去纠结', MARGIN + 4, y)
      ctx.fillStyle = C.terracotta
      ctx.textAlign = 'right'
      ctx.fillText('✦ 每一餐都值得手写记录 ✦', DW - MARGIN - 4, y)
      ctx.textAlign = 'center'
      ctx.textBaseline = 'alphabetic'
    },

    _drawPolaroids(ctx, { dishes, count, colW, photoH, cardH, dishImgs }) {
      const gridTop = HEADER_H
      // 三道第二排的间距同样收紧
      const rowGap = count > 2 ? ROW_GAP_3 : ROW_GAP
      // 胶带色按序号交替，模拟手账里随手贴的不同颜色纸带
      const tapeOf = (i) => (i % 2 ? C.tapeGreen : C.tape)
      if (count <= 1) {
        this._drawPolaroid(ctx, DW / 2, gridTop + cardH / 2, colW, photoH, cardH, -0.02, dishes[0], dishImgs[0], tapeOf(0))
        return
      }
      const x1 = MARGIN + colW / 2
      const x2 = DW - MARGIN - colW / 2
      const yRow1 = gridTop + cardH / 2
      this._drawPolaroid(ctx, x1, yRow1, colW, photoH, cardH, -0.04, dishes[0], dishImgs[0], tapeOf(0))
      this._drawPolaroid(ctx, x2, yRow1, colW, photoH, cardH, 0.035, dishes[1], dishImgs[1], tapeOf(1))
      if (count > 2) {
        const yRow2 = gridTop + cardH + rowGap + cardH / 2
        this._drawPolaroid(ctx, DW / 2, yRow2, colW, photoH, cardH, -0.015, dishes[2], dishImgs[2], tapeOf(2))
      }
    },

    // 以 (cx, cy) 为中心绘制一张微旋转的拍立得卡片
    _drawPolaroid(ctx, cx, cy, w, photoH, h, angle, dish, img, tape = C.tape) {
      ctx.save()
      ctx.translate(cx, cy)
      ctx.rotate(angle)

      ctx.shadowColor = 'rgba(60, 45, 35, 0.15)'
      ctx.shadowBlur = 16
      ctx.shadowOffsetY = 6
      ctx.fillStyle = C.card
      ctx.fillRect(-w / 2, -h / 2, w, h)
      ctx.shadowColor = 'transparent'
      ctx.shadowBlur = 0
      ctx.shadowOffsetY = 0
      ctx.strokeStyle = '#EAE2D7'
      ctx.lineWidth = 1.5
      ctx.strokeRect(-w / 2, -h / 2, w, h)

      const pad = 10
      const px = -w / 2 + pad
      const py = -h / 2 + pad
      const pw = w - pad * 2
      ctx.fillStyle = '#F5ECE1'
      ctx.fillRect(px, py, pw, photoH)

      if (img) {
        ctx.save()
        this._roundRect(ctx, px, py, pw, photoH, 4)
        ctx.clip()
        this._drawCover(ctx, img, px, py, pw, photoH)
        ctx.restore()
      } else {
        // 连默认封面都加载失败时的最后兜底（仅极端机型/断网）
        ctx.font = `${Math.floor(photoH * 0.4)}px sans-serif`
        ctx.textAlign = 'center'
        ctx.textBaseline = 'middle'
        ctx.fillText('🍽️', 0, py + photoH / 2)
        ctx.textBaseline = 'alphabetic'
      }

      // 菜名 / 做过次数 / 评分星三行（按文字区高度等比取位，三道小卡与常规卡共用）
      const cap = h - photoH
      const lineY = (i) => py + photoH + Math.round(cap * LINE_RATIOS[i])
      ctx.textAlign = 'center'
      ctx.textBaseline = 'alphabetic'
      ctx.fillStyle = C.ink
      ctx.font = 'bold 28px sans-serif'
      ctx.fillText(this._fit(ctx, dish?.name || '', pw), 0, lineY(0))
      ctx.fillStyle = C.pencil
      ctx.font = '18px sans-serif'
      ctx.fillText(`做过 ${dish?.cookCount || 0} 次`, 0, lineY(1))
      ctx.fillStyle = C.mustard
      ctx.font = '22px sans-serif'
      ctx.fillText(this._stars(dish?.rating || 0), 0, lineY(2))

      // 左上角斜贴一段小胶带
      this._tapePiece(ctx, -w / 2 + 30, -h / 2, -0.3, tape, 96, 24)

      ctx.restore()
    },

    _stars(rating) {
      const n = Math.max(0, Math.min(5, Math.round(rating)))
      return '★'.repeat(n) + '☆'.repeat(5 - n)
    },

    _drawFooter(ctx, DH, qrImg) {
      const fy = DH - FOOTER_H
      const noteH = FOOTER_H - 40 // 底部内容带高度，仅用于垂直居中定位

      // 左侧朱红双线圆角印章
      ctx.save()
      ctx.translate(150, fy + noteH / 2)
      ctx.rotate(-0.16)
      ctx.strokeStyle = C.stampRed
      ctx.lineWidth = 4
      this._roundRect(ctx, -78, -44, 156, 88, 16)
      ctx.stroke()
      ctx.lineWidth = 1.5
      this._roundRect(ctx, -72, -38, 144, 76, 11)
      ctx.stroke()
      ctx.fillStyle = C.stampRed
      ctx.textAlign = 'center'
      ctx.textBaseline = 'middle'
      ctx.font = 'bold 26px sans-serif'
      ctx.fillText('大厨准奏', 0, -8)
      ctx.font = 'bold 15px sans-serif'
      ctx.fillText('厨菜记 · 今日开火', 0, 22)
      ctx.restore()

      // 右侧小程序码：白底圆角衬底（只作扫码对比度，不描边）
      // 右边距留 20：衬底右下角若靠 PAPER_INSET 太近会探出纸张圆角弧线，在角外留个白刃
      const qrSize = 110
      const qx = DW - NOTE_INSET - 20 - qrSize
      const qy = fy + 10
      ctx.save()
      this._roundRect(ctx, qx - 9, qy - 9, qrSize + 18, qrSize + 18, 12)
      ctx.fillStyle = '#FFFFFF'
      ctx.shadowColor = 'rgba(60, 45, 35, 0.12)'
      ctx.shadowBlur = 10
      ctx.shadowOffsetY = 4
      ctx.fill()
      // 投影必须在 clip 前关掉，否则残留状态会污染后面的码图与占位描边
      ctx.shadowColor = 'transparent'
      ctx.shadowBlur = 0
      ctx.shadowOffsetY = 0
      ctx.clip()
      if (qrImg) {
        ctx.drawImage(qrImg, qx, qy, qrSize, qrSize)
      } else {
        // 无带参码时画占位太阳码（仅丢归因精度，不阻断出图）
        ctx.strokeStyle = C.ink
        ctx.lineWidth = 3
        ctx.beginPath()
        ctx.arc(qx + qrSize / 2, qy + qrSize / 2, qrSize / 2 - 8, 0, Math.PI * 2)
        ctx.stroke()
        ctx.fillStyle = '#07C160'
        ctx.beginPath()
        ctx.arc(qx + qrSize / 2, qy + qrSize / 2, 16, 0, Math.PI * 2)
        ctx.fill()
      }
      ctx.restore()
    },

    _roundRect(ctx, x, y, w, h, r) {
      ctx.beginPath()
      ctx.moveTo(x + r, y)
      ctx.arcTo(x + w, y, x + w, y + h, r)
      ctx.arcTo(x + w, y + h, x, y + h, r)
      ctx.arcTo(x, y + h, x, y, r)
      ctx.arcTo(x, y, x + w, y, r)
      ctx.closePath()
    },

    _drawCover(ctx, img, x, y, w, h) {
      const ir = img.width / img.height
      const r = w / h
      let sx, sy, sw, sh
      if (ir > r) {
        sh = img.height
        sw = sh * r
        sx = (img.width - sw) / 2
        sy = 0
      } else {
        sw = img.width
        sh = sw / r
        sx = 0
        sy = (img.height - sh) / 2
      }
      ctx.drawImage(img, sx, sy, sw, sh, x, y, w, h)
    },

    // 导出当前画布为临时图片路径（必须在 render 完成后调用）
    exportImage() {
      return new Promise((resolve, reject) => {
        if (!this._canvas || !this._rendered) {
          reject(new Error('海报尚未渲染完成'))
          return
        }
        wx.canvasToTempFilePath({
          canvas: this._canvas,
          success: ({ tempFilePath }) => resolve(tempFilePath),
          fail: reject,
        })
      })
    },
  },
})
