import { storePageBehavior, paginationBehavior } from '../../../../behaviors/index'
import {
  grpDetailApi,
  grpDissolveApi,
  grpLeaveApi,
  decisionPageGrpApi,
  reviewSubmitApi,
} from '../../../../apis/index'
import {
  GRP_ROLE_ENUM,
  GRP_STATUS_ENUM,
  DECISION_STATUS_ENUM,
  DECISION_SCENE_ENUM,
} from '../../../../common/enums/index'
import {
  decisionDetailUrl,
  decisionListUrl,
  groupMembersUrl,
  inviteLandingUrl,
} from '../../../../common/constants/index'

Page({
  behaviors: [storePageBehavior, paginationBehavior('getData')],
  data: {
    grpId: null,
    loading: true,
    group: {},
    GRP_ROLE_ENUM,
    GRP_STATUS_ENUM,
    pendingAction: null,
    // 就地评议弹窗：当前待评议决策单ID、完整 item、选项数组、预选倾向下标
    showReview: false,
    reviewId: null,
    reviewItem: {},
    reviewOptions: [],
    reviewPreselect: -1,
  },

  async onLoad(options) {
    this.bindStore({ fields: ['user', 'system'] })
    await wx.$onLogined
    this.setData({ grpId: options.id })
    await this.getDetail()
    this.reloadPageData()
  },

  async getDetail() {
    const r = await grpDetailApi({ grpId: this.data.grpId })
    this.setData({ group: r.data || {} })
  },

  async getData(params) {
    // 仅展示群内进行中（未结束）的决策单：固定“评议中”状态，无状态 Tab
    const r = await decisionPageGrpApi({
      ...params,
      grpId: this.data.grpId,
      status: DECISION_STATUS_ENUM.REVIEWING,
    })
    this.pageDataHandler(r.data)
    this.setData({ loading: false })
  },

  onCardTap(e) {
    wx.$navigateTo(decisionDetailUrl(e.detail.id))
  },

  // 卡片选项按钮 → 就地弹评议层，预选中的倾向已带下（myOption=点击下标）
  onReviewFromCard(e) {
    const { id, option, options, item } = e.detail
    this.setData({
      showReview: true,
      reviewId: id,
      reviewItem: item || {},
      reviewOptions: options || [],
      reviewPreselect: option,
    })
  },
  onReviewClose() {
    this.setData({ showReview: false })
  },
  async onReviewSubmit(e) {
    const { option, content } = e.detail
    await reviewSubmitApi({ decisionId: this.data.reviewId, option, content })
    this.setData({ showReview: false })
    wx.$toast('评议已提交')
    this.reloadPageData()
  },

  onMembersTap() {
    wx.$navigateTo(groupMembersUrl(this.data.grpId))
  },

  // ── 群内决策单列表页入口跳转（组件仅回抛点击，页面持有 grpId）──
  onDecisionListTap() {
    wx.$navigateTo(decisionListUrl(DECISION_SCENE_ENUM.GROUP, `&grpId=${ this.data.grpId }`))
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

  // ── 编辑群名（仅群主，弹窗显隐与更新流程内聚于 edit-popup 组件）──
  onEditTap() {
    this.selectComponent('#editPopup').open({ grpId: this.data.grpId, name: this.data.group.name })
  },
  onEditConfirm() {
    this.getDetail()
  },

  // ── 解散 / 退出 ──
  onDissolveTap() {
    this._openConfirm('dissolve', '解散群组', '解散后群内决策单将一并关闭，确认解散？')
  },
  onLeaveTap() {
    this._openConfirm('leave', '退出群组', '确认退出该群组？')
  },
  _openConfirm(action, title, message) {
    this.setData({ pendingAction: action })
    this.selectComponent('#actionConfirmPopup').open({ title, content: message })
  },
  async onConfirmDone() {
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
