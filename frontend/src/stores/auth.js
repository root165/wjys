import { defineStore } from 'pinia'
import * as authApi from '@/api/auth'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken: localStorage.getItem('wjys:accessToken') || '',
    refreshToken: localStorage.getItem('wjys:refreshToken') || '',
    user: JSON.parse(localStorage.getItem('wjys:user') || 'null'),
    menus: [],
  }),
  getters: { isAuthenticated: (state) => Boolean(state.accessToken) },
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
    },
    async logout() {
      try { if (this.accessToken) await authApi.logout() } finally { this.clear() }
    },
    clear() {
      this.accessToken = ''
      this.refreshToken = ''
      this.user = null
      this.menus = []
      localStorage.removeItem('wjys:accessToken')
      localStorage.removeItem('wjys:refreshToken')
      localStorage.removeItem('wjys:user')
    },
  },
})
