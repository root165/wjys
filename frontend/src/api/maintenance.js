import http from './http'

export const getMaintenancePlans = (params) => http.get('/maintenance/plans', { params })
export const createMaintenancePlan = (payload) => http.post('/maintenance/plans', payload)
export const updateMaintenancePlanStatus = (id, status) => http.put(`/maintenance/plans/${id}/status`, { status })
export const getMaintenanceRecords = (params) => http.get('/maintenance/records', { params })
export const submitMaintenanceRecord = (payload) => http.post('/maintenance/records', payload)
export const auditMaintenanceRecord = (id, payload) => http.put(`/maintenance/records/${id}/audit`, payload)
export const createRepairFromMaintenance = (id) => http.post(`/maintenance/records/${id}/to-repair`)
export const uploadMaintenancePhoto = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return http.post('/files/upload', formData)
}
