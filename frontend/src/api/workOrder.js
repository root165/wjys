import http from './http'

export const getWorkOrders = (params) => http.get('/work-orders', { params })
export const getWorkOrder = (id) => http.get(`/work-orders/${id}`)
export const getWorkOrderLogs = (id) => http.get(`/work-orders/${id}/logs`)
export const getSpareRecords = (id) => http.get(`/work-orders/${id}/spares/records`)
