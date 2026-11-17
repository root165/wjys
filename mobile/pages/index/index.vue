<script setup>
import { computed, onMounted, ref } from 'vue'
import { request } from '../../src/api/http'
import { storageRepository } from '../../src/repositories/storageRepository'
import { syncQueue } from '../../src/repositories/syncQueue'

const tasks = ref([])
const loading = ref(false)
const errorMessage = ref('')
const pendingCount = computed(() => syncQueue.pending().length)

async function loadTasks() {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await request({ url: '/inspection/tasks?pageNum=1&pageSize=20' })
    tasks.value = response.data?.records || []
  } catch (error) {
    errorMessage.value = error.message || '任务加载失败，当前可继续填写离线草稿'
  } finally {
    loading.value = false
    uni.stopPullDownRefresh()
  }
}

function refreshQueue() {
  uni.showToast({ title: `待同步 ${pendingCount.value} 条`, icon: 'none' })
}

onMounted(loadTasks)
onPullDownRefresh(loadTasks)
</script>

<template>
  <view class="page">
    <view class="hero">
      <text class="eyebrow">FIELD OPERATIONS</text>
      <text class="title">今日巡检</text>
      <text class="subtitle">在线与离线都可以继续工作</text>
    </view>
    <view class="queue" @click="refreshQueue">
      <text>离线待同步</text>
      <text class="queue-count">{{ pendingCount }}</text>
    </view>
    <view v-if="loading" class="state">正在加载任务…</view>
    <view v-else-if="errorMessage" class="state error">{{ errorMessage }}</view>
    <view v-else-if="!tasks.length" class="state">暂无巡检任务</view>
    <view v-for="task in tasks" :key="task.id" class="task-card">
      <text class="task-no">{{ task.taskNo }}</text>
      <text class="task-name">计划日期 {{ task.planTime }}</text>
      <text class="task-status">状态 {{ task.status }}</text>
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
.queue-count { color: #2563eb; font-size: 42rpx; font-weight: 700; }
.task-no, .task-name, .task-status { display: block; }
.task-no { font-weight: 700; }
.task-name, .task-status { margin-top: 12rpx; color: #64748b; font-size: 26rpx; }
.state { padding: 70rpx 20rpx; color: #64748b; text-align: center; }
.error { color: #b45309; }
</style>
