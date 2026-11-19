<script setup>
import { computed, onMounted } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { BRAND_IMAGE_URL } from '@/constants/brand'
import {
  HomeFilled, User, Setting, Monitor, List, Calendar, Finished, WarningFilled,
  Box, Tickets, Tools, Message, UserFilled,
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const isLoginPage = computed(() => route.name === 'login')

async function logout() {
  await ElMessageBox.confirm('确认退出当前账号吗？', '退出登录', { type: 'warning' })
  await auth.logout()
  router.replace({ name: 'login' })
}

const navIcons = {
  home: HomeFilled,
  users: User,
  settings: Setting,
  equipment: Monitor,
  plans: Calendar,
  config: List,
  tasks: Finished,
  abnormal: WarningFilled,
  spares: Box,
  orders: Tickets,
  maintenance: Tools,
  messages: Message,
}

const navItems = [
  { to: '/', label: '工作台', icon: navIcons.home, permission: 'dashboard:view' },
  { to: '/system/users', label: '用户管理', icon: navIcons.users, permission: 'system:user:list' },
  { to: '/system/settings', label: '系统配置', icon: navIcons.settings, permission: 'system:config:view' },
  { to: '/equipment', label: '设备台账', icon: navIcons.equipment, permission: 'equipment:view' },
  { to: '/inspection/plans', label: '巡检计划', icon: navIcons.plans, permission: 'inspection:plan:view' },
  { to: '/inspection/config', label: '巡检配置', icon: navIcons.config, permission: 'inspection:config:view' },
  { to: '/inspection/tasks', label: '巡检任务', icon: navIcons.tasks, permission: 'inspection:task:view' },
  { to: '/inspection/abnormals', label: '异常管理', icon: navIcons.abnormal, permission: 'inspection:abnormal:view' },
  { to: '/spares', label: '备件台账', icon: navIcons.spares, permission: 'spare:view' },
  { to: '/work-orders', label: '维修工单', icon: navIcons.orders, permission: 'repair:view' },
  { to: '/maintenance', label: '维保管理', icon: navIcons.maintenance, permission: 'maintenance:view' },
  { to: '/messages', label: '消息中心', icon: navIcons.messages, permission: 'message:view' },
]
const visibleNavItems = computed(() => navItems.filter((item) => auth.hasPermission(item.permission)))

onMounted(() => {
  if (auth.accessToken && !auth.user?.avatarFileId) auth.loadInfo().catch(() => auth.clear())
  else if (auth.accessToken && auth.user?.avatarFileId) auth.loadAvatar().catch(() => { auth.avatarSrc = '' })
})
</script>

<template>
  <RouterView v-if="isLoginPage" />
  <div v-else class="app-shell">
    <aside class="sidebar">
      <div class="brand-lockup"><img class="brand-image" :src="BRAND_IMAGE_URL" alt="维检云枢" /><div><strong>维检云枢</strong><span>运维协同平台</span></div></div>
      <nav class="side-nav">
        <p class="nav-label">工作空间</p>
        <RouterLink v-for="item in visibleNavItems" :key="item.to" :to="item.to" class="nav-item"><el-icon class="nav-icon"><component :is="item.icon" /></el-icon><span>{{ item.label }}</span></RouterLink>
      </nav>
      <div class="sidebar-foot"><div class="status-dot"></div><span>系统运行正常</span></div>
    </aside>
    <section class="main-shell">
      <header class="topbar"><div><p class="eyebrow">OPERATIONS CONTROL CENTER</p><h1>{{ route.meta.title || '工作台' }}</h1></div><div class="user-area"><RouterLink to="/profile" class="user-profile-link">{{ auth.user?.nickname || '未登录' }}</RouterLink><button class="avatar-button" type="button" title="打开个人资料" @click="router.push({ name: 'profile' })"><img v-if="auth.avatarSrc" :src="auth.avatarSrc" alt="当前用户头像" /><el-icon v-else><UserFilled /></el-icon></button><button class="logout-link" type="button" @click="logout">退出</button></div></header>
      <main class="page-content"><RouterView /></main>
    </section>
  </div>
</template>
