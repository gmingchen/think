import { FILE_TYPE_ENUM } from '../../common/enums/index'
import { normalizeFile, previewImage, previewMedia } from '../../utils/index'

Component({
  properties: {
    value: {
      optionalTypes: [String, Object, Array],
      value: '',
    },
    width: {
      type: String,
      value: '100%',
    },
    height: {
      type: String,
      value: '',
    },
    radius: {
      type: String,
      value: '8rpx',
    },
    fit: {
      type: String,
      value: 'cover',
    },
    gap: {
      type: String,
      value: '8rpx',
    },
    preview: {
      type: Boolean,
      value: true,
    },
    deletable: {
      type: Boolean,
      value: false,
    },
    cls: {
      type: String,
      value: '',
    },
  },
  data: {
    FILE_TYPE_ENUM,
    list: [],
  },
  observers: {
    value: function (value) {
      this.setData({ list: normalizeFile(value) })
    },
  },
  lifetimes: {
    attached: function () {
      this.setData({ list: normalizeFile(this.properties.value) })
    },
  },
  methods: {
    onTouch(e) {
      const data = e.currentTarget.dataset
      const { index } = data
      if (this.properties.preview) {
        this.previewAt(index)
      }
      this.triggerEvent('on-touch', data)
    },
    // 预览：纯图片走 previewImage，含视频才走 previewMedia
    // previewMedia 必须逐条给出可识别的 type，后缀取不到（heic、带查询串、本地临时路径无后缀）时
    // 界面照常拉起却只有一片黑；previewImage 只吃 url 字符串，缩略图能显示它就能显示
    previewAt(index) {
      const active = Number(index)
      const sources = []
      let current = 0
      this.data.list.forEach((item, i) => {
        // url 优先：缩略图渲染用的就是它，能显示即证明可访问；path 是微信本地临时文件，留作兜底
        const url = item.url || item.path
        if (!url) return
        if (i === active) current = sources.length
        sources.push({ ...item, url })
      })
      if (!sources.length) return
      if (sources.some(({ type }) => type === FILE_TYPE_ENUM.VIDEO)) {
        previewMedia({
          current,
          sources: sources.map(({ url, type }) => ({ url, type: type || FILE_TYPE_ENUM.IMAGE })),
        })
      } else {
        const urls = sources.map(({ url }) => url)
        previewImage({ current: urls[current], urls })
      }
    },
    onDelete(e) {
      const data = e.currentTarget.dataset
      this.triggerEvent('on-delete', data)
    },
  },
})
