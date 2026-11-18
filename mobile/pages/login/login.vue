<script setup>
import { ref } from 'vue'
import { login } from '../../src/api/auth.js'
import { storageRepository } from '../../src/repositories/storageRepository.js'

const username = ref('')
const password = ref('')
const loading = ref(false)
const errorMessage = ref('')

async function submit() {
  if (!username.value.trim() || !password.value) {
    errorMessage.value = '请输入用户名和密码'
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await login(username.value.trim(), password.value)
    storageRepository.setAuth(response.data)
    uni.reLaunch({ url: '/pages/index/index' })
  } catch (error) {
    errorMessage.value = error.message || '登录失败，请检查网络和账号'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <view class="page">
    <view class="brand">维检云枢</view>
    <text class="subtitle">移动巡检工作台</text>
    <view class="card">
      <text class="label">用户名</text>
      <input v-model="username" class="input" placeholder="请输入用户名" maxlength="64" />
      <text class="label">密码</text>
      <input v-model="password" class="input" password placeholder="请输入密码" maxlength="128" />
      <view v-if="errorMessage" class="error">{{ errorMessage }}</view>
      <button class="primary" :disabled="loading" @click="submit">{{ loading ? '登录中…' : '登录' }}</button>
    </view>
    <text class="hint">首次进入需要联网登录，登录后可在弱网环境继续填写巡检草稿。</text>
  </view>
</template>

<style>
.page { min-height: 100vh; padding: 150rpx 48rpx 60rpx; background: #f8fafc; color: #0f172a; }
.brand { font-size: 64rpx; font-weight: 800; letter-spacing: 4rpx; }
.subtitle { display: block; margin-top: 18rpx; color: #64748b; font-size: 28rpx; }
.card { margin-top: 70rpx; padding: 36rpx; border-radius: 28rpx; background: #fff; box-shadow: 0 16rpx 50rpx rgba(15, 23, 42, .08); }
.label { display: block; margin: 18rpx 0 12rpx; color: #475569; font-size: 26rpx; }
.input { height: 88rpx; padding: 0 24rpx; border: 2rpx solid #e2e8f0; border-radius: 16rpx; box-sizing: border-box; }
.error { margin-top: 18rpx; color: #b45309; font-size: 25rpx; }
.primary { margin-top: 34rpx; color: #fff; background: #2563eb; border-radius: 16rpx; }
.hint { display: block; margin-top: 30rpx; color: #94a3b8; font-size: 24rpx; line-height: 1.6; }
</style>
