<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const loading = ref(false)
const form = reactive({ username: 'admin', password: 'password' })

async function submit() {
  if (!form.username || !form.password) return ElMessage.warning('请输入用户名和密码')
  loading.value = true
  try {
    await auth.login(form)
    await router.replace(route.query.redirect || '/')
  } catch (error) {
    ElMessage.error(error.message || '登录失败，请检查服务是否启动')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <section class="login-art">
      <div><div class="brand-lockup"><div class="brand-mark">维</div><div><strong>维检云枢</strong><span>运维协同平台</span></div></div><h1>让每一次<br />维护都有迹可循。</h1><p>连接设备、人员与现场，让巡检、维修和保养从分散记录变成一条清晰、可追踪的工作流。</p></div>
      <div class="login-art-foot">WJYS / MAINTENANCE INTELLIGENCE</div>
    </section>
    <section class="login-card-wrap"><div class="login-card"><h2>欢迎回来</h2><p class="sub">登录管理端，查看今天的设备运行状态。</p><form @submit.prevent="submit"><label class="form-label" for="username">用户名</label><input id="username" v-model="form.username" class="form-input" autocomplete="username" /><label class="form-label" for="password">密码</label><input id="password" v-model="form.password" class="form-input" type="password" autocomplete="current-password" /><button class="primary-button" :disabled="loading" type="submit">{{ loading ? '正在登录…' : '进入工作台' }}</button></form><div class="login-tip">本地演示账号：<strong>admin</strong> / <strong>password</strong><br />后端服务需先完成数据库迁移并启动。</div></div></section>
  </div>
</template>
