import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import SystemUsersView from '../views/SystemUsersView.vue'
import EquipmentView from '../views/EquipmentView.vue'
import InspectionPlansView from '../views/InspectionPlansView.vue'
import InspectionTasksView from '../views/InspectionTasksView.vue'
import SpareView from '../views/SpareView.vue'
import WorkOrdersView from '../views/WorkOrdersView.vue'
import MaintenanceView from '../views/MaintenanceView.vue'
import MessageCenterView from '../views/MessageCenterView.vue'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      meta: { title: '工作台' },
      component: HomeView,
    },
    { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
    { path: '/system/users', name: 'system-users', component: SystemUsersView, meta: { title: '用户管理' } },
    { path: '/equipment', name: 'equipment', component: EquipmentView, meta: { title: '设备台账' } },
    { path: '/inspection/plans', name: 'inspection-plans', component: InspectionPlansView, meta: { title: '巡检计划' } },
    { path: '/inspection/tasks', name: 'inspection-tasks', component: InspectionTasksView, meta: { title: '巡检任务' } },
    { path: '/spares', name: 'spares', component: SpareView, meta: { title: '备件台账' } },
    { path: '/work-orders', name: 'work-orders', component: WorkOrdersView, meta: { title: '维修工单' } },
    { path: '/maintenance', name: 'maintenance', component: MaintenanceView, meta: { title: '维保管理' } },
    { path: '/messages', name: 'messages', component: MessageCenterView, meta: { title: '消息中心' } },
    {
      path: '/about',
      name: 'about',
      // route level code-splitting
      // this generates a separate chunk (About.[hash].js) for this route
      // which is lazy-loaded when the route is visited.
      component: () => import('../views/AboutView.vue'),
    },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.public) return true
  if (!auth.isAuthenticated) return { name: 'login', query: { redirect: to.fullPath } }
  return true
})

export default router
