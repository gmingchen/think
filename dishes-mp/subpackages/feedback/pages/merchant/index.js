import { fieldBehavior } from '../../../../behaviors/index'
import { AD_UNIT_MERCHANT_BANNER } from '../../../../common/constants/index'
import { feedbackMerchantCreateApi } from '../../../../apis/index'
import {
  BUSINESS_TYPE_LIST,
  CURRENT_TOOL_LIST,
  DISH_SCALE_LIST,
  WILLINGNESS_LIST,
} from '../../../../common/enums/index'

Page({
  behaviors: [fieldBehavior],
  data: {
    AD_UNIT_MERCHANT_BANNER,
    BUSINESS_TYPE_LIST,
    CURRENT_TOOL_LIST,
    DISH_SCALE_LIST,
    WILLINGNESS_LIST,
    businessType: null,
    currentTool: [],
    dishScale: null,
    painPoint: '',
    expectFeature: '',
    willingness: null,
    contactWx: '',
    contactPhone: '',
    submitting: false,
  },

  onBusinessTypeTap(e) {
    this.setData({ businessType: Number(e.currentTarget.dataset.value) })
  },

  onDishScaleTap(e) {
    this.setData({ dishScale: Number(e.currentTarget.dataset.value) })
  },

  onWillingnessTap(e) {
    this.setData({ willingness: Number(e.currentTarget.dataset.value) })
  },

  // 多选：命中已存在则移除，否则追加，不依赖原生 checkbox-group（全站选择类交互均走自定义 chip 切换，保持一致手感）
  onCurrentToolTap(e) {
    const value = Number(e.currentTarget.dataset.value)
    const { currentTool } = this.data
    const index = currentTool.indexOf(value)
    const next = index > -1
      ? currentTool.filter((_, i) => i !== index)
      : [...currentTool, value]
    this.setData({ currentTool: next })
  },

  async onSubmit() {
    const { businessType, currentTool, dishScale, painPoint, expectFeature, willingness, contactWx, contactPhone, submitting } = this.data
    if (submitting) return
    // 仅 painPoint / expectFeature 必填，其余字段（含 businessType/currentTool/dishScale/willingness）均已改为选填，
    // 降低提交门槛，不做客户端强校验
    if (!painPoint.trim()) {
      wx.$toast('请填写目前最头疼的问题')
      return
    }
    if (!expectFeature.trim()) {
      wx.$toast('请填写希望解决什么问题')
      return
    }

    this.setData({ submitting: true })
    // 提交为登录态接口，须等登录完成再发（对齐全站约定，禁止直接调原生接口）
    await wx.$onLogined
    try {
      await feedbackMerchantCreateApi({
        businessType,
        currentTool,
        dishScale,
        painPoint: painPoint.trim(),
        expectFeature: expectFeature.trim(),
        willingness,
        contactWx: contactWx.trim(),
        contactPhone: contactPhone.trim(),
      })
      wx.$toast('已收到，感谢反馈 🙏')
      setTimeout(() => { wx.$navigateBack() }, 800)
    } finally {
      this.setData({ submitting: false })
    }
  },
})
