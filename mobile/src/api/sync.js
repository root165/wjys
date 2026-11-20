import { request, upload } from './http.js'
import { syncQueue } from '../repositories/syncQueue.js'

export function pull(cursor = null, modules = ['inspection', 'messages']) {
  return request({ url: '/mobile/sync/pull', data: { cursor, modules: modules.join(','), _ts: Date.now() } })
}

export function push(items) {
  return request({ url: '/mobile/sync/push', method: 'POST', data: { items } })
}

export async function syncPending({ uploadPhoto = upload, pushBatch = push } = {}) {
  return syncQueue.retryAll(async (record) => {
    const payload = JSON.parse(JSON.stringify(record.payload))
    const photoBindings = (record.photoBindings || []).map((item) => ({ ...item }))
    for (const binding of photoBindings) {
      if (!binding.taskItemId) throw Object.assign(new Error('照片缺少巡检项绑定'), { statusCode: 422 })
      if (!binding.fileId) {
        const uploadResponse = await uploadPhoto(binding.path)
        const fileId = uploadResponse.data?.fileId
        if (!fileId) throw Object.assign(new Error('照片上传未返回 fileId'), { statusCode: 422 })
        binding.fileId = fileId
        const item = payload.items?.find((entry) => String(entry.taskItemId) === String(binding.taskItemId))
        if (!item) throw Object.assign(new Error('照片关联的巡检项不存在'), { statusCode: 422 })
        item.fileIds = [...new Set([...(item.fileIds || []), fileId])]
        syncQueue.updatePayload(record.submitNo, payload, photoBindings)
      }
    }
    const response = await pushBatch([{ taskId: record.bizId, payload }])
    const result = response.data?.results?.[0]
    if (!result || result.status !== 'confirmed') {
      const error = new Error(result?.message || '同步未确认')
      error.statusCode = result?.status === 'conflict' ? 409 : 422
      throw error
    }
    return result
  })
}
