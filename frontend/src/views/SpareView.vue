<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as spareApi from '@/api/spare'

const loading = ref(false)
const saving = ref(false)
const spares = ref([])
const alerts = ref([])
const keyword = ref('')
const showCreate = ref(false)
const consumeForm = reactive({ orderId: '', spareId: '', quantity: 1, remark: '' })
const createForm = reactive({ spareCode: '', spareName: '', specification: '', unit: '件', stockQty: 0, warningQty: 0, unitPrice: '0.00', status: 1 })
const activeSpare = computed(() => spares.value.find((item) => String(item.id) === String(consumeForm.spareId)))

async function load() {
  loading.value = true
  try {
    const [listResponse, alertResponse] = await Promise.all([spareApi.getSpares({ keyword: keyword.value || undefined }), spareApi.getSpareAlerts()])
    spares.value = listResponse.data || []
    alerts.value = alertResponse.data || []
  } catch (error) { ElMessage.error(error.message || '备件数据加载失败') } finally { loading.value = false }
}

async function create() {
  saving.value = true
  try { await spareApi.createSpare({ ...createForm, stockQty: Number(createForm.stockQty), warningQty: Number(createForm.warningQty), unitPrice: Number(createForm.unitPrice) }); ElMessage.success('备件已创建'); showCreate.value = false; Object.assign(createForm, { spareCode: '', spareName: '', specification: '', unit: '件', stockQty: 0, warningQty: 0, unitPrice: '0.00', status: 1 }); await load() } catch (error) { ElMessage.error(error.message || '备件创建失败') } finally { saving.value = false }
}

async function adjust(spare) {
  const delta = Number(window.prompt(`调整“${spare.spareName}”库存，正数入库，负数扣减`, '1'))
  if (!Number.isInteger(delta) || delta === 0) return
  try { await spareApi.adjustSpareStock(spare.id, { delta, remark: 'Web 管理端库存调整' }); ElMessage.success('库存已调整'); await load() } catch (error) { ElMessage.error(error.message || '库存调整失败') }
}

async function consume() {
  if (!consumeForm.orderId || !consumeForm.spareId || Number(consumeForm.quantity) <= 0) { ElMessage.warning('请填写工单、备件和领用数量'); return }
  try { await spareApi.consumeSpares(consumeForm.orderId, [{ spareId: Number(consumeForm.spareId), quantity: Number(consumeForm.quantity), remark: consumeForm.remark || 'Web 工单领用' }]); ElMessage.success(`已领用 ${activeSpare.value?.spareName || '备件'}`); Object.assign(consumeForm, { quantity: 1, remark: '' }); await load() } catch (error) { ElMessage.error(error.message || '备件领用失败') }
}

onMounted(load)
</script>

<template>
  <div class="page-intro"><div><h2>备件台账</h2><p>维护库存、预警阈值，并为维修工单登记备件消耗。</p></div><button class="primary-button compact-button" type="button" @click="showCreate = !showCreate">{{ showCreate ? '收起表单' : '新增备件' }}</button></div>
  <section v-if="showCreate" class="soft-card panel spare-form-card"><div class="panel-heading"><h3>新增备件</h3></div><form class="spare-form" @submit.prevent="create"><input v-model="createForm.spareCode" class="form-input" required placeholder="备件编码" /><input v-model="createForm.spareName" class="form-input" required placeholder="备件名称" /><input v-model="createForm.specification" class="form-input" placeholder="规格型号" /><input v-model="createForm.unit" class="form-input" required placeholder="单位" /><input v-model="createForm.stockQty" class="form-input" type="number" min="0" placeholder="初始库存" /><input v-model="createForm.warningQty" class="form-input" type="number" min="0" placeholder="预警库存" /><input v-model="createForm.unitPrice" class="form-input" type="number" min="0" step="0.01" placeholder="单价" /><button class="primary-button compact-button" type="submit" :disabled="saving">保存</button></form></section>
  <section class="metric-grid spare-metrics"><div class="soft-card metric-card"><span class="metric-label">备件总数</span><strong class="metric-value">{{ spares.length }}</strong><span class="metric-foot">当前筛选结果</span></div><div class="soft-card metric-card"><span class="metric-label">库存预警</span><strong class="metric-value warning-value">{{ alerts.length }}</strong><span class="metric-foot">库存不高于阈值</span></div></section>
  <section class="soft-card table-card"><div class="toolbar"><input v-model="keyword" class="form-input" placeholder="搜索编码或名称" @keyup.enter="load" /><button class="table-action" type="button" @click="load">刷新数据</button></div><div v-if="loading" class="empty-state">正在加载备件数据…</div><div v-else-if="!spares.length" class="empty-state">暂无备件数据</div><div v-else class="table-wrap"><table class="data-table"><thead><tr><th>编码</th><th>名称</th><th>规格</th><th>库存</th><th>预警线</th><th>单价</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="spare in spares" :key="spare.id"><td><strong>{{ spare.spareCode }}</strong></td><td>{{ spare.spareName }}</td><td>{{ spare.specification || '—' }}</td><td><span :class="spare.warning ? 'warning-text' : ''">{{ spare.stockQty }} {{ spare.unit }}</span></td><td>{{ spare.warningQty }}</td><td>¥ {{ spare.unitPrice }}</td><td><span class="state-pill" :class="spare.status === 1 ? 'active' : 'inactive'">{{ spare.status === 1 ? '启用' : '停用' }}</span></td><td><button class="table-action" type="button" @click="adjust(spare)">调整库存</button></td></tr></tbody></table></div></section>
  <section class="soft-card panel consume-panel"><div class="panel-heading"><div><h3>工单领用</h3><span>仅允许执行中或待验收工单领用</span></div></div><div class="consume-form"><input v-model="consumeForm.orderId" class="form-input" type="number" min="1" placeholder="工单 ID" /><select v-model="consumeForm.spareId" class="form-input"><option value="">选择备件</option><option v-for="spare in spares.filter((item) => item.status === 1)" :key="spare.id" :value="spare.id">{{ spare.spareCode }} · {{ spare.spareName }}（库存 {{ spare.stockQty }}）</option></select><input v-model="consumeForm.quantity" class="form-input" type="number" min="1" placeholder="数量" /><input v-model="consumeForm.remark" class="form-input" placeholder="领用说明" /><button class="primary-button compact-button" type="button" @click="consume">登记领用</button></div></section>
</template>

<style scoped>
.compact-button { width: auto; padding: 0 22px; }
.spare-form-card { margin-bottom: 18px; }
.spare-form, .consume-form { display: grid; grid-template-columns: repeat(4, minmax(120px, 1fr)); gap: 12px; align-items: center; }
.spare-form .primary-button, .consume-form .primary-button { height: 44px; }
.spare-metrics { grid-template-columns: repeat(2, minmax(160px, 1fr)); margin-bottom: 18px; }
.warning-value, .warning-text { color: #b76b35; }
.consume-panel { margin-top: 18px; }
.consume-form { grid-template-columns: 1fr 2fr 1fr 2fr auto; }
@media (max-width: 1000px) { .spare-form, .consume-form { grid-template-columns: repeat(2, 1fr); } }
</style>
