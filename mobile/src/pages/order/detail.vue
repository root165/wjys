<script setup>
import { onMounted, ref } from 'vue'
import { getWorkOrder, getWorkOrderLogs, startWorkOrder, finishWorkOrder } from '../../api/workOrders.js'
import { consumeSpares, listSpareRecords, listSpares } from '../../api/spares.js'

const order = ref(null)
const logs = ref([])
const spareRecords = ref([])
const spares = ref([])
const loading = ref(true)
const submitting = ref(false)
const errorMessage = ref('')
const form = ref({ finishReason: '', repairPlan: '', finishResult: '', downtimeMinutes: 0 })
const spareForm = ref({ spareId: '', quantity: 1, remark: '' })

function orderId() {
  const pages = getCurrentPages()
  return Number(pages[pages.length - 1]?.options?.id || 0)
}

const statusLabels = ['待接单', '维修中', '待验收', '已完成', '已退回']

async function load() {
  try {
    const id = orderId()
    const [detail, timeline, records, catalog] = await Promise.all([getWorkOrder(id), getWorkOrderLogs(id), listSpareRecords(id), listSpares()])
    order.value = detail.data
    logs.value = timeline.data || []
    spareRecords.value = records.data || []
    spares.value = catalog.data || []
  } catch (error) {
    errorMessage.value = error.message || '工单加载失败'
  } finally {
    loading.value = false
  }
}

async function consume() {
  if (!spareForm.value.spareId || Number(spareForm.value.quantity) < 1) {
    uni.showToast({ title: '请选择备件并填写数量', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    const response = await consumeSpares(order.value.id, [{ spareId: Number(spareForm.value.spareId), quantity: Number(spareForm.value.quantity), remark: spareForm.value.remark || null }])
    spareRecords.value = response.data || []
    spareForm.value = { spareId: '', quantity: 1, remark: '' }
    uni.showToast({ title: '备件已登记', icon: 'success' })
  } catch (error) { uni.showToast({ title: error.message || '备件登记失败', icon: 'none' }) }
  finally { submitting.value = false }
}

async function accept() {
  submitting.value = true
  try {
    const response = await startWorkOrder(order.value.id)
    order.value = response.data
    uni.showToast({ title: '已接单', icon: 'success' })
    await load()
  } catch (error) { uni.showToast({ title: error.message || '接单失败', icon: 'none' }) }
  finally { submitting.value = false }
}

async function finish() {
  const payload = form.value
  if (payload.finishReason.trim().length < 2 || payload.repairPlan.trim().length < 2 || payload.finishResult.trim().length < 2) {
    uni.showToast({ title: '请完整填写完工原因、维修方案和维修结果', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    const response = await finishWorkOrder(order.value.id, { ...payload, downtimeMinutes: Number(payload.downtimeMinutes) || 0, fileIds: [] })
    order.value = response.data
    uni.showToast({ title: '已提交完工', icon: 'success' })
    await load()
  } catch (error) { uni.showToast({ title: error.message || '完工提交失败', icon: 'none' }) }
  finally { submitting.value = false }
}

onMounted(load)
</script>

<template>
  <view class="page">
    <view v-if="loading" class="state">正在加载工单…</view>
    <view v-else-if="errorMessage" class="state error">{{ errorMessage }}</view>
    <template v-else-if="order">
      <view class="hero"><text class="eyebrow">REPAIR ORDER</text><text class="title">{{ order.orderNo }}</text><text class="subtitle">设备 #{{ order.equipId }} · {{ statusLabels[order.status] || '未知状态' }}</text></view>
      <view class="card detail"><text>派工说明</text><text class="muted">{{ order.dispatchRemark || '—' }}</text><text>期望完成</text><text class="muted">{{ order.expectFinishTime || '—' }}</text><text>备件费用</text><text class="muted">{{ order.spareCost ?? '—' }}</text></view>
      <view v-if="order.status === 0" class="card"><text class="section-title">现场接单</text><text class="muted">确认到达现场后开始维修，系统会记录接单人和时间。</text><button class="primary" :disabled="submitting" @click="accept">接单并开始维修</button></view>
      <view v-if="order.status === 1" class="card"><text class="section-title">提交完工</text><textarea v-model="form.finishReason" class="input" placeholder="完工原因，例如：更换主轴轴承" /><textarea v-model="form.repairPlan" class="input" placeholder="维修方案，例如：停机、拆卸、换件、复测" /><textarea v-model="form.finishResult" class="input" placeholder="维修结果，例如：设备恢复运行" /><input v-model="form.downtimeMinutes" class="input" type="number" placeholder="停机分钟数" /><button class="primary" :disabled="submitting" @click="finish">提交完工</button></view>
      <view v-if="order.status === 1 && spares.length" class="card"><text class="section-title">备件登记</text><picker mode="selector" :range="spares" range-key="spareName" @change="spareForm.spareId = String(spares[Number($event.detail.value)].id)"><view class="input picker">{{ spares.find((item) => String(item.id) === String(spareForm.spareId))?.spareName || '选择备件' }}</view></picker><input v-model="spareForm.quantity" class="input" type="number" placeholder="领用数量" /><input v-model="spareForm.remark" class="input" placeholder="领用说明（可选）" /><button class="primary" :disabled="submitting" @click="consume">登记领用</button><view v-for="record in spareRecords" :key="record.id" class="log"><text>#{{ record.spareId }} × {{ record.quantity }}</text><text class="muted">{{ record.remark || '—' }}</text></view></view>
      <view class="card"><text class="section-title">工单流转</text><view v-if="!logs.length" class="muted">暂无流转记录</view><view v-for="item in logs" :key="item.id" class="log"><text>{{ item.action }}</text><text class="muted">{{ item.createdTime }} · 操作人 #{{ item.operatorId }}</text></view></view>
    </template>
  </view>
</template>

<style>
.page { min-height: 100vh; padding: 28rpx 30rpx 70rpx; background: #f8fafc; color: #0f172a; }.hero { padding: 18rpx 8rpx 28rpx; }.eyebrow { display: block; color: #0f766e; font-size: 22rpx; letter-spacing: 5rpx; }.title { display: block; margin-top: 12rpx; font-size: 44rpx; font-weight: 750; }.subtitle { display: block; margin-top: 10rpx; color: #64748b; font-size: 26rpx; }.card { margin-bottom: 22rpx; padding: 28rpx; border-radius: 22rpx; background: #fff; box-shadow: 0 10rpx 30rpx rgba(15,23,42,.06); }.detail { display: grid; grid-template-columns: 1fr 1fr; gap: 16rpx; }.muted { display: block; color: #64748b; line-height: 1.6; }.section-title { display: block; margin-bottom: 12rpx; font-size: 31rpx; font-weight: 700; }.input { width: 100%; min-height: 82rpx; margin-top: 16rpx; padding: 18rpx; border: 2rpx solid #e2e8f0; border-radius: 14rpx; box-sizing: border-box; background: #fff; }.primary { margin-top: 22rpx; color: #fff; background: #2563eb; }.log { display: flex; justify-content: space-between; gap: 12rpx; padding: 18rpx 0; border-bottom: 1px solid #e2e8f0; }.state { padding: 100rpx 20rpx; color: #64748b; text-align: center; }.error { color: #b45309; }
</style>
