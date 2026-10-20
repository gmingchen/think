import { FILE_TYPE_ENUM } from '../../common/enums/index'
import { normalizeFile } from '../../utils/index'

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
        const sources = this.data.list.map(item => ({ ...item, url: item.path || item.url }))
        wx.previewMedia({
          current: index,
          sources,
        })
      }
      this.triggerEvent('on-touch', data)
    },
    onDelete(e) {
      const data = e.currentTarget.dataset
      this.triggerEvent('on-delete', data)
    },
  },
})
