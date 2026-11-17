import http from './http'

export const getUsers = (params) => http.get('/system/users', { params })
export const createUser = (payload) => http.post('/system/users', payload)
export const changeUserStatus = (id, status) => http.put(`/system/users/${id}/status`, { status })
