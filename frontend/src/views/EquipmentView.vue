<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import * as equipmentApi from '@/api/equipment'

const loading = ref(false)
const auth = useAuthStore()
const equipments = ref([])
const keyword = ref('')
const pagination = reactive({ pageNum: 1, pageSize: 20, total: 0 })
const categories = ref([])
const formVisible = ref(false)
const categoryDialogVisible = ref(false)
const categoryEditingId = ref(null)
const categoryForm = reactive({ categoryName: '', parentId: null, sortNo: 1 })
const categoryEditor = reactive({ categoryName: '', parentId: null, sortNo: 1 })
const createForm = reactive({ categoryId: '', equipCode: '', equipName: '', model: '', manufacturer: '', location: '', purchaseDate: '', warrantyExpire: '', description: '' })
const selectedDetail = ref(null)
const detailVisible = ref(false)
const qrVisible = ref(false)
const qrSrc = ref('')
const qrTarget = ref(null)
const detailForm = reactive({ categoryId: null, equipCode: '', equipName: '', model: '', manufacturer: '', location: '', responsibleUserId: null, purchaseDate: '', warrantyExpire: '', description: '' })
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

async function editCategory(category) {
  categoryEditingId.value = category.id
  Object.assign(categoryEditor, { categoryName: category.categoryName, parentId: category.parentId || null, sortNo: category.sortNo || 0 })
  categoryDialogVisible.value = true
}
async function saveCategory() {
  if (!categoryEditor.categoryName.trim()) return ElMessage.warning('请填写分类名称')
  try { await equipmentApi.updateCategory(categoryEditingId.value, { categoryName: categoryEditor.categoryName.trim(), parentId: categoryEditor.parentId ? Number(categoryEditor.parentId) : null, sortNo: Number(categoryEditor.sortNo) || 0 }); ElMessage.success('设备分类已更新'); categoryDialogVisible.value = false; await loadCategories() } catch (error) { ElMessage.error(error.message || '设备分类编辑失败') }
}

async function removeCategory(category) {
  try { await ElMessageBox.confirm(`确认删除分类“${category.categoryName}”吗？删除前需先移出下级分类和设备。`, '删除设备分类', { type: 'warning' }); await equipmentApi.deleteCategory(category.id); ElMessage.success('设备分类已删除'); await loadCategories() } catch (error) { if (error !== 'cancel') ElMessage.error(error.message || '设备分类删除失败') }
}

async function createEquipment() {
  try { await equipmentApi.createEquipment({ ...createForm, categoryId: createForm.categoryId ? Number(createForm.categoryId) : null, purchaseDate: createForm.purchaseDate || null, warrantyExpire: createForm.warrantyExpire || null }); ElMessage.success('设备已创建'); Object.assign(createForm, { categoryId: '', equipCode: '', equipName: '', model: '', manufacturer: '', location: '', purchaseDate: '', warrantyExpire: '', description: '' }); formVisible.value = false; await loadEquipments() } catch (error) { ElMessage.error(error.message || '设备创建失败') }
}

async function openDetail(equipment) {
  try { const response = await equipmentApi.getEquipment(equipment.id); selectedDetail.value = response.data; Object.assign(detailForm, response.data, { purchaseDate: response.data.purchaseDate || '', warrantyExpire: response.data.warrantyExpire || '' }); detailVisible.value = true } catch (error) { ElMessage.error(error.message || '设备详情加载失败') }
}

async function saveEquipment() {
  try { const response = await equipmentApi.updateEquipment(selectedDetail.value.id, { ...detailForm, categoryId: detailForm.categoryId ? Number(detailForm.categoryId) : null, responsibleUserId: detailForm.responsibleUserId ? Number(detailForm.responsibleUserId) : null, purchaseDate: detailForm.purchaseDate || null, warrantyExpire: detailForm.warrantyExpire || null }); selectedDetail.value = { ...selectedDetail.value, ...response.data }; ElMessage.success('设备信息已保存') } catch (error) { ElMessage.error(error.message || '设备信息保存失败') }
}

