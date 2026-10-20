import { kitchenUpdateApi } from '../../../../../../apis/index'
import { fieldBehavior } from '../../../../../../behaviors/index'

Component({
  behaviors: [fieldBehavior],
  data: {
    visible: false,
    id: null,
    form: { id: null,name: '' },
  },
  methods: {
    open(data) {
      this.setData({
        form: { ...data },
        visible: true,
      })
    },
    onClose() {
      this.setData({ visible: false })
    },
    async onSave() {
      let { id, name } = this.data.form
      name = name.trim()
      if (!name) {
        return wx.$toast('请输入厨房名称')
      }
      const params = { id, name }
      await kitchenUpdateApi(params)
      wx.$toast('修改成功')
      this.setData({ visible: false })
      this.triggerEvent('confirm')
    },
  },
})
