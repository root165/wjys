<script setup>
import { onMounted, ref } from 'vue'
import { changePassword, info, logout, updateProfile } from '../../api/auth.js'
import { storageRepository } from '../../repositories/storageRepository.js'
import { syncQueue } from '../../repositories/syncQueue.js'

const user = ref(storageRepository.getUser() || {})
const editing = ref(false)
const passwordEditing = ref(false)
const saving = ref(false)
const message = ref('')
const profileForm = ref({ nickname: '', phone: '', email: '' })
const passwordForm = ref({ currentPassword: '', newPassword: '' })

function syncForm() {
  profileForm.value = { nickname: user.value.nickname || '', phone: user.value.phone || '', email: user.value.email || '' }
}

async function load() {
  try {
    const response = await info()
    user.value = response.data?.user || user.value
    storageRepository.setUser(user.value)
    syncForm()
  } catch (error) { message.value = error.message || '资料加载失败' }
}

async function saveProfile() {
  if (!profileForm.value.nickname.trim()) { message.value = '姓名不能为空'; return }
  saving.value = true
  try {
    const response = await updateProfile(profileForm.value)
    user.value = response.data
    storageRepository.setUser(user.value)
    editing.value = false
    message.value = '资料已保存'
  } catch (error) { message.value = error.message || '资料保存失败' }
  finally { saving.value = false }
}

async function savePassword() {
  if (!passwordForm.value.currentPassword || passwordForm.value.newPassword.length < 8) { message.value = '新密码至少 8 位'; return }
  saving.value = true
  try {
    await changePassword(passwordForm.value)
    await logout().catch(() => {})
    storageRepository.clearAuth()
    uni.showToast({ title: '密码已修改，请重新登录', icon: 'none' })
    uni.reLaunch({ url: '/pages/login/login' })
  } catch (error) { message.value = error.message || '密码修改失败' }
  finally { saving.value = false }
}

async function signOut() {
  await logout().catch(() => {})
  storageRepository.clearAuth()
  uni.reLaunch({ url: '/pages/login/login' })
}

onMounted(() => { syncForm(); load() })
</script>

<template>
  <view class="page">
    <view class="hero"><text class="eyebrow">ACCOUNT</text><text class="title">我的</text><text class="subtitle">个人资料、登录安全和离线队列</text></view>
    <view v-if="message" class="notice">{{ message }}</view>
    <view class="card profile"><view class="avatar">{{ (user.nickname || user.username || '员').slice(0, 1) }}</view><view><text class="name">{{ user.nickname || '未填写姓名' }}</text><text class="muted">{{ user.username || '—' }}</text></view></view>
    <view class="card"><text class="section-title">个人资料</text><template v-if="editing"><input v-model="profileForm.nickname" class="input" placeholder="姓名" /><input v-model="profileForm.phone" class="input" placeholder="手机号" /><input v-model="profileForm.email" class="input" placeholder="邮箱" /><view class="actions"><button size="mini" @click="editing = false">取消</button><button size="mini" class="primary" :disabled="saving" @click="saveProfile">保存</button></view></template><template v-else><text class="line">姓名：{{ user.nickname || '—' }}</text><text class="line">手机：{{ user.phone || '—' }}</text><text class="line">邮箱：{{ user.email || '—' }}</text><text class="line">角色：{{ (user.roles || []).join('、') || '—' }}</text><button size="mini" class="secondary" @click="editing = true">编辑资料</button></template></view>
    <view class="card"><text class="section-title">离线状态</text><text class="line">待同步记录：{{ syncQueue.pending().length }}</text><text class="muted">离线巡检提交会在联网后自动重试，未确认记录不会被清理。</text></view>
    <view class="card"><text class="section-title">账号安全</text><template v-if="passwordEditing"><input v-model="passwordForm.currentPassword" class="input" password placeholder="当前密码" /><input v-model="passwordForm.newPassword" class="input" password placeholder="新密码（至少 8 位）" /><view class="actions"><button size="mini" @click="passwordEditing = false">取消</button><button size="mini" class="primary" :disabled="saving" @click="savePassword">修改密码</button></view></template><button v-else size="mini" class="secondary" @click="passwordEditing = true">修改密码</button></view>
    <button class="danger" @click="signOut">退出登录</button>
  </view>
</template>

<style>
.page { min-height: 100vh; padding: 28rpx 30rpx 70rpx; background: #f8fafc; color: #0f172a; }.hero { padding: 18rpx 8rpx 28rpx; }.eyebrow { display: block; color: #0f766e; font-size: 22rpx; letter-spacing: 5rpx; }.title { display: block; margin-top: 12rpx; font-size: 52rpx; font-weight: 750; }.subtitle, .muted { display: block; margin-top: 10rpx; color: #64748b; line-height: 1.6; }.notice { margin-bottom: 18rpx; padding: 18rpx; border-radius: 12rpx; background: #eff6ff; color: #1d4ed8; }.card { margin-bottom: 20rpx; padding: 28rpx; border-radius: 22rpx; background: #fff; box-shadow: 0 10rpx 30rpx rgba(15,23,42,.06); }.profile { display: flex; align-items: center; gap: 18rpx; }.avatar { width: 92rpx; height: 92rpx; display: grid; place-items: center; border-radius: 26rpx; background: #dbeafe; color: #1d4ed8; font-size: 40rpx; font-weight: 700; }.name { display: block; font-size: 34rpx; font-weight: 700; }.section-title { display: block; margin-bottom: 12rpx; font-size: 31rpx; font-weight: 700; }.line { display: block; padding: 13rpx 0; border-bottom: 1px solid #e2e8f0; }.input { width: 100%; min-height: 78rpx; margin-top: 14rpx; padding: 16rpx; border: 2rpx solid #e2e8f0; border-radius: 14rpx; box-sizing: border-box; }.actions { display: flex; gap: 14rpx; margin-top: 18rpx; }.actions button { margin: 0; }.primary { color: #fff; background: #2563eb; }.secondary { margin-top: 18rpx; color: #1d4ed8; background: #eff6ff; }.danger { margin: 8rpx 0 40rpx; color: #b91c1c; background: #fee2e2; }
</style>
