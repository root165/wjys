import http from './http'

export const getSpares = (params) => http.get('/spares', { params })
export const getSpareAlerts = () => http.get('/spares/alerts')
export const createSpare = (payload) => http.post('/spares', payload)
export const adjustSpareStock = (id, payload) => http.put(`/spares/${id}/stock`, payload)
export const consumeSpares = (orderId, items) => http.post(`/work-orders/${orderId}/spares/consume`, { items })
export const reverseSpareRecord = (recordId, remark) => http.post(`/work-orders/spare-records/${recordId}/reverse`, { remark })
