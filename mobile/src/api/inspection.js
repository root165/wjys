import { request } from './http.js'

export function receiveTask(id) {
  return request({ url: `/mobile/tasks/${id}/receive`, method: 'PUT' })
}
