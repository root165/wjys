import http from './http'

export const getUsers = (params) => http.get('/system/users', { params })
export const createUser = (payload) => http.post('/system/users', payload)
export const changeUserStatus = (id, status) => http.put(`/system/users/${id}/status`, { status })
export const getRoles = () => http.get('/system/roles')
export const createRole = (payload) => http.post('/system/roles', payload)
export const getDepts = () => http.get('/system/depts')
export const createDept = (payload) => http.post('/system/depts', payload)
export const getDicts = (dictType) => http.get(`/system/dicts/${encodeURIComponent(dictType)}`)
export const createDict = (payload) => http.post('/system/dicts', payload)
export const getOperLogs = (params) => http.get('/system/logs', { params })
