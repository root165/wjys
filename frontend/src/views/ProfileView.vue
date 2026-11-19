<script setup>
import { reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as authApi from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const profileVisible = ref(false)
const passwordVisible = ref(false)
const avatarVisible = ref(false)
const saving = ref(false)
const uploading = ref(false)
const avatarPreview = ref('')
const profileForm = reactive({ nickname: '', phone: '', email: '', avatarFileId: null })
const passwordForm = reactive({ currentPassword: '', newPassword: '', confirmPassword: '' })

watch(() => auth.user, (user) => {
  if (!user) return
  profileForm.nickname = user.nickname || ''
  profileForm.phone = user.phone || ''
  profileForm.email = user.email || ''
  profileForm.avatarFileId = user.avatarFileId || null
}, { immediate: true, deep: true })

function openProfileEditor() { avatarPreview.value = ''; profileVisible.value = true }
function openAvatarEditor() { avatarPreview.value = ''; avatarVisible.value = true }

async function uploadAvatar(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) return ElMessage.warning('请选择 PNG、JPG 或 WebP 图片')
  if (file.size > 5 * 1024 * 1024) return ElMessage.warning('头像不能超过 5 MB')
  uploading.value = true
  try {
    const response = await authApi.uploadAvatar(file)
    profileForm.avatarFileId = response.data.fileId
    avatarPreview.value = URL.createObjectURL(file)
    ElMessage.success('头像已上传，请点击保存头像完成绑定')
  } catch (error) { ElMessage.error(error.message || '头像上传失败') } finally { uploading.value = false }
}

async function saveProfile() {
  if (!profileForm.nickname.trim()) return ElMessage.warning('请填写姓名，方便同事识别')
  saving.value = true
  try {
    const response = await authApi.updateProfile({ ...profileForm, nickname: profileForm.nickname.trim() })
    auth.user = response.data
    localStorage.setItem('wjys:user', JSON.stringify(auth.user))
    await auth.loadAvatar()
    profileVisible.value = false
    ElMessage.success('个人资料已保存')
  } catch (error) { ElMessage.error(error.message || '资料保存失败') } finally { saving.value = false }
}

async function saveAvatar() {
  if (!profileForm.avatarFileId) return ElMessage.warning('请先选择并上传新头像')
  saving.value = true
  try {
    const response = await authApi.updateProfile({ ...profileForm, nickname: profileForm.nickname.trim() })
    auth.user = response.data
    localStorage.setItem('wjys:user', JSON.stringify(auth.user))
    await auth.loadAvatar()
    avatarVisible.value = false
    avatarPreview.value = ''
    ElMessage.success('头像已更新')
  } catch (error) { ElMessage.error(error.message || '头像保存失败') } finally { saving.value = false }
}

async function savePassword() {
  if (!passwordForm.currentPassword) return ElMessage.warning('请输入当前密码')
  if (passwordForm.newPassword.length < 8) return ElMessage.warning('新密码至少需要 8 位')
  if (passwordForm.newPassword !== passwordForm.confirmPassword) return ElMessage.warning('两次输入的新密码不一致')
  saving.value = true
  try {
    await authApi.changePassword({ currentPassword: passwordForm.currentPassword, newPassword: passwordForm.newPassword })
    Object.assign(passwordForm, { currentPassword: '', newPassword: '', confirmPassword: '' })
    passwordVisible.value = false
    ElMessage.success('密码已修改，请重新登录')
    auth.clear()
    await router.replace({ name: 'login' })
  } catch (error) { ElMessage.error(error.message || '密码修改失败') } finally { saving.value = false }
}
</script>

