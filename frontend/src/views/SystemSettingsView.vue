<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as systemApi from '@/api/system'

const tab = ref('roles')
const loading = ref(false)
const roles = ref([])
const depts = ref([])
const dicts = ref([])
const logs = ref([])
const menus = ref([])
const selectedRole = ref(null)
const selectedMenuIds = ref([])
const dictType = ref('status')
const roleForm = reactive({ roleCode: '', roleName: '', dataScope: 1 })
const deptForm = reactive({ parentId: null, deptName: '', sortNo: 1 })
const dictForm = reactive({ dictType: 'status', dictLabel: '', dictValue: '', sortNo: 1 })

function reset(form, values) { Object.assign(form, values) }
async function load() {
  loading.value = true
  try {
    if (tab.value === 'roles') { const [roleResponse, menuResponse] = await Promise.all([systemApi.getRoles(), systemApi.getMenus()]); roles.value = roleResponse.data || []; menus.value = menuResponse.data || [] }
    if (tab.value === 'depts') depts.value = (await systemApi.getDepts()).data || []
    if (tab.value === 'dicts') dicts.value = (await systemApi.getDicts(dictType.value)).data || []
    if (tab.value === 'logs') logs.value = (await systemApi.getOperLogs({ pageNum: 1, pageSize: 50 })).data?.list || []
  } catch (error) { ElMessage.error(error.message || '系统配置加载失败') } finally { loading.value = false }
}
function selectRole(role) { selectedRole.value = role; selectedMenuIds.value = [...(role.menuIds || [])] }
async function saveRoleMenus() { try { await systemApi.updateRoleMenus(selectedRole.value.id, selectedMenuIds.value.map(Number)); ElMessage.success('角色菜单权限已保存'); await load(); selectedRole.value = roles.value.find((role) => role.id === selectedRole.value.id) || null } catch (error) { ElMessage.error(error.message || '角色菜单保存失败') } }
async function createRole() { try { await systemApi.createRole({ ...roleForm, dataScope: Number(roleForm.dataScope) }); ElMessage.success('角色已创建'); reset(roleForm, { roleCode: '', roleName: '', dataScope: 1 }); await load() } catch (error) { ElMessage.error(error.message || '角色创建失败') } }
async function createDept() { try { await systemApi.createDept({ ...deptForm, parentId: deptForm.parentId ? Number(deptForm.parentId) : null, sortNo: Number(deptForm.sortNo) }); ElMessage.success('部门已创建'); reset(deptForm, { parentId: null, deptName: '', sortNo: 1 }); await load() } catch (error) { ElMessage.error(error.message || '部门创建失败') } }
async function createDict() { try { await systemApi.createDict({ ...dictForm, sortNo: Number(dictForm.sortNo) }); ElMessage.success('字典项已创建'); await load() } catch (error) { ElMessage.error(error.message || '字典项创建失败') } }
onMounted(load)
</script>

