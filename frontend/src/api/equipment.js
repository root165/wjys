import http from './http'

export const getEquipments = (params) => http.get('/equipment/equipments', { params })
export const createEquipment = (payload) => http.post('/equipment/equipments', payload)
export const changeEquipmentStatus = (id, payload) => http.put(`/equipment/equipments/${id}/status`, payload)
export const getCategories = () => http.get('/equipment/categories')
export const createCategory = (payload) => http.post('/equipment/categories', payload)
