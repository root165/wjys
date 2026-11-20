<script setup>
import { ref } from 'vue'

const result = ref('')
const scanning = ref(false)

function scan() {
  scanning.value = true
  uni.scanCode({ scanType: ['qrCode', 'barCode'], success: (response) => {
    result.value = String(response.result || '').trim()
    uni.showToast({ title: result.value ? '扫码成功' : '二维码为空', icon: result.value ? 'success' : 'none' })
  }, fail: () => uni.showToast({ title: '扫码已取消', icon: 'none' }), complete: () => { scanning.value = false } })
}
</script>

<template>
  <view class="page"><view class="hero"><text class="eyebrow">FIELD SCAN</text><text class="title">扫码核验</text><text class="subtitle">扫描设备或巡检点二维码后，可回到任务详情继续填报。</text></view><view class="card"><button class="primary" :disabled="scanning" @click="scan">{{ scanning ? '打开相机…' : '开始扫码' }}</button><view v-if="result" class="result"><text class="label">最近一次结果</text><text class="code">{{ result }}</text></view></view></view>
</template>

<style>
.page { min-height: 100vh; padding: 32rpx; background: #f8fafc; color: #0f172a; }.hero { padding: 24rpx 8rpx 32rpx; }.eyebrow { display: block; color: #0f766e; font-size: 22rpx; letter-spacing: 5rpx; }.title { display: block; margin-top: 14rpx; font-size: 52rpx; font-weight: 750; }.subtitle { display: block; margin-top: 12rpx; color: #64748b; line-height: 1.6; }.card { padding: 30rpx; border-radius: 22rpx; background: #fff; box-shadow: 0 10rpx 30rpx rgba(15,23,42,.06); }.primary { color: #fff; background: #2563eb; }.result { margin-top: 28rpx; padding: 22rpx; border-radius: 14rpx; background: #eff6ff; }.label { display: block; color: #64748b; font-size: 24rpx; }.code { display: block; margin-top: 12rpx; color: #1d4ed8; font-weight: 700; word-break: break-all; }
</style>
