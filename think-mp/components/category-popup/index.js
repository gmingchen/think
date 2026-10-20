import { categoryCreateApi, categoryUpdateApi } from '../../apis/index'
import { fieldBehavior } from '../../behaviors/index'
import { LIMIT_REACHED_CODE } from '../../common/constants/index'

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
        title: id ? '编辑分类' : '新增分类',
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
        wx.$toast('请输入分类名称')
        return
      }
      const params = { id, name }
      try {
        id ? await categoryUpdateApi(params) : await categoryCreateApi(params)
      } catch (err) {
        // 4005 已达上限：交由宿主页弹「看视频 +1」引导，本弹窗保持打开不关闭（表单不丢）
        if (err && err.code === LIMIT_REACHED_CODE) this.triggerEvent('limit', { message: err.message })
        return
      }
      wx.$toast(id ? '修改成功' : '新增成功')
      this.setData({ visible: false })
      this.triggerEvent('confirm')
    },
  },
})
