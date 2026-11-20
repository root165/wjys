<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { pull, syncPending } from '../../api/sync'
import { logout } from '../../api/auth.js'
import { listWorkOrders } from '../../api/workOrders.js'
import { storageRepository } from '../../repositories/storageRepository'
import { syncQueue } from '../../repositories/syncQueue'

const tasks = ref([])
const orders = ref([])
const loading = ref(false)
const errorMessage = ref('')
const updatedAt = ref('')
const queueVersion = ref(0)
let refreshTimer = null
const networkHandler = (status) => { if (status.isConnected) loadTasks() }
const pendingCount = computed(() => { queueVersion.value; return syncQueue.pending().length })
const user = computed(() => storageRepository.getUser() || {})
const isRepairer = computed(() => (user.value.roles || []).includes('repairer'))
const isInspector = computed(() => (user.value.roles || []).includes('inspector'))
const todoCount = computed(() => isRepairer.value ? orders.value.length : tasks.value.length)

async function loadTasks() {
  if (!storageRepository.getAuth()?.accessToken) {
    uni.reLaunch({ url: '/pages/login/login' })
    return
  }
  loading.value = true
  errorMessage.value = ''
  tasks.value = storageRepository.getTasks()
  try {
    if (isRepairer.value) {
      const response = await listWorkOrders()
      orders.value = response.data || []
      updatedAt.value = new Date().toLocaleTimeString()
      return
    }
    if (pendingCount.value) {
      await syncPending()
      queueVersion.value += 1
    }
    const response = await pull(storageRepository.getSyncCursor())
    tasks.value = response.data?.tasks || []
    tasks.value.forEach((task) => storageRepository.setTask(task))
    if (response.data?.messages?.length) storageRepository.mergeMessages(response.data.messages)
    if (response.data?.cursor) storageRepository.setSyncCursor(response.data.cursor)
    updatedAt.value = new Date().toLocaleTimeString()
  } catch (error) {
    if (error.statusCode === 403) { storageRepository.clearAuth(); uni.reLaunch({ url: '/pages/login/login' }); return }
    errorMessage.value = error.message || '任务加载失败，当前可继续填写离线草稿'
  } finally {
    loading.value = false
    uni.stopPullDownRefresh()
  }
}

function openTask(task) {
  uni.navigateTo({ url: `/pages/task/detail?id=${task.id}` })
}

function openMessages() {
  uni.switchTab({ url: '/pages/messages/index' })
}

function openScan() {
  uni.navigateTo({ url: '/pages/scan/index' })
}

function openOrder(order) {
  uni.navigateTo({ url: `/pages/order/detail?id=${order.id}` })
}

async function signOut() {
  try { await logout() } catch (error) { /* 令牌失效时仍清理本地会话 */ }
  storageRepository.clearAuth()
  uni.reLaunch({ url: '/pages/login/login' })
}

async function refreshQueue() {
  if (!pendingCount.value) { uni.showToast({ title: '暂无待同步记录', icon: 'none' }); return }
  uni.showLoading({ title: '同步中' })
  try {
    const results = await syncPending()
    queueVersion.value += 1
    const conflicts = results.filter((item) => item.status === 'conflict').length
    uni.showToast({ title: conflicts ? `${conflicts} 条冲突待处理` : '同步完成', icon: conflicts ? 'none' : 'success' })
  } catch (error) { uni.showToast({ title: error.message || '同步失败', icon: 'none' }) } finally { uni.hideLoading() }
}

onMounted(() => {
  loadTasks()
  refreshTimer = setInterval(loadTasks, 30000)
  if (typeof uni.onNetworkStatusChange === 'function') uni.onNetworkStatusChange(networkHandler)
})
onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (typeof uni.offNetworkStatusChange === 'function') uni.offNetworkStatusChange(networkHandler)
})
onPullDownRefresh(loadTasks)
onShow(loadTasks)
</script>

