<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as workOrderApi from '@/api/workOrder'

const loading = ref(false)
const orders = ref([])
const selected = ref(null)
const logs = ref([])
const spareRecords = ref([])
const actionVisible = ref(false)
const actionName = ref('')
const actionForm = reactive({ action: '', assigneeId: '', remark: '', finishReason: '', repairPlan: '', finishResult: '', downtimeMinutes: 0 })
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

function openAction(action) {
  Object.assign(actionForm, { action, assigneeId: '', remark: '', finishReason: '', repairPlan: '', finishResult: '', downtimeMinutes: 0 })
  actionName.value = { dispatch: '派工', finish: '提交完工', reject: '验收退回' }[action] || action
  actionVisible.value = true
}

async function transition(action, payload = actionForm) {
  if (!selected.value) return
  try {
    if (action === 'dispatch') { const assigneeId = Number(payload.assigneeId); if (!Number.isInteger(assigneeId) || assigneeId < 1 || !payload.remark.trim()) return ElMessage.warning('请填写有效的维修人用户 ID 和派工说明'); await workOrderApi.dispatchWorkOrder(selected.value.id, { assigneeId, remark: payload.remark.trim() }) }
    if (action === 'start') await workOrderApi.startWorkOrder(selected.value.id)
    if (action === 'finish') { if (!payload.finishReason.trim() || !payload.repairPlan.trim() || !payload.finishResult.trim()) return ElMessage.warning('请填写完工原因、维修方案和维修结果'); await workOrderApi.finishWorkOrder(selected.value.id, { finishReason: payload.finishReason.trim(), repairPlan: payload.repairPlan.trim(), finishResult: payload.finishResult.trim(), downtimeMinutes: Number(payload.downtimeMinutes) || 0, fileIds: [] }) }
    if (action === 'accept') await workOrderApi.acceptWorkOrder(selected.value.id, { remark: '管理端验收通过' })
    if (action === 'reject') { if (!payload.remark.trim()) return ElMessage.warning('请填写退回原因，维修人员才能明确整改内容'); await workOrderApi.rejectWorkOrder(selected.value.id, { remark: payload.remark.trim() }) }
    ElMessage.success('工单状态已更新'); actionVisible.value = false; await loadOrders(); await view(selected.value)
  } catch (error) { ElMessage.error(error.message || '工单状态更新失败') }
}

onMounted(loadOrders)
</script>

