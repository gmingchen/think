import { DEFAULT_DISH_COVER } from '../../../../../../common/constants/index'
import slotAudio from './slot-audio'

const ITEM_H = 168 // 滚轮单格高度 rpx，对应 .slot__item 高度
const REPEATS = 4  // 滚轮重复组数，保证连续旋转
const MAX_DRAW = 3 // 最大抽取数量（即最大列数）
const MIN_SPINS = 2 // 每次摇动至少向前转的整圈数
const CENTER_ADJUST = 0 // 落点居中补偿 rpx：viewport 内容区 168rpx = item 168rpx，无需补偿
const SPIN_DURATION = 2000 // 滚轮 transition 基础时长 ms（第 0 列）
const STAGGER = 500 // 逐列时长递增间隔 ms：第 i 列时长 = SPIN_DURATION + i*STAGGER，结束时刻左→右
const SETTLE_FALLBACK = 500 // 停轮兜底缓冲 ms：transitionend 漏触发时强制归位
const RENDER_GAP = 100 // 跨步提交真实延时缓冲 ms：setData 回调可能早于视图层样式生效，防批合并

// setData 回调可能早于视图层样式独立成帧，跨帧提交用真实延时双保险
const sleep = ms => new Promise(resolve => setTimeout(resolve, ms))

