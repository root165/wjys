<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as maintenanceApi from '@/api/maintenance'
import * as equipmentApi from '@/api/equipment'

const loading = ref(false)
const saving = ref(false)
const plans = ref([])
const records = ref([])
const equipments = ref([])
const planFormVisible = ref(false)
const planForm = reactive({ planCode: '', planName: '', equipId: '', maintainType: 0, cycleType: 1, cycleValue: 1, content: '', assigneeId: '', nextDate: '' })
const recordForm = reactive({ planId: '', equipId: '', maintainDate: new Date().toISOString().slice(0, 10), maintainContent: '', maintainResult: 0, cost: '', downtimeMinutes: 0, fileIds: [], remark: '' })
const typeLabels = ['日常保养', '一级保养', '二级保养', '专项保养']
const cycleLabels = ['按小时', '按天', '按周', '按月']
const recordStatusLabels = ['待审核', '已归档', '已退回']
const uploading = ref(false)

async function load() {
  loading.value = true
  try {
    const [planResponse, recordResponse, equipmentResponse] = await Promise.all([
      maintenanceApi.getMaintenancePlans(),
      maintenanceApi.getMaintenanceRecords(),
      equipmentApi.getEquipments({ pageSize: 100 }),
    ])
    plans.value = planResponse.data || []
    records.value = recordResponse.data || []
    equipments.value = equipmentResponse.data?.list || []
  } catch (error) { ElMessage.error(error.message || '维保数据加载失败') } finally { loading.value = false }
}

async function createPlan() {
  if (!planForm.nextDate || !planForm.equipId) { ElMessage.warning('请填写设备和下次保养日期'); return }
  saving.value = true
  try {
    await maintenanceApi.createMaintenancePlan({ ...planForm, equipId: Number(planForm.equipId), cycleValue: Number(planForm.cycleValue), assigneeId: planForm.assigneeId ? Number(planForm.assigneeId) : null })
    ElMessage.success('保养计划已创建')
    Object.assign(planForm, { planCode: '', planName: '', equipId: '', maintainType: 0, cycleType: 1, cycleValue: 1, content: '', assigneeId: '', nextDate: '' })
    planFormVisible.value = false
    await load()
  } catch (error) { ElMessage.error(error.message || '保养计划创建失败') } finally { saving.value = false }
}

function selectPlan() {
  const plan = plans.value.find((item) => String(item.id) === String(recordForm.planId))
  if (plan) { recordForm.equipId = plan.equipId; if (!recordForm.maintainDate) recordForm.maintainDate = plan.nextDate }
}

async function uploadPhoto(event) {
  const file = event.target.files?.[0]
  if (!file) return
  uploading.value = true
  try {
    const response = await maintenanceApi.uploadMaintenancePhoto(file)
    recordForm.fileIds.push(response.data.fileId)
    ElMessage.success(`照片已上传，文件 #${response.data.fileId} 待提交绑定`)
  } catch (error) { ElMessage.error(error.message || '照片上传失败') } finally { uploading.value = false; event.target.value = '' }
}

async function submitRecord() {
  if (!recordForm.equipId || !recordForm.maintainContent || recordForm.fileIds.length === 0) { ElMessage.warning('请填写设备、保养内容并至少上传一张照片'); return }
  saving.value = true
  try {
    await maintenanceApi.submitMaintenanceRecord({ ...recordForm, planId: recordForm.planId ? Number(recordForm.planId) : null, equipId: Number(recordForm.equipId), cost: recordForm.cost === '' ? null : Number(recordForm.cost), downtimeMinutes: Number(recordForm.downtimeMinutes), fileIds: recordForm.fileIds })
    ElMessage.success('维保记录已提交，等待审核')
    Object.assign(recordForm, { planId: '', equipId: '', maintainDate: new Date().toISOString().slice(0, 10), maintainContent: '', maintainResult: 0, cost: '', downtimeMinutes: 0, fileIds: [], remark: '' })
    await load()
  } catch (error) { ElMessage.error(error.message || '维保记录提交失败') } finally { saving.value = false }
}

