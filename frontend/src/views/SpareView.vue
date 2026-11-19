<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import * as spareApi from '@/api/spare'

const loading = ref(false)
const auth = useAuthStore()
const saving = ref(false)
const spares = ref([])
const alerts = ref([])
const keyword = ref('')
const createVisible = ref(false)
const adjustVisible = ref(false)
const consumeVisible = ref(false)
const adjustTarget = ref(null)
const consumeForm = reactive({ orderId: '', spareId: '', quantity: 1, remark: '' })
const adjustForm = reactive({ delta: 1, remark: '' })
const createForm = reactive({ spareCode: '', spareName: '', specification: '', unit: '件', stockQty: 0, warningQty: 0, unitPrice: '0.00', status: 1 })
const activeSpare = computed(() => spares.value.find((item) => String(item.id) === String(consumeForm.spareId)))
const activeUnit = computed(() => activeSpare.value?.unit || '件')
const stockedKinds = computed(() => spares.value.filter((item) => Number(item.stockQty) > 0).length)
const stockValue = computed(() => spares.value.reduce((total, item) => total + Number(item.stockQty || 0) * Number(item.unitPrice || 0), 0).toFixed(2))

async function load() {
  loading.value = true
  try {
    const [listResponse, alertResponse] = await Promise.all([spareApi.getSpares({ keyword: keyword.value.trim() || undefined }), spareApi.getSpareAlerts()])
    spares.value = listResponse.data || []
    alerts.value = alertResponse.data || []
  } catch (error) { ElMessage.error(error.message || '备件数据加载失败') } finally { loading.value = false }
}

function resetCreate() { Object.assign(createForm, { spareCode: '', spareName: '', specification: '', unit: '件', stockQty: 0, warningQty: 0, unitPrice: '0.00', status: 1 }) }
function openCreate() { resetCreate(); createVisible.value = true }
async function create() {
  if (!/^SP-[A-Z0-9]+-\d{3}$/.test(createForm.spareCode.trim())) return ElMessage.warning('备件编码应为 SP-类别-三位流水号，例如 SP-BRG-001')
  if (!createForm.spareName.trim() || !createForm.unit) return ElMessage.warning('请填写备件名称和计量单位')
  saving.value = true
  try {
    await spareApi.createSpare({ ...createForm, spareCode: createForm.spareCode.trim().toUpperCase(), spareName: createForm.spareName.trim(), specification: createForm.specification.trim(), stockQty: Number(createForm.stockQty), warningQty: Number(createForm.warningQty), unitPrice: Number(createForm.unitPrice) })
    ElMessage.success('备件已创建'); createVisible.value = false; await load()
  } catch (error) { ElMessage.error(error.message || '备件创建失败') } finally { saving.value = false }
}

function openAdjust(spare) { adjustTarget.value = spare; Object.assign(adjustForm, { delta: 1, remark: '' }); adjustVisible.value = true }
async function adjust() {
  const delta = Number(adjustForm.delta)
  if (!Number.isInteger(delta) || delta === 0) return ElMessage.warning('调整数量必须是非零整数，正数入库、负数扣减')
  if (!adjustForm.remark.trim()) return ElMessage.warning('请填写调整原因，便于追溯库存变化')
  try { await spareApi.adjustSpareStock(adjustTarget.value.id, { delta, remark: adjustForm.remark.trim() }); ElMessage.success('库存已调整'); adjustVisible.value = false; await load() } catch (error) { ElMessage.error(error.message || '库存调整失败') }
}

async function consume() {
  if (!consumeForm.orderId || !consumeForm.spareId || Number(consumeForm.quantity) <= 0) return ElMessage.warning('请填写工单 ID、备件和领用数量')
  if (Number(consumeForm.quantity) > Number(activeSpare.value?.stockQty || 0)) return ElMessage.warning(`当前库存只有 ${activeSpare.value?.stockQty || 0} ${activeUnit.value}，不能超量领用`)
  try { await spareApi.consumeSpares(consumeForm.orderId, [{ spareId: Number(consumeForm.spareId), quantity: Number(consumeForm.quantity), remark: consumeForm.remark.trim() || 'Web 工单领用' }]); ElMessage.success(`已领用 ${activeSpare.value?.spareName || '备件'}`); Object.assign(consumeForm, { orderId: '', spareId: '', quantity: 1, remark: '' }); consumeVisible.value = false; await load() } catch (error) { ElMessage.error(error.message || '备件领用失败') }
}

