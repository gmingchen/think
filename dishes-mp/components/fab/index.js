// 条目占高 = 38 圆钮 + 2 间隙 + 15 文字行，需与 .fab__child 样式保持一致
const MAIN_SIZE = 38
const ITEM_HEIGHT = 55
const ITEM_GAP = 8

Component({
  properties: {
    items: {
      type: Array,
      value: []
    },
    badge: {
      type: Number,
      value: 0
    },
    hint: {
      type: String,
      value: ''
    },
    // 默认锚定边：'right' 靠右（默认）/ 'left' 靠左，仅影响初始位置，拖拽后以当前锚定边为准
    side: {
      type: String,
      value: 'right'
    }
  },

  data: {
    expanded: false,
    bottom: 150,
    anchor: 'right',
    right: 16,
    left: 16,
    positionedItems: []
  },

  lifetimes: {
    attached() {
      this.setData({ anchor: this.properties.side === 'left' ? 'left' : 'right' })
      this._positionItems()
    }
  },

  observers: {
    items() {
      this._positionItems()
    },
    expanded(val) {
      this.triggerEvent('change', { expanded: val })
    }
  },

  methods: {
    _positionItems() {
      const items = this.data.items
      if (!items.length) return
      const positionedItems = items.map((item, i) => ({
        ...item,
        offsetY: -(MAIN_SIZE + ITEM_GAP + i * (ITEM_HEIGHT + ITEM_GAP))
      }))
      this.setData({ positionedItems })
    },

    _startX: 0,
    _startY: 0,
    _startBottom: 0,
    _startEdge: 0,
    _dragged: false,

    onMainTouchStart(e) {
      const touch = e.touches[0]
      this._startX = touch.clientX
      this._startY = touch.clientY
      this._startBottom = this.data.bottom
      this._startEdge = this.data[this.data.anchor]
      this._dragged = false
    },

    onMainTouchMove(e) {
      const touch = e.touches[0]
      const deltaX = touch.clientX - this._startX
      const deltaY = touch.clientY - this._startY
      if (Math.abs(deltaX) > 3 || Math.abs(deltaY) > 3) this._dragged = true
      const sysInfo = wx.getSystemInfoSync()
      const { windowWidth, windowHeight } = sysInfo
      // Y轴：手指下移(clientY增大) → bottom减小
      const patch = { bottom: Math.min(Math.max(40, this._startBottom - deltaY), windowHeight - 60) }
      if (this.data.anchor === 'left') {
        // 左锚定：手指右移(clientX增大) → left增大
        patch.left = Math.min(Math.max(16, this._startEdge + deltaX), windowWidth - 60)
      } else {
        // 右锚定：手指左移(clientX减小) → right增大
        patch.right = Math.min(Math.max(16, this._startEdge - deltaX), windowWidth - 60)
      }
      this.setData(patch)
    },

    onMainTouchEnd() {
      if (!this._dragged) {
        this.setData({ expanded: !this.data.expanded })
      }
    },

    onClose() {
      this.setData({ expanded: false })
    },

    onTap(e) {
      const { value } = e.currentTarget.dataset
      const item = this.data.positionedItems.find(i => i.value === value)
      if (!item || item.closeOnClick !== false) {
        this.setData({ expanded: false })
      }
      this.triggerEvent('action', { value })
    }
  }
})
