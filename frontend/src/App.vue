<script setup>
import { computed } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const isLoginPage = computed(() => route.name === 'login')

async function logout() {
  await ElMessageBox.confirm('确认退出当前账号吗？', '退出登录', { type: 'warning' })
  await auth.logout()
  router.replace({ name: 'login' })
}
</script>

<template>
  <RouterView v-if="isLoginPage" />
  <div v-else class="app-shell">
    <aside class="sidebar">
      <div class="brand-lockup"><div class="brand-mark">维</div><div><strong>维检云枢</strong><span>运维协同平台</span></div></div>
      <nav class="side-nav">
        <p class="nav-label">工作空间</p>
        <RouterLink to="/" class="nav-item"><span class="nav-icon">⌂</span><span>工作台</span></RouterLink>
        <p class="nav-label">系统设置</p>
        <RouterLink to="/system/users" class="nav-item"><span class="nav-icon">◎</span><span>用户管理</span></RouterLink>
        <RouterLink to="/equipment" class="nav-item"><span class="nav-icon">◇</span><span>设备台账</span></RouterLink>
        <RouterLink to="/inspection/plans" class="nav-item"><span class="nav-icon">◌</span><span>巡检计划</span></RouterLink>
        <RouterLink to="/inspection/tasks" class="nav-item"><span class="nav-icon">◈</span><span>巡检任务</span></RouterLink>
        <RouterLink to="/spares" class="nav-item"><span class="nav-icon">▦</span><span>备件台账</span></RouterLink>
        <RouterLink to="/work-orders" class="nav-item"><span class="nav-icon">↺</span><span>维修工单</span></RouterLink>
        <RouterLink to="/maintenance" class="nav-item"><span class="nav-icon">◫</span><span>维保管理</span></RouterLink>
        <RouterLink to="/messages" class="nav-item"><span class="nav-icon">✉</span><span>消息中心</span></RouterLink>
      </nav>
      <div class="sidebar-foot"><div class="status-dot"></div><span>系统运行正常</span></div>
    </aside>
    <section class="main-shell">
      <header class="topbar"><div><p class="eyebrow">OPERATIONS CONTROL CENTER</p><h1>{{ route.meta.title || '工作台' }}</h1></div><div class="user-area"><span class="user-role">{{ auth.user?.nickname || '未登录' }}</span><button class="avatar-button" type="button" @click="logout">{{ (auth.user?.nickname || '管').slice(0, 1) }}</button></div></header>
      <main class="page-content"><RouterView /></main>
    </section>
  </div>
</template>
