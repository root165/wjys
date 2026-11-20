import { request } from './http.js'

export function listSpares() {
  return request({ url: '/spares' })
}

export function listSpareRecords(orderId) {
  return request({ url: `/work-orders/${orderId}/spares/records` })
}

export function consumeSpares(orderId, items) {
  return request({ url: `/work-orders/${orderId}/spares/consume`, method: 'POST', data: { items } })
}
