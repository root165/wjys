import { request } from './http.js'

export function list(readFlag) {
  return request({ url: '/messages', data: readFlag === undefined ? {} : { readFlag } })
}

export function markRead(id) {
  return request({ url: `/mobile/messages/${id}/read`, method: 'PUT' })
}
