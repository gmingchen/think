export const paginationBehavior = (fn) => Behavior({
  data: {
    list: [],
    page: {
      current: 0,
      size: 10,
      total: 0
    }
  },
  methods: {
    pageDataHandler(r) {
      if (r) {
        const { current, list: records, total } = r
        const list = current === 1 ? records : [...this.data.list, ...records]
        this.setData({
          list: list,
          'page.total': total
        })
      }
    },
    async getPageData() {
      const { current, size } = this.data.page
      const params = { 
        current,
        size
      }
      const r = await this[fn](params)
      this.pageDataHandler(r)
    },
    loadPageData() {
      const { list, page: { current, total } } = this.data
      if (!current || list.length < total) {
        this.setData({ 'page.current': current + 1 })
        this.getPageData()
      }
    },
    reloadPageData() {
      this.setData({ 'page.current': 0 })
      this.loadPageData()
    }
  },
})