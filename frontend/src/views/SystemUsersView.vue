<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as systemApi from '@/api/system'

const loading = ref(false)
const users = ref([])
const roles = ref([])
const depts = ref([])
const keyword = ref('')
const pagination = reactive({ pageNum: 1, pageSize: 20, total: 0 })
const formVisible = ref(false)
const roleVisible = ref(false)
const createForm = reactive({ username: '', password: '', nickname: '', deptId: '', roleIds: [] })
const roleEditor = reactive({ user: null, roleIds: [] })

async function loadUsers() {
  loading.value = true
  try {
    const response = await systemApi.getUsers({ pageNum: pagination.pageNum, pageSize: pagination.pageSize, keyword: keyword.value.trim() })
    users.value = response.data.list || []
    pagination.total = response.data.total || 0
  } catch (error) { ElMessage.error(error.message || '用户列表加载失败') } finally { loading.value = false }
}

async function loadFoundation() {
  try {
    const [roleResponse, deptResponse] = await Promise.all([systemApi.getRoles(), systemApi.getDepts()])
    roles.value = roleResponse.data || []
    depts.value = deptResponse.data || []
  } catch (error) { ElMessage.error(error.message || '用户配置数据加载失败') }
}

function resetCreateForm() { Object.assign(createForm, { username: '', password: '', nickname: '', deptId: '', roleIds: [] }) }
function openCreate() { resetCreateForm(); formVisible.value = true }

async function toggle(user) {
  const nextStatus = user.status === 1 ? 0 : 1
  await ElMessageBox.confirm(`确认${nextStatus === 1 ? '启用' : '停用'}用户“${user.nickname}”吗？`, '状态变更', { type: 'warning' })
  try { await systemApi.changeUserStatus(user.id, nextStatus); ElMessage.success('状态已更新'); await loadUsers() } catch (error) { ElMessage.error(error.message || '状态更新失败') }
}

async function createUser() {
  if (!createForm.username.trim() || !createForm.nickname.trim()) return ElMessage.warning('请填写用户名和姓名')
  if (createForm.password.length < 8) return ElMessage.warning('初始密码至少需要 8 位')
  try {
    await systemApi.createUser({ ...createForm, username: createForm.username.trim(), nickname: createForm.nickname.trim(), deptId: createForm.deptId ? Number(createForm.deptId) : null, roleIds: createForm.roleIds.map(Number) })
    ElMessage.success('用户已创建')
    formVisible.value = false
    await loadUsers()
  } catch (error) { ElMessage.error(error.message || '用户创建失败') }
}

function openRoleEditor(user) { roleEditor.user = user; roleEditor.roleIds = [...(user.roleIds || [])]; roleVisible.value = true }
async function saveRoles() {
  try { await systemApi.updateUserRoles(roleEditor.user.id, roleEditor.roleIds.map(Number)); ElMessage.success('用户角色已保存'); roleVisible.value = false; await loadUsers() } catch (error) { ElMessage.error(error.message || '用户角色保存失败') }
}

onMounted(() => { loadUsers(); loadFoundation() })
</script>

<template>
  <div class="page-intro"><div><h2>用户管理</h2><p>账号由系统管理员创建；用户登录后只能维护自己的资料和密码。</p></div><button class="primary-button compact-button" type="button" @click="openCreate">新增用户</button></div>
  <section class="soft-card table-card"><div class="toolbar"><input v-model="keyword" class="form-input" placeholder="搜索用户名或姓名，按 Enter 查询" @keyup.enter="loadUsers" /><button class="primary-button search-button" type="button" @click="loadUsers">查询</button><span class="toolbar-hint">共 {{ pagination.total }} 个账号</span></div><div v-if="loading" class="empty-state">正在加载用户数据…</div><div v-else-if="!users.length" class="empty-state">暂无用户数据。点击右上角新增第一个账号。</div><div v-else class="table-wrap"><table class="data-table"><thead><tr><th>账号</th><th>姓名</th><th>部门</th><th>角色</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="user in users" :key="user.id"><td><strong>{{ user.username }}</strong></td><td>{{ user.nickname }}</td><td>{{ depts.find((dept) => dept.id === user.deptId)?.deptName || '未分配' }}</td><td>{{ (user.roleIds || []).map((id) => roles.find((role) => role.id === id)?.roleName || `#${id}`).join('、') || '未分配' }}</td><td><span class="state-pill" :class="user.status === 1 ? 'active' : 'inactive'">{{ user.status === 1 ? '启用' : '停用' }}</span></td><td><button class="table-action" type="button" @click="openRoleEditor(user)">配置角色</button><button class="table-action" type="button" @click="toggle(user)">{{ user.status === 1 ? '停用' : '启用' }}</button></td></tr></tbody></table></div></section>
  <el-dialog v-model="formVisible" title="新增用户" width="520px" destroy-on-close><p class="dialog-tip">账号由管理员发放。请给用户设置一个临时初始密码，用户登录后可自行修改。角色会同时决定左侧菜单和后端可执行操作。</p><form class="dialog-form" @submit.prevent="createUser"><label class="form-label" for="new-username">用户名</label><input id="new-username" v-model="createForm.username" class="form-input" autocomplete="off" placeholder="例如：zhangming（登录账号，不建议使用中文）" required /><label class="form-label" for="new-password">初始密码</label><input id="new-password" v-model="createForm.password" class="form-input" type="password" minlength="8" autocomplete="new-password" placeholder="至少 8 位，建议字母、数字组合" required /><label class="form-label" for="new-nickname">姓名</label><input id="new-nickname" v-model="createForm.nickname" class="form-input" placeholder="例如：张明（页面展示姓名）" required /><label class="form-label" for="new-dept">所属部门</label><el-select id="new-dept" v-model="createForm.deptId" class="full-control" filterable clearable placeholder="选择所属部门；可暂不分配"><el-option v-for="dept in depts" :key="dept.id" :label="dept.deptName" :value="dept.id" /></el-select><label class="form-label" for="new-roles">初始角色</label><el-select id="new-roles" v-model="createForm.roleIds" class="full-control" multiple filterable collapse-tags collapse-tags-tooltip clearable placeholder="选择角色，可多选"><el-option v-for="role in roles" :key="role.id" :label="`${role.roleName} · ${role.roleCode}`" :value="role.id" /></el-select><p class="field-help">角色是权限集合。例如“维修工”可接单、填写维修方案和备件消耗；没有角色的账号只能看到被明确授权的公共功能。</p><div class="dialog-actions"><button class="secondary-button" type="button" @click="formVisible = false">取消</button><button class="primary-button compact-button" type="submit">创建账号</button></div></form></el-dialog>
  <el-dialog v-model="roleVisible" :title="`${roleEditor.user?.nickname || ''} · 角色配置`" width="460px" destroy-on-close><p class="dialog-tip">角色决定账号可访问的菜单和后端操作范围，请按最小权限原则分配；多个角色的权限会合并。</p><el-select v-model="roleEditor.roleIds" class="full-control" multiple filterable collapse-tags collapse-tags-tooltip clearable placeholder="选择角色，可多选"><el-option v-for="role in roles" :key="role.id" :label="`${role.roleName} · ${role.roleCode}`" :value="role.id" /></el-select><template #footer><button class="secondary-button" type="button" @click="roleVisible = false">取消</button><button class="primary-button dialog-primary" type="button" @click="saveRoles">保存角色</button></template></el-dialog>
</template>

<style scoped>
.compact-button { width: auto; padding: 0 18px; }.search-button { width: auto; padding: 0 20px; }.toolbar-hint { color: #87988f; font-size: 12px; }.dialog-tip { margin: -4px 0 16px; color: #789087; font-size: 13px; line-height: 1.7; }.dialog-form { display: grid; gap: 2px; }.full-control { width: 100%; }.field-help { margin: 4px 0 0; color: #789087; font-size: 12px; line-height: 1.6; }.dialog-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px; }.secondary-button, .dialog-primary { width: auto; height: 42px; padding: 0 18px; border-radius: 9px; cursor: pointer; }.secondary-button { border: 1px solid #cbd9ce; background: #f7faf7; color: #335f4d; }.dialog-primary { border: 0; background: #244f3f; color: #fff; }.secondary-button:hover { border-color: #8bb197; background: #eef6ed; }
</style>
