import http from './http'

export const getEquipments = (params) => http.get('/equipment/equipments', { params })
export const createEquipment = (payload) => http.post('/equipment/equipments', payload)
export const getEquipment = (id) => http.get(`/equipment/equipments/${id}`)
export const updateEquipment = (id, payload) => http.put(`/equipment/equipments/${id}`, payload)
export const deleteEquipment = (id) => http.delete(`/equipment/equipments/${id}`)
export const changeEquipmentStatus = (id, payload) => http.put(`/equipment/equipments/${id}/status`, payload)
export const updateEquipmentParam = (equipmentId, paramId, payload) => http.put(`/equipment/equipments/${equipmentId}/params/${paramId}`, payload)
export const getEquipmentQr = (id) => http.get(`/equipment/equipments/${id}/qrcode`, { responseType: 'blob' })
export const getCategories = () => http.get('/equipment/categories')
export const createCategory = (payload) => http.post('/equipment/categories', payload)
export const updateCategory = (id, payload) => http.put(`/equipment/categories/${id}`, payload)
export const deleteCategory = (id) => http.delete(`/equipment/categories/${id}`)
