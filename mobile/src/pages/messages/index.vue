<script setup>
import { onMounted, ref } from 'vue'
import { pull } from '../../api/sync.js'
import { markRead } from '../../api/messages.js'
import { storageRepository } from '../../repositories/storageRepository.js'

const messages = ref([])
const loading = ref(true)
const errorMessage = ref('')

async function load() {
  messages.value = storageRepository.getMessages()
  try {
    const response = await pull(storageRepository.getSyncCursor(), ['messages'])
    messages.value = storageRepository.mergeMessages(response.data?.messages || [])
    if (response.data?.cursor) storageRepository.setSyncCursor(response.data.cursor)
  } catch (error) {
    if (error.statusCode === 403) { storageRepository.clearAuth(); uni.reLaunch({ url: '/pages/login/login' }); return }
    if (!messages.value.length) errorMessage.value = error.message || '消息加载失败'
  }
  finally { loading.value = false }
}

async function read(message) {
  if (message.readFlag === 1) return
  try {
    await markRead(message.id)
    message.readFlag = 1
    storageRepository.setMessages(messages.value)
    messages.value = [...messages.value]
  } catch (error) { uni.showToast({ title: error.message || '标记失败', icon: 'none' }) }
}

onMounted(() => {
  if (!storageRepository.getAuth()?.accessToken) { uni.reLaunch({ url: '/pages/login/login' }); return }
  load()
})
</script>

<template>
  <view class="page">
    <view v-if="loading" class="state">正在加载消息…</view>
    <view v-else-if="errorMessage" class="state error">{{ errorMessage }}</view>
    <view v-else-if="!messages.length" class="state">暂无消息</view>
    <view v-for="message in messages" :key="message.id" class="card" :class="message.readFlag === 0 ? 'unread' : ''" @click="read(message)">
      <view class="row"><text class="title">{{ message.title }}</text><text class="time">{{ message.createdTime }}</text></view>
      <text class="content">{{ message.content }}</text>
      <text v-if="message.readFlag === 0" class="badge">未读 · 点击标记已读</text>
    </view>
  </view>
</template>

<style>
.page { min-height: 100vh; padding: 28rpx; background: #f8fafc; color: #0f172a; }.card { margin-bottom: 18rpx; padding: 26rpx; border-radius: 22rpx; background: #fff; box-shadow: 0 10rpx 30rpx rgba(15,23,42,.05); }.unread { border-left: 8rpx solid #2563eb; }.row { display: flex; justify-content: space-between; gap: 16rpx; }.title { font-size: 30rpx; font-weight: 700; }.time { color: #94a3b8; font-size: 21rpx; }.content { display: block; margin-top: 14rpx; color: #475569; line-height: 1.6; }.badge { display: inline-block; margin-top: 18rpx; padding: 8rpx 14rpx; color: #1d4ed8; background: #eff6ff; border-radius: 10rpx; font-size: 22rpx; }.state { padding: 100rpx 20rpx; color: #64748b; text-align: center; }.error { color: #b45309; }
</style>
