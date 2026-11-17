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