async function audit(record, pass) {
  const remark = window.prompt(pass ? '请输入归档意见（可留空）' : '请输入退回原因')
  if (!pass && !remark) return
  try { await maintenanceApi.auditMaintenanceRecord(record.id, { pass, remark }); ElMessage.success(pass ? '记录已归档' : '记录已退回'); await load() } catch (error) { ElMessage.error(error.message || '审核失败') }
}

async function toRepair(record) {
  try { await maintenanceApi.createRepairFromMaintenance(record.id); ElMessage.success('已生成报修单'); await load() } catch (error) { ElMessage.error(error.message || '转报修失败') }
}

async function togglePlan(plan) {
  try { await maintenanceApi.updateMaintenancePlanStatus(plan.id, plan.status === 1 ? 0 : 1); ElMessage.success('计划状态已更新'); await load() } catch (error) { ElMessage.error(error.message || '计划状态更新失败') }
}

onMounted(load)
</script>

<template>
  <div class="page-intro"><div><h2>维保管理</h2><p>配置设备保养周期，提交现场记录并完成审核归档。</p></div><div class="intro-actions"><button class="table-action" type="button" @click="load">刷新</button><button class="primary-button compact-button" type="button" @click="planFormVisible = !planFormVisible">{{ planFormVisible ? '收起计划' : '新建计划' }}</button></div></div>
  <section v-if="planFormVisible" class="soft-card panel maintenance-form-card"><div class="panel-heading"><h3>新建保养计划</h3></div><form class="maintenance-form" @submit.prevent="createPlan"><input v-model="planForm.planCode" class="form-input" required placeholder="计划编码" /><input v-model="planForm.planName" class="form-input" required placeholder="计划名称" /><select v-model="planForm.equipId" class="form-input" required><option value="">选择设备</option><option v-for="equipment in equipments" :key="equipment.id" :value="equipment.id">{{ equipment.equipCode }} · {{ equipment.equipName }}</option></select><select v-model.number="planForm.maintainType" class="form-input"><option v-for="(label, index) in typeLabels" :key="label" :value="index">{{ label }}</option></select><select v-model.number="planForm.cycleType" class="form-input"><option v-for="(label, index) in cycleLabels" :key="label" :value="index">{{ label }}</option></select><input v-model.number="planForm.cycleValue" class="form-input" type="number" min="1" required placeholder="周期值" /><input v-model="planForm.nextDate" class="form-input" type="date" required /><input v-model="planForm.content" class="form-input wide-input" placeholder="保养内容" /><button class="primary-button compact-button" type="submit" :disabled="saving">保存计划</button></form></section>
  <section class="soft-card table-card"><div class="panel-heading"><div><h3>保养计划</h3><span>{{ plans.length }} 条计划 · 启用计划按 nextDate 提醒</span></div></div><div v-if="loading" class="empty-state">正在加载维保数据…</div><div v-else-if="!plans.length" class="empty-state">暂无保养计划</div><div v-else class="table-wrap"><table class="data-table"><thead><tr><th>计划</th><th>设备</th><th>类型</th><th>周期</th><th>下次日期</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="plan in plans" :key="plan.id"><td><strong>{{ plan.planCode }}</strong><span class="table-subtitle">{{ plan.planName }}</span></td><td>设备 #{{ plan.equipId }}</td><td>{{ typeLabels[plan.maintainType] }}</td><td>{{ cycleLabels[plan.cycleType] }} {{ plan.cycleValue }}</td><td>{{ plan.nextDate }}</td><td><span class="state-pill" :class="plan.status === 1 ? 'active' : 'inactive'">{{ plan.status === 1 ? '启用' : '停用' }}</span></td><td><button class="table-action" type="button" @click="togglePlan(plan)">{{ plan.status === 1 ? '停用' : '启用' }}</button></td></tr></tbody></table></div></section>
  <section class="soft-card panel record-submit-panel"><div class="panel-heading"><div><h3>提交维保记录</h3><span>记录归档前可审核退回，归档后不可改写</span></div></div><form class="record-form" @submit.prevent="submitRecord"><select v-model="recordForm.planId" class="form-input" @change="selectPlan"><option value="">临时记录</option><option v-for="plan in plans.filter((item) => item.status === 1)" :key="plan.id" :value="plan.id">{{ plan.planCode }} · {{ plan.planName }}</option></select><select v-model="recordForm.equipId" class="form-input" required><option value="">选择设备</option><option v-for="equipment in equipments" :key="equipment.id" :value="equipment.id">{{ equipment.equipCode }} · {{ equipment.equipName }}</option></select><input v-model="recordForm.maintainDate" class="form-input" type="date" required /><select v-model.number="recordForm.maintainResult" class="form-input"><option :value="0">正常</option><option :value="1">异常</option></select><input v-model="recordForm.cost" class="form-input" type="number" min="0" step="0.01" placeholder="费用" /><input v-model="recordForm.downtimeMinutes" class="form-input" type="number" min="0" placeholder="停机分钟" /><textarea v-model="recordForm.maintainContent" class="form-input wide-input" required minlength="10" placeholder="保养内容（至少 10 字）"></textarea><label class="upload-button">{{ uploading ? '上传中…' : '上传现场照片' }}<input type="file" accept="image/*" :disabled="uploading" @change="uploadPhoto" /></label><span class="photo-count">已上传 {{ recordForm.fileIds.length }} 张</span><button class="primary-button compact-button" type="submit" :disabled="saving || uploading">提交记录</button></form></section>
  <section class="soft-card table-card"><div class="panel-heading"><div><h3>维保记录</h3><span>异常结果归档后可转报修</span></div></div><div v-if="!records.length" class="empty-state">暂无维保记录</div><div v-else class="table-wrap"><table class="data-table"><thead><tr><th>记录编号</th><th>设备</th><th>日期</th><th>结果</th><th>费用</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="record in records" :key="record.id"><td><strong>{{ record.recordCode }}</strong><span class="table-subtitle">{{ record.maintainContent }}</span></td><td>设备 #{{ record.equipId }}</td><td>{{ record.maintainDate }}</td><td><span :class="record.maintainResult === 1 ? 'warning-text' : ''">{{ record.maintainResult === 1 ? '异常' : '正常' }}</span></td><td>¥ {{ record.cost || '0.00' }}</td><td><span class="state-pill" :class="record.status === 1 ? 'active' : 'inactive'">{{ recordStatusLabels[record.status] }}</span></td><td class="row-actions"><button v-if="record.status === 0" class="table-action" type="button" @click="audit(record, true)">归档</button><button v-if="record.status === 0" class="table-action danger-action" type="button" @click="audit(record, false)">退回</button><button v-if="record.status === 1 && record.maintainResult === 1" class="table-action" type="button" @click="toRepair(record)">转报修</button></td></tr></tbody></table></div></section>