async function saveParam(param) {
  try { const response = await equipmentApi.updateEquipmentParam(selectedDetail.value.id, param.id, { paramName: param.paramName, paramValue: param.paramValue, unit: param.unit, sortNo: Number(param.sortNo || 0) }); Object.assign(param, response.data); ElMessage.success('设备参数已保存') } catch (error) { ElMessage.error(error.message || '设备参数保存失败') }
}

async function downloadQr(equipment) {
  try { const blob = await equipmentApi.getEquipmentQr(equipment.id); const url = URL.createObjectURL(blob); const link = document.createElement('a'); link.href = url; link.download = `${equipment.equipCode}-qrcode.png`; link.click(); URL.revokeObjectURL(url) } catch (error) { ElMessage.error(error.message || '二维码下载失败') }
}

async function previewQr(equipment) {
  try {
    const blob = await equipmentApi.getEquipmentQr(equipment.id)
    if (qrSrc.value) URL.revokeObjectURL(qrSrc.value)
    qrSrc.value = URL.createObjectURL(blob)
    qrTarget.value = equipment
    qrVisible.value = true
  } catch (error) { ElMessage.error(error.message || '二维码预览失败') }
}

function closeQr() {
  qrVisible.value = false
  if (qrSrc.value) { URL.revokeObjectURL(qrSrc.value); qrSrc.value = '' }
  qrTarget.value = null
}

