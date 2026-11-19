<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as inspectionApi from '@/api/inspection'

const loading = ref(false)
const tasks = ref([])
const selected = ref(null)
const detailVisible = ref(false)
const statusLabels = { 0: '待领取', 1: '执行中', 2: '已完成', 3: '已逾期' }

async function loadTasks() {
  loading.value = true
  try { tasks.value = (await inspectionApi.getTasks({ pageNum: 1, pageSize: 50 })).data.list } catch (error) { ElMessage.error(error.message || '巡检任务加载失败') } finally { loading.value = false }
}

async function view(task) {
  try { selected.value = (await inspectionApi.getTask(task.id)).data; detailVisible.value = true } catch (error) { ElMessage.error(error.message || '任务详情加载失败') }
}

onMounted(loadTasks)
</script>

<template>
  <div class="page-intro"><div><h2>巡检任务</h2><p>管理端用于查看任务快照、执行人和异常数量；领取与填写请由巡检员在移动端完成。</p></div></div>
  <section class="soft-card table-card"><div v-if="loading" class="empty-state">正在加载任务…</div><div v-else-if="!tasks.length" class="empty-state">暂无巡检任务，请先在计划页生成任务</div><div v-else class="table-wrap"><table class="data-table"><thead><tr><th>巡检任务</th><th>计划日期</th><th>执行人</th><th>检查项</th><th>异常</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="task in tasks" :key="task.id"><td><strong>{{ task.planName || '未命名计划' }}</strong><span class="table-subtitle">{{ task.taskNo }}</span></td><td>{{ task.planTime }}</td><td>{{ task.assigneeId ? `用户 #${task.assigneeId}` : '待巡检员领取' }}</td><td>{{ task.totalItems }}</td><td><span :class="task.abnormalCount ? 'warning-text' : ''">{{ task.abnormalCount }}</span></td><td><span class="state-pill" :class="task.status === 2 ? 'active' : 'inactive'">{{ statusLabels[task.status] }}</span></td><td><button class="table-action" type="button" @click="view(task)">查看详情</button></td></tr></tbody></table></div></section>
  <el-dialog v-if="selected" v-model="detailVisible" :title="`${selected.planName || '巡检任务'} · 任务详情`" width="820px" destroy-on-close>
    <div class="task-detail-head"><div><span class="detail-label">任务编号</span><strong>{{ selected.taskNo }}</strong></div><div><span class="detail-label">计划日期</span><strong>{{ selected.planTime }}</strong></div><div><span class="detail-label">执行人</span><strong>{{ selected.assigneeId ? `用户 #${selected.assigneeId}` : '待巡检员领取' }}</strong></div><div><span class="detail-label">状态</span><span class="state-pill" :class="selected.status === 2 ? 'active' : 'inactive'">{{ statusLabels[selected.status] }}</span></div></div>
    <div class="detail-section"><div class="panel-heading"><div><h3>检查项快照</h3><span>生成任务时保存的现场检查范围</span></div><span>{{ selected.items?.length || 0 }} 项 · 异常 {{ selected.abnormalCount || 0 }} 项</span></div><div class="task-item-list"><article v-for="item in selected.items" :key="item.id" class="task-item"><div class="task-item-title"><strong>{{ item.itemName }}</strong><span class="signal-state" :class="item.checkResult === 1 ? 'warning-text' : ''">{{ item.checkResult === 1 ? '异常' : item.checkResult === 0 ? '正常' : '待检查' }}</span></div><div class="task-item-meta"><span>点位：{{ item.pointCode }}</span><span>设备：{{ item.equipCode }}</span><span>方法：{{ item.checkMethod || '现场检查' }}</span><span v-if="item.unit">单位：{{ item.unit }}</span></div><p class="task-item-value">记录值：{{ item.checkValue || '未填写' }}<span v-if="item.remark"> · {{ item.remark }}</span></p></article></div></div>
  </el-dialog>
</template>

<style scoped>
.table-subtitle { display: block; margin-top: 5px; color: #87988f; font-size: 12px; font-weight: 400; }
.warning-text { color: #b76b35; }
.task-detail-head { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; padding: 16px; border: 1px solid #e0e9e1; border-radius: 12px; background: #f8fbf8; }
.detail-label { display: block; margin-bottom: 7px; color: #87988f; font-size: 12px; }
.detail-section { margin-top: 20px; }
.task-item-list { display: grid; gap: 10px; }
.task-item { padding: 15px 16px; border: 1px solid #e3ebe4; border-radius: 10px; background: #fff; }
.task-item-title, .task-item-meta { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.task-item-meta { justify-content: flex-start; flex-wrap: wrap; margin-top: 9px; color: #789087; font-size: 12px; }
.task-item-value { margin: 10px 0 0; color: #335f4d; font-size: 13px; line-height: 1.6; }
@media (max-width: 700px) { .task-detail-head { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
</style>
