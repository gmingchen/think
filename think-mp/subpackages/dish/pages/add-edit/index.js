import { storePageBehavior, sharePageBehavior } from '../../../../behaviors/index'
import { dishInfoApi, dishCreateApi, dishUpdateApi, categoryListApi, aiParseQuotaApi, aiImageQuotaApi, kitchenExpandDishApi } from '../../../../apis/index'
import { playExpandAd, canExpandByAd } from '../../../../utils/index'
import { AD_UNIT_ADD_EDIT_BANNER, AI_PARSE_DAILY_LIMIT, AI_IMAGE_DAILY_LIMIT, LIMIT_REACHED_CODE } from '../../../../common/constants/index'

Page({
  behaviors: [storePageBehavior, sharePageBehavior],

  data: {
    AD_UNIT_ADD_EDIT_BANNER,
    // 扩容广告进行中（防重复发放）
    expanding: false,
    // 表单数据
    form: {
      id: null,
      name: '',
      categoryId: '',
      categoryName: '',
      photo: '',
      images: [],
      ingredients: [],
      seasonings: [],
      steps: [],
      note: '',
    },

    // 弹窗控制
    showCategoryPicker: false,
    showFoodPicker: false,
    showSeasoningPicker: false,

    // 步骤录入：弹窗式（新增/编辑单条），拖拽排序

    // 步骤拖拽排序
    dragIndex: -1,
    dragOffsetY: 0,
    dragStartY: 0,
    dragHeight: 0,
    dragCenters: [],
    targetIndex: -1,
    dragOffsets: [],

    // 已选中的ID列表（供 picker 组件复显）
    selectedFoodIds: [],
    selectedSeasoningIds: [],

    // 拍照提示
    photoTip: '俯拍 / 45°侧拍效果最佳',

    // 封面上传配置（供 uploader 透传给 uploadMedia：仅图片 + dish 存储前缀）
    coverOptions: { mediaTypes: ['image'], prefix: 'dish' },

    // 详情图上传配置（与封面同：仅图片 + dish 存储前缀，上限 3 张）
    detailOptions: { mediaTypes: ['image'], prefix: 'dish' },

    // 今日剩余 AI 文案识别次数：进页面拉后端回显，parse 成功后本地 -1（后端为准）
    parseRemainCount: AI_PARSE_DAILY_LIMIT,

    // 今日剩余 AI 封面生图次数：同上，生图成功后本地 -1
    imageRemainCount: AI_IMAGE_DAILY_LIMIT,
  },

  async onLoad(options) {
    this.bindStore({ fields: ['user', 'kitchen', 'ad'], actions: ['getKitchen'] })

    await wx.$onKitchenLoaded
    if (options.id) {
      this.setData({ 'form.id': options.id })
      await this._loadDishInfo(options.id)
    }
    // 额度回显不阻塞表单编辑，异步拉一次，拉失则保持本地默认
    this._loadParseQuota()
    this._loadImageQuota()
  },

  async _loadParseQuota() {
    try {
      const r = await aiParseQuotaApi()
      if (r.data != null) this.setData({ parseRemainCount: r.data })
    } catch (e) {
      // 拉取失败 service 已统一 toast，不阻塞页面使用
    }
  },

  async _loadImageQuota() {
    try {
      const r = await aiImageQuotaApi()
      if (r.data != null) this.setData({ imageRemainCount: r.data })
    } catch (e) {
      // 同上，拉取失败不阻塞页面
    }
  },

  async _loadDishInfo(id) {
    wx.showLoading({ title: '加载中...' })
    try {
      const r = await dishInfoApi({ id })
      const dish = r.data
      if (!dish) return
      // 查找分类名称
      let categoryName = ''
      if (dish.categoryId) {
        const catRes = await categoryListApi()
        const cat = (catRes.data || []).find(c => c.id === dish.categoryId)
        categoryName = cat ? cat.name : ''
      }
      const ingredients = (dish.foods || []).map(item => ({
        id: item.foodId,
        name: item.foodName,
        amount: item.amount || '',
        process: item.process || '',
        isOptional: item.isOptional || 0,
      }))
      const seasonings = (dish.seasonings || []).map(item => ({
        id: item.seasoningId,
        name: item.name,
        amount: item.amount || '',
        isOptional: item.isOptional || 0,
      }))
      this.setData({
        'form.id': dish.id,
        'form.name': dish.name,
        'form.categoryId': dish.categoryId || '',
        'form.categoryName': categoryName,
        'form.photo': dish.cover || '',
        'form.images': dish.images || [],
        'form.note': dish.remark || '',
        'form.ingredients': ingredients,
        'form.seasonings': seasonings,
        'form.steps': (dish.steps || []).map(item => ({
          text: item.description || '',
        })),
        selectedFoodIds: ingredients.map(i => i.id),
        selectedSeasoningIds: seasonings.map(i => i.id),
      })
    } finally {
      wx.hideLoading()
    }
  },

  // ── 表单输入 ──
  onNameInput(e) {
    this.setData({ 'form.name': e.detail.value })
  },

  // ── 封面图 ──
  // uploader 上传完成：单选取首图 URL
  onCoverComplete(e) {
    const res = e.detail
    const url = Array.isArray(res) ? (res[0] && res[0].url) : (res && res.url)
    if (url) {
      this.setData({ 'form.photo': url })
    }
  },
  // uploader 删除：清空封面
  onCoverDelete() {
    this.setData({ 'form.photo': '' })
  },

  // ── 详情图 ──
  // uploader 上传完成：将新增的 URL 追加到 form.images，超出 3 张截断并提示（后端 @Size(max=3) 兼作兑底）
  onDetailComplete(e) {
    const rows = Array.isArray(e.detail) ? e.detail : [e.detail]
    const urls = rows.map(row => row && row.url).filter(Boolean)
    if (!urls.length) return
    let images = this.data.form.images.concat(urls)
    if (images.length > 3) {
      images = images.slice(0, 3)
      wx.$toast('详情图最多 3 张')
    }
    this.setData({ 'form.images': images })
  },
  // uploader 删除：按索引从 form.images 移除（仅解除引用，不删存储文件，沿用 cover 现状）
  onDetailDelete(e) {
    const { index } = e.detail
    const images = [...this.data.form.images]
    images.splice(index, 1)
    this.setData({ 'form.images': images })
  },

  // ── 分类选择 ──
  onOpenCategoryPicker() {
    this.setData({ showCategoryPicker: true })
  },
  onCloseCategoryPicker() {
    this.setData({ showCategoryPicker: false })
  },
  onCategoryConfirm(e) {
    const { id, name } = e.detail
    this.setData({
      'form.categoryId': id,
      'form.categoryName': name,
      showCategoryPicker: false,
    })
  },

  // ── 食材 ──
  onOpenFoodPicker() {
    this.setData({ showFoodPicker: true })
  },
  onCloseFoodPicker() {
    this.setData({ showFoodPicker: false })
  },
  onFoodConfirm(e) {
    const { items } = e.detail
    // 弹窗确认为准：未勾选的移除，已存在的保留原位与用量，新勾选的按点选顺序追加
    const confirmIds = items.map(item => item.id)
    const ingredients = this.data.form.ingredients.filter(i => confirmIds.includes(i.id))
    items.forEach(({ id, name }) => {
      if (!name || ingredients.some(i => i.id === id)) return
      ingredients.push({ id, name, amount: '', process: '', isOptional: 0 })
    })
    const selectedFoodIds = ingredients.map(i => i.id)
    this.setData({ 'form.ingredients': ingredients, selectedFoodIds })
  },
  onRemoveFood(e) {
    const { index } = e.currentTarget.dataset
    const ingredients = [...this.data.form.ingredients]
    ingredients.splice(index, 1)
    const selectedFoodIds = ingredients.map(i => i.id)
    this.setData({ 'form.ingredients': ingredients, selectedFoodIds })
  },
  onFoodInput(e) {
    const { index, field } = e.currentTarget.dataset
    this.setData({ [`form.ingredients[${index}].${field}`]: e.detail.value })
  },
  // 切换食材「可选」（is_optional=1 → 采购清单归入可不买组），后端未传时落默认 0
  onFoodOptionalToggle(e) {
    const { index } = e.currentTarget.dataset
    const item = this.data.form.ingredients[index]
    if (!item) return
    this.setData({ [`form.ingredients[${index}].isOptional`]: item.isOptional ? 0 : 1 })
  },

  // ── 调料 ──
  onOpenSeasoningPicker() {
    this.setData({ showSeasoningPicker: true })
  },
  onCloseSeasoningPicker() {
    this.setData({ showSeasoningPicker: false })
  },
  onSeasoningConfirm(e) {
    const { items } = e.detail
    // 弹窗确认为准：未勾选的移除，已存在的保留原位与用量，新勾选的按点选顺序追加
    const confirmIds = items.map(item => item.id)
    const seasonings = this.data.form.seasonings.filter(i => confirmIds.includes(i.id))
    items.forEach(({ id, name }) => {
      if (!name || seasonings.some(i => i.id === id)) return
      seasonings.push({ id, name, amount: '', isOptional: 0 })
    })
    const selectedSeasoningIds = seasonings.map(i => i.id)
    this.setData({ 'form.seasonings': seasonings, selectedSeasoningIds })
  },
  onRemoveSeasoning(e) {
    const { index } = e.currentTarget.dataset
    const seasonings = [...this.data.form.seasonings]
    seasonings.splice(index, 1)
    const selectedSeasoningIds = seasonings.map(i => i.id)
    this.setData({ 'form.seasonings': seasonings, selectedSeasoningIds })
  },
  onSeasoningInput(e) {
    const { index, field } = e.currentTarget.dataset
    this.setData({ [`form.seasonings[${index}].${field}`]: e.detail.value })
  },
  // 切换调料「可选」，同上
  onSeasoningOptionalToggle(e) {
    const { index } = e.currentTarget.dataset
    const item = this.data.form.seasonings[index]
    if (!item) return
    this.setData({ [`form.seasonings[${index}].isOptional`]: item.isOptional ? 0 : 1 })
  },

  // ── 步骤（弹窗录入）──
  // 新增：打开弹窗录入一条，弹窗内可选「确认」关闭或「确认并继续」连录
  onAddStep() {
    this.selectComponent('#stepPopup').open('', -1)
  },
  // 点击已有步骤：打开弹窗编辑该条
  onEditStep(e) {
    const { index } = e.currentTarget.dataset
    const step = this.data.form.steps[index]
    this.selectComponent('#stepPopup').open(step ? step.text : '', index)
  },
  // 弹窗确认：index >= 0 更新该条，否则追加
  onStepConfirm(e) {
    const { text, index } = e.detail
    const steps = [...this.data.form.steps]
    if (index >= 0) {
      steps.splice(index, 1, { text })
    } else {
      steps.push({ text })
    }
    this.setData({ 'form.steps': steps })
  },
  onRemoveStep(e) {
    this._removeStepAt(e.currentTarget.dataset.index)
  },
  _removeStepAt(index) {
    const steps = [...this.data.form.steps]
    steps.splice(index, 1)
    this.setData({ 'form.steps': steps })
  },

  // ── 步骤批量导入 ──
  onOpenStepImport() {
    this.selectComponent('#stepImportPopup').open()
  },
  onStepImportConfirm(e) {
    const imported = (e.detail.steps || []).map(text => ({ text }))
    if (!imported.length) return
    this.setData({ 'form.steps': this.data.form.steps.concat(imported) })
    wx.$toast(`已导入 ${imported.length} 步`)
  },

  // ── 步骤拖拽排序 ──
  onStepDragStart(e) {
    const index = e.currentTarget.dataset.index
    const startY = e.touches[0].clientY
    const count = this.data.form.steps.length
    if (index < 0 || index >= count) return
    const query = wx.createSelectorQuery()
    query.selectAll('.add-edit__step-item').boundingClientRect()
    query.exec(rects => {
      // 只取步骤行（草稿行类名已区分），slice 作为多测到行的兜底
      const items = (rects[0] || []).slice(0, count)
      if (!items[index]) return
      this.setData({
        dragIndex: index,
        targetIndex: index,
        dragStartY: startY,
        dragOffsetY: 0,
        // 步骤条目高度不固定，用被拖条目高度做其他条目的位移量，用各条目中心点判定落点
        dragHeight: items[index].height,
        dragCenters: items.map(i => i.top + i.height / 2),
        dragOffsets: new Array(count).fill(0),
      })
    })
  },

  onStepDragMove(e) {
    const { dragIndex, dragStartY, dragHeight, dragCenters } = this.data
    if (dragIndex < 0 || !dragHeight) return
    const offsetY = e.touches[0].clientY - dragStartY
    const movedCenter = dragCenters[dragIndex] + offsetY
    const len = dragCenters.length
    let target = 0
    for (let i = 0; i < len; i++) {
      if (i !== dragIndex && dragCenters[i] < movedCenter) target++
    }
    const offsets = new Array(len).fill(0)
    for (let i = 0; i < len; i++) {
      if (i === dragIndex) continue
      if (dragIndex < target && i > dragIndex && i <= target) {
        offsets[i] = -dragHeight
      } else if (dragIndex > target && i >= target && i < dragIndex) {
        offsets[i] = dragHeight
      }
    }
    this.setData({ dragOffsetY: offsetY, targetIndex: target, dragOffsets: offsets })
  },

  onStepDragEnd() {
    const { dragIndex, targetIndex, form } = this.data
    if (dragIndex < 0) return
    // 位移清理与列表重排放在同一次 setData，避免闪帧
    const reset = { dragIndex: -1, dragOffsetY: 0, targetIndex: -1, dragOffsets: [], dragCenters: [], dragHeight: 0 }
    if (dragIndex === targetIndex) {
      this.setData(reset)
      return
    }
    const steps = [...form.steps]
    const [item] = steps.splice(dragIndex, 1)
    steps.splice(targetIndex, 0, item)
    this.setData({ ...reset, 'form.steps': steps })
  },

  // ── 备注 ──
  onNoteInput(e) {
    this.setData({ 'form.note': e.detail.value })
  },

  // ── AI 文案识别 ──
  onOpenAiParse() {
    const kitchenId = this.data.kitchen && this.data.kitchen.active && this.data.kitchen.active.id
    if (!kitchenId) {
      wx.$toast('请先选择厨房')
      return
    }
    this.selectComponent('#aiParsePopup').open()
  },
  // 弹窗 commit 确认回传带 id 结构：全量覆盖文本字段（图片/详情图/编辑态 id 不动）
  onParseConfirm(e) {
    this._applyParseResult(e.detail.data)
  },
  // 弹窗 parse 成功：本地额度 -1（下限 0），后端为准；后端先自增后调 LLM，失败也已扣额，下一轮重进页面自然回正
  onAiParsed() {
    this.setData({ parseRemainCount: Math.max(0, this.data.parseRemainCount - 1) })
  },

  // ── AI 封面生图 ──
  // 前置守卫后把当前表单快照传入弹窗，弹窗打开即自动生图（不需用户再点确认）
  onOpenAiCover() {
    const kitchenId = this.data.kitchen && this.data.kitchen.active && this.data.kitchen.active.id
    if (!kitchenId) {
      wx.$toast('请先选择厨房')
      return
    }
    const { form } = this.data
    if (!form.name) {
      wx.$toast('请先填写菜名')
      return
    }
    if (this.data.imageRemainCount <= 0) {
      wx.$toast('今日 AI 生图次数已用完，明天再来')
      return
    }
    this.selectComponent('#aiCoverPopup').open({
      name: form.name,
      categoryHint: form.categoryName,
      foods: (form.ingredients || []).map((row) => ({ name: row.name, amount: row.amount })),
      seasonings: (form.seasonings || []).map((row) => ({ name: row.name, amount: row.amount })),
      steps: (form.steps || []).map((row) => row.text),
    })
  },
  // 弹窗生图成功：本地额度 -1（后端失败已回退计数，所以下发 consumed 才扣）
  onAiImageConsumed() {
    this.setData({ imageRemainCount: Math.max(0, this.data.imageRemainCount - 1) })
  },
  // 确认使用这张：弹窗已上传七牛回传 URL，仅回填封面，菜谱仍需手动保存
  onAiCoverConfirm(e) {
    this.setData({ 'form.photo': e.detail.url })
    wx.$toast('封面已应用')
  },
  _applyParseResult(data) {
    if (!data) return
    const ingredients = (data.foods || []).map(item => ({
      id: item.foodId,
      name: item.name,
      amount: item.amount || '',
      process: item.process || '',
      isOptional: item.isOptional || 0,
    }))
    const seasonings = (data.seasonings || []).map(item => ({
      id: item.seasoningId,
      name: item.name,
      amount: item.amount || '',
      isOptional: item.isOptional || 0,
    }))
    const steps = (data.steps || []).filter(text => !!text).map(text => ({ text }))
    this.setData({
      'form.name': data.name || '',
      'form.categoryId': data.categoryId || '',
      'form.categoryName': data.categoryName || '',
      'form.ingredients': ingredients,
      'form.seasonings': seasonings,
      'form.steps': steps,
      'form.note': data.remark || '',
      selectedFoodIds: ingredients.map(i => i.id),
      selectedSeasoningIds: seasonings.map(i => i.id),
    })
    wx.$toast('识别好啦，记得核对下～')
  },

  // ── 保存 ──
  async onSaveAndContinue() {
    if (!this._validateForm()) return
    this._expandRetry = () => this.onSaveAndContinue()
    try {
      await this._submitForm()
      wx.$toast('保存成功，继续添加')
      this._resetForm()
    } catch (err) {
      this._handleSaveError(err)
    }
  },

  async onSaveAndBack() {
    if (!this._validateForm()) return
    this._expandRetry = () => this.onSaveAndBack()
    try {
      await this._submitForm()
      wx.$toast('保存成功')
      wx.$navigateBack()
    } catch (err) {
      this._handleSaveError(err)
    }
  },

  // 保存失败：命中 4005 且当前身份/开关允许扩容时，弹「看视频 +1」引导；否则按普通错误处理
  _handleSaveError(err) {
    if (err && err.code === LIMIT_REACHED_CODE) {
      if (this._canExpand()) {
        this.selectComponent('#limitPopup').open({
          title: '菜谱数已达上限',
          content: '看个小视频即可 +1，不限次数～或删除部分旧内容后可继续添加',
          confirmText: '📺 看视频 +1',
          cancelText: '以后再说',
          autoClose: false,
        })
        return
      }
      wx.$toast(err.message || '菜谱数已达上限')
      return
    }
    wx.$print('保存失败', err)
  },

  // 仅厨师长/管厨且可扩容时才可看视频（食客不发奖）
  // ⚠️ 入参取 active 厨房的 KitchenVO.unlimitedCapacity（厨房级）而不是 store.member 的同名位（人级），
  //    否则畅厨在别人厨房当管厨时会连同这条看视频扩容的路径一起被关掉（§1.4 决策 1）
  _canExpand() {
    const active = this.data.kitchen?.active
    return (active?.role === 1 || active?.role === 2) && canExpandByAd(active?.unlimitedCapacity)
  },

  // confirm-popup 确认：拉起激励视频，完整观看后 +1 并自动重试保存
  onExpandConfirm() {
    if (this.data.expanding) return
    this.setData({ expanding: true })
    playExpandAd('dish', async ({ ended }) => {
      this.setData({ expanding: false })
      // 广告结束（看完/中途关/加载失败）后才关闭达上限引导弹窗（点击确认时不立即关）
      const limitPopup = this.selectComponent('#limitPopup')
      limitPopup && limitPopup.close()
      if (!ended) return
      try {
        await kitchenExpandDishApi({ kitchenId: this.data.kitchen.active.id })
        await this.getKitchen()
        wx.$toast('已扩容 +1')
        this._expandRetry && this._expandRetry()
      } catch (e) {
        // 扩容接口异常（非4005）service 已提示，此处静默
      }
    })
  },

  _buildParams() {
    const { form } = this.data
    const params = {
      name: form.name,
      categoryId: form.categoryId || null,
      cover: form.photo || null,
      images: form.images || [],
      remark: form.note || null,
      foods: form.ingredients.map((item, index) => ({
        foodId: item.id,
        amount: item.amount || null,
        process: item.process || null,
        isOptional: item.isOptional || 0,
        sort: index,
      })),
      seasonings: form.seasonings.map((item, index) => ({
        seasoningId: item.id,
        amount: item.amount || null,
        isOptional: item.isOptional || 0,
        sort: index,
      })),
      steps: form.steps
        .filter(item => !!item.text.trim())
        .map((item, index) => ({
          description: item.text.trim(),
          sort: index,
        })),
    }
    if (form.id) {
      params.id = form.id
    } else {
      params.kitchenId = this.data.kitchen.active.id
    }
    return params
  },

  async _submitForm() {
    const params = this._buildParams()
    if (this.data.form.id) {
      await dishUpdateApi(params)
    } else {
      await dishCreateApi(params)
    }
  },

  _validateForm() {
    if (!this.data.form.name.trim()) {
      wx.$toast('请输入菜名')
      return false
    }
    return true
  },

  _resetForm() {
    this.setData({
      form: {
        id: null,
        name: '',
        categoryId: '',
        categoryName: '',
        photo: '',
        images: [],
        ingredients: [],
        seasonings: [],
        steps: [],
        note: '',
      },
      selectedFoodIds: [],
      selectedSeasoningIds: [],
    })
  },
})
