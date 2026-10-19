import { action } from 'mobx-miniprogram'

export default {
  // 海报页临时数据（抽菜页跳转海报页时写入，跨分包复杂数据不走 URL）
  poster: {
    drawData: null, // { dishes: [], kitchenName: '' }
  },
  setPosterDrawData: action(function (drawData) {
    this.poster = { ...this.poster, drawData }
  }),
}
