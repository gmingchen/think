// 摇奖合成音效：基于 wx.createWebAudioContext 振荡器生成，零音频文件、不占包体积
// 需基础库 2.19.0+；不支持或创建失败时全部静默降级，不影响摇奖流程

class SlotAudio {
  constructor() {
    this.ctx = null
    this._resumeTimer = null
  }

  // 需在用户点击手势上下文中调用（iOS 音频需触摸触发后才可出声），幂等
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

  // 拉杆拉动：低频下滑的机械沉稳声
  playLeverPull() {
    if (!this.ctx) return
    const now = this.ctx.currentTime
    const osc = this.ctx.createOscillator()
    const gain = this.ctx.createGain()
    osc.type = 'triangle'
    osc.frequency.setValueAtTime(160, now)
    osc.frequency.exponentialRampToValueAtTime(60, now + 0.16)
    gain.gain.setValueAtTime(0.3, now)
    gain.gain.exponentialRampToValueAtTime(0.01, now + 0.18)
    osc.connect(gain)
    gain.connect(this.ctx.destination)
    osc.start(now)
    osc.stop(now + 0.18)
  }

  // 滚轮飞转中的木质齿轮咔嗒声（高频短促，随机音高避免机械重复感）
  playTick() {
    if (!this.ctx) return
    const now = this.ctx.currentTime
    const osc = this.ctx.createOscillator()
    const gain = this.ctx.createGain()
    osc.type = 'sine'
    osc.frequency.setValueAtTime(480 + Math.random() * 120, now)
    gain.gain.setValueAtTime(0.08, now)
    gain.gain.exponentialRampToValueAtTime(0.001, now + 0.04)
    osc.connect(gain)
    gain.connect(this.ctx.destination)
    osc.start(now)
    osc.stop(now + 0.04)
  }

  // 单轴制动锁死：C-E-G 和弦音按列号升调（左→右越停越亮，压轴感）
  playReelBrake(pitchLevel = 1) {
    if (!this.ctx) return
    const now = this.ctx.currentTime
    const osc = this.ctx.createOscillator()
    const gain = this.ctx.createGain()
    const freqMap = [523.25, 659.25, 783.99] // C5, E5, G5
    osc.type = 'sine'
    osc.frequency.setValueAtTime(freqMap[pitchLevel] || 600, now)
    gain.gain.setValueAtTime(0.22, now)
    gain.gain.exponentialRampToValueAtTime(0.001, now + 0.28)
    osc.connect(gain)
    gain.connect(this.ctx.destination)
    osc.start(now)
    osc.stop(now + 0.28)
  }

  // 全部停稳出便签：C 大调上行琶音
  playSuccessFanfare() {
    if (!this.ctx) return
    const notes = [523.25, 659.25, 783.99, 1046.5] // C E G C
    notes.forEach((freq, idx) => {
      const now = this.ctx.currentTime + idx * 0.08
      const osc = this.ctx.createOscillator()
      const gain = this.ctx.createGain()
      osc.type = 'triangle'
      osc.frequency.setValueAtTime(freq, now)
      gain.gain.setValueAtTime(0.18, now)
      gain.gain.exponentialRampToValueAtTime(0.001, now + 0.35)
      osc.connect(gain)
      gain.connect(this.ctx.destination)
      osc.start(now)
      osc.stop(now + 0.35)
    })
  }

  // 盖朱红印章：低频急降的木槌闷响
  playStampThud() {
    if (!this.ctx) return
    const now = this.ctx.currentTime
    const osc = this.ctx.createOscillator()
    const gain = this.ctx.createGain()
    osc.type = 'square'
    osc.frequency.setValueAtTime(120, now)
    osc.frequency.exponentialRampToValueAtTime(40, now + 0.12)
    gain.gain.setValueAtTime(0.25, now)
    gain.gain.exponentialRampToValueAtTime(0.01, now + 0.14)
    osc.connect(gain)
    gain.connect(this.ctx.destination)
    osc.start(now)
    osc.stop(now + 0.14)
  }
}

export default new SlotAudio()
