import { storePageBehavior } from '../../../../behaviors/index'
import { decisionDetailApi, recapDetailApi, recapSaveApi } from '../../../../apis/index'

const OTHER = -1 // 最终选择「其他/未选」
const RESULT_MAX = 2000

Page({
  behaviors: [storePageBehavior],
  data: {
    id: null,
    loading: true,
    saving: false,
    OTHER,
    RESULT_MAX,
    title: '',
    options: [],
    finalChoice: null,
    actualResult: '',
    recapId: null,
  },

  async onLoad(options) {
    this.bindStore({ fields: ['system'] })
    await wx.$onLogined
    this.setData({ id: options.id })
    await this.getData()
  },

  async getData() {
    const [detailR, recapR] = await Promise.all([
      decisionDetailApi({ id: this.data.id }),
      recapDetailApi({ decisionId: this.data.id }),
    ])
    const data = detailR.data || {}
    const recap = recapR.data
    this.setData({
      loading: false,
      title: data.title || '',
      options: data.options || [],
      finalChoice: recap ? recap.finalChoice : null,
      actualResult: recap ? (recap.actualResult || '') : '',
      recapId: recap ? recap.id : null,
    })
  },

  onSelectChoice(e) {
    this.setData({ finalChoice: Number(e.currentTarget.dataset.index) })
  },
  onResultInput(e) {
    this.setData({ actualResult: e.detail.value })
  },

  async onSave() {
    if (this.data.finalChoice === null) {
      return wx.$toast('请选择最终选择')
    }
    this.setData({ saving: true })
    try {
      await recapSaveApi({
        decisionId: this.data.id,
        finalChoice: this.data.finalChoice,
        actualResult: this.data.actualResult.trim(),
      })
      wx.$toast(this.data.recapId ? '复盘已更新' : '复盘已记录')
      wx.$navigateBack()
    } finally {
      this.setData({ saving: false })
    }
  },
})
