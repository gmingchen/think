import { getValueByPath } from '../../utils/index'

export const fileBehavior = Behavior({
  methods: {
    onFileComplete(e) {
      let { field } = e.currentTarget.dataset
      if (field) {
        let result = null
        const response = e.detail
        if (response instanceof Array) {
          let data = getValueByPath(field, this.data)
          if (data instanceof Array) {
            result = [...data, ...response.map(item => item.url)]
          } else {
            result = response[0].url
          }
        } else if (response instanceof Object) {
          const { index, row } = response
          field += `[${ index }]`
          result = row
        }
        this.setData({ [field]: result })
      }
    },
    onFileDelete(e) {
      const { field } = e.currentTarget.dataset
      if (field) {
        let result = null
        let data = getValueByPath(field, this.data)
        if (data instanceof Array) {
          const { index } = e.detail
          result = data.filter((_item, _index) => _index !== index)
        }
        this.setData({ [field]: result })
      }
    }
  },
})
