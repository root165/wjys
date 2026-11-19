<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import * as inspectionApi from '@/api/inspection'

const loading = ref(false)
const auth = useAuthStore()
const plans = ref([])
const points = ref([])
const assignees = ref([])
const formVisible = ref(false)
const editingId = ref(null)
const planForm = ref(emptyForm())
const statusLabels = { 0: '草稿', 1: '启用', 2: '停用' }
const assignmentLabels = { SELF_CLAIM: '自行领取', FIXED_USER: '固定执行人', EQUIPMENT_MANAGER: '设备责任人', TEAM_ROUND_ROBIN: '班组轮询' }
const departments = computed(() => [...new Map(assignees.value.filter((user) => user.deptId).map((user) => [user.deptId, user.deptName || `部门 #${user.deptId}`])).entries()])

function emptyForm() { return { planCode: '', planName: '', cycleType: 'DAILY', cycleValue: '1', startDate: new Date().toISOString().slice(0, 10), endDate: '', executeTime: '09:00', assignmentMode: 'SELF_CLAIM', assignmentDeptId: null, responsibleUserId: null, pointIds: [] } }
async function loadPlans() { loading.value = true; try { plans.value = (await inspectionApi.getPlans()).data || [] } catch (error) { ElMessage.error(error.message || '巡检计划加载失败') } finally { loading.value = false } }
async function savePlan() { try { const payload = { ...planForm.value, pointIds: planForm.value.pointIds.map(Number), assignmentDeptId: planForm.value.assignmentDeptId ? Number(planForm.value.assignmentDeptId) : null, responsibleUserId: planForm.value.responsibleUserId ? Number(planForm.value.responsibleUserId) : null, endDate: planForm.value.endDate || null }; if (editingId.value) await inspectionApi.updatePlan(editingId.value, payload); else await inspectionApi.createPlan(payload); ElMessage.success(editingId.value ? '巡检计划已更新' : '巡检计划已创建'); resetForm(); formVisible.value = false; await loadPlans() } catch (error) { ElMessage.error(error.message || (editingId.value ? '巡检计划更新失败' : '巡检计划创建失败')) } }
function resetForm() { editingId.value = null; planForm.value = emptyForm() }
function openCreate() { resetForm(); formVisible.value = true }
function openEdit(plan) { editingId.value = plan.id; planForm.value = { planCode: plan.planCode, planName: plan.planName, cycleType: plan.cycleType, cycleValue: plan.cycleValue || '1', startDate: plan.startDate, endDate: plan.endDate || '', executeTime: (plan.executeTime || '09:00').slice(0, 5), assignmentMode: plan.assignmentMode || 'SELF_CLAIM', assignmentDeptId: plan.assignmentDeptId || null, responsibleUserId: plan.responsibleUserId || null, pointIds: [...(plan.pointIds || [])] }; formVisible.value = true }
async function enable(plan) { try { await inspectionApi.changePlanStatus(plan.id, 1); ElMessage.success('计划已启用'); await loadPlans() } catch (error) { ElMessage.error(error.message || '计划状态更新失败') } }
async function generate(plan) { try { await inspectionApi.generateTask(plan.id, new Date().toISOString().slice(0, 10)); ElMessage.success('今日任务已生成，可在移动端领取') } catch (error) { ElMessage.error(error.message || '任务生成失败') } }
onMounted(async () => { await loadPlans(); try { const [pointResponse, assigneeResponse] = await Promise.all([inspectionApi.getPoints(), inspectionApi.getAssignees()]); points.value = pointResponse.data || []; assignees.value = assigneeResponse.data || [] } catch (error) { ElMessage.error(error.message || '巡检计划配置数据加载失败') } })
</script>

