<template>
  <div class="dashboard">
    <div ref="toolbarRef" class="dash-toolbar">
      <div class="toolbar-left">
        <span class="brand">厨菜记 · 运营数据看板</span>
        <div class="anchors">
          <el-button
            v-for="a in anchors"
            :key="a.id"
            size="small"
            text
            bg
            @click="scrollTo(a.id)"
          >{{ a.label }}</el-button>
        </div>
      </div>
      <div class="toolbar-right">
        <DateRangeBar v-model="range" />
        <el-button size="small" @click="toggleDark">{{ isDark ? '☀️ 浅色' : '🌙 深色' }}</el-button>
        <el-button type="primary" plain @click="refreshAll">刷新全部</el-button>
        <span v-if="refreshedAt" class="refreshed-at">数据请求于 {{ refreshedAt }}</span>
      </div>
    </div>

    <div class="toolbar-tip">
      上半区看「变化」：今日数据 / 累计总览为快照，累计与增量走势随日期区间联动（默认近 30 天）；
      下半区看「结构」：构成、分布与排行，随日期区间联动。
    </div>

    <div class="sections">
      <OverviewSection ref="secToday" />
      <CumulativeHero ref="secHero" />
      <CumulativeTrendSection ref="secCumTrend" :range="range" />
      <DailyIncrementSection ref="secDailyInc" :range="range" />
      <UserSection ref="secUser" :range="range" />
      <LoginRankSection ref="secLoginRank" :range="range" />
      <KitchenDishSection ref="secKitchenDish" :range="range" />
      <OrderReviewSection ref="secOrderReview" :range="range" />
      <MemberSection ref="secMember" :range="range" />
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import DateRangeBar from '@/components/DateRangeBar.vue'
import CumulativeHero from '@/components/CumulativeHero.vue'
import CumulativeTrendSection from '@/components/sections/CumulativeTrendSection.vue'
import OverviewSection from '@/components/sections/OverviewSection.vue'
import DailyIncrementSection from '@/components/sections/DailyIncrementSection.vue'
import UserSection from '@/components/sections/UserSection.vue'
import LoginRankSection from '@/components/sections/LoginRankSection.vue'
import KitchenDishSection from '@/components/sections/KitchenDishSection.vue'
import OrderReviewSection from '@/components/sections/OrderReviewSection.vue'
import MemberSection from '@/components/sections/MemberSection.vue'
import { defaultRange } from '@/api/stats'
import { isDark, toggleDark } from '@/utils/theme'

const range = ref([defaultRange().start, defaultRange().end])

const anchors = [
  { id: 'sec-overview', label: '今日数据' },
  { id: 'sec-hero', label: '累计总览' },
  { id: 'sec-cum-trend', label: '累计走势' },
  { id: 'sec-daily-inc', label: '每日增量' },
  { id: 'sec-user', label: '用户与活跃' },
  { id: 'sec-login-rank', label: '登录排行' },
  { id: 'sec-kitchen', label: '内容生态' },
  { id: 'sec-order', label: '做饭体验' },
  { id: 'sec-member', label: '会员与收入' }
]

// 每块自治：各自请求 / loading / 失败态，页面只持有区间与锚点
const secToday = ref(null)
const secHero = ref(null)
const secCumTrend = ref(null)
const secDailyInc = ref(null)
const secUser = ref(null)
const secLoginRank = ref(null)
const secKitchenDish = ref(null)
const secOrderReview = ref(null)
const secMember = ref(null)

const toolbarRef = ref(null)

// sticky 工具栏换行后高度会变，固定 scroll-margin 盖不住，改为点击时实测工具栏高度做偏移
const scrollTo = (id) => {
  const el = document.getElementById(id)
  if (!el) return
  const offset = (toolbarRef.value?.offsetHeight || 0) + 12
  window.scrollTo({ top: el.getBoundingClientRect().top + window.scrollY - offset, behavior: 'smooth' })
}

// 记录各区块发请求的时间点（首屏自请求 / 区间切换重载 / 手动刷新全部），展示在工具栏
const refreshedAt = ref('')
const markRequestTime = () => {
  refreshedAt.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
}

const refreshAll = () => {
  markRequestTime()
  secToday.value?.load()
  secHero.value?.load()
  secCumTrend.value?.load()
  secDailyInc.value?.load()
  secUser.value?.load()
  secLoginRank.value?.load()
  secKitchenDish.value?.load()
  secOrderReview.value?.load()
  secMember.value?.load()
}

onMounted(() => {
  // 支持 ?start=&end= 直达指定区间，便于分享看板快照；各块自请求，这里只需恢复区间
  const q = new URLSearchParams(location.search)
  const s = q.get('start')
  const e = q.get('end')
  if (s && e && /^\d{4}-\d{2}-\d{2}$/.test(s) && /^\d{4}-\d{2}-\d{2}$/.test(e)) {
    range.value = [s, e]
  }
  markRequestTime()
})

// 区间变化只同步 URL；需要区间的块各自 watch props 重载
watch(range, ([s, e]) => {
  history.replaceState(null, '', `?start=${s}&end=${e}`)
  markRequestTime()
})
</script>

<style scoped>
.dashboard {
  padding: 16px 20px 48px;
}
.dash-toolbar {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  padding: 12px 16px;
  margin: -16px -20px 8px;
  background: var(--panel-bg);
  border-bottom: 1px solid var(--panel-border);
}
.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.brand {
  font-size: 16px;
  font-weight: 700;
}
.anchors {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.toolbar-tip {
  font-size: 12px;
  color: var(--text-2);
  margin-bottom: 16px;
}
.refreshed-at {
  font-size: 12px;
  color: var(--text-2);
  white-space: nowrap;
}
.sections {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.sections :deep(.section) {
  border-radius: 10px;
}
</style>
