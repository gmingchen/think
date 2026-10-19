// van-image 的 fit 到小程序原生 image mode 的映射
const FIT_MODE = {
  cover: 'aspectFill',
  contain: 'aspectFit',
  fill: 'scaleToFill',
  none: 'scaleToFill',
}

Component({
  properties: {
    value: {
      type: String,
      value: '',
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
      observer: function (fit) {
        this.setData({ fitMode: FIT_MODE[fit] || 'aspectFill' })
      },
    },
  },
  data: {
    fitMode: 'aspectFill',
  },
  methods: {},
})