<template>
  <div class="page-intro"><div><h2>巡检计划</h2><p>配置周期、执行时刻、点位和派发方式；计划只定义规则，任务由调度器生成。</p></div><button v-if="auth.hasPermission('inspection:plan:write')" class="primary-button compact-button" type="button" @click="formVisible ? (formVisible = false) : openCreate()">{{ formVisible ? '收起表单' : '新增计划' }}</button></div>
  <el-dialog v-model="formVisible" :title="editingId ? '编辑巡检计划' : '新增巡检计划'" width="760px" destroy-on-close>
    <p class="dialog-tip">计划定义“多久巡一次、哪天几点执行、巡哪些点位”。到达执行时刻后生成当天任务；只有巡检员能在移动端领取和提交，管理员只负责配置与监管。</p>
    <form class="plan-form" @submit.prevent="savePlan">
      <div><label class="form-label">计划编码</label><input v-model="planForm.planCode" class="form-input" required placeholder="例如 PLAN-WEEKLY-001" /><p class="field-help">用于任务编号和报表识别，必须唯一。</p></div>
      <div><label class="form-label">计划名称</label><input v-model="planForm.planName" class="form-input" required placeholder="例如 空压机每周巡检" /></div>
      <div><label class="form-label">巡检周期</label><select v-model="planForm.cycleType" class="form-input"><option value="DAILY">按天</option><option value="WEEKLY">按周</option><option value="MONTHLY">按月</option></select></div>
      <div><label class="form-label">周期值</label><input v-model="planForm.cycleValue" class="form-input" placeholder="按天填间隔天数；按周填星期 1-7；按月填日期 1-31" /></div>
      <div><label class="form-label">开始日期</label><input v-model="planForm.startDate" class="form-input" type="date" required /></div>
      <div><label class="form-label">结束日期（可选）</label><input v-model="planForm.endDate" class="form-input" type="date" /></div>
      <div><label class="form-label">执行时间</label><input v-model="planForm.executeTime" class="form-input" type="time" required /><p class="field-help">到达该时刻生成当天任务；30 分钟未完成会标记逾期，但仍允许补交。</p></div>
      <div><label class="form-label">派发方式</label><select v-model="planForm.assignmentMode" class="form-input"><option value="SELF_CLAIM">巡检员自行领取（推荐）</option><option value="FIXED_USER">固定执行人</option><option value="EQUIPMENT_MANAGER">设备责任人</option><option value="TEAM_ROUND_ROBIN">班组轮询</option></select><p class="field-help">自行领取不预绑定人员；设备责任人模式要求所有点位对应同一名巡检员。</p></div>
      <div v-if="planForm.assignmentMode === 'FIXED_USER'"><label class="form-label">固定执行人</label><el-select v-model="planForm.responsibleUserId" class="full-control" filterable placeholder="选择一名启用中的巡检员"><el-option v-for="user in assignees" :key="user.id" :label="`${user.nickname} · ${user.username}`" :value="user.id" /></el-select></div>
      <div v-if="planForm.assignmentMode === 'TEAM_ROUND_ROBIN'"><label class="form-label">轮询部门</label><el-select v-model="planForm.assignmentDeptId" class="full-control" filterable placeholder="选择有巡检员的部门"><el-option v-for="dept in departments" :key="dept[0]" :label="dept[1]" :value="dept[0]" /></el-select></div>
      <div class="point-picker"><label class="form-label">关联巡检点位</label><el-select v-model="planForm.pointIds" class="full-control" multiple filterable collapse-tags collapse-tags-tooltip placeholder="选择一个或多个点位" required><el-option v-for="point in points" :key="point.id" :label="`${point.pointCode} · ${point.pointName}`" :value="point.id" /></el-select><p class="field-help">任务会按点位和检查项生成历史快照，后续修改模板不会改变已生成任务。</p></div>
      <div class="dialog-actions"><button class="secondary-button compact-button" type="button" @click="formVisible = false">取消</button><button class="primary-button compact-button" type="submit">{{ editingId ? '保存修改' : '保存计划' }}</button></div>
    </form>
  </el-dialog>
  <section class="soft-card table-card"><div v-if="loading" class="empty-state">正在加载计划…</div><div v-else-if="!plans.length" class="empty-state">暂无巡检计划</div><div v-else class="table-wrap"><table class="data-table"><thead><tr><th>计划编码</th><th>计划名称</th><th>周期</th><th>执行时间</th><th>派发方式</th><th>点位数</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="plan in plans" :key="plan.id"><td><strong>{{ plan.planCode }}</strong></td><td>{{ plan.planName }}</td><td>{{ plan.cycleType }}</td><td>{{ plan.executeTime || '09:00' }}</td><td>{{ assignmentLabels[plan.assignmentMode] || '自行领取' }}</td><td>{{ plan.pointIds.length }}</td><td><span class="state-pill" :class="plan.status === 1 ? 'active' : 'inactive'">{{ statusLabels[plan.status] }}</span></td><td><button class="table-action" type="button" @click="openEdit(plan)">编辑</button><button v-if="plan.status !== 1" class="table-action" type="button" @click="enable(plan)">启用</button><button v-else class="table-action" type="button" @click="generate(plan)">手动生成今日任务</button></td></tr></tbody></table></div></section>
</template>

<style scoped>
.compact-button { width: auto; padding: 0 18px; }.plan-form { display: grid; grid-template-columns: repeat(2, minmax(150px, 1fr)); gap: 4px 14px; align-items: start; }.point-picker { grid-column: span 2; }.full-control { width: 100%; }.dialog-tip { margin: -4px 0 16px; color: #789087; font-size: 13px; line-height: 1.7; }.field-help { margin: 4px 0 10px; color: #789087; font-size: 12px; line-height: 1.55; }.dialog-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 14px; grid-column: span 2; }.dialog-actions .primary-button, .dialog-actions .secondary-button { width: auto; }@media (max-width: 600px) { .plan-form { grid-template-columns: 1fr; }.point-picker, .dialog-actions { grid-column: span 1; } }
</style>
