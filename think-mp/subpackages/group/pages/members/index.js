import { storePageBehavior, paginationBehavior, sharePageBehavior } from '../../../../behaviors/index'
import {
  grpMembersPageApi,
  grpDetailApi,
  grpRemoveMemberApi,
  grpLeaveApi,
  grpDissolveApi,
} from '../../../../apis/index'
import { GRP_ROLE_ENUM, GRP_STATUS_ENUM } from '../../../../common/enums/index'
import { inviteLandingUrl } from '../../../../common/constants/index'
import dayjs from 'dayjs'

Page({
  behaviors: [storePageBehavior, paginationBehavior('getData'), sharePageBehavior],
  data: {
    grpId: null,
    loading: true,
    group: {},
    myRole: null,
    pendingAction: null,
    GRP_ROLE_ENUM,
    GRP_STATUS_ENUM,
  },

  async onLoad(options) {
    this.bindStore({ fields: ['user', 'system'] })
    await wx.$onLogined
    this.setData({ grpId: options.grpId })
    await this.getGroupInfo()
    this.reloadPageData()
  },

  // 群头部信息：名称 / 成员数 / 我的角色（非分页，仅一次拉取）
  async getGroupInfo() {
    const { data } = await grpDetailApi({ grpId: this.data.grpId })
    this.setData({ group: data || {}, myRole: (data || {}).myRole })
  },

  // 成员分页列表（滚动翻页，分页调度交由 paginationBehavior）
  async getData(params) {
    const r = await grpMembersPageApi({ ...params, grpId: this.data.grpId })
    const page = r.data || {}
    page.list = (page.list || []).map(m => ({
      ...m,
      joinedAt: m.joinedAt ? dayjs(m.joinedAt).format('YYYY-MM-DD') : '',
    }))
    this.pageDataHandler(page)
    this.setData({ loading: false })
  },

  onRemove(e) {
    const { userid, nickname } = e.currentTarget.dataset
    this.pendingRemove = { userid }
    this.selectComponent('#removeConfirmPopup').open({
      title: '移出成员',
      content: `确认将「${nickname || '该成员'}」移出群组？`,
    })
  },

  async onRemoveConfirm() {
    const target = this.pendingRemove
    if (!target) return
    await grpRemoveMemberApi({ grpId: this.data.grpId, memberId: target.userid })
    this.pendingRemove = null
    wx.$toast('已移出成员')
    await this.getGroupInfo()
    this.reloadPageData()
  },

  // ── 邀请入群（弹窗显隐与申请码内聚于 invite-popup 组件）──
  onInviteTap() {
    this.selectComponent('#invitePopup').open({ grpId: this.data.grpId })
  },
  onShareAppMessage() {
    // 转发卡片携带 invite-popup 当前申请到的邀请码
    const invite = this.selectComponent('#invitePopup')
    const code = (invite && invite.data.code) || ''
    return {
      title: `邀请你加入「${ this.data.group.name }」一起三思`,
      path: inviteLandingUrl(code),
    }
  },

  // ── 解散 / 退出 ──
  onDissolveTap() {
    this._openActionConfirm('dissolve', '解散群组', '解散后群内决策单将一并关闭，确认解散？')
  },
  onLeaveTap() {
    this._openActionConfirm('leave', '退出群组', '确认退出该群组？')
  },
  _openActionConfirm(action, title, message) {
    this.setData({ pendingAction: action })
    this.selectComponent('#actionConfirmPopup').open({ title, content: message })
  },
  async onActionConfirm() {
    const action = this.data.pendingAction
    this.setData({ pendingAction: null })
    if (!action) return
    if (action === 'dissolve') {
      await grpDissolveApi({ grpId: this.data.grpId })
      wx.$toast('群组已解散')
    } else if (action === 'leave') {
      await grpLeaveApi({ grpId: this.data.grpId })
      wx.$toast('已退出群组')
    }
    wx.$navigateBack()
  },
})