onMounted(load)
</script>

<template>
  <div class="page-intro"><div><h2>备件台账</h2><p>维护库存、预警阈值，并为维修工单登记备件消耗。编码和单位统一后，领用记录才容易追溯。</p></div><div class="intro-actions"><button v-if="auth.hasPermission('spare:consume')" class="secondary-button" type="button" @click="consumeVisible = true">登记工单领用</button><button v-if="auth.hasPermission('spare:write')" class="primary-button compact-button" type="button" @click="openCreate">新增备件</button></div></div>
  <section class="metric-grid spare-metrics"><div class="soft-card metric-card"><span class="metric-label">备件品类</span><strong class="metric-value">{{ spares.length }}</strong><span class="metric-foot">当前筛选结果</span></div><div class="soft-card metric-card"><span class="metric-label">当前有库存</span><strong class="metric-value">{{ stockedKinds }}</strong><span class="metric-foot">库存数量在下方表格逐项展示</span></div><div class="soft-card metric-card"><span class="metric-label">库存预警</span><strong class="metric-value warning-value">{{ alerts.length }}</strong><span class="metric-foot">库存小于等于预警阈值</span></div><div class="soft-card metric-card"><span class="metric-label">库存参考金额</span><strong class="metric-value">¥ {{ stockValue }}</strong><span class="metric-foot">库存数量 × 参考单价</span></div></section>
  <section class="soft-card table-card"><div class="toolbar"><input v-model="keyword" class="form-input" placeholder="搜索编码或名称，按 Enter 查询" @keyup.enter="load" /><button class="table-action" type="button" @click="load">刷新数据</button></div><div v-if="loading" class="empty-state">正在加载备件数据…</div><div v-else-if="!spares.length" class="empty-state">暂无备件数据。点击右上角“新增备件”建立首条库存。</div><div v-else class="table-wrap"><table class="data-table"><thead><tr><th>编码</th><th>名称</th><th>规格型号</th><th>库存</th><th>预警线</th><th>单价</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="spare in spares" :key="spare.id"><td><strong>{{ spare.spareCode }}</strong></td><td>{{ spare.spareName }}</td><td>{{ spare.specification || '—' }}</td><td><span :class="spare.warning ? 'warning-text' : ''">{{ spare.stockQty }} {{ spare.unit }}</span></td><td>{{ spare.warningQty }} {{ spare.unit }}</td><td>¥ {{ spare.unitPrice }}</td><td><span class="state-pill" :class="spare.status === 1 ? 'active' : 'inactive'">{{ spare.status === 1 ? '启用' : '停用' }}</span></td><td><button class="table-action" type="button" @click="openAdjust(spare)">调整库存</button></td></tr></tbody></table></div></section>

  <el-dialog v-model="createVisible" title="新增备件" width="620px" destroy-on-close><p class="dialog-tip">先确定编码和计量单位，再填写库存。编码用于工单消耗、库存流水和报表查询，保存后不建议随意变更。</p><form class="dialog-form" @submit.prevent="create"><div class="field-grid"><div><label class="form-label">备件编码</label><input v-model="createForm.spareCode" class="form-input" placeholder="SP-BRG-001" required /><p class="field-help">格式：SP-类别-三位流水号，例如轴承可用 SP-BRG-001。</p></div><div><label class="form-label">备件名称</label><input v-model="createForm.spareName" class="form-input" placeholder="例如：深沟球轴承" required /><p class="field-help">填写现场人员能直接识别的名称。</p></div><div><label class="form-label">规格型号</label><input v-model="createForm.specification" class="form-input" placeholder="例如：6205-2RS / 10A / ISO VG68" /><p class="field-help">填写厂商型号、尺寸、材质或等级。</p></div><div><label class="form-label">计量单位</label><el-select v-model="createForm.unit" class="full-control" placeholder="选择领用单位"><el-option v-for="unit in ['件', '个', '盒', '套', '米', '升']" :key="unit" :label="unit" :value="unit" /></el-select><p class="field-help">领用和库存扣减都会按此单位计算。</p></div><div><label class="form-label">初始库存</label><input v-model="createForm.stockQty" class="form-input" type="number" min="0" step="1" required /><p class="field-help">首次入库数量，只能填写 0 或正整数。</p></div><div><label class="form-label">预警阈值</label><input v-model="createForm.warningQty" class="form-input" type="number" min="0" step="1" required /><p class="field-help">库存小于等于此值时进入预警。</p></div><div><label class="form-label">参考单价（元）</label><input v-model="createForm.unitPrice" class="form-input" type="number" min="0" step="0.01" required /><p class="field-help">用于消耗金额估算，不等同于采购结算价。</p></div></div><div class="dialog-actions"><button class="secondary-button" type="button" @click="createVisible = false">取消</button><button class="primary-button compact-button" type="submit" :disabled="saving">保存备件</button></div></form></el-dialog>
  <el-dialog v-model="adjustVisible" :title="`调整库存 · ${adjustTarget?.spareName || ''}`" width="480px" destroy-on-close><p class="dialog-tip">当前库存 {{ adjustTarget?.stockQty || 0 }} {{ adjustTarget?.unit || '' }}。正数表示入库，负数表示盘亏、报废或其他扣减。</p><form class="dialog-form" @submit.prevent="adjust"><label class="form-label">调整数量</label><input v-model="adjustForm.delta" class="form-input" type="number" step="1" placeholder="例如：10 或 -2" required /><label class="form-label">调整原因</label><textarea v-model="adjustForm.remark" class="form-textarea" rows="3" placeholder="例如：2026-11-19 到货入库 / 盘点发现损耗" required></textarea><div class="dialog-actions"><button class="secondary-button" type="button" @click="adjustVisible = false">取消</button><button class="primary-button compact-button" type="submit">保存流水</button></div></form></el-dialog>
  <el-dialog v-model="consumeVisible" title="登记工单领用" width="520px" destroy-on-close><p class="dialog-tip">只有执行中或待验收的维修工单可以领用备件；系统会校验库存并生成可追溯的消耗流水。</p><form class="dialog-form" @submit.prevent="consume"><label class="form-label">工单 ID</label><input v-model="consumeForm.orderId" class="form-input" type="number" min="1" placeholder="填写维修工单 ID，例如 1001" required /><label class="form-label">备件</label><el-select v-model="consumeForm.spareId" class="full-control" filterable clearable placeholder="选择要领用的备件"><el-option v-for="spare in spares.filter((item) => item.status === 1)" :key="spare.id" :label="`${spare.spareCode} · ${spare.spareName}（库存 ${spare.stockQty} ${spare.unit}）`" :value="spare.id" /></el-select><label class="form-label">领用数量</label><input v-model="consumeForm.quantity" class="form-input" type="number" min="1" step="1" :placeholder="`按 ${activeUnit} 填写`" required /><label class="form-label">领用说明</label><textarea v-model="consumeForm.remark" class="form-textarea" rows="3" placeholder="例如：更换一号装配线主轴轴承"></textarea><div class="dialog-actions"><button class="secondary-button" type="button" @click="consumeVisible = false">取消</button><button class="primary-button compact-button" type="submit">登记领用</button></div></form></el-dialog>
