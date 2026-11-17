<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as inspectionApi from '@/api/inspection'

const loading = ref(false)
const tasks = ref([])
const selected = ref(null)
const statusLabels = { 0: '待领取', 1: '执行中', 2: '已完成', 3: '已逾期' }

async function loadTasks() {
  loading.value = true
  try { tasks.value = (await inspectionApi.getTasks({ pageNum: 1, pageSize: 50 })).data.list } catch (error) { ElMessage.error(error.message || '巡检任务加载失败') } finally { loading.value = false }
}

async function receive(task) {
  try { await inspectionApi.receiveTask(task.id); ElMessage.success('任务已领取'); await loadTasks() } catch (error) { ElMessage.error(error.message || '任务领取失败') }
}

async function view(task) {
  try { selected.value = (await inspectionApi.getTask(task.id)).data } catch (error) { ElMessage.error(error.message || '任务详情加载失败') }
}

onMounted(loadTasks)
</script>

<template>
  <div class="page-intro"><div><h2>巡检任务</h2><p>查看任务快照、领取状态和异常数量。</p></div></div>
  <section class="soft-card table-card"><div v-if="loading" class="empty-state">正在加载任务…</div><div v-else-if="!tasks.length" class="empty-state">暂无巡检任务，请先在计划页生成任务</div><div v-else class="table-wrap"><table class="data-table"><thead><tr><th>任务编号</th><th>计划日期</th><th>执行人</th><th>检查项</th><th>异常</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="task in tasks" :key="task.id"><td><strong>{{ task.taskNo }}</strong></td><td>{{ task.planTime }}</td><td>{{ task.assigneeId ? `用户 #${task.assigneeId}` : '未分配' }}</td><td>{{ task.totalItems }}</td><td>{{ task.abnormalCount }}</td><td><span class="state-pill" :class="task.status === 2 ? 'active' : 'inactive'">{{ statusLabels[task.status] }}</span></td><td><button class="table-action" type="button" @click="view(task)">详情</button><button v-if="task.status === 0 || task.status === 3" class="table-action" type="button" @click="receive(task)">领取</button></td></tr></tbody></table></div></section>
  <section v-if="selected" class="soft-card panel" style="margin-top: 18px"><div class="panel-heading"><h3>{{ selected.taskNo }} · 检查项快照</h3><button class="table-action" type="button" @click="selected = null">关闭</button></div><div class="signal-list"><div v-for="item in selected.items" :key="item.id" class="signal-row"><div class="signal-badge">{{ item.pointCode }}</div><div class="signal-content"><strong>{{ item.itemName }}</strong><span>{{ item.equipCode }} · {{ item.checkMethod || '现场检查' }} · {{ item.checkValue || '未填写' }}</span></div><span class="signal-state">{{ item.checkResult === 1 ? '异常' : item.checkResult === 0 ? '正常' : '待检查' }}</span></div></div></section>
</template>
