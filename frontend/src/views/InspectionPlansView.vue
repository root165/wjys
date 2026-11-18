<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as inspectionApi from '@/api/inspection'

const loading = ref(false)
const plans = ref([])
const points = ref([])
const formVisible = ref(false)
const planForm = ref({ planCode: '', planName: '', cycleType: 'DAILY', cycleValue: '1', startDate: new Date().toISOString().slice(0, 10), endDate: '', responsibleUserId: null, pointIds: [] })
const statusLabels = { 0: '草稿', 1: '启用', 2: '停用' }

async function loadPlans() {
  loading.value = true
  try { plans.value = (await inspectionApi.getPlans()).data } catch (error) { ElMessage.error(error.message || '巡检计划加载失败') } finally { loading.value = false }
}

async function createPlan() {
  try { await inspectionApi.createPlan({ ...planForm.value, pointIds: planForm.value.pointIds.map(Number), responsibleUserId: planForm.value.responsibleUserId ? Number(planForm.value.responsibleUserId) : null, endDate: planForm.value.endDate || null }); ElMessage.success('巡检计划已创建'); planForm.value = { planCode: '', planName: '', cycleType: 'DAILY', cycleValue: '1', startDate: new Date().toISOString().slice(0, 10), endDate: '', responsibleUserId: null, pointIds: [] }; formVisible.value = false; await loadPlans() } catch (error) { ElMessage.error(error.message || '巡检计划创建失败') }
}

async function enable(plan) {
  try { await inspectionApi.changePlanStatus(plan.id, 1); ElMessage.success('计划已启用'); await loadPlans() } catch (error) { ElMessage.error(error.message || '计划状态更新失败') }
}

async function generate(plan) {
  try { await inspectionApi.generateTask(plan.id, new Date().toISOString().slice(0, 10)); ElMessage.success('今日任务已生成，可在任务监控中领取'); } catch (error) { ElMessage.error(error.message || '任务生成失败') }
}

onMounted(async () => { await loadPlans(); try { points.value = (await inspectionApi.getPoints()).data || [] } catch (error) { ElMessage.error(error.message || '巡检点位加载失败') } })
</script>

<template>
  <div class="page-intro"><div><h2>巡检计划</h2><p>配置巡检周期和点位，并按日期生成任务快照。</p></div><button class="primary-button compact-button" type="button" @click="formVisible = !formVisible">{{ formVisible ? '收起新增' : '新增计划' }}</button></div>
  <section v-if="formVisible" class="soft-card panel"><div class="panel-heading"><h3>新增巡检计划</h3></div><form class="plan-form" @submit.prevent="createPlan"><input v-model="planForm.planCode" class="form-input" required placeholder="计划编码" /><input v-model="planForm.planName" class="form-input" required placeholder="计划名称" /><select v-model="planForm.cycleType" class="form-input"><option value="DAILY">每天</option><option value="WEEKLY">每周</option><option value="MONTHLY">每月</option></select><input v-model="planForm.cycleValue" class="form-input" placeholder="周期值" /><input v-model="planForm.startDate" class="form-input" type="date" required /><input v-model="planForm.endDate" class="form-input" type="date" /><select v-model="planForm.pointIds" class="form-input points-select" multiple required><option v-for="point in points" :key="point.id" :value="point.id">{{ point.pointCode }} · {{ point.pointName }}</option></select><button class="primary-button compact-button" type="submit">保存计划</button></form></section>
  <section class="soft-card table-card"><div v-if="loading" class="empty-state">正在加载计划…</div><div v-else-if="!plans.length" class="empty-state">暂无巡检计划</div><div v-else class="table-wrap"><table class="data-table"><thead><tr><th>计划编码</th><th>计划名称</th><th>周期</th><th>点位数</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="plan in plans" :key="plan.id"><td><strong>{{ plan.planCode }}</strong></td><td>{{ plan.planName }}</td><td>{{ plan.cycleType }}</td><td>{{ plan.pointIds.length }}</td><td><span class="state-pill" :class="plan.status === 1 ? 'active' : 'inactive'">{{ statusLabels[plan.status] }}</span></td><td><button v-if="plan.status !== 1" class="table-action" type="button" @click="enable(plan)">启用</button><button v-else class="table-action" type="button" @click="generate(plan)">生成今日任务</button></td></tr></tbody></table></div></section>
</template>

<style scoped>
.compact-button { width: auto; padding: 0 18px; }.plan-form { display: grid; grid-template-columns: repeat(4, minmax(150px, 1fr)); gap: 10px; align-items: center; }.points-select { min-height: 100px; grid-column: span 2; }@media (max-width: 800px) { .plan-form { grid-template-columns: repeat(2, 1fr); } }@media (max-width: 600px) { .plan-form { grid-template-columns: 1fr; }.points-select { grid-column: span 1; } }
</style>
