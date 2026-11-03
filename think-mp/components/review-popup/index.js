import { REQUIRE_CONTENT_ENUM } from '../../common/enums/index'

Component({
  properties: {
    show: {
      type: Boolean,
      value: false,
      observer: 'onShowChange',
    },
    // 备选方案数组：[string]（方案文案）
    options: {
      type: Array,
      value: [],
    },
    // 本人已评议时回显：倾向下标 + 观点（新增时分别传 -1 / ''）
    myOption: {
      type: Number,
      value: -1,
    },
    myContent: {
      type: String,
      value: '',
    },
    // 观点是否必填（decision.require_content）：仅 0 为选填，其余（含未传）一律按必填，与后端校验口径一致
    requireContent: {
      type: Number,
      value: REQUIRE_CONTENT_ENUM.YES,
    },
    // 提交中（页面控制）：防连点/重复提交，提交中禁用按钮
    submitting: {
      type: Boolean,
      value: false,
    },
  },
  data: {
    selected: -1,
    content: '',
    // 由 requireContent 派生的展示/校验位（WXML 不直接比枚举值）
    contentOptional: false,
  },
  methods: {
    onShowChange(show) {
      if (show) {
        // 打开时以当前 props 回显，避免上次输入残留
        this.setData({
          selected: this.data.myOption,
          content: this.data.myContent || '',
          contentOptional: this.data.requireContent === REQUIRE_CONTENT_ENUM.NO,
        })
      }
    },
    onSelectOption(e) {
      this.setData({ selected: Number(e.currentTarget.dataset.index) })
    },
    onInputContent(e) {
      this.setData({ content: e.detail.value })
    },
    onClose() {
      this.triggerEvent('close')
    },
    onSubmit() {
      if (this.data.submitting) {
        return
      }
      const { selected, content } = this.data
      if (selected < 0) {
        wx.$toast('请选择倾向的备选项')
        return
      }
      // 选填单（发起人关掉观点必填）允许只表倾向不写观点
      if (!this.data.contentOptional && !content.trim()) {
        wx.$toast('请填写评议观点')
        return
      }
      this.triggerEvent('submit', { option: selected, content: content.trim() })
    },
  },
})
