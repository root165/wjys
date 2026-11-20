import { request } from './http.js'

export function listWorkOrders(status) {
  return request({ url: '/work-orders', data: status === undefined || status === null ? {} : { status } })
}

export function getWorkOrder(id) {
  return request({ url: `/work-orders/${id}` })
}

export function getWorkOrderLogs(id) {
  return request({ url: `/work-orders/${id}/logs` })
}

export function startWorkOrder(id) {
  return request({ url: `/work-orders/${id}/start`, method: 'PUT', data: {} })
}

export function finishWorkOrder(id, payload) {
  return request({ url: `/work-orders/${id}/finish`, method: 'PUT', data: payload })
}

export function acceptWorkOrder(id, remark = '') {
  return request({ url: `/work-orders/${id}/accept`, method: 'PUT', data: { remark } })
}

export function rejectWorkOrder(id, reason) {
  return request({ url: `/work-orders/${id}/reject`, method: 'PUT', data: { reason } })
}
