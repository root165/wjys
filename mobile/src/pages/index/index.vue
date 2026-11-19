<script setup>
import { computed, onMounted, ref } from 'vue'
import { pull, syncPending } from '../../api/sync'
import { logout } from '../../api/auth.js'
import { storageRepository } from '../../repositories/storageRepository'
import { syncQueue } from '../../repositories/syncQueue'

const tasks = ref([])
const loading = ref(false)
const errorMessage = ref('')
const queueVersion = ref(0)
const pendingCount = computed(() => { queueVersion.value; return syncQueue.pending().length })

async function loadTasks() {
  if (!storageRepository.getAuth()?.accessToken) {
    uni.reLaunch({ url: '/pages/login/login' })
    return
  }
  loading.value = true
  errorMessage.value = ''
  tasks.value = storageRepository.getTasks()
  try {
    if (pendingCount.value) {
      await syncPending()
      queueVersion.value += 1
    }
    const response = await pull(storageRepository.getSyncCursor())
    tasks.value = response.data?.tasks || []
    tasks.value.forEach((task) => storageRepository.setTask(task))
    if (response.data?.messages?.length) storageRepository.mergeMessages(response.data.messages)
    if (response.data?.cursor) storageRepository.setSyncCursor(response.data.cursor)
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
  uni.navigateTo({ url: '/pages/messages/index' })
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

onMounted(loadTasks)
onPullDownRefresh(loadTasks)
onShow(loadTasks)
</script>

<template>
  <view class="page">
    <view class="hero">
      <text class="eyebrow">FIELD OPERATIONS</text>
      <text class="title">今日巡检</text>
      <text class="subtitle">在线与离线都可以继续工作</text>
    </view>
    <view class="queue" @click="refreshQueue">
      <text>离线待同步 · 点击同步</text>
      <text class="queue-count">{{ pendingCount }}</text>
    </view>
    <view class="actions">
      <button size="mini" @click="openMessages">消息中心</button>
      <button size="mini" @click="signOut">退出登录</button>
    </view>
    <view v-if="loading" class="state">正在加载任务…</view>
    <view v-else-if="errorMessage" class="state error">{{ errorMessage }}</view>
    <view v-else-if="!tasks.length" class="state">暂无巡检任务</view>
    <view v-for="task in tasks" :key="task.id" class="task-card" @click="openTask(task)">
      <text class="task-name">{{ task.planName || '巡检任务' }}</text>
      <text class="task-no">{{ task.taskNo }}</text>
      <text class="task-name">计划日期 {{ task.planTime }}</text>
      <text class="task-status">{{ task.statusLabel || '待处理' }} · {{ task.totalItems || 0 }} 个检查项 · 异常 {{ task.abnormalCount || 0 }}</text>
    </view>
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
</style>
