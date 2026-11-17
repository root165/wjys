<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as workOrderApi from '@/api/workOrder'

const loading = ref(false)
const orders = ref([])
const selected = ref(null)
const logs = ref([])
const spareRecords = ref([])
const statusLabels = { 0: '待接单', 1: '维修中', 2: '待验收', 3: '已完成', 4: '验收退回' }

async function loadOrders() {
  loading.value = true
  try { orders.value = (await workOrderApi.getWorkOrders()).data || [] } catch (error) { ElMessage.error(error.message || '工单加载失败') } finally { loading.value = false }
}

async function view(order) {
  try {
    const [detailResponse, logsResponse, recordsResponse] = await Promise.all([workOrderApi.getWorkOrder(order.id), workOrderApi.getWorkOrderLogs(order.id), workOrderApi.getSpareRecords(order.id)])
    selected.value = detailResponse.data
    logs.value = logsResponse.data || []
    spareRecords.value = recordsResponse.data || []
  } catch (error) { ElMessage.error(error.message || '工单详情加载失败') }
}

onMounted(loadOrders)
</script>

<template>
  <div class="page-intro"><div><h2>维修工单</h2><p>查看工单状态、操作日志和备件消耗流水。</p></div><button class="table-action" type="button" @click="loadOrders">刷新</button></div>
  <section class="soft-card table-card"><div v-if="loading" class="empty-state">正在加载工单…</div><div v-else-if="!orders.length" class="empty-state">暂无维修工单</div><div v-else class="table-wrap"><table class="data-table"><thead><tr><th>工单号</th><th>设备</th><th>维修人</th><th>状态</th><th>备件费用</th><th>操作</th></tr></thead><tbody><tr v-for="order in orders" :key="order.id"><td><strong>{{ order.orderNo }}</strong></td><td>设备 #{{ order.equipId }}</td><td>{{ order.assigneeId ? `用户 #${order.assigneeId}` : '未派工' }}</td><td><span class="state-pill" :class="order.status === 3 ? 'active' : 'inactive'">{{ statusLabels[order.status] || '未知' }}</span></td><td>¥ {{ order.spareCost || '0.00' }}</td><td><button class="table-action" type="button" @click="view(order)">查看流水</button></td></tr></tbody></table></div></section>
  <section v-if="selected" class="work-order-detail">
    <section class="soft-card panel"><div class="panel-heading"><div><h3>{{ selected.orderNo }} · 工单详情</h3><span>设备 #{{ selected.equipId }} · {{ statusLabels[selected.status] }}</span></div><button class="table-action" type="button" @click="selected = null">关闭</button></div><div class="detail-grid"><div><span>维修人</span><strong>{{ selected.assigneeId || '未派工' }}</strong></div><div><span>完工时间</span><strong>{{ selected.finishedTime || '—' }}</strong></div><div><span>维修原因</span><strong>{{ selected.finishReason || '—' }}</strong></div><div><span>维修结果</span><strong>{{ selected.finishResult || '—' }}</strong></div><div><span>方案</span><strong>{{ selected.repairPlan || '—' }}</strong></div><div><span>停机分钟</span><strong>{{ selected.downtimeMinutes ?? '—' }}</strong></div></div></section>
    <div class="detail-columns"><section class="soft-card panel"><div class="panel-heading"><h3>工单操作日志</h3></div><div v-if="!logs.length" class="empty-state">暂无日志</div><div v-else class="timeline"><div v-for="log in logs" :key="log.id" class="timeline-row"><span class="timeline-dot"></span><div><strong>{{ log.action }}</strong><span>{{ log.remark || '—' }} · {{ log.createdTime }}</span></div></div></div></section><section class="soft-card panel"><div class="panel-heading"><h3>备件消耗流水</h3></div><div v-if="!spareRecords.length" class="empty-state">暂无备件流水</div><div v-else class="record-list"><div v-for="record in spareRecords" :key="record.id" class="record-row"><div><strong>备件 #{{ record.spareId }}</strong><span>{{ record.direction }} · {{ record.remark || '—' }}</span></div><b :class="record.direction === 'REVERSE' ? 'reverse-amount' : ''">{{ record.direction === 'REVERSE' ? '+' : '-' }}¥ {{ Math.abs(Number(record.amount || 0)).toFixed(2) }}</b></div></div></section></div>
  </section>
</template>

<style scoped>
.work-order-detail { display: grid; gap: 18px; margin-top: 18px; }
.detail-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 18px; }
.detail-grid span, .detail-grid strong, .timeline-row span, .record-row span { display: block; }
.detail-grid span { color: #87988f; font-size: 12px; }.detail-grid strong { margin-top: 7px; color: #294b40; font-size: 13px; font-weight: 500; }
.detail-columns { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; }
.timeline, .record-list { display: grid; gap: 13px; }.timeline-row, .record-row { display: flex; align-items: start; gap: 10px; padding-bottom: 12px; border-bottom: 1px solid #edf2ee; }.timeline-row:last-child, .record-row:last-child { border-bottom: 0; padding-bottom: 0; }.timeline-dot { width: 8px; height: 8px; margin-top: 5px; border-radius: 50%; background: #d4ed45; flex: 0 0 auto; }.timeline-row strong, .record-row strong { color: #294b40; font-size: 13px; }.timeline-row span, .record-row span { margin-top: 4px; color: #87988f; font-size: 12px; }.record-row { justify-content: space-between; }.record-row b { color: #a15b4e; font-size: 13px; }.record-row b.reverse-amount { color: #4c8b69; }
@media (max-width: 900px) { .detail-grid, .detail-columns { grid-template-columns: 1fr; } }
</style>
