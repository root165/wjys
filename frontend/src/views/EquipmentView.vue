<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as equipmentApi from '@/api/equipment'

const loading = ref(false)
const equipments = ref([])
const keyword = ref('')
const pagination = reactive({ pageNum: 1, pageSize: 20, total: 0 })
const statusLabels = { 0: '闲置', 1: '运行中', 2: '保养中', 3: '故障', 4: '已报废' }

async function loadEquipments() {
  loading.value = true
  try {
    const response = await equipmentApi.getEquipments({ ...pagination, keyword: keyword.value })
    equipments.value = response.data.list
    pagination.total = response.data.total
  } catch (error) { ElMessage.error(error.message || '设备列表加载失败') } finally { loading.value = false }
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

onMounted(loadEquipments)
</script>

<template>
  <div class="page-intro"><div><h2>设备台账</h2><p>维护设备编码、位置、运行状态和状态履历。</p></div></div>
  <section class="soft-card table-card"><div class="toolbar"><input v-model="keyword" class="form-input" placeholder="搜索设备编码或名称" @keyup.enter="loadEquipments" /><button class="primary-button" style="width: auto; padding: 0 20px" type="button" @click="loadEquipments">查询</button></div><div v-if="loading" class="empty-state">正在加载设备数据…</div><div v-else-if="!equipments.length" class="empty-state">暂无设备数据</div><div v-else class="table-wrap"><table class="data-table"><thead><tr><th>设备编码</th><th>设备名称</th><th>型号</th><th>位置</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="equipment in equipments" :key="equipment.id"><td><strong>{{ equipment.equipCode }}</strong></td><td>{{ equipment.equipName }}</td><td>{{ equipment.model || '—' }}</td><td>{{ equipment.location || '—' }}</td><td><span class="state-pill" :class="equipment.status === 3 ? 'inactive' : 'active'">{{ equipment.statusLabel }}</span></td><td><button v-if="equipment.status !== 4" class="table-action" type="button" @click="changeStatus(equipment)">{{ equipment.status === 1 ? '设为空闲' : '设为运行' }}</button></td></tr></tbody></table></div></section>
</template>
