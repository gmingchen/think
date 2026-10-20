Component({
  properties: {
    // FreshDish：name / categoryHint / hint / foods / seasonings / steps / adopted
    fresh: {
      type: Object,
      value: null,
      observer(val) {
        if (!val) return
        this.setData({
          foodText: (val.foods || []).map(f => `${f.name} ${f.amount || ''}${f.note ? '（' + f.note + '）' : ''}`.trim()).join('、'),
          seasoningText: (val.seasonings || []).map(s => `${s.name} ${s.amount || ''}`.trim()).join('、'),
        })
      },
    },
    index: {
      type: Number,
      value: -1,
    },
  },

  data: {
    expanded: false,
    // 折叠态仅展示前 2 步摘要，点开展示全文
    previewCount: 2,
    foodText: '',
    seasoningText: '',
  },

  methods: {
    // 点整张卡：仅已采纳时展示入库后的菜谱详情弹窗（未采纳时点空白区不误触）
    onCardTap() {
      const fresh = this.data.fresh
      if (fresh && fresh.adopted) {
        this.triggerEvent('detail', { dishId: fresh.dishId })
      }
    },
    onToggleSteps() {
      this.setData({ expanded: !this.data.expanded })
    },
    onAdopt() {
      const fresh = this.data.fresh
      if (!fresh) return
      // 已采纳：点击展示入库后的菜谱详情弹窗
      if (fresh.adopted) {
        this.triggerEvent('detail', { dishId: fresh.dishId })
        return
      }
      this.triggerEvent('adopt', { index: this.data.index })
    },
  },
})
