import { foodCreateApi, foodUpdateApi } from '../../apis/index'
import { fieldBehavior } from '../../behaviors/index'

Component({
  behaviors: [fieldBehavior],
  data: {
    visible: false,
    title: '',
    form: { name: '' },
  },
  methods: {
    open(item) {
      const { id, name } = item || {}
      this.setData({
        title: id ? '编辑食材' : '新增食材',
        form: { id, name: name || '' },
        visible: true,
      })
    },
    onClose() {
      this.setData({ visible: false })
    },
    async onSave() {
      const { form } = this.data
      const id = form.id
      const name = form.name.trim()
      if (!name) {
        wx.$toast('请输入食材名称')
        return
      }
      const params = { id, name }
      // 新增回传新 id，供 picker 自动选中刚建的项
      const r = id ? await foodUpdateApi(params) : await foodCreateApi(params)
      wx.$toast(id ? '修改成功' : '新增成功')
      this.setData({ visible: false })
      this.triggerEvent('confirm', { id: id || r.data, name, created: !id })
    },
  },
})
