Component({
  properties: {
    // 是否显示
    show: {
      type: Boolean,
      value: false,
    },
    // 当前选中日期 yyyy-MM-dd
    date: {
      type: String,
      value: '',
    },
    // 最小可选日期 yyyy-MM-dd（空=不限）
    minDate: {
      type: String,
      value: '',
    },
    // 最大可选日期 yyyy-MM-dd（空=不限）
    maxDate: {
      type: String,
      value: '',
    },
  },

  data: {
    calYear: 0,
    calMonth: 0,
    calLabel: '',
    weeks: [],
    weekDays: ['日', '一', '二', '三', '四', '五', '六'],
    todayStr: '',
  },

  lifetimes: {
    attached() {
      const today = this._formatDate(new Date())
      this.setData({ todayStr: today })
    },
  },

  observers: {
    show(val) {
      if (val) {
        const d = this.properties.date ? this._parseDate(this.properties.date) : new Date()
        this._buildCalendar(d.getFullYear(), d.getMonth())
      }
    },
  },

  methods: {
    onClose() {
      this.triggerEvent('close')
    },

    onPrevMonth() {
      let { calYear, calMonth } = this.data
      calMonth--
      if (calMonth < 0) { calMonth = 11; calYear-- }
      // 检查是否超出最小范围
      if (this._isBeforeMonth(calYear, calMonth, this._getMinYear(), this._getMinMonth())) return
      this._buildCalendar(calYear, calMonth)
    },

    onNextMonth() {
      let { calYear, calMonth } = this.data
      calMonth++
      if (calMonth > 11) { calMonth = 0; calYear++ }
      // 检查是否超出最大范围
      if (this._isAfterMonth(calYear, calMonth, this._getMaxYear(), this._getMaxMonth())) return
      this._buildCalendar(calYear, calMonth)
    },

    onPickDay(e) {
      const { day } = e.currentTarget.dataset
      if (!day) return
      // 检查是否在可选范围内
      const { minDate, maxDate } = this.properties
      if (minDate && day < minDate) return
      if (maxDate && day > maxDate) return
      this.triggerEvent('confirm', { date: day })
    },

    _buildCalendar(year, month) {
      const monthNames = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']
      const calLabel = `${year}年${monthNames[month]}`

      const firstDay = new Date(year, month, 1)
      const startDow = firstDay.getDay()
      const daysInMonth = new Date(year, month + 1, 0).getDate()

      const { todayStr, date: selectedDay } = this.data
      const days = []
      for (let i = 0; i < startDow; i++) {
        days.push({ day: '', dateStr: '' })
      }
      for (let d = 1; d <= daysInMonth; d++) {
        const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`
        const { minDate, maxDate } = this.properties
        const isDisabled = (minDate && dateStr < minDate) || (maxDate && dateStr > maxDate)
        days.push({
          day: d,
          dateStr,
          isToday: dateStr === todayStr,
          isSelected: dateStr === selectedDay,
          isDisabled,
        })
      }

      const weeks = []
      let week = []
      days.forEach((item, i) => {
        week.push(item)
        if (week.length === 7 || i === days.length - 1) {
          while (week.length < 7) week.push({ day: '', dateStr: '' })
          weeks.push(week)
          week = []
        }
      })

      // 月份导航边界
      const { minDate, maxDate } = this.properties
      const canPrev = !minDate || !this._isBeforeOrSameMonth(year, month, this._getMinYear(), this._getMinMonth())
      const canNext = !maxDate || !this._isAfterOrSameMonth(year, month, this._getMaxYear(), this._getMaxMonth())

      this.setData({ calYear: year, calMonth: month, calLabel, weeks, canPrev, canNext })
    },

    _formatDate(d) {
      const y = d.getFullYear()
      const m = String(d.getMonth() + 1).padStart(2, '0')
      const dd = String(d.getDate()).padStart(2, '0')
      return `${y}-${m}-${dd}`
    },

    _parseDate(str) {
      const [y, m, d] = str.split('-').map(Number)
      return new Date(y, m - 1, d)
    },

    // 范围辅助方法
    _getMinYear() { return this.properties.minDate ? parseInt(this.properties.minDate) : 0 },
    _getMinMonth() { return this.properties.minDate ? parseInt(this.properties.minDate.split('-')[1]) - 1 : 0 },
    _getMaxYear() { return this.properties.maxDate ? parseInt(this.properties.maxDate) : 9999 },
    _getMaxMonth() { return this.properties.maxDate ? parseInt(this.properties.maxDate.split('-')[1]) - 1 : 11 },
    _isBeforeMonth(y, m, ty, tm) { return y < ty || (y === ty && m < tm) },
    _isAfterMonth(y, m, ty, tm) { return y > ty || (y === ty && m > tm) },
    _isBeforeOrSameMonth(y, m, ty, tm) { return y < ty || (y === ty && m <= tm) },
    _isAfterOrSameMonth(y, m, ty, tm) { return y > ty || (y === ty && m >= tm) },

    stopPropagation() {},
  },
})