<template>
  <div class="profile-page">
    <div class="page-intro"><div><h2>个人资料</h2><p>查看自己的账号与联系方式；角色、部门和权限由系统管理员统一管理。</p></div></div>
    <div class="profile-grid">
      <section class="soft-card profile-card">
        <div class="profile-heading"><div class="profile-avatar"><img v-if="auth.avatarSrc" :src="auth.avatarSrc" alt="当前用户头像" /><span v-else>{{ (auth.user?.nickname || '用').slice(0, 1) }}</span></div><div><h3>{{ auth.user?.nickname || '未命名用户' }}</h3><p>@{{ auth.user?.username || '—' }}</p></div></div>
        <div class="profile-actions"><button class="secondary-button" type="button" @click="openAvatarEditor">更换头像</button><button class="primary-button compact-button" type="button" @click="openProfileEditor">编辑资料</button></div>
        <dl class="profile-details"><div><dt>姓名</dt><dd>{{ auth.user?.nickname || '未填写' }}</dd></div><div><dt>手机号</dt><dd>{{ auth.user?.phone || '未填写' }}</dd></div><div><dt>邮箱</dt><dd>{{ auth.user?.email || '未填写' }}</dd></div><div><dt>账号</dt><dd>{{ auth.user?.username || '—' }}</dd></div><div class="profile-detail-wide"><dt>角色</dt><dd><span v-for="role in auth.user?.roles || []" :key="role" class="role-tag">{{ role }}</span><span v-if="!auth.user?.roles?.length">未分配</span></dd></div></dl>
      </section>
      <section class="soft-card profile-card security-card"><div class="panel-heading"><div><h3>账号安全</h3><span>定期修改密码，避免多人共用账号。</span></div></div><div class="security-row"><div><strong>登录密码</strong><p>使用当前密码验证后设置新密码，修改后需要重新登录。</p></div><button class="secondary-button" type="button" @click="passwordVisible = true">修改密码</button></div></section>
    </div>

    <el-dialog v-model="profileVisible" title="编辑个人资料" width="520px" destroy-on-close><p class="dialog-tip">这里修改的是工作台展示信息和联系方式，不会改变账号、角色或部门。</p><form class="dialog-form" @submit.prevent="saveProfile"><label class="form-label">姓名</label><input v-model="profileForm.nickname" class="form-input" maxlength="64" placeholder="例如：张明；会展示在工作台和消息中" required /><label class="form-label">手机号</label><input v-model="profileForm.phone" class="form-input" maxlength="32" placeholder="例如：13800000000；仅供联系" /><label class="form-label">邮箱</label><input v-model="profileForm.email" class="form-input" type="email" maxlength="128" placeholder="例如：zhangming@example.com" /><div class="dialog-actions"><button class="secondary-button" type="button" @click="profileVisible = false">取消</button><button class="primary-button compact-button" type="submit" :disabled="saving">{{ saving ? '保存中…' : '保存资料' }}</button></div></form></el-dialog>
    <el-dialog v-model="avatarVisible" title="更换头像" width="460px" destroy-on-close><p class="dialog-tip">支持 PNG、JPG、WebP，文件不超过 5 MB。上传后点击保存头像才会绑定到当前账号。</p><div class="avatar-dialog-content"><div class="profile-avatar avatar-preview"><img v-if="avatarPreview" :src="avatarPreview" alt="新头像预览" /><img v-else-if="auth.avatarSrc" :src="auth.avatarSrc" alt="当前头像" /><span v-else>{{ (auth.user?.nickname || '用').slice(0, 1) }}</span></div><label class="upload-button">{{ uploading ? '上传中…' : '选择图片' }}<input type="file" accept="image/png,image/jpeg,image/webp" :disabled="uploading" @change="uploadAvatar" /></label></div><div class="dialog-actions"><button class="secondary-button" type="button" @click="avatarVisible = false">取消</button><button class="primary-button compact-button" type="button" :disabled="saving || !profileForm.avatarFileId" @click="saveAvatar">{{ saving ? '保存中…' : '保存头像' }}</button></div></el-dialog>
    <el-dialog v-model="passwordVisible" title="修改密码" width="500px" destroy-on-close><p class="dialog-tip">新密码至少 8 位。保存后当前登录令牌会失效，请使用新密码重新登录。</p><form class="dialog-form" @submit.prevent="savePassword"><label class="form-label">当前密码</label><input v-model="passwordForm.currentPassword" class="form-input" type="password" autocomplete="current-password" placeholder="输入当前正在使用的密码" required /><label class="form-label">新密码</label><input v-model="passwordForm.newPassword" class="form-input" type="password" autocomplete="new-password" minlength="8" placeholder="至少 8 位，建议混合字母和数字" required /><label class="form-label">确认新密码</label><input v-model="passwordForm.confirmPassword" class="form-input" type="password" autocomplete="new-password" minlength="8" placeholder="再次输入新密码" required /><div class="dialog-actions"><button class="secondary-button" type="button" @click="passwordVisible = false">取消</button><button class="primary-button compact-button" type="submit" :disabled="saving">{{ saving ? '提交中…' : '确认修改' }}</button></div></form></el-dialog>
  </div>
