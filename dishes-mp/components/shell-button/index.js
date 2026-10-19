import { uploadAvatar } from '../../apis/index'

Component({
  properties: {
    openType: {
      type: String,
      value: ''
    },
    appParameter: {
      type: String,
      value: ''
    },
    disabled: {
      type: Boolean,
      value: false
    },
    custom: {
      type: Boolean,
      value: false
    },
  },
  data: {

  },
  methods: {
    onTap(e) {
      if (!this.properties.openType) {
        this.triggerEvent('on-tap')
      }
    },
    async triggerHandle(e) {
      const { custom, openType } = this.properties
      if (custom) {
        this.triggerEvent(e.type, e, { bubbles: true, composed: true })
        return
      }
      
      let handle = ''
      if (openType === 'getPhoneNumber') handle = 'handleGetPhoneNumber'
      if (openType === 'chooseAvatar') handle = 'handleChooseAvatar'
      if (openType === 'contact') handle = 'handleContact'
      if (openType === 'openSetting') handle = 'handleOpenSetting'
      if (openType === 'getRealtimePhoneNumber') handle = 'handleGetRealtimePhoneNumber'
      if (openType === 'agreePrivacyAuthorization') handle = 'handleAgreePrivacyAuthorization'
      if (openType === 'launchApp') handle = 'handleLaunchApp'
      const r = await this[handle](e)
      this.triggerEvent('on-complete', r)
    },
    async handleGetPhoneNumber(e) {
      const { errMsg, encryptedData, code, iv } = e.detail
      if (errMsg === 'getPhoneNumber:ok') {

      }
    },
    async handleChooseAvatar(e) {
      const { avatarUrl } = e.detail
      return uploadAvatar(avatarUrl)
    },
    handleContact(e) {
      console.log(e.type, e.detail);
    },
    handleOpenSetting(e) {
      return e.detail
    },
    handleGetRealtimePhoneNumber(e) {
      console.log(e.type, e.detail);
    },
    handleAgreePrivacyAuthorization(e) {
      console.log(e.type, e.detail);
    },
    handleLaunchApp(e) {
      console.log(e.type, e.detail);
    },
    handleError(e) {
      console.log(e);
    },
  }
})