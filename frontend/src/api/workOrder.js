import http from './http'

export const getWorkOrders = (params) => http.get('/work-orders', { params })
export const getWorkOrder = (id) => http.get(`/work-orders/${id}`)
export const getWorkOrderLogs = (id) => http.get(`/work-orders/${id}/logs`)
export const getSpareRecords = (id) => http.get(`/work-orders/${id}/spares/records`)
export const dispatchWorkOrder = (id, payload) => http.put(`/work-orders/${id}/dispatch`, payload)
export const startWorkOrder = (id, payload = {}) => http.put(`/work-orders/${id}/start`, payload)
export const finishWorkOrder = (id, payload) => http.put(`/work-orders/${id}/finish`, payload)
export const acceptWorkOrder = (id, payload = {}) => http.put(`/work-orders/${id}/accept`, payload)
export const rejectWorkOrder = (id, payload) => http.put(`/work-orders/${id}/reject`, payload)
