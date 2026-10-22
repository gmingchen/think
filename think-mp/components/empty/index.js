Component({
  options: {
    multipleSlots: true,
  },
  properties: {
    title: {
      type: String,
      value: '',
    },
    desc: {
      type: String,
      value: '',
    },
    // text 为 desc 的别名（兼容项目内 <empty text=.../> 写法）
    text: {
      type: String,
      value: '',
    },
  },
})
