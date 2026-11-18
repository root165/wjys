<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as equipmentApi from '@/api/equipment'

const loading = ref(false)
const equipments = ref([])
const keyword = ref('')
const pagination = reactive({ pageNum: 1, pageSize: 20, total: 0 })
const categories = ref([])
const formVisible = ref(false)
const categoryForm = reactive({ categoryName: '', parentId: null, sortNo: 1 })
const createForm = reactive({ categoryId: '', equipCode: '', equipName: '', model: '', manufacturer: '', location: '', purchaseDate: '', warrantyExpire: '', description: '' })
const statusLabels = { 0: '闲置', 1: '运行中', 2: '保养中', 3: '故障', 4: '已报废' }

async function loadEquipments() {
  loading.value = true
  try {
    const response = await equipmentApi.getEquipments({ ...pagination, keyword: keyword.value })
    equipments.value = response.data.list
    pagination.total = response.data.total
  } catch (error) { ElMessage.error(error.message || '设备列表加载失败') } finally { loading.value = false }
}

async function loadCategories() {
  try { categories.value = (await equipmentApi.getCategories()).data || [] } catch (error) { ElMessage.error(error.message || '设备分类加载失败') }
}

async function createCategory() {
  try { await equipmentApi.createCategory({ ...categoryForm, parentId: categoryForm.parentId ? Number(categoryForm.parentId) : null, sortNo: Number(categoryForm.sortNo) }); ElMessage.success('设备分类已创建'); Object.assign(categoryForm, { categoryName: '', parentId: null, sortNo: 1 }); await loadCategories() } catch (error) { ElMessage.error(error.message || '设备分类创建失败') }
}

async function createEquipment() {
  try { await equipmentApi.createEquipment({ ...createForm, categoryId: createForm.categoryId ? Number(createForm.categoryId) : null, purchaseDate: createForm.purchaseDate || null, warrantyExpire: createForm.warrantyExpire || null }); ElMessage.success('设备已创建'); Object.assign(createForm, { categoryId: '', equipCode: '', equipName: '', model: '', manufacturer: '', location: '', purchaseDate: '', warrantyExpire: '', description: '' }); formVisible.value = false; await loadEquipments() } catch (error) { ElMessage.error(error.message || '设备创建失败') }
}

async function changeStatus(equipment) {
  const nextStatus = equipment.status === 1 ? 0 : 1
  await ElMessageBox.confirm(`确认将“${equipment.equipName}”变更为${statusLabels[nextStatus]}吗？`, '设备状态', { type: 'warning' })
  try {
    await equipmentApi.changeEquipmentStatus(equipment.id, { status: nextStatus, sourceType: 'web', reason: '管理端手动调整' })
    ElMessage.success('设备状态已更新')
    await loadEquipments()
  } catch (error) { ElMessage.error(error.message || '设备状态更新失败') }
}

onMounted(() => { loadCategories(); loadEquipments() })
</script>

<template>
  <div class="page-intro"><div><h2>设备台账</h2><p>维护设备编码、位置、运行状态和状态履历。</p></div><button class="primary-button compact-button" type="button" @click="formVisible = !formVisible">{{ formVisible ? '收起新增' : '新增设备' }}</button></div>
  <section v-if="formVisible" class="soft-card panel"><div class="panel-heading"><h3>新增设备</h3></div><form class="equipment-form" @submit.prevent="createEquipment"><select v-model="createForm.categoryId" class="form-input"><option value="">选择分类（可空）</option><option v-for="category in categories" :key="category.id" :value="category.id">{{ category.categoryName }}</option></select><input v-model="createForm.equipCode" class="form-input" required placeholder="设备编码" /><input v-model="createForm.equipName" class="form-input" required placeholder="设备名称" /><input v-model="createForm.model" class="form-input" placeholder="型号" /><input v-model="createForm.manufacturer" class="form-input" placeholder="厂商" /><input v-model="createForm.location" class="form-input" placeholder="位置" /><input v-model="createForm.purchaseDate" class="form-input" type="date" /><input v-model="createForm.warrantyExpire" class="form-input" type="date" /><input v-model="createForm.description" class="form-input wide-input" placeholder="说明" /><button class="primary-button compact-button" type="submit">保存设备</button></form><form class="category-form" @submit.prevent="createCategory"><input v-model="categoryForm.categoryName" class="form-input" required placeholder="快速新增分类" /><input v-model="categoryForm.parentId" class="form-input" type="number" min="1" placeholder="父分类 ID" /><button class="table-action" type="submit">新增分类</button></form></section>
  <section class="soft-card table-card"><div class="toolbar"><input v-model="keyword" class="form-input" placeholder="搜索设备编码或名称" @keyup.enter="loadEquipments" /><button class="primary-button" style="width: auto; padding: 0 20px" type="button" @click="loadEquipments">查询</button></div><div v-if="loading" class="empty-state">正在加载设备数据…</div><div v-else-if="!equipments.length" class="empty-state">暂无设备数据</div><div v-else class="table-wrap"><table class="data-table"><thead><tr><th>设备编码</th><th>设备名称</th><th>型号</th><th>位置</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="equipment in equipments" :key="equipment.id"><td><strong>{{ equipment.equipCode }}</strong></td><td>{{ equipment.equipName }}</td><td>{{ equipment.model || '—' }}</td><td>{{ equipment.location || '—' }}</td><td><span class="state-pill" :class="equipment.status === 3 ? 'inactive' : 'active'">{{ equipment.statusLabel }}</span></td><td><button v-if="equipment.status !== 4" class="table-action" type="button" @click="changeStatus(equipment)">{{ equipment.status === 1 ? '设为空闲' : '设为运行' }}</button></td></tr></tbody></table></div></section>
</template>

<style scoped>
.compact-button { width: auto; padding: 0 18px; }.equipment-form, .category-form { display: grid; grid-template-columns: repeat(4, minmax(150px, 1fr)); gap: 10px; align-items: center; }.category-form { grid-template-columns: 1fr 1fr auto; margin-top: 14px; }.wide-input { grid-column: span 2; }@media (max-width: 800px) { .equipment-form { grid-template-columns: repeat(2, 1fr); } }@media (max-width: 600px) { .equipment-form, .category-form { grid-template-columns: 1fr; }.wide-input { grid-column: span 1; } }
</style>
