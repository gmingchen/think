import { storePageBehavior, fieldBehavior, sharePageBehavior } from '../../../../behaviors/index'
import { userUpdateApi, uploadAvatar } from '../../../../apis/index'
import { SEX_ENUM, SEX_LIST } from '../../../../common/enums/index'
import { DEFAULT_AVATAR } from '../../../../common/constants/index'

Page({
  behaviors: [storePageBehavior, fieldBehavior, sharePageBehavior],
  data: {
    nickname: '',
    avatar: '',
    defaultAvatar: DEFAULT_AVATAR,
    sex: SEX_ENUM.UNKNOWN,
    SEX_LIST,
  },

  onLoad() {
    this.bindStore({ fields: ['user'], actions: ['getUser'] })
    // 主动拉起平台隐私授权弹窗：昵称输入 type="nickname" 与头像 chooseAvatar 均属隐私接口，
    // 未授权时不会自动弹窗，而是静默降级（errno:104），只能显式 requirePrivacyAuthorize 调起
    this.ensurePrivacyAuthorize()
    wx.nextTick(() => {
      const { user = {} } = this.data
      this.setData({
        nickname: user.nickname || '',
        avatar: user.avatar || '',
        sex: user.sex ?? SEX_ENUM.UNKNOWN,
      })
    })
  },

  ensurePrivacyAuthorize() {
    if (!wx.requirePrivacyAuthorize) return // 低版本基础库无隐私接口，走普通输入即可
    wx.requirePrivacyAuthorize({
      fail: () => {
        // 用户在弹窗点了拒绝：昵称快捷键不可用，input 会自动降级，仅提示一次
        wx.$toast('未同意隐私协议，昵称将手动输入')
      },
    })
  },

  onSexChange(e) {
    this.setData({ sex: Number(e.currentTarget.dataset.value) })
  },

  async onChooseAvatar(e) {
    const { avatarUrl } = e.detail
    if (!avatarUrl) return
    try {
      const url = await uploadAvatar(avatarUrl, 'avatar')
      this.setData({ avatar: url })
    } catch (err) {
      wx.$toast('上传失败')
    }
  },

  async onSave() {
    const { nickname, avatar, sex } = this.data
    if (!nickname.trim()) {
      wx.$toast('请输入昵称')
      return
    }
    await userUpdateApi({ nickname: nickname.trim(), avatar, sex })
    wx.$toast('保存成功')
    // 更新 store 中的用户信息
    this.getUser()
    setTimeout(() => { wx.$navigateBack() }, 500)
  },
})
