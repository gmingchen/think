// 图表基座：统一各分区 EChart 的 grid / dataZoom / 调色板，保证视觉语言一致

export const PALETTE = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399']

export const baseGrid = { left: 60, right: 24, top: 30, bottom: 54 }

// 每个调用处需返回独立对象，避免 ECharts 内部修改污染共享引用
export const axisDataZoom = () => [{ type: 'inside' }, { type: 'slider', height: 16, bottom: 12 }]

// 时间轴：同年只显 MM-dd、跨年带年份；hideOverlap 自动隐藏重叠标签，数据多时不挤成乱码
export const xAxisCategory = (dates) => {
  const crossYear = dates.length > 1 && dates[0].slice(0, 4) !== dates[dates.length - 1].slice(0, 4)
  return {
    type: 'category',
    boundaryGap: false,
    data: dates,
    axisLabel: {
      hideOverlap: true,
      formatter: (v) => (crossYear ? v : v.slice(5))
    }
  }
}
