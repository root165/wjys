import axios from 'axios'

const http = axios.create({ baseURL: '/api', timeout: 10000 })
let refreshPromise = null

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('wjys:accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (response) => response.data,
  async (error) => {
    const original = error.config
    const status = error.response?.status
    const isAuthRequest = original?.url?.endsWith('/auth/login') || original?.url?.endsWith('/auth/refresh')
    if (status === 401 && original && !original._retry && !isAuthRequest) {
      const refreshToken = localStorage.getItem('wjys:refreshToken')
      if (refreshToken) {
        original._retry = true
        if (!refreshPromise) {
          refreshPromise = axios.post('/api/auth/refresh', { refreshToken })
            .then((response) => {
              const pair = response.data?.data
              if (!pair?.accessToken || !pair?.refreshToken) throw new Error('刷新令牌响应无效')
              localStorage.setItem('wjys:accessToken', pair.accessToken)
              localStorage.setItem('wjys:refreshToken', pair.refreshToken)
              return pair
            })
            .finally(() => { refreshPromise = null })
        }
        try {
          const pair = await refreshPromise
          original.headers = original.headers || {}
          original.headers.Authorization = `Bearer ${pair.accessToken}`
          return http(original)
        } catch (refreshError) {
          localStorage.removeItem('wjys:accessToken')
          localStorage.removeItem('wjys:refreshToken')
          localStorage.removeItem('wjys:user')
          if (window.location.pathname !== '/login') window.location.href = '/login'
          return Promise.reject(refreshError.response?.data || { message: '登录已失效，请重新登录' })
        }
      }
    }
    return Promise.reject(error.response?.data || { message: '网络连接失败' })
  },
)

export default http