</template>

<style scoped>
.profile-grid { display: grid; grid-template-columns: minmax(0, 1.1fr) minmax(320px, .9fr); gap: 18px; max-width: 1020px; }.profile-card { padding: 26px; }.profile-heading { display: flex; align-items: center; gap: 16px; margin-bottom: 18px; }.profile-heading h3 { margin: 0 0 5px; color: #183a2f; font-size: 20px; }.profile-heading p { margin: 0; color: #84948c; font-size: 13px; }.profile-avatar { width: 84px; height: 84px; display: grid; place-items: center; overflow: hidden; flex: 0 0 84px; border-radius: 22px; background: #dceca2; color: #2c604c; font-size: 28px; font-weight: 700; }.profile-avatar img { width: 100%; height: 100%; display: block; object-fit: cover; }.profile-actions { display: flex; gap: 10px; margin-bottom: 20px; }.profile-actions .primary-button, .profile-actions .secondary-button { width: auto; padding: 0 18px; }.profile-details { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 22px; margin: 0; }.profile-details > div { padding: 14px 0; border-top: 1px solid #edf2ed; }.profile-details dt { color: #82938a; font-size: 12px; }.profile-details dd { margin: 5px 0 0; color: #244a3c; font-size: 14px; }.profile-detail-wide { grid-column: span 2; }.role-tag { display: inline-flex; margin: 0 6px 4px 0; padding: 4px 8px; border-radius: 6px; background: #eef5d6; color: #5c7833; font-size: 12px; }.security-card { align-self: start; }.security-row { display: flex; justify-content: space-between; align-items: start; gap: 18px; padding: 12px 0; }.security-row strong { color: #244a3c; font-size: 14px; }.security-row p { max-width: 300px; margin: 8px 0 0; color: #87988f; font-size: 12px; line-height: 1.6; }.secondary-button { height: 42px; border: 1px solid #cbd9ce; border-radius: 9px; background: #f7faf7; color: #335f4d; cursor: pointer; white-space: nowrap; }.secondary-button:hover { border-color: #8bb197; background: #eef6ed; }.compact-button { width: auto; padding: 0 18px; }.dialog-tip { margin: -4px 0 16px; color: #789087; font-size: 13px; line-height: 1.7; }.dialog-form { display: grid; gap: 2px; }.dialog-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px; }.avatar-dialog-content { display: flex; align-items: center; gap: 20px; padding: 10px 0 8px; }.avatar-preview { width: 112px; height: 112px; flex-basis: 112px; }.upload-button { position: relative; display: inline-flex; align-items: center; justify-content: center; min-height: 42px; padding: 0 16px; border: 1px solid #cbd9ce; border-radius: 9px; background: #f7faf7; color: #335f4d; cursor: pointer; }.upload-button input { position: absolute; inset: 0; width: 100%; height: 100%; opacity: 0; cursor: pointer; }.upload-button input:disabled { cursor: not-allowed; }@media (max-width: 850px) { .profile-grid { grid-template-columns: 1fr; } }@media (max-width: 560px) { .profile-details { grid-template-columns: 1fr; }.profile-detail-wide { grid-column: span 1; }.security-row { flex-direction: column; }.avatar-dialog-content { align-items: flex-start; flex-direction: column; } }
</style>
