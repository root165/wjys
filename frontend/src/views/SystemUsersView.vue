<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as systemApi from '@/api/system'

const loading = ref(false)
const users = ref([])
const keyword = ref('')
const pagination = reactive({ pageNum: 1, pageSize: 20, total: 0 })
const formVisible = ref(false)
const createForm = reactive({ username: '', password: '', nickname: '', deptId: null, roleIds: [] })
const roles = ref([])
const roleEditor = reactive({ user: null, roleIds: [] })

async function loadUsers() {
  loading.value = true
  try {
    const response = await systemApi.getUsers({ ...pagination, keyword: keyword.value })
    users.value = response.data.list
    pagination.total = response.data.total
  } catch (error) { ElMessage.error(error.message || '用户列表加载失败') } finally { loading.value = false }
}

async function loadRoles() {
  try { roles.value = (await systemApi.getRoles()).data || [] } catch (error) { ElMessage.error(error.message || '角色列表加载失败') }
}

async function toggle(user) {
  const nextStatus = user.status === 1 ? 0 : 1
  await ElMessageBox.confirm(`确认${nextStatus === 1 ? '启用' : '停用'}用户“${user.nickname}”吗？`, '状态变更', { type: 'warning' })
  try { await systemApi.changeUserStatus(user.id, nextStatus); ElMessage.success('状态已更新'); await loadUsers() } catch (error) { ElMessage.error(error.message || '状态更新失败') }
}

async function createUser() {
  try { await systemApi.createUser({ ...createForm, deptId: createForm.deptId ? Number(createForm.deptId) : null, roleIds: createForm.roleIds.map(Number) }); ElMessage.success('用户已创建'); Object.assign(createForm, { username: '', password: '', nickname: '', deptId: null, roleIds: [] }); formVisible.value = false; await loadUsers() } catch (error) { ElMessage.error(error.message || '用户创建失败') }
}

async function saveRoles() {
  try { await systemApi.updateUserRoles(roleEditor.user.id, roleEditor.roleIds.map(Number)); ElMessage.success('用户角色已保存'); roleEditor.user = null; await loadUsers() } catch (error) { ElMessage.error(error.message || '用户角色保存失败') }
}

onMounted(() => { loadUsers(); loadRoles() })
</script>

<template>
  <div class="page-intro"><div><h2>用户管理</h2><p>管理登录账号、部门归属与使用状态。</p></div><button class="primary-button compact-button" type="button" @click="formVisible = !formVisible">{{ formVisible ? '收起新增' : '新增用户' }}</button></div>
  <section v-if="formVisible" class="soft-card panel"><form class="user-form" @submit.prevent="createUser"><input v-model="createForm.username" class="form-input" required placeholder="用户名" /><input v-model="createForm.password" class="form-input" type="password" minlength="8" required placeholder="初始密码（至少 8 位）" /><input v-model="createForm.nickname" class="form-input" required placeholder="姓名" /><input v-model="createForm.deptId" class="form-input" type="number" min="1" placeholder="部门 ID（可空）" /><select v-model="createForm.roleIds" class="form-input" multiple><option v-for="role in roles" :key="role.id" :value="role.id">{{ role.roleName }}</option></select><button class="primary-button compact-button" type="submit">保存用户</button></form></section>
  <section class="soft-card table-card"><div class="toolbar"><input v-model="keyword" class="form-input" placeholder="搜索用户名或姓名" @keyup.enter="loadUsers" /><button class="primary-button" style="width: auto; padding: 0 20px" type="button" @click="loadUsers">查询</button></div><div v-if="loading" class="empty-state">正在加载用户数据…</div><div v-else-if="!users.length" class="empty-state">暂无用户数据</div><div v-else class="table-wrap"><table class="data-table"><thead><tr><th>用户名</th><th>姓名</th><th>部门</th><th>角色</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="user in users" :key="user.id"><td><strong>{{ user.username }}</strong></td><td>{{ user.nickname }}</td><td>{{ user.deptId ? `部门 #${user.deptId}` : '未分配' }}</td><td>{{ (user.roleIds || []).map((id) => roles.find((role) => role.id === id)?.roleName || `#${id}`).join('、') || '未分配' }}</td><td><span class="state-pill" :class="user.status === 1 ? 'active' : 'inactive'">{{ user.status === 1 ? '启用' : '停用' }}</span></td><td><button class="table-action" type="button" @click="roleEditor.user = user; roleEditor.roleIds = [...(user.roleIds || [])]">配置角色</button><button class="table-action" type="button" @click="toggle(user)">{{ user.status === 1 ? '停用' : '启用' }}</button></td></tr></tbody></table></div></section>
  <section v-if="roleEditor.user" class="soft-card panel role-editor"><div class="panel-heading"><h3>{{ roleEditor.user.nickname }} · 角色配置</h3><button class="primary-button compact-button" type="button" @click="saveRoles">保存角色</button></div><select v-model="roleEditor.roleIds" class="form-input" multiple><option v-for="role in roles" :key="role.id" :value="role.id">{{ role.roleName }} · {{ role.roleCode }}</option></select></section>
</template>

<style scoped>
.compact-button { width: auto; padding: 0 18px; }.user-form { display: grid; grid-template-columns: repeat(6, minmax(140px, 1fr)); gap: 10px; align-items: center; }.user-form select, .role-editor select { min-height: 80px; }.role-editor { margin-top: 18px; }.role-editor .form-input { max-width: 500px; }@media (max-width: 800px) { .user-form { grid-template-columns: repeat(2, 1fr); } }@media (max-width: 600px) { .user-form { grid-template-columns: 1fr; } }
</style>
