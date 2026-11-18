<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as inspectionApi from '@/api/inspection'
import * as equipmentApi from '@/api/equipment'

const points = ref([])
const items = ref([])
const equipments = ref([])
const selectedPointId = ref('')
const pointForm = reactive({ equipId: '', pointCode: '', pointName: '', location: '' })
const itemForm = reactive({ pointId: '', itemCode: '', itemName: '', checkMethod: '', valueType: 'TEXT', minValue: '', maxValue: '', unit: '', optionsJson: '', requiredPhoto: 0 })
async function load() {
  try {
    const [pointResponse, equipmentResponse] = await Promise.all([inspectionApi.getPoints(), equipmentApi.getEquipments({ pageNum: 1, pageSize: 200 })])
    points.value = pointResponse.data || []; equipments.value = equipmentResponse.data?.list || []
    if (selectedPointId.value) items.value = (await inspectionApi.getItems(selectedPointId.value)).data || []
  } catch (error) { ElMessage.error(error.message || '巡检配置加载失败') }
}
async function choosePoint() { items.value = selectedPointId.value ? ((await inspectionApi.getItems(selectedPointId.value)).data || []) : [] }
async function createPoint() { try { await inspectionApi.createPoint({ ...pointForm, equipId: Number(pointForm.equipId) }); ElMessage.success('巡检点位已创建'); Object.assign(pointForm, { equipId: '', pointCode: '', pointName: '', location: '' }); await load() } catch (error) { ElMessage.error(error.message || '点位创建失败') } }
async function createItem() { try { await inspectionApi.createItem({ ...itemForm, pointId: Number(itemForm.pointId), minValue: itemForm.minValue === '' ? null : itemForm.minValue, maxValue: itemForm.maxValue === '' ? null : itemForm.maxValue, requiredPhoto: Number(itemForm.requiredPhoto) }); ElMessage.success('检查项已创建'); await choosePoint() } catch (error) { ElMessage.error(error.message || '检查项创建失败') } }
onMounted(load)
</script>

<template>
  <div class="page-intro"><div><h2>巡检配置</h2><p>先配置设备点位和检查项，再由巡检计划引用点位生成任务快照。</p></div><button class="table-action" type="button" @click="load">刷新</button></div>
  <section class="soft-card panel"><div class="panel-heading"><h3>新增巡检点位</h3></div><form class="config-form" @submit.prevent="createPoint"><select v-model="pointForm.equipId" class="form-input" required><option value="">选择设备</option><option v-for="equipment in equipments" :key="equipment.id" :value="equipment.id">{{ equipment.equipCode }} · {{ equipment.equipName }}</option></select><input v-model="pointForm.pointCode" class="form-input" required placeholder="点位编码" /><input v-model="pointForm.pointName" class="form-input" required placeholder="点位名称" /><input v-model="pointForm.location" class="form-input" placeholder="位置" /><button class="primary-button compact-button" type="submit">新增点位</button></form></section>
  <section class="soft-card table-card"><div class="toolbar"><select v-model="selectedPointId" class="form-input" @change="choosePoint"><option value="">选择点位查看检查项</option><option v-for="point in points" :key="point.id" :value="point.id">{{ point.pointCode }} · {{ point.pointName }}</option></select></div><table class="data-table"><thead><tr><th>点位编码</th><th>点位名称</th><th>设备</th><th>位置</th></tr></thead><tbody><tr v-for="point in points" :key="point.id"><td>{{ point.pointCode }}</td><td>{{ point.pointName }}</td><td>设备 #{{ point.equipId }}</td><td>{{ point.location || '—' }}</td></tr></tbody></table></section>
  <section class="soft-card panel"><div class="panel-heading"><h3>新增检查项</h3></div><form class="config-form" @submit.prevent="createItem"><select v-model="itemForm.pointId" class="form-input" required><option value="">选择点位</option><option v-for="point in points" :key="point.id" :value="point.id">{{ point.pointCode }} · {{ point.pointName }}</option></select><input v-model="itemForm.itemCode" class="form-input" required placeholder="检查项编码" /><input v-model="itemForm.itemName" class="form-input" required placeholder="检查项名称" /><input v-model="itemForm.checkMethod" class="form-input" placeholder="检查方法" /><select v-model="itemForm.valueType" class="form-input"><option value="TEXT">文本</option><option value="NUMBER">数值</option><option value="BOOLEAN">布尔</option></select><input v-model="itemForm.minValue" class="form-input" type="number" placeholder="最小值" /><input v-model="itemForm.maxValue" class="form-input" type="number" placeholder="最大值" /><input v-model="itemForm.unit" class="form-input" placeholder="单位" /><select v-model.number="itemForm.requiredPhoto" class="form-input"><option :value="0">非必拍</option><option :value="1">必须拍照</option></select><button class="primary-button compact-button" type="submit">新增检查项</button></form><table v-if="selectedPointId" class="data-table"><thead><tr><th>编码</th><th>名称</th><th>类型</th><th>范围</th><th>照片</th></tr></thead><tbody><tr v-for="item in items" :key="item.id"><td>{{ item.itemCode }}</td><td>{{ item.itemName }}</td><td>{{ item.valueType }}</td><td>{{ item.minValue ?? '—' }} ~ {{ item.maxValue ?? '—' }}</td><td>{{ item.requiredPhoto === 1 ? '必拍' : '否' }}</td></tr></tbody></table></section>
</template>

<style scoped>
.config-form { display: grid; grid-template-columns: repeat(5, minmax(140px, 1fr)); gap: 10px; align-items: center; }.compact-button { width: auto; padding: 0 18px; }.toolbar { display: flex; gap: 10px; margin-bottom: 14px; }.toolbar .form-input { max-width: 360px; }@media (max-width: 900px) { .config-form { grid-template-columns: repeat(2, 1fr); } }@media (max-width: 600px) { .config-form { grid-template-columns: 1fr; } }
</style>