<template>
  <div class="page-intro"><div><h2>系统配置</h2><p>集中维护角色、部门、字典和操作日志，支撑权限与业务配置。</p></div><button class="table-action" type="button" @click="load">刷新</button></div>
  <div class="tab-bar"><button v-for="item in [['roles','角色'],['depts','部门'],['dicts','字典'],['logs','操作日志']]" :key="item[0]" class="table-action" :class="{ active: tab === item[0] }" type="button" @click="tab = item[0]; load()">{{ item[1] }}</button></div>
  <section v-if="tab === 'roles'" class="soft-card table-card"><form class="inline-form" @submit.prevent="createRole"><input v-model="roleForm.roleCode" class="form-input" required placeholder="角色编码" /><input v-model="roleForm.roleName" class="form-input" required placeholder="角色名称" /><select v-model.number="roleForm.dataScope" class="form-input"><option :value="1">全部数据</option><option :value="2">本部门数据</option><option :value="3">本人数据</option></select><button class="primary-button compact-button" type="submit">新增角色</button></form><div v-if="loading" class="empty-state">加载中…</div><table v-else class="data-table"><thead><tr><th>编码</th><th>名称</th><th>数据范围</th><th>状态</th><th>权限</th></tr></thead><tbody><tr v-for="role in roles" :key="role.id"><td>{{ role.roleCode }}</td><td>{{ role.roleName }}</td><td>{{ role.dataScope }}</td><td>{{ role.status === 1 ? '启用' : '停用' }}</td><td><button class="table-action" type="button" @click="selectRole(role)">配置菜单</button></td></tr></tbody></table><div v-if="selectedRole" class="menu-editor"><div class="panel-heading"><h3>{{ selectedRole.roleName }} · 菜单权限</h3><button class="primary-button compact-button" type="button" @click="saveRoleMenus">保存权限</button></div><label v-for="menu in menus" :key="menu.id" class="menu-option"><input v-model="selectedMenuIds" type="checkbox" :value="menu.id" />{{ menu.menuName }} <small>{{ menu.permissionCode || menu.routePath || '按钮权限' }}</small></label></div></section>
  <section v-else-if="tab === 'depts'" class="soft-card table-card"><form class="inline-form" @submit.prevent="createDept"><input v-model="deptForm.deptName" class="form-input" required placeholder="部门名称" /><input v-model="deptForm.parentId" class="form-input" type="number" min="1" placeholder="父部门 ID（可空）" /><input v-model.number="deptForm.sortNo" class="form-input" type="number" min="0" placeholder="排序" /><button class="primary-button compact-button" type="submit">新增部门</button></form><table class="data-table"><thead><tr><th>ID</th><th>部门名称</th><th>父部门</th><th>状态</th></tr></thead><tbody><tr v-for="dept in depts" :key="dept.id"><td>{{ dept.id }}</td><td>{{ dept.deptName }}</td><td>{{ dept.parentId || '—' }}</td><td>{{ dept.status === 1 ? '启用' : '停用' }}</td></tr></tbody></table></section>
  <section v-else-if="tab === 'dicts'" class="soft-card table-card"><form class="inline-form" @submit.prevent="createDict"><input v-model="dictForm.dictType" class="form-input" required placeholder="字典类型" /><input v-model="dictForm.dictLabel" class="form-input" required placeholder="显示标签" /><input v-model="dictForm.dictValue" class="form-input" required placeholder="字典值" /><button class="primary-button compact-button" type="submit">新增字典项</button></form><div class="toolbar"><input v-model="dictType" class="form-input" placeholder="查询字典类型" @keyup.enter="load" /><button class="table-action" type="button" @click="load">查询</button></div><table class="data-table"><thead><tr><th>类型</th><th>标签</th><th>值</th><th>排序</th><th>状态</th></tr></thead><tbody><tr v-for="item in dicts" :key="item.id"><td>{{ item.dictType }}</td><td>{{ item.dictLabel }}</td><td>{{ item.dictValue }}</td><td>{{ item.sortNo }}</td><td>{{ item.status === 1 ? '启用' : '停用' }}</td></tr></tbody></table></section>
  <section v-else class="soft-card table-card"><table class="data-table"><thead><tr><th>时间</th><th>用户</th><th>操作</th><th>请求</th><th>结果</th><th>Trace ID</th></tr></thead><tbody><tr v-for="log in logs" :key="log.id"><td>{{ log.createdTime }}</td><td>{{ log.username || log.userId || '—' }}</td><td>{{ log.operation }}</td><td>{{ log.requestMethod }} {{ log.requestUri }}</td><td>{{ log.success === 1 ? '成功' : '失败' }}</td><td>{{ log.traceId || '—' }}</td></tr></tbody></table></section>
</template>

<style scoped>
.tab-bar { display: flex; gap: 10px; margin-bottom: 16px; }.tab-bar .active { color: #fff; background: #244f3f; }.inline-form { display: grid; grid-template-columns: repeat(4, minmax(150px, 1fr)); gap: 10px; margin-bottom: 18px; }.compact-button { width: auto; padding: 0 18px; }.toolbar { display: flex; gap: 10px; margin-bottom: 14px; }.toolbar .form-input { max-width: 260px; }@media (max-width: 800px) { .inline-form { grid-template-columns: 1fr; } }
.tab-bar { display: flex; gap: 10px; margin-bottom: 16px; }.tab-bar .active { color: #fff; background: #244f3f; }.inline-form { display: grid; grid-template-columns: repeat(4, minmax(150px, 1fr)); gap: 10px; margin-bottom: 18px; }.compact-button { width: auto; padding: 0 18px; }.toolbar { display: flex; gap: 10px; margin-bottom: 14px; }.toolbar .form-input { max-width: 260px; }.menu-editor { margin-top: 20px; padding-top: 18px; border-top: 1px solid #e5ece5; }.menu-option { display: inline-flex; align-items: center; gap: 7px; min-width: 260px; margin: 8px 18px 8px 0; color: #315747; }.menu-option small { color: #8a9b91; }@media (max-width: 800px) { .inline-form { grid-template-columns: 1fr; } }
</style>
