<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as systemApi from '@/api/system'

const loading = ref(false)
const users = ref([])
const keyword = ref('')
const pagination = reactive({ pageNum: 1, pageSize: 20, total: 0 })

async function loadUsers() {
  loading.value = true
  try {
    const response = await systemApi.getUsers({ ...pagination, keyword: keyword.value })
    users.value = response.data.list
    pagination.total = response.data.total
  } catch (error) { ElMessage.error(error.message || '用户列表加载失败') } finally { loading.value = false }
}

async function toggle(user) {
  const nextStatus = user.status === 1 ? 0 : 1
  await ElMessageBox.confirm(`确认${nextStatus === 1 ? '启用' : '停用'}用户“${user.nickname}”吗？`, '状态变更', { type: 'warning' })
  try { await systemApi.changeUserStatus(user.id, nextStatus); ElMessage.success('状态已更新'); await loadUsers() } catch (error) { ElMessage.error(error.message || '状态更新失败') }
}

onMounted(loadUsers)
</script>

<template>
  <div class="page-intro"><div><h2>用户管理</h2><p>管理登录账号、部门归属与使用状态。</p></div></div>
  <section class="soft-card table-card"><div class="toolbar"><input v-model="keyword" class="form-input" placeholder="搜索用户名或姓名" @keyup.enter="loadUsers" /><button class="primary-button" style="width: auto; padding: 0 20px" type="button" @click="loadUsers">查询</button></div><div v-if="loading" class="empty-state">正在加载用户数据…</div><div v-else-if="!users.length" class="empty-state">暂无用户数据</div><div v-else class="table-wrap"><table class="data-table"><thead><tr><th>用户名</th><th>姓名</th><th>部门</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="user in users" :key="user.id"><td><strong>{{ user.username }}</strong></td><td>{{ user.nickname }}</td><td>{{ user.deptId ? `部门 #${user.deptId}` : '未分配' }}</td><td><span class="state-pill" :class="user.status === 1 ? 'active' : 'inactive'">{{ user.status === 1 ? '启用' : '停用' }}</span></td><td><button class="table-action" type="button" @click="toggle(user)">{{ user.status === 1 ? '停用' : '启用' }}</button></td></tr></tbody></table></div></section>
</template>