async function removeEquipment(equipment) {
  try { await ElMessageBox.confirm('设备必须已报废且没有未闭环业务才能删除，确认继续吗？', '删除设备', { type: 'warning' }); await equipmentApi.deleteEquipment(equipment.id); ElMessage.success('设备已删除'); detailVisible.value = false; await loadEquipments() } catch (error) { if (error !== 'cancel') ElMessage.error(error.message || '设备删除失败') }
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
  <div class="page-intro"><div><h2>设备台账</h2><p>维护设备编码、位置、运行状态和状态履历。</p></div><button v-if="auth.hasPermission('equipment:write')" class="primary-button compact-button" type="button" @click="formVisible = !formVisible">{{ formVisible ? '收起新增' : '新增设备' }}</button></div>
  <el-dialog v-model="formVisible" title="新增设备" width="820px" destroy-on-close><p class="dialog-tip">设备编码是台账、二维码、巡检和报修关联的唯一标识；日期按设备采购和质保凭证填写，未知时可留空。</p><section class="panel"><form class="equipment-form" @submit.prevent="createEquipment"><select v-model="createForm.categoryId" class="form-input"><option value="">选择分类（可空）</option><option v-for="category in categories" :key="category.id" :value="category.id">{{ category.categoryName }}</option></select><input v-model="createForm.equipCode" class="form-input" required placeholder="设备编码，例如 EQ-003" /><input v-model="createForm.equipName" class="form-input" required placeholder="设备名称，例如 冷却水泵" /><input v-model="createForm.model" class="form-input" placeholder="型号，例如 CP-80" /><input v-model="createForm.manufacturer" class="form-input" placeholder="厂商或品牌" /><input v-model="createForm.location" class="form-input" placeholder="安装位置，例如 A栋一层" /><input v-model="createForm.purchaseDate" class="form-input" type="date" /><input v-model="createForm.warrantyExpire" class="form-input" type="date" /><input v-model="createForm.description" class="form-input wide-input" placeholder="补充说明、风险或维护备注" /><button class="primary-button compact-button" type="submit">保存设备</button></form><form class="category-form" @submit.prevent="createCategory"><input v-model="categoryForm.categoryName" class="form-input" required placeholder="快速新增分类，例如 仪表设备" /><input v-model="categoryForm.parentId" class="form-input" type="number" min="1" placeholder="父分类 ID；根分类留空" /><button class="table-action" type="submit">新增分类</button></form><div class="category-list"><div class="panel-heading"><h3>设备分类维护</h3><span>有下级分类或在用设备时不可删除</span></div><table class="data-table"><thead><tr><th>ID</th><th>分类名称</th><th>父分类</th><th>排序</th><th>操作</th></tr></thead><tbody><tr v-for="category in categories" :key="category.id"><td>{{ category.id }}</td><td>{{ category.categoryName }}</td><td>{{ category.parentId || '—' }}</td><td>{{ category.sortNo }}</td><td class="row-actions"><button class="table-action" type="button" @click="editCategory(category)">编辑</button><button class="table-action danger-action" type="button" @click="removeCategory(category)">删除</button></td></tr></tbody></table></div></section></el-dialog>
  <el-dialog v-model="categoryDialogVisible" title="编辑设备分类" width="440px" destroy-on-close><p class="dialog-tip">分类用于设备筛选和统计。父分类填写已有分类 ID；存在下级分类或在用设备时不能删除。</p><form class="dialog-form" @submit.prevent="saveCategory"><label class="form-label">分类名称</label><input v-model="categoryEditor.categoryName" class="form-input" required placeholder="例如：生产设备" /><label class="form-label">父分类 ID</label><input v-model="categoryEditor.parentId" class="form-input" type="number" min="1" placeholder="根分类留空" /><label class="form-label">排序</label><input v-model="categoryEditor.sortNo" class="form-input" type="number" min="0" placeholder="数字越小越靠前" /><div class="dialog-actions"><button class="secondary-button" type="button" @click="categoryDialogVisible = false">取消</button><button class="primary-button compact-button" type="submit">保存分类</button></div></form></el-dialog>
  <section class="soft-card table-card"><div class="toolbar"><input v-model="keyword" class="form-input" placeholder="搜索设备编码或名称" @keyup.enter="loadEquipments" /><button class="primary-button" style="width: auto; padding: 0 20px" type="button" @click="loadEquipments">查询</button></div><div v-if="loading" class="empty-state">正在加载设备数据…</div><div v-else-if="!equipments.length" class="empty-state">暂无设备数据</div><div v-else class="table-wrap"><table class="data-table"><thead><tr><th>设备编码</th><th>设备名称</th><th>型号</th><th>位置</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="equipment in equipments" :key="equipment.id"><td><strong>{{ equipment.equipCode }}</strong></td><td>{{ equipment.equipName }}</td><td>{{ equipment.model || '—' }}</td><td>{{ equipment.location || '—' }}</td><td><span class="state-pill" :class="equipment.status === 3 ? 'inactive' : 'active'">{{ equipment.statusLabel }}</span></td><td class="row-actions"><button class="table-action" type="button" @click="openDetail(equipment)">详情/参数</button><button class="table-action" type="button" @click="previewQr(equipment)">预览二维码</button><button class="table-action" type="button" @click="downloadQr(equipment)">下载二维码</button><button v-if="equipment.status !== 4" class="table-action" type="button" @click="changeStatus(equipment)">{{ equipment.status === 1 ? '设为空闲' : '设为运行' }}</button></td></tr></tbody></table></div></section>
  <el-dialog v-if="qrTarget" v-model="qrVisible" :title="`${qrTarget.equipCode} · 设备二维码`" width="420px" destroy-on-close @closed="closeQr"><div class="qr-preview"><img :src="qrSrc" :alt="`${qrTarget.equipName}设备二维码`" /><strong>{{ qrTarget.equipName }}</strong><span>{{ qrTarget.equipCode }} · {{ qrTarget.location || '未填写位置' }}</span><p>现场扫码可用于设备核验、巡检点关联和报修定位。</p><button class="primary-button compact-button" type="button" @click="downloadQr(qrTarget)">下载 PNG</button></div></el-dialog>
  <el-dialog v-if="selectedDetail" v-model="detailVisible" :title="`${selectedDetail.equipCode} · 设备详情`" width="860px" destroy-on-close><div class="panel-heading"><div><span>{{ selectedDetail.statusLabel }} · 状态履历 {{ selectedDetail.statusLogs?.length || 0 }} 条</span></div></div><form class="equipment-form" @submit.prevent="saveEquipment"><select v-model="detailForm.categoryId" class="form-input"><option value="">无分类</option><option v-for="category in categories" :key="category.id" :value="category.id">{{ category.categoryName }}</option></select><input v-model="detailForm.equipCode" class="form-input" required placeholder="设备编码" /><input v-model="detailForm.equipName" class="form-input" required placeholder="设备名称" /><input v-model="detailForm.model" class="form-input" placeholder="型号" /><input v-model="detailForm.manufacturer" class="form-input" placeholder="厂商" /><input v-model="detailForm.location" class="form-input" placeholder="位置" /><input v-model="detailForm.purchaseDate" class="form-input" type="date" /><input v-model="detailForm.warrantyExpire" class="form-input" type="date" /><input v-model="detailForm.description" class="form-input wide-input" placeholder="说明" /><button class="primary-button compact-button" type="submit">保存设备信息</button></form><div class="parameter-list"><div class="panel-heading"><h3>设备参数</h3><span>修改后同步用于巡检快照展示；参数名、值和单位都要填写清楚</span></div><div v-if="!selectedDetail.params?.length" class="empty-note">暂无设备参数</div><div v-for="param in selectedDetail.params" :key="param.id" class="param-row"><input v-model="param.paramName" class="form-input" placeholder="参数名，例如额定压力" /><input v-model="param.paramValue" class="form-input" placeholder="参数值，例如 0.8" /><input v-model="param.unit" class="form-input" placeholder="单位，例如 MPa" /><input v-model="param.sortNo" class="form-input" type="number" placeholder="排序" /><button class="table-action" type="button" @click="saveParam(param)">保存参数</button></div></div><button v-if="selectedDetail.status === 4" class="table-action danger-action" type="button" @click="removeEquipment(selectedDetail)">删除已报废设备</button></el-dialog>
</template>

<style scoped>
.compact-button { width: auto; padding: 0 18px; }.equipment-form, .category-form { display: grid; grid-template-columns: repeat(4, minmax(150px, 1fr)); gap: 10px; align-items: center; }.category-form { grid-template-columns: 1fr 1fr auto; margin-top: 14px; }.wide-input { grid-column: span 2; }.row-actions { display: flex; flex-wrap: wrap; gap: 8px; }.detail-panel { margin-top: 18px; }.parameter-list, .category-list { margin-top: 24px; }.param-row { display: grid; grid-template-columns: 1.3fr 1.3fr .8fr .6fr auto; gap: 8px; margin-top: 10px; }.danger-action { color: #a15b4e; }.dialog-tip { margin: -4px 0 16px; color: #789087; font-size: 13px; line-height: 1.7; }.dialog-form { display: grid; gap: 2px; }.dialog-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px; }.secondary-button { width: auto; height: 42px; padding: 0 18px; border: 1px solid #cbd9ce; border-radius: 9px; background: #f7faf7; color: #335f4d; cursor: pointer; }@media (max-width: 800px) { .equipment-form { grid-template-columns: repeat(2, 1fr); }.param-row { grid-template-columns: repeat(2, 1fr); } }@media (max-width: 600px) { .equipment-form, .category-form, .param-row { grid-template-columns: 1fr; }.wide-input { grid-column: span 1; } }
.qr-preview { display: flex; flex-direction: column; align-items: center; gap: 10px; padding: 8px 0 4px; text-align: center; }.qr-preview img { width: 250px; height: 250px; padding: 12px; border: 1px solid #e0e9e1; border-radius: 12px; background: #fff; image-rendering: pixelated; }.qr-preview span, .qr-preview p { color: #789087; font-size: 13px; }.qr-preview p { margin: 0; line-height: 1.6; }
</style>