<template>
  <view class="page">
    <view class="hero">
      <text class="eyebrow">FIELD OPERATIONS</text>
      <text class="title">{{ isRepairer ? '维修工作台' : '今日巡检' }}</text>
      <text class="subtitle">{{ user.nickname || user.username || '现场执行人员' }} · 在线与离线都可以继续工作</text>
    </view>
    <view class="queue" @click="refreshQueue">
      <text>离线待同步 · 点击同步</text>
      <text class="queue-count">{{ pendingCount }}</text>
    </view>
    <view class="actions">
      <button v-if="isInspector" size="mini" @click="openScan">扫码</button>
      <button size="mini" @click="openMessages">消息中心</button>
      <button size="mini" @click="signOut">退出登录</button>
    </view>
    <text v-if="updatedAt" class="updated">最近同步 {{ updatedAt }}</text>
    <view v-if="loading" class="state">正在加载任务…</view>
    <view v-else-if="errorMessage" class="state error">{{ errorMessage }}</view>
    <view v-else-if="isInspector && !tasks.length" class="state">暂无巡检任务</view>
    <view v-else-if="isRepairer && !orders.length" class="state">暂无维修工单</view>
    <template v-if="isInspector">
      <view v-for="task in tasks" :key="`task-${task.id}`" class="task-card" @click="openTask(task)">
        <text class="task-name">{{ task.planName || '巡检任务' }}</text>
        <text class="task-no">{{ task.taskNo }}</text>
        <text class="task-name">计划日期 {{ task.planTime }}</text>
        <text class="task-status">{{ task.statusLabel || '待处理' }} · {{ task.totalItems || 0 }} 个检查项 · 异常 {{ task.abnormalCount || 0 }}</text>
      </view>
    </template>
    <template v-else-if="isRepairer">
      <view v-for="order in orders" :key="`order-${order.id}`" class="task-card" @click="openOrder(order)">
        <text class="task-name">维修工单</text>
        <text class="task-no">{{ order.orderNo }}</text>
        <text class="task-name">设备 #{{ order.equipId }} · {{ order.expectFinishTime || '未设置期望完成时间' }}</text>
        <text class="task-status">{{ ['待接单', '维修中', '待验收', '已完成', '已退回'][order.status] || '未知状态' }} · 停机 {{ order.downtimeMinutes || 0 }} 分钟</text>
      </view>
    </template>
    <view class="summary"><text>当前待办</text><text class="summary-count">{{ todoCount }}</text></view>
  </view>
</template>

<style>
.page { min-height: 100vh; padding: 40rpx; background: #f8fafc; color: #0f172a; }
.hero { padding: 48rpx 8rpx 36rpx; }
.eyebrow { display: block; color: #64748b; font-size: 22rpx; letter-spacing: 5rpx; }
.title { display: block; margin-top: 18rpx; font-size: 58rpx; font-weight: 700; }
.subtitle { display: block; margin-top: 12rpx; color: #64748b; font-size: 28rpx; }
.queue, .task-card { margin-bottom: 22rpx; padding: 30rpx; border-radius: 24rpx; background: #fff; box-shadow: 0 12rpx 36rpx rgba(15, 23, 42, .06); }
.queue { display: flex; justify-content: space-between; align-items: center; color: #334155; }
.actions { display: flex; justify-content: space-between; margin-bottom: 22rpx; }
.actions button { margin: 0; color: #2563eb; background: #eff6ff; }
.queue-count { color: #2563eb; font-size: 42rpx; font-weight: 700; }
.task-no, .task-name, .task-status { display: block; }
.task-no { font-weight: 700; }
.task-name, .task-status { margin-top: 12rpx; color: #64748b; font-size: 26rpx; }
.state { padding: 70rpx 20rpx; color: #64748b; text-align: center; }
.error { color: #b45309; }
.updated { display: block; margin: -10rpx 0 20rpx; color: #94a3b8; font-size: 22rpx; }
.summary { display: flex; justify-content: space-between; margin-top: 8rpx; padding: 24rpx 30rpx; border-radius: 20rpx; background: #0f172a; color: #fff; }.summary-count { color: #93c5fd; font-size: 36rpx; font-weight: 700; }
</style>
