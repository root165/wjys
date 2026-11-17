import { request } from './http.js'
import { syncQueue } from '../repositories/syncQueue.js'

export function pull(cursor = null, modules = ['inspection', 'messages']) {
  return request({ url: '/mobile/sync/pull', data: { cursor, modules: modules.join(',') } })
}

export function push(items) {
  return request({ url: '/mobile/sync/push', method: 'POST', data: { items } })
}

export async function syncPending() {
  return syncQueue.retryAll(async (record) => {
    const response = await push([{ taskId: record.bizId, payload: record.payload }])
    const result = response.data?.results?.[0]
    if (!result || result.status !== 'confirmed') {
      const error = new Error(result?.message || '同步未确认')
      error.statusCode = result?.status === 'conflict' ? 409 : 422
      throw error
    }
    return result
  })
}