Component({
  properties: {
    // 菜品数据 [{ id, name, photo, ... }]，由页面传入
    data: {
      type: Array,
      value: [],
      observer(dishes) {
        this._buildReel(dishes || [])
      },
    },
  },

  data: {
    DEFAULT_DISH_COVER,
    drawCount: 3,
    maxCount: MAX_DRAW,
    spinning: false,
    leverPulled: false,
    reelList: [],
    // 每列滚轮状态：offset/duration/anim(transition 开关)/blur(运动模糊)，列数 = 抽取数量
    cols: [],
  },

  lifetimes: {
    detached() {
      this._destroyed = true
      this._stopTick()
    },
  },

  methods: {
    // 量取真实单项高度（DOM 渲染后调用），消除 rpx→px 浮点舍入偏差
    _measureItemHeight() {
      return new Promise((resolve) => {
        const query = this.createSelectorQuery()
        query.select('.slot__item').boundingClientRect((rect) => {
          if (rect && rect.height) {
            this._realItemH = rect.height
            resolve(rect.height)
          } else {
            const { windowWidth } = wx.getSystemInfoSync()
            this._realItemH = (168 * windowWidth) / 750
            resolve(this._realItemH)
          }
        }).exec()
      })
    },

    // 构建滚轮列表：重复多组保证连续旋转
    _buildReel(dishes) {
      const reelList = []
      for (let r = 0; r < REPEATS; r++) {
        dishes.forEach(d => reelList.push({ ...d, _key: `${r}_${d.id}` }))
      }
      const maxCount = Math.min(dishes.length, MAX_DRAW)
      const drawCount = Math.min(this.data.drawCount, maxCount) || 1
      this.setData({ reelList, maxCount, drawCount }, () => {
        // 确保 DOM 渲染后量取真实单项高度
        this._measureItemHeight()
      })
      // 菜品集变更后重建列，停靠索引归零
      this._resetCols(drawCount)
    },

    // 按抽取数量重建滚轮列（数量变更/菜品集变更时调用，停靠索引归零）
    _resetCols(count) {
      const cols = []
      for (let i = 0; i < count; i++) {
        // 各列同时开始，不同时长：第 i 列时长递增，结束时刻严格左→右
        cols.push({ offset: 0, duration: SPIN_DURATION + i * STAGGER, anim: false, blur: false })
      }
      this._landIndexes = new Array(count).fill(0)
      this.setData({ cols })
    },

    // 点击拉杆 / 开火按钮：只通知页面开始摇，结果由页面通过 spin() 回传
    onDraw() {
      if (!this.properties.data.length || this.data.spinning) return
      // 音效上下文必须在点击手势上下文中创建（iOS 限制），幂等
      slotAudio.init()
      this.triggerEvent('start', { count: this.data.drawCount })
    },

    /**
     * 页面抽好菜后调用：每列滚到结果列表对应的一道菜（value 与列一一对应），最后一列停稳后触发 change
     * 单向滚动保证：落点索引只从当前停靠位向深处累加，动画位移永远同向；
     * 停稳后趁 transition:none 回移整数圈归位（内容每 N 项重复，肉眼不可见）
     * @param {Array} options.value 结果列表，每项为该列落点菜品
     */
    spin({ value = [] } = {}) {
      const dishes = this.properties.data
      if (this._destroyed || this.data.spinning || !dishes.length || !value.length) return
      if (this.data.cols.length !== value.length) this._resetCols(value.length)
    
      this._result = value
      this._pullLever()
    
      const poolSize = dishes.length
    
      // 预归位（每次 spin 无条件执行）：所有列先无动画瞬移回浅位 norm。
      // 滚轮内容每 poolSize 项重复，norm 与 base 显示完全相同，跳转视觉无痕；
      // 这样停靠位深度清零，锁定列也能保证本轮至少滚 MIN_SPINS 圈，不会“原地不动”
      const preResetUpdates = { spinning: true }
      const norms = []
      this.data.cols.forEach((col, i) => {
        const base = this._landIndexes[i] || 0
        const norm = base % poolSize
        norms.push(norm)
        this._landIndexes[i] = norm
        preResetUpdates[`cols[${i}].anim`] = false
        preResetUpdates[`cols[${i}].offset`] = -(norm * (this._realItemH || ITEM_H))
      })
      // 记录浅位序列，供预归位视图层校验使用
      this._normList = norms
    
      // 停靠位已清零到 [0, N) 浅位，落点 = 第一个 ≡ targetIndex (mod N) 且至少领先 MIN_SPINS 圈的位置，
      // 最大 land = (poolSize-1) + (MIN_SPINS+1)*N - 1 < poolSize*REPEATS，不会超出滚轮总长
      const lands = value.map((dish, i) => {
        const targetIndex = Math.max(dishes.findIndex(d => d.id === dish.id), 0)
        const base = this._landIndexes[i] || 0
        let land = targetIndex
        while (land < base + MIN_SPINS * poolSize) land += poolSize
        return land
      })
    
      // 记录本轮落点，供 transitionend 停稳时使用；
      // 轮次代际标记：旧轮遗留的兜底定时器/滞后的 transitionend 不得影响新一轮
      // 本轮起始时间戳：供停轮做时长下界防伪（虚假 transitionend 提前判停）
      this._lands = lands
      this._settled = new Array(lands.length).fill(false)
      this._spinTs = Date.now()
      const gen = this._gen = (this._gen || 0) + 1
    
      // 三步提交，每步“setData 回调 + 真实延时”双保险确保独立渲染到视图层：
      // iOS 上纯回调链可能被批合并吞掉预归位，动画从上一轮深位起步 → 短距离滚动甚至反向滚动
      // A：预归位（anim:false + 浅位 offset，无动画），同步置 spinning：
      //    data 立即生效，堵住 spin 调用到回调执行之间重复触发 spin 的竞态窗口
      // A2：实测视图层各列位置校验归位已生效，未生效则重发（_ensureSnapped）
      // B：所有列同时开启 transition + 运动模糊
      // C：所有列同时提交落点 offset（各列时长不同，结束自然错峰左→右）
      const startSpin = async () => {
        await sleep(RENDER_GAP)
        if (this._destroyed) return
        await this._ensureSnapped()
        if (this._destroyed) return
        const startUpdates = {}
        lands.forEach((_, i) => {
          startUpdates[`cols[${i}].anim`] = true
          startUpdates[`cols[${i}].blur`] = true
        })
        this.setData(startUpdates, async () => {
          await sleep(RENDER_GAP)
          if (this._destroyed) return
          const offsetUpdates = {}
          lands.forEach((land, i) => {
            offsetUpdates[`cols[${i}].offset`] = -(land * (this._realItemH || ITEM_H))
          })
          this.setData(offsetUpdates)
    
          // 兜底定时器从落点真实提交时刻起算：帧校验重试会推迟动画起步，
          // 若仍从 spin 入口起算，兜底可能在动画未完时抢跑判停（表现为瞬停）
          lands.forEach((_, i) => {
            setTimeout(() => this._settleColumn(i, gen), SPIN_DURATION + i * STAGGER + SETTLE_FALLBACK)
          })
        })
      }
    
      // 先提交预归位（无动画），确认渲染到视图层后再开启动画，避免样式批合并跳过 transition
      this.setData(preResetUpdates, startSpin)
    },
    
    // 实测校验视图层各列确实停在浅位 norm：预归位若被批合并吞掉，
    // 动画会从上一轮深位起步（短滚/反滚），这里重发归位并复检，最多重试两轮
    async _ensureSnapped() {
      const H = this._realItemH || ITEM_H
      const norms = this._normList || []
      for (let round = 0; round < 2; round++) {
        const ok = await new Promise(resolve => {
          const query = this.createSelectorQuery()
          query.select('.slot__viewport').boundingClientRect()
          query.selectAll('.slot__reel').boundingClientRect()
          query.exec(res => {
            const vp = res && res[0]
            const rects = res && res[1]
            // 查询异常不阻塞流程，误差由时长下界防伪与兜底定时器兜底
            if (!vp || !Array.isArray(rects) || rects.length !== norms.length) {
              resolve(true)
              return
            }
            // reel top = 视窗 top - norm*H（容差半格，覆盖边框与取整误差）
            resolve(norms.every((norm, i) => Math.abs(vp.top - rects[i].top - norm * H) < H * 0.5))
          })
        })
        if (ok || this._destroyed) return
        const updates = {}
        norms.forEach((norm, i) => {
          updates[`cols[${i}].anim`] = false
          updates[`cols[${i}].offset`] = -(norm * H)
        })
        this.setData(updates)
        await sleep(RENDER_GAP * 2)
      }
    },

    // 某列滚轮 transition 结束：立即归位，避免用固定定时器抢跑导致的“向下回弹”
    onReelEnd(e) {
      // 仅在飞转中响应，只认 transform 的结束事件（filter 等其他属性变更不误触）
      if (!this.data.spinning) return
      if (e.propertyName && e.propertyName !== 'transform') return
      const idx = e.currentTarget.dataset.index
      if (idx == null) return
      this._settleColumn(Number(idx), this._gen)
    },

    // 单列停稳（transitionend 与兜底定时器共用，settled 标记保证只执行一次）
    // 不做归位跳变，保持在 land 位置；下一次 spin 开始前会预归位
    _settleColumn(i, gen) {
      if (this._destroyed || !this.data.spinning) return
      // 旧一轮遗留的定时器：不能停掉新一轮正在进行的动画
      if (gen != null && gen !== this._gen) return
      if (!this._settled || this._settled[i]) return
      // 时长下界防伪：该列动画不可能早于自身时长的 60% 结束，
      // 早到的多为上一轮取消 transition/重新渲染补发的虚假 transitionend，
      // 误停会把正在起步的动画直接掐掉（表现为“瞬间结束”或“不滚动”），
      // 真事件在时长末尾到达、兜底定时器在时长+缓冲后到达，均不受影响
      const expected = SPIN_DURATION + i * STAGGER
      if (this._spinTs && Date.now() - this._spinTs < expected * 0.6) return
      // 落点校验：只认“滚轮确实已提交本轮目标 offset”
      const col = this.data.cols[i]
      const land = this._lands && this._lands[i]
      if (!col || land == null) return
      const targetOffset = -(land * (this._realItemH || ITEM_H))
      if (Math.abs(col.offset - targetOffset) > 1) return
      this._settled[i] = true

      // 记录当前停靠的 land 位置（不是 norm），供下一次 spin 使用
      this._landIndexes[i] = this._lands[i]
      // 单轴刹停铜铃音：按列号升调，与视觉停轮严格同步
      slotAudio.playReelBrake(i)

      const doneUpdates = {
        [`cols[${i}].anim`]: false,
        [`cols[${i}].blur`]: false,
      }
      const allDone = this._settled.every(Boolean)
      if (allDone) {
        doneUpdates.spinning = false
        this._stopTick()
        slotAudio.playSuccessFanfare()
      }
      this.setData(doneUpdates)
      if (allDone) this.triggerEvent('change', { value: this._result })
    },

    _pullLever() {
      slotAudio.init()
      slotAudio.playLeverPull()
      this._startTick()
      this.setData({ leverPulled: true })
      setTimeout(() => {
        if (!this._destroyed) this.setData({ leverPulled: false })
      }, 280)
    },

    // 飞转期间的咔嗒声循环：60ms 一发，全停/销毁时关闭
    _startTick() {
      this._stopTick()
      this._tickTimer = setInterval(() => slotAudio.playTick(), 60)
    },

    _stopTick() {
      if (this._tickTimer) {
        clearInterval(this._tickTimer)
        this._tickTimer = null
      }
    },

    onCountChange(e) {
      if (this.data.spinning) {
        wx.$toast('摇奖飞转中，请停机后再调数量~')
        return
      }
      const count = e.detail.value
      this.setData({ drawCount: count })
      // 列数随抽取数量联动
      this._resetCols(count)
    },
  },
})
