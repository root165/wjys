<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { receiveTask } from '../../src/api/inspection.js'
import { syncPending } from '../../src/api/sync.js'
import { photoRepository } from '../../src/repositories/photoRepository.js'
import { storageRepository } from '../../src/repositories/storageRepository.js'
import { syncQueue } from '../../src/repositories/syncQueue.js'

const task = ref(null)
const loading = ref(true)
const submitting = ref(false)
const errorMessage = ref('')
const refreshPhotos = ref(0)
const states = reactive({})

function taskId() {
  const pages = getCurrentPages()
  const current = pages[pages.length - 1]
  return Number(current?.options?.id || 0)
}
function localIso() {
  const date = new Date()
  const pad = (value) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function stateFor(item) {
  if (!states[item.id]) states[item.id] = { checkResult: item.checkResult ?? 0, checkValue: item.checkValue || '', remark: item.remark || '' }
  return states[item.id]
}

function photosFor(item) {
  refreshPhotos.value
  return photoRepository.listForTaskItem(item.id)
}

async function load() {
  const id = taskId()
  task.value = storageRepository.getTask(id)
  if (!task.value) errorMessage.value = '任务缓存不存在，请返回首页重新同步'
  else {
    Object.assign(states, storageRepository.getDraft(id))
    task.value.items.forEach((item) => stateFor(item))
  }
  loading.value = false
}

watch(states, () => {
  if (task.value) storageRepository.setDraft(task.value.id, JSON.parse(JSON.stringify(states)))
}, { deep: true })

async function receive() {
  submitting.value = true
  try {
    const response = await receiveTask(task.value.id)
    task.value = response.data
    storageRepository.setTask(task.value)
  } catch (error) { errorMessage.value = error.message || '领取失败' }
  finally { submitting.value = false }
}

async function takePhoto(item) {
  uni.chooseImage({ count: 3, sourceType: ['camera', 'album'], success: async (result) => {
    try {
      await photoRepository.saveForTaskItem(task.value.id, item.id, result.tempFilePaths)
      refreshPhotos.value += 1
    } catch (error) { uni.showToast({ title: error.message || '照片保存失败', icon: 'none' }) }
  } })
}

function scanCode(item) {
  uni.scanCode({ scanType: ['qrCode', 'barCode'], success: (result) => {
    const code = String(result.result || '').trim()
    const accepted = [item.equipCode, item.pointCode, item.itemCode].filter(Boolean).includes(code)
    if (!accepted) {
      uni.showToast({ title: '二维码/条码与当前检查项不匹配', icon: 'none' })
      return
    }
    stateFor(item).scanValue = code
    uni.showToast({ title: '扫码核验通过', icon: 'success' })
  }, fail: () => {
    uni.showToast({ title: '扫码已取消', icon: 'none' })
  } })
}

function removePhoto(photo) {
  photoRepository.remove(photo.id).then(() => { refreshPhotos.value += 1 })
}

function validate() {
  for (const item of task.value.items) {
    const state = stateFor(item)
    const photos = photosFor(item)
    if (item.requiredPhoto === 1 && photos.length === 0) return `“${item.itemName}”必须上传照片`
    if (state.checkResult === 1 && !state.remark.trim()) return `“${item.itemName}”为异常时必须填写说明`
    if (item.valueType === 'NUMBER' && state.checkValue) {
      const numericValue = Number(state.checkValue)
      if (Number.isNaN(numericValue)) return `“${item.itemName}”必须填写数字`
      if (item.minValue != null && numericValue < Number(item.minValue)) return `“${item.itemName}”不能低于 ${item.minValue}`
      if (item.maxValue != null && numericValue > Number(item.maxValue)) return `“${item.itemName}”不能高于 ${item.maxValue}`
    }
  }
  return ''
}

async function submit() {
  const validation = validate()
  if (validation) { uni.showToast({ title: validation, icon: 'none' }); return }
  const payload = {
    submitNo: `${task.value.id}-${Date.now()}-${Math.random().toString(16).slice(2)}`,
    clientSubmitTime: localIso(), offlineFlag: false,
    items: task.value.items.map((item) => ({
      taskItemId: item.id, checkResult: stateFor(item).checkResult,
      checkValue: stateFor(item).checkValue || null, remark: stateFor(item).remark || null,
      checkTime: localIso(), fileIds: [],
    })),
  }
  const photoBindings = task.value.items.flatMap((item) => photosFor(item).map((photo) => ({ taskItemId: item.id, path: photo.path })))
  syncQueue.enqueueInspectionSubmit({ taskId: task.value.id, payload, photoBindings })
  storageRepository.setDraft(task.value.id, states)
  submitting.value = true
  try {
    const results = await syncPending()
    const result = results[0]
    if (result?.status === 'confirmed') {
      await photoRepository.removeForTask(task.value.id)
      storageRepository.removeDraft(task.value.id)
      uni.showToast({ title: '提交成功', icon: 'success' })
      setTimeout(() => uni.navigateBack(), 500)
    } else {
      uni.showToast({ title: '已保存，联网后将自动同步', icon: 'none' })
    }
  } catch (error) { uni.showToast({ title: error.message || '已保存到离线队列', icon: 'none' }) }
  finally { submitting.value = false }
}

onMounted(load)
</script>

<template>
  <view class="page">
    <view v-if="loading" class="state">正在加载任务…</view>
    <view v-else-if="errorMessage" class="state error">{{ errorMessage }}</view>
    <template v-else-if="task">
      <view class="hero">
        <text class="task-no">{{ task.taskNo }}</text>
        <text class="title">巡检填写</text>
        <text class="subtitle">{{ task.planTime }} · {{ task.statusLabel }}</text>
      </view>
      <view v-if="task.status === 0 || task.status === 3" class="card">
        <text>领取任务后开始填写，当前设备/点位快照会保存在本地。</text>
        <button class="primary" :disabled="submitting" @click="receive">领取任务</button>
      </view>
      <view v-for="item in task.items" :key="item.id" class="card">
        <text class="item-title">{{ item.itemName }}</text>
        <text class="meta">{{ item.equipCode }} · {{ item.pointCode }} · {{ item.checkMethod || '按标准检查' }}</text>
        <view class="result-row">
          <button size="mini" :class="stateFor(item).checkResult === 0 ? 'selected' : ''" @click="stateFor(item).checkResult = 0">正常</button>
          <button size="mini" :class="stateFor(item).checkResult === 1 ? 'selected abnormal' : ''" @click="stateFor(item).checkResult = 1">异常</button>
        </view>
        <input v-if="item.valueType === 'NUMBER'" v-model="stateFor(item).checkValue" class="input" type="digit" :placeholder="item.unit ? `数值（${item.unit}）` : '填写数值'" />
        <textarea v-model="stateFor(item).remark" class="textarea" placeholder="检查说明 / 异常原因" />
        <view class="photo-row">
          <view v-for="photo in photosFor(item)" :key="photo.id" class="photo-chip" @click="removePhoto(photo)">已选照片 · 点击移除</view>
          <button size="mini" @click="scanCode(item)">扫码核验</button>
          <button size="mini" @click="takePhoto(item)">拍照 / 相册</button>
        </view>
      </view>
      <button v-if="task.status === 1 || task.status === 3" class="primary submit" :disabled="submitting" @click="submit">{{ submitting ? '提交处理中…' : '提交巡检结果' }}</button>
    </template>
  </view>
</template>

<style>
.page { min-height: 100vh; padding: 26rpx 30rpx 70rpx; background: #f8fafc; color: #0f172a; }
.hero { padding: 18rpx 8rpx 28rpx; }.task-no, .subtitle, .meta { display: block; color: #64748b; font-size: 25rpx; }.title { display: block; margin-top: 10rpx; font-size: 48rpx; font-weight: 750; }.subtitle { margin-top: 8rpx; }
.card { margin-bottom: 22rpx; padding: 28rpx; border-radius: 22rpx; background: #fff; box-shadow: 0 10rpx 30rpx rgba(15,23,42,.06); }.item-title { display: block; font-size: 31rpx; font-weight: 700; }.meta { margin-top: 10rpx; }.result-row, .photo-row { display: flex; gap: 14rpx; align-items: center; margin-top: 20rpx; }.result-row button, .photo-row button { margin: 0; color: #475569; background: #f1f5f9; }.result-row .selected { color: #fff; background: #16a34a; }.result-row .abnormal { background: #dc2626; }.input, .textarea { width: 100%; margin-top: 18rpx; padding: 18rpx; border: 2rpx solid #e2e8f0; border-radius: 14rpx; box-sizing: border-box; }.textarea { min-height: 130rpx; }.photo-chip { padding: 12rpx 16rpx; color: #1d4ed8; background: #eff6ff; border-radius: 12rpx; font-size: 23rpx; }.primary { margin-top: 22rpx; color: #fff; background: #2563eb; }.submit { margin-bottom: 30rpx; }.state { padding: 100rpx 20rpx; color: #64748b; text-align: center; }.error { color: #b45309; }
</style>
