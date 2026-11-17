import http from './http'

export const getEquipments = (params) => http.get('/equipment/equipments', { params })
export const changeEquipmentStatus = (id, payload) => http.put(`/equipment/equipments/${id}/status`, payload)
export const getCategories = () => http.get('/equipment/categories')
