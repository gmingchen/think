// 采购清单合成音效：与抽菜页 slot-audio 同源方案（wx.createWebAudioContext 振荡器，零音频文件、不占包体积）
// 需基础库 2.19.0+；不支持或创建失败时全部静默降级，不影响勾选、折叠与缓存主流程
class ShoppingAudio {
  constructor() {
    this.ctx = null
    this._resumeTimer = null
  }

  // iOS 首次创建必须在触摸手势上下文中，故由各 play* 幂等调用（页面 onUnload 里 destroy）
  init() {
    if (this.ctx || !wx.createWebAudioContext) return
    try {
      this.ctx = wx.createWebAudioContext()
    } catch (e) {
      this.ctx = null
      return
    }
    // iOS 端 AudioContext 时钟不自动推进，必须周期性 resume 泵活，否则声音不响
    this._resumeTimer = setInterval(() => {
      if (!this.ctx) return
      try {
        this.ctx.resume()
      } catch (e) { /* 低版本基础库无 resume，忽略 */ }
    }, 100)
  }

  destroy() {
    if (this._resumeTimer) {
      clearInterval(this._resumeTimer)
      this._resumeTimer = null
    }
    this.ctx = null
  }

  // 单音发声：振荡器 + 指数衰减包络，to 非 0 时做频率滑音；delay 用于多音错拍
  tone({ type = 'sine', from = 440, to = 0, dur = 0.08, vol = 0.15, delay = 0 }) {
    this.init()
    if (!this.ctx) return
    const now = this.ctx.currentTime + delay
    const osc = this.ctx.createOscillator()
    const gain = this.ctx.createGain()
    osc.type = type
    osc.frequency.setValueAtTime(from, now)
    if (to) osc.frequency.exponentialRampToValueAtTime(to, now + dur)
    gain.gain.setValueAtTime(vol, now)
    gain.gain.exponentialRampToValueAtTime(0.001, now + dur)
    osc.connect(gain)
    gain.connect(this.ctx.destination)
    osc.start(now)
    osc.stop(now + dur)
  }

  // 打勾：铅笔落纸的轻点上扬
  playCheck() {
    this.tone({ type: 'sine', from: 520, to: 780, dur: 0.09, vol: 0.18 })
  }

  // 取消勾选：比打勾更低更短，听得出「擦回原样」
  playUncheck() {
    this.tone({ type: 'sine', from: 420, to: 240, dur: 0.07, vol: 0.09 })
  }

  // 分组展开/收起：三声纸页翻动，展开音上行、收起音下行
  playPaper(open = true) {
    const up = [620, 720, 840]
    ;(open ? up : up.slice().reverse()).forEach((freq, i) => {
      this.tone({ type: 'sine', from: freq, dur: 0.045, vol: 0.055, delay: i * 0.028 })
    })
  }

  // 清空勾选：四声由密到疏的下滑擦除
  playClear() {
    [700, 560, 440, 330].forEach((freq, i) => {
      this.tone({ type: 'sine', from: freq, to: freq * 0.7, dur: 0.05, vol: 0.09, delay: i * 0.055 })
    })
  }

  // 买齐盖章：低频急降的木槌闷响（配方同抽菜页 playStampThud）
  playStamp() {
    this.tone({ type: 'square', from: 120, to: 40, dur: 0.12, vol: 0.25 })
  }

  // 成功乐句：C-E-G 上行琶音，用于入篮与买齐瞬间接在闷响后
  playChime() {
    ;[523.25, 659.25, 783.99].forEach((freq, i) => {
      this.tone({ type: 'triangle', from: freq, dur: 0.28, vol: 0.14, delay: i * 0.07 })
    })
  }
}

export default new ShoppingAudio()