</template>

<style scoped>
.compact-button { width: auto; padding: 0 22px; }.intro-actions { display: flex; gap: 10px; }.secondary-button { width: auto; min-height: 44px; padding: 0 18px; border: 1px solid #cbd9ce; border-radius: 9px; background: #f7faf7; color: #335f4d; cursor: pointer; }.secondary-button:hover { border-color: #8bb197; background: #eef6ed; }.spare-metrics { grid-template-columns: repeat(4, minmax(150px, 1fr)); margin-bottom: 18px; }.warning-value, .warning-text { color: #b76b35; }.dialog-tip { margin: -4px 0 14px; color: #789087; font-size: 13px; line-height: 1.7; }.dialog-form { display: grid; gap: 2px; }.field-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 2px 16px; }.full-control { width: 100%; }.field-help { margin: 4px 0 10px; color: #789087; font-size: 12px; line-height: 1.55; }.form-textarea { width: 100%; padding: 11px 13px; border: 1px solid #d7e2d9; border-radius: 8px; outline: 0; background: #fbfdfb; color: #23493b; font: inherit; resize: vertical; }.form-textarea:focus { border-color: #7aaa8c; box-shadow: 0 0 0 3px rgba(122,170,140,.12); }.dialog-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 18px; }.dialog-actions .primary-button { width: auto; }.table-action { margin-right: 8px; }
@media (max-width: 1000px) { .spare-metrics { grid-template-columns: repeat(2, minmax(160px, 1fr)); } }@media (max-width: 700px) { .field-grid { grid-template-columns: 1fr; }.intro-actions { flex-direction: column; }.intro-actions button { width: 100%; } }
</style>
