import http from './http'

export const login = (payload) => http.post('/auth/login', payload)
export const refresh = (refreshToken) => http.post('/auth/refresh', { refreshToken })
export const logout = () => http.post('/auth/logout')
export const getAuthInfo = () => http.get('/auth/info')
export const updateProfile = (payload) => http.put('/auth/profile', payload)
export const changePassword = (payload) => http.put('/auth/password', payload)
export const uploadAvatar = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return http.post('/files/upload', formData)
}
