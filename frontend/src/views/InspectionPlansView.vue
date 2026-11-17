<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as inspectionApi from '@/api/inspection'

const loading = ref(false)
const plans = ref([])
const statusLabels = { 0: '草稿', 1: '启用', 2: '停用' }

async function loadPlans() {
  loading.value = true
  try { plans.value = (await inspectionApi.getPlans()).data } catch (error) { ElMessage.error(error.message || '巡检计划加载失败') } finally { loading.value = false }
}

async function enable(plan) {
  try { await inspectionApi.changePlanStatus(plan.id, 1); ElMessage.success('计划已启用'); await loadPlans() } catch (error) { ElMessage.error(error.message || '计划状态更新失败') }
}

async function generate(plan) {
  try { await inspectionApi.generateTask(plan.id, new Date().toISOString().slice(0, 10)); ElMessage.success('今日任务已生成，可在任务监控中领取'); } catch (error) { ElMessage.error(error.message || '任务生成失败') }
}

onMounted(loadPlans)
</script>

<template>
  <div class="page-intro"><div><h2>巡检计划</h2><p>配置巡检周期和点位，并按日期生成任务快照。</p></div></div>
  <section class="soft-card table-card"><div v-if="loading" class="empty-state">正在加载计划…</div><div v-else-if="!plans.length" class="empty-state">暂无巡检计划</div><div v-else class="table-wrap"><table class="data-table"><thead><tr><th>计划编码</th><th>计划名称</th><th>周期</th><th>点位数</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="plan in plans" :key="plan.id"><td><strong>{{ plan.planCode }}</strong></td><td>{{ plan.planName }}</td><td>{{ plan.cycleType }}</td><td>{{ plan.pointIds.length }}</td><td><span class="state-pill" :class="plan.status === 1 ? 'active' : 'inactive'">{{ statusLabels[plan.status] }}</span></td><td><button v-if="plan.status !== 1" class="table-action" type="button" @click="enable(plan)">启用</button><button v-else class="table-action" type="button" @click="generate(plan)">生成今日任务</button></td></tr></tbody></table></div></section>
</template>
