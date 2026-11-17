import http from './http'

export const getPlans = () => http.get('/inspection/plans')
export const changePlanStatus = (id, status) => http.put(`/inspection/plans/${id}/status`, { status })
export const generateTask = (id, planTime) => http.post(`/inspection/plans/${id}/generate`, { planTime })
export const getTasks = (params) => http.get('/inspection/tasks', { params })
export const receiveTask = (id) => http.put(`/inspection/tasks/${id}/receive`)
export const getTask = (id) => http.get(`/inspection/tasks/${id}`)
