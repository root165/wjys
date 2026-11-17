<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as dashboardApi from '@/api/dashboard'

const loading = ref(false)
const overview = ref({})
const trend = ref([])
const ranking = ref([])
const pareto = ref([])
const inspection = ref({})
const today = new Date().toISOString().slice(0, 10)
const rangeStart = new Date(Date.now() - 6 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10)

const metrics = computed(() => [
  { label: '设备总数', value: overview.value.equipTotal ?? '—', foot: '纳入统计的设备台账' },
  { label: '待处理工单', value: overview.value.unhandledRepair ?? '—', foot: `今日故障 ${overview.value.faultToday ?? 0} 条` },
  { label: '今日巡检', value: overview.value.taskToday ?? '—', foot: `完成率 ${overview.value.taskFinishRate ?? 0}%` },
  { label: '备件预警', value: overview.value.spareAlerts ?? '—', foot: '低于安全库存阈值' },
])

const maxTrendValue = computed(() => Math.max(1, ...trend.value.map((item) => item.taskTotal || 0)))

async function load() {
  loading.value = true
  try {
    const [overviewResponse, trendResponse, rankingResponse, paretoResponse, inspectionResponse] = await Promise.all([
      dashboardApi.getOverview(),
      dashboardApi.getTrend({ startDate: rangeStart, endDate: today }),
      dashboardApi.getEquipmentRanking({ startDate: rangeStart, endDate: today, topN: 5 }),
      dashboardApi.getFaultPareto({ startDate: rangeStart, endDate: today, dimension: 'faultType' }),
      dashboardApi.getInspectionSummary({ startDate: rangeStart, endDate: today }),
    ])
    overview.value = overviewResponse.data || {}
    trend.value = trendResponse.data || []
    ranking.value = rankingResponse.data?.list || []
    pareto.value = paretoResponse.data?.list || []
    inspection.value = inspectionResponse.data || {}
  } catch (error) {
    ElMessage.error(error.message || '统计数据加载失败')
  } finally { loading.value = false }
}

onMounted(load)
</script>

<template>
  <div class="page-intro"><div><h2>现场运维，保持可见。</h2><p>统计分析把设备、巡检、故障和备件状态汇总到同一张运行视图。</p></div><div class="hero-date">{{ today }}</div></div>
  <div v-if="loading" class="soft-card empty-state">正在加载统计数据…</div>
  <template v-else>
    <section class="metric-grid"><article v-for="metric in metrics" :key="metric.label" class="soft-card metric-card"><div class="metric-label">{{ metric.label }}</div><div class="metric-value">{{ metric.value }}</div><div class="metric-foot">{{ metric.foot }}</div></article></section>
    <section class="dashboard-grid dashboard-grid-wide"><article class="soft-card panel"><div class="panel-heading"><h3>近 7 日巡检趋势</h3><span>任务完成 / 异常</span></div><div v-if="!trend.length" class="empty-note">暂无巡检统计数据</div><div v-else class="trend-list"><div v-for="item in trend" :key="item.statDate" class="trend-row"><span>{{ item.statDate.slice(5) }}</span><div class="trend-track"><i :style="{ width: `${(item.taskTotal / maxTrendValue) * 100}%` }"></i></div><strong>{{ item.taskFinish }}/{{ item.taskTotal }}</strong><em>{{ item.abnormalCount }} 异常</em></div></div></article><article class="soft-card panel"><div class="panel-heading"><h3>巡检质量</h3><span>近 7 日</span></div><div class="quality-grid"><div><strong>{{ inspection.finishRate ?? 0 }}%</strong><span>完成率</span></div><div><strong>{{ inspection.timeoutRate ?? 0 }}%</strong><span>超时率</span></div><div><strong>{{ inspection.abnormalRate ?? 0 }}%</strong><span>异常率</span></div></div></article></section>
    <section class="dashboard-grid dashboard-grid-wide"><article class="soft-card panel"><div class="panel-heading"><h3>设备故障排行</h3><span>按故障次数</span></div><div v-if="!ranking.length" class="empty-note">暂无故障排行数据</div><div v-else class="ranking-list"><div v-for="(item, index) in ranking" :key="item.equipCode" class="ranking-row"><b>{{ String(index + 1).padStart(2, '0') }}</b><div class="signal-content"><strong>{{ item.equipName || item.equipCode }}</strong><span>{{ item.equipCode }} · 停机 {{ item.downtimeMinutes || 0 }} 分钟</span></div><strong class="ranking-value">{{ item.faultCount }}</strong></div></div></article><article class="soft-card panel"><div class="panel-heading"><h3>故障类型分布</h3><span>近 7 日 Pareto</span></div><div v-if="!pareto.length" class="empty-note">暂无故障类型数据</div><div v-else class="pareto-list"><div v-for="item in pareto" :key="item.name" class="pareto-row"><div><strong>{{ item.name }}</strong><span>{{ item.count }} 次 · 累计 {{ item.cumulative }}%</span></div><div class="pareto-track"><i :style="{ width: `${Math.max(4, Number(item.ratio || 0))}%` }"></i></div></div></div></article></section>
  </template>
</template>

<style scoped>
.hero-date { color: #739080; font-family: Georgia, serif; font-size: 20px; }.dashboard-grid-wide { grid-template-columns: 1.35fr .65fr; }.trend-list, .ranking-list, .pareto-list { display: grid; gap: 14px; }.trend-row { display: grid; grid-template-columns: 44px 1fr 54px 54px; align-items: center; gap: 10px; color: #768a80; font-size: 12px; }.trend-row strong { color: #3f6854; font-size: 12px; text-align: right; }.trend-row em { color: #ad7546; font-size: 11px; font-style: normal; text-align: right; }.trend-track, .pareto-track { height: 8px; overflow: hidden; border-radius: 99px; background: #eef2ec; }.trend-track i, .pareto-track i { display: block; height: 100%; border-radius: inherit; background: #7caa82; }.quality-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; text-align: center; }.quality-grid div { padding: 18px 8px; border-radius: 12px; background: #f3f7ef; }.quality-grid strong { display: block; color: #2f654e; font-size: 22px; }.quality-grid span { display: block; margin-top: 6px; color: #82948a; font-size: 11px; }.ranking-row { display: flex; align-items: center; gap: 12px; padding: 11px 0; border-bottom: 1px solid #edf1ed; }.ranking-row:last-child { border: 0; }.ranking-row > b { color: #b1bd8a; font-size: 12px; }.ranking-value { color: #a8663e; font-size: 17px; }.pareto-row > div:first-child { display: flex; justify-content: space-between; align-items: center; margin-bottom: 7px; }.pareto-row strong { color: #315747; font-size: 13px; }.pareto-row span { color: #899a91; font-size: 11px; }.pareto-track i { background: #d19b64; }
@media (max-width: 900px) { .dashboard-grid-wide { grid-template-columns: 1fr; }.trend-row { grid-template-columns: 38px 1fr 44px; }.trend-row em { display: none; } }
</style>
