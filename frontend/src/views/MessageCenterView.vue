<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as messageApi from '@/api/message'

const loading = ref(false)
const messages = ref([])
const filter = ref('')

async function load() {
  loading.value = true
  try { const response = await messageApi.getMessages(filter.value === '' ? {} : { readFlag: Number(filter.value) }); messages.value = response.data || [] } catch (error) { ElMessage.error(error.message || '消息加载失败') } finally { loading.value = false }
}

async function markRead(message) {
  if (message.readFlag === 1) return
  try { await messageApi.markMessageRead(message.id); message.readFlag = 1; ElMessage.success('消息已读') } catch (error) { ElMessage.error(error.message || '消息标记失败') }
}

function formatTime(value) { return value ? value.replace('T', ' ').slice(0, 16) : '—' }
onMounted(load)
</script>

<template>
  <div class="page-intro"><div><h2>消息中心</h2><p>查看系统提醒、工单流转和统计任务通知。</p></div><div class="message-filter"><select v-model="filter" class="form-input" @change="load"><option value="">全部消息</option><option value="0">未读消息</option><option value="1">已读消息</option></select><button class="table-action" type="button" @click="load">刷新</button></div></div>
  <section class="soft-card table-card"><div v-if="loading" class="empty-state">正在加载消息…</div><div v-else-if="!messages.length" class="empty-state">暂无消息</div><div v-else class="message-list"><article v-for="message in messages" :key="message.id" class="message-item" :class="{ unread: message.readFlag === 0 }"><div class="message-mark">{{ message.readFlag === 0 ? '!' : '✓' }}</div><div class="message-body"><div class="message-title"><strong>{{ message.title }}</strong><span>{{ formatTime(message.createdTime) }}</span></div><p>{{ message.content }}</p><small>{{ message.bizType || '系统通知' }}<template v-if="message.bizId"> · #{{ message.bizId }}</template></small></div><button v-if="message.readFlag === 0" class="table-action" type="button" @click="markRead(message)">标记已读</button></article></div></section>
</template>

<style scoped>
.message-filter { display: flex; align-items: center; gap: 14px; }.message-filter .form-input { width: 140px; height: 38px; }.message-list { display: grid; }.message-item { display: flex; align-items: center; gap: 14px; padding: 20px 22px; border-bottom: 1px solid #edf2ee; }.message-item:last-child { border-bottom: 0; }.message-item.unread { background: #fbfdf5; }.message-mark { width: 30px; height: 30px; display: grid; place-items: center; border-radius: 50%; background: #e8f0e3; color: #6f956e; font-weight: 700; }.unread .message-mark { background: #f2e4d3; color: #b46c35; }.message-body { flex: 1; min-width: 0; }.message-title { display: flex; justify-content: space-between; gap: 14px; }.message-title strong { color: #294c3f; font-size: 14px; }.message-title span, .message-body small { color: #8b9a91; font-size: 11px; }.message-body p { margin: 8px 0; color: #667c70; font-size: 13px; line-height: 1.6; }
@media (max-width: 640px) { .message-filter { align-items: flex-start; flex-direction: column; }.message-item { align-items: flex-start; }.message-title { flex-direction: column; gap: 5px; } }
</style>
