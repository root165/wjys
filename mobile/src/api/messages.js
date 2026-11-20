import { request } from './http.js'

export function list(readFlag) {
  return request({ url: '/messages', data: { ...(readFlag === undefined ? {} : { readFlag }), _ts: Date.now() } })
}

export function markRead(id) {
  return request({ url: `/messages/${id}/read`, method: 'PUT' })
}
