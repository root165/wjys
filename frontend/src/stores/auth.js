import { defineStore } from 'pinia'
import * as authApi from '@/api/auth'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken: localStorage.getItem('wjys:accessToken') || '',
    refreshToken: localStorage.getItem('wjys:refreshToken') || '',
    user: JSON.parse(localStorage.getItem('wjys:user') || 'null'),
    menus: [],
    avatarSrc: '',
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.accessToken),
    hasRole: (state) => (roleCode) => state.user?.roles?.includes(roleCode) || false,
    hasPermission: (state) => (permissionCode) => state.user?.roles?.includes('system_admin') || state.user?.permissions?.includes(permissionCode) || false,
  },
  actions: {
    persist(pair) {
      this.accessToken = pair.accessToken
      this.refreshToken = pair.refreshToken
      localStorage.setItem('wjys:accessToken', pair.accessToken)
      localStorage.setItem('wjys:refreshToken', pair.refreshToken)
    },
    async login(payload) {
      const response = await authApi.login(payload)
      this.persist(response.data)
      await this.loadInfo()
    },
    async loadInfo() {
      const response = await authApi.getAuthInfo()
      this.user = response.data.user
      this.menus = response.data.menus
      localStorage.setItem('wjys:user', JSON.stringify(this.user))
      await this.loadAvatar()
    },
    async loadAvatar() {
      if (!this.user?.avatarFileId || !this.accessToken) { this.avatarSrc = ''; return }
      const response = await fetch('/api/auth/avatar', { headers: { Authorization: `Bearer ${this.accessToken}` } })
      if (!response.ok) { this.avatarSrc = ''; return }
      if (this.avatarSrc) URL.revokeObjectURL(this.avatarSrc)
      this.avatarSrc = URL.createObjectURL(await response.blob())
    },
    async logout() {
      try { if (this.accessToken) await authApi.logout() } finally { this.clear() }
    },
    clear() {
      this.accessToken = ''
      this.refreshToken = ''
      this.user = null
      this.menus = []
      if (this.avatarSrc) URL.revokeObjectURL(this.avatarSrc)
      this.avatarSrc = ''
      localStorage.removeItem('wjys:accessToken')
      localStorage.removeItem('wjys:refreshToken')
      localStorage.removeItem('wjys:user')
    },
  },
})
