<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as inspectionApi from '@/api/inspection'

const loading = ref(false)
const abnormalities = ref([])
const status = ref('')
async function load() {
  loading.value = true
  try { abnormalities.value = (await inspectionApi.getAbnormals(status.value)).data || [] } catch (error) { ElMessage.error(error.message || '异常列表加载失败') } finally { loading.value = false }
}
async function toRepair(item) {
  try { await inspectionApi.createRepairFromAbnormal(item.id); ElMessage.success('已生成维修报修单'); await load() } catch (error) { ElMessage.error(error.message || '转报修失败') }
}
onMounted(load)
</script>

<template>
  <div class="page-intro"><div><h2>异常管理</h2><p>集中查看巡检异常，并将未处理异常转入维修工单。</p></div><div class="toolbar"><select v-model="status" class="form-input" @change="load"><option value="">全部状态</option><option value="0">未处理</option><option value="1">已转报修</option></select><button class="table-action" type="button" @click="load">刷新</button></div></div>
  <section class="soft-card table-card"><div v-if="loading" class="empty-state">正在加载异常…</div><div v-else-if="!abnormalities.length" class="empty-state">暂无异常记录</div><table v-else class="data-table"><thead><tr><th>异常编号</th><th>设备/点位</th><th>说明</th><th>发现时间</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="item in abnormalities" :key="item.id"><td>#{{ item.id }} · 任务 #{{ item.taskId }}</td><td>设备 #{{ item.equipId }} · 点位 #{{ item.pointId }}</td><td>{{ item.description || '—' }}</td><td>{{ item.createdTime || '—' }}</td><td>{{ item.status === 1 ? '已转报修' : '待处理' }}</td><td><button v-if="item.status !== 1" class="table-action" type="button" @click="toRepair(item)">转报修</button></td></tr></tbody></table></section>
</template>
