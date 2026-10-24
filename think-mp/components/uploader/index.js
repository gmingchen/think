import { uploadMedia } from '../../apis/index'
import { normalizeFile } from '../../utils/index'

Component({
  properties: {
    value: {
      optionalTypes: [String, Object, Array],
      value: '',
      observer: function () {
        this.handleList()
      },
    },
    type: {
      type: String,
      value: 'media', // 文件上传方式 media: chooseMedia  message: chooseMessageFile
    },
    fileType: {
      type: Array,
      value: [], // 文件类型
    },
    count: {
      type: Number,
      value: 1, // 上传文件数量
    },
    limit: {
      type: Number,
      value: 0, // 上传文件的大小（单位 M，0 表示不限制）
    },
    preview: {
      type: Boolean,
      value: false, // 是否触摸预览
    },
    touchUpload: {
      type: Boolean,
      value: false, // 是否触摸上传（替换单张）
    },
    deletable: {
      type: Boolean,
      value: false,
    },
    disabled: {
      type: Boolean,
      value: false,
    },
    width: {
      type: String,
      value: '100%',
    },
    height: {
      type: String,
      value: '100%',
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
    icon: {
      type: String,
      value: 'plus', // 本项目 iconfont 可用图标名（invite/exit/dissolve/share/edit/fork/tick/plus/left/right/mine/group/talk/home）
    },
    placeholder: {
      type: String,
      value: '',
    },
    options: {
      type: Object,
      value: {}, // 透传给 uploadMedia 的第三参（如 mediaTypes / prefix）
    },
  },
  data: {
    list: [],
    uploadList: [],
  },
  methods: {
    handleList() {
      const { value, uploadList } = this.data
      const list = normalizeFile(value)
      list.forEach(item => {
        for (let i = 0; i < uploadList.length; i++) {
          const { url, path } = uploadList[i]
          if (item.url === url) {
            item.path = path
            break
          }
        }
      })
      this.setData({ list })
    },
    async onUpload() {
      const { value, count, limit } = this.properties
      const remain = value instanceof Array ? count - value.length : count
      const r = await uploadMedia(remain, limit, this.properties.options)
      this.setData({ uploadList: [...this.data.uploadList, ...r] })
      this.triggerEvent('on-complete', r)
    },
    async onTouch(e) {
      const { preview, touchUpload, limit } = this.properties
      if (!preview && touchUpload) {
        const [row] = await uploadMedia(1, limit, this.properties.options)
        this.triggerEvent('on-complete', { index: e.detail.index, row })
      }
    },
    onDelete(e) {
      this.triggerEvent('on-delete', e.detail)
    },
  },
})
