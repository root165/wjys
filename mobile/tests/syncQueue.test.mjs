import assert from 'node:assert/strict'
import test from 'node:test'

const values = new Map()
globalThis.uni = {
  getStorageSync(key) { return values.has(key) ? values.get(key) : '' },
  setStorageSync(key, value) { values.set(key, value) },
  removeStorageSync(key) { values.delete(key) },
  getStorageInfoSync() { return { keys: [...values.keys()], currentSize: 1 } }
}

const { syncQueue } = await import('../src/repositories/syncQueue.js')

test('offline submit keeps one submitNo and confirms after retry', async () => {
  const record = syncQueue.enqueueInspectionSubmit({ taskId: 17, payload: { items: [{ taskItemId: 1, checkResult: 0 }] }, photoPaths: ['/private/photo.jpg'] })
  assert.ok(record.submitNo)
  assert.equal(syncQueue.pending().length, 1)
  const results = await syncQueue.retryAll(async (item) => ({ taskId: item.bizId, accepted: true }))
  assert.deepEqual(results, [{ submitNo: record.submitNo, status: 'confirmed' }])
  assert.equal(syncQueue.pending().length, 0)
  assert.equal(syncQueue.list()[0].photoPaths[0], '/private/photo.jpg')
})

test('409 conflict remains visible instead of being dropped', async () => {
  const record = syncQueue.enqueueInspectionSubmit({ taskId: 18, payload: { items: [] } })
  const results = await syncQueue.retryAll(async () => { throw Object.assign(new Error('幂等号冲突'), { statusCode: 409 }) })
  assert.deepEqual(results, [{ submitNo: record.submitNo, status: 'conflict' }])
  assert.equal(syncQueue.list().find((item) => item.submitNo === record.submitNo).status, 'conflict')
})

test('online recovery uploads bound photos before pushing inspection payload', async () => {
  const { syncPending } = await import('../src/api/sync.js')
  const record = syncQueue.enqueueInspectionSubmit({
    taskId: 19,
    payload: { items: [{ taskItemId: 21, checkResult: 0 }] },
    photoBindings: [{ taskItemId: 21, path: '/private/inspection.jpg' }],
  })
  let uploaded = 0
  let pushedPayload
  const results = await syncPending({
    uploadPhoto: async () => { uploaded += 1; return { data: { fileId: 71 } } },
    pushBatch: async (items) => { pushedPayload = items[0].payload; return { data: { results: [{ status: 'confirmed' }] } } },
  })
  assert.equal(uploaded, 1)
  assert.deepEqual(pushedPayload.items[0].fileIds, [71])
  assert.equal(results[0].status, 'confirmed')
  assert.equal(syncQueue.pending().length, 0)
  assert.equal(syncQueue.list().find((item) => item.submitNo === record.submitNo).photoBindings[0].fileId, 71)
})
