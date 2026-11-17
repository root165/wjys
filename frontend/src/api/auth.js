import http from './http'

export const login = (payload) => http.post('/auth/login', payload)
export const refresh = (refreshToken) => http.post('/auth/refresh', { refreshToken })
export const logout = () => http.post('/auth/logout')
export const getAuthInfo = () => http.get('/auth/info')
