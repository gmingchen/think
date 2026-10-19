<template>
  <el-card id="sec-login-rank" shadow="never" class="section">
    <template #header>
      <SectionHeader title="登录活跃排行" :insight="insight" />
    </template>

    <div v-loading="loading">
      <el-alert
        v-if="failed"
        type="error"
        :closable="false"
        title="数据加载失败，请确认后端服务（8810）是否正常"
      />
      <template v-else>
        <div class="sec-toolbar">
          <span class="sec-toolbar__label">榜单条数</span>
          <el-radio-group v-model="top" size="small" @change="load">
            <el-radio-button v-for="n in TOP_OPTIONS" :key="n" :value="n">{{ n }}</el-radio-button>
          </el-radio-group>
        </div>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-card shadow="never" :header="`登录天数榜 · TOP${top}`">
              <el-table :data="byDays" size="small" max-height="420px">
                <el-table-column type="index" label="#" width="46" />
                <el-table-column prop="nickname" label="昵称" min-width="90" show-overflow-tooltip />
                <el-table-column prop="userId" label="用户ID" width="80" />
                <el-table-column prop="num" label="登录天数" width="90" sortable />
              </el-table>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="never" :header="`登录次数榜 · TOP${top}`">
              <el-table :data="byCount" size="small" max-height="420px">
                <el-table-column type="index" label="#" width="46" />
                <el-table-column prop="nickname" label="昵称" min-width="90" show-overflow-tooltip />
                <el-table-column prop="userId" label="用户ID" width="80" />
                <el-table-column prop="num" label="登录次数" width="90" sortable />
              </el-table>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="never" :header="`单日峰值榜 · TOP${top}`">
              <el-table :data="byPeak" size="small" max-height="420px">
                <el-table-column type="index" label="#" width="46" />
                <el-table-column prop="nickname" label="昵称" min-width="90" show-overflow-tooltip />
                <el-table-column prop="userId" label="用户ID" width="80" />
                <el-table-column prop="num" label="单日峰值" width="90" sortable />
              </el-table>
            </el-card>
          </el-col>
        </el-row>
        <div class="sec-note">口径：基于 user_login_log 登录流水，随顶部日期区间联动，榜单条数可切换（默认 TOP10）；天数 = 区间内去重登录日（粘性），次数 = 区间内总登录流水（频次），峰值 = 单人最高一天的登录次数（最疯日）；上线前历史无流水不入榜</div>
      </template>
    </div>
  </el-card>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { getLoginRank } from '@/api/stats'
import SectionHeader from '@/components/SectionHeader.vue'
import { useLoad } from '@/utils/load'

const props = defineProps({ range: { type: Array, default: () => [] } })

const TOP_OPTIONS = [10, 20, 50, 100]
const top = ref(10)

const raw = ref({})
const byDays = computed(() => raw.value.byDays || [])
const byCount = computed(() => raw.value.byCount || [])
const byPeak = computed(() => raw.value.byPeak || [])

const insight = computed(() => {
  const t = byDays.value[0]
  return t ? `${t.nickname} 以 ${t.num} 天领跑登录天数榜` : '区间内暂无登录流水'
})

// 块级自治：自请求 login-rank，随区间与榜单条数联动
const { loading, failed, load } = useLoad(async () => {
  raw.value = await getLoginRank({ start: props.range[0], end: props.range[1], limit: top.value })
})

watch(() => props.range, load, { deep: true })
onMounted(load)
defineExpose({ load })
</script>

<style scoped>
.sec-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.sec-toolbar__label {
  font-size: 13px;
  color: var(--text-3);
}

.sec-note {
  font-size: 12px;
  color: var(--text-3);
  margin-top: 12px;
}
</style>