</template>

<style scoped>
.compact-button { width: auto; padding: 0 22px; }.intro-actions, .row-actions { display: flex; gap: 10px; align-items: center; }.maintenance-form-card, .record-submit-panel { margin-bottom: 18px; }.maintenance-form, .record-form { display: grid; grid-template-columns: repeat(4, minmax(150px, 1fr)); gap: 12px; align-items: center; }.wide-input { grid-column: span 2; }.record-form textarea { min-height: 72px; padding-top: 12px; resize: vertical; }.upload-button { display: inline-flex; align-items: center; justify-content: center; min-height: 44px; border: 1px dashed #b3c3b5; border-radius: 12px; color: #416b5a; cursor: pointer; font-size: 13px; }.upload-button input { display: none; }.photo-count, .table-subtitle { display: block; margin-top: 4px; color: #87988f; font-size: 12px; }.warning-text { color: #b76b35; }.danger-action { color: #a15b4e; }.row-actions { flex-wrap: wrap; }@media (max-width: 1000px) { .maintenance-form, .record-form { grid-template-columns: repeat(2, 1fr); }.wide-input { grid-column: span 2; } }@media (max-width: 640px) { .maintenance-form, .record-form { grid-template-columns: 1fr; }.wide-input { grid-column: span 1; }.page-intro { align-items: start; gap: 14px; flex-direction: column; } }
</style>