<template>
  <div class="page-intro"><div><h2>维修工单</h2><p>查看工单状态、操作日志和备件消耗流水。</p></div><button class="table-action" type="button" @click="loadOrders">刷新</button></div>
  <section class="soft-card table-card"><div v-if="loading" class="empty-state">正在加载工单…</div><div v-else-if="!orders.length" class="empty-state">暂无维修工单</div><div v-else class="table-wrap"><table class="data-table"><thead><tr><th>工单号</th><th>设备</th><th>维修人</th><th>状态</th><th>备件费用</th><th>操作</th></tr></thead><tbody><tr v-for="order in orders" :key="order.id"><td><strong>{{ order.orderNo }}</strong></td><td>设备 #{{ order.equipId }}</td><td>{{ order.assigneeId ? `用户 #${order.assigneeId}` : '未派工' }}</td><td><span class="state-pill" :class="order.status === 3 ? 'active' : 'inactive'">{{ statusLabels[order.status] || '未知' }}</span></td><td>¥ {{ order.spareCost || '0.00' }}</td><td><button class="table-action" type="button" @click="view(order)">查看流水</button></td></tr></tbody></table></div></section>
  <section v-if="selected" class="work-order-detail">
    <section class="soft-card panel"><div class="panel-heading"><div><h3>{{ selected.orderNo }} · 工单详情</h3><span>设备 #{{ selected.equipId }} · {{ statusLabels[selected.status] }}</span></div><div class="row-actions"><button v-if="selected.status === 0" class="table-action" type="button" @click="openAction('dispatch')">派工</button><button v-if="selected.status === 1" class="table-action" type="button" @click="transition('start')">开始维修</button><button v-if="selected.status === 1" class="table-action" type="button" @click="openAction('finish')">提交完工</button><button v-if="selected.status === 2" class="table-action" type="button" @click="transition('accept')">验收通过</button><button v-if="selected.status === 2" class="table-action danger-action" type="button" @click="openAction('reject')">验收退回</button><button class="table-action" type="button" @click="selected = null">关闭</button></div></div><div class="detail-grid"><div><span>维修人</span><strong>{{ selected.assigneeId || '未派工' }}</strong></div><div><span>完工时间</span><strong>{{ selected.finishedTime || '—' }}</strong></div><div><span>维修原因</span><strong>{{ selected.finishReason || '—' }}</strong></div><div><span>维修结果</span><strong>{{ selected.finishResult || '—' }}</strong></div><div><span>方案</span><strong>{{ selected.repairPlan || '—' }}</strong></div><div><span>停机分钟</span><strong>{{ selected.downtimeMinutes ?? '—' }}</strong></div></div></section>
    <div class="detail-columns"><section class="soft-card panel"><div class="panel-heading"><h3>工单操作日志</h3></div><div v-if="!logs.length" class="empty-state">暂无日志</div><div v-else class="timeline"><div v-for="log in logs" :key="log.id" class="timeline-row"><span class="timeline-dot"></span><div><strong>{{ log.action }}</strong><span>{{ log.remark || '—' }} · {{ log.createdTime }}</span></div></div></div></section><section class="soft-card panel"><div class="panel-heading"><h3>备件消耗流水</h3></div><div v-if="!spareRecords.length" class="empty-state">暂无备件流水</div><div v-else class="record-list"><div v-for="record in spareRecords" :key="record.id" class="record-row"><div><strong>备件 #{{ record.spareId }}</strong><span>{{ record.direction }} · {{ record.remark || '—' }}</span></div><b :class="record.direction === 'REVERSE' ? 'reverse-amount' : ''">{{ record.direction === 'REVERSE' ? '+' : '-' }}¥ {{ Math.abs(Number(record.amount || 0)).toFixed(2) }}</b></div></div></section></div>
  </section>
  <el-dialog v-model="actionVisible" :title="`${actionName}工单`" width="540px" destroy-on-close><p class="dialog-tip">请把操作依据写在表单里，系统会把内容写入工单流水，便于维修人员和验收人员复盘。</p><form class="dialog-form" @submit.prevent="transition(actionForm.action)"><template v-if="actionForm.action === 'dispatch'"><label class="form-label">维修人用户 ID</label><input v-model="actionForm.assigneeId" class="form-input" type="number" min="1" placeholder="例如：3；可在用户管理查看 ID" /><label class="form-label">派工说明</label><textarea v-model="actionForm.remark" class="form-textarea" rows="3" placeholder="例如：优先处理主轴异响，今日完成初检"></textarea></template><template v-else-if="actionForm.action === 'finish'"><label class="form-label">完工原因</label><textarea v-model="actionForm.finishReason" class="form-textarea" rows="2" placeholder="说明故障原因或更换原因"></textarea><label class="form-label">维修方案</label><textarea v-model="actionForm.repairPlan" class="form-textarea" rows="2" placeholder="说明检查、维修、更换的具体步骤"></textarea><label class="form-label">维修结果</label><textarea v-model="actionForm.finishResult" class="form-textarea" rows="2" placeholder="说明设备是否恢复、还需什么后续动作"></textarea><label class="form-label">停机时长（分钟）</label><input v-model="actionForm.downtimeMinutes" class="form-input" type="number" min="0" placeholder="例如：45"></template><template v-else><label class="form-label">退回原因</label><textarea v-model="actionForm.remark" class="form-textarea" rows="4" placeholder="请明确需要补充或整改的内容"></textarea></template><div class="dialog-actions"><button class="secondary-button" type="button" @click="actionVisible = false">取消</button><button class="primary-button compact-button" type="submit">确认{{ actionName }}</button></div></form></el-dialog>
</template>

<style scoped>
.work-order-detail { display: grid; gap: 18px; margin-top: 18px; }
.row-actions { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; }.danger-action { color: #a15b4e; }
.detail-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 18px; }
.detail-grid span, .detail-grid strong, .timeline-row span, .record-row span { display: block; }
.detail-grid span { color: #87988f; font-size: 12px; }.detail-grid strong { margin-top: 7px; color: #294b40; font-size: 13px; font-weight: 500; }
.detail-columns { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; }
.dialog-tip { margin: -4px 0 16px; color: #789087; font-size: 13px; line-height: 1.7; }.dialog-form { display: grid; gap: 2px; }.form-textarea { width: 100%; padding: 11px 13px; border: 1px solid #d7e2d9; border-radius: 8px; outline: 0; background: #fbfdfb; color: #23493b; font: inherit; resize: vertical; }.form-textarea:focus { border-color: #7aaa8c; box-shadow: 0 0 0 3px rgba(122,170,140,.12); }.dialog-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 18px; }.secondary-button { width: auto; min-height: 42px; padding: 0 18px; border: 1px solid #cbd9ce; border-radius: 9px; background: #f7faf7; color: #335f4d; cursor: pointer; }.dialog-actions .primary-button { width: auto; }
.timeline, .record-list { display: grid; gap: 13px; }.timeline-row, .record-row { display: flex; align-items: start; gap: 10px; padding-bottom: 12px; border-bottom: 1px solid #edf2ee; }.timeline-row:last-child, .record-row:last-child { border-bottom: 0; padding-bottom: 0; }.timeline-dot { width: 8px; height: 8px; margin-top: 5px; border-radius: 50%; background: #d4ed45; flex: 0 0 auto; }.timeline-row strong, .record-row strong { color: #294b40; font-size: 13px; }.timeline-row span, .record-row span { margin-top: 4px; color: #87988f; font-size: 12px; }.record-row { justify-content: space-between; }.record-row b { color: #a15b4e; font-size: 13px; }.record-row b.reverse-amount { color: #4c8b69; }
@media (max-width: 900px) { .detail-grid, .detail-columns { grid-template-columns: 1fr; } }
</style>
