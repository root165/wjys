import assert from 'node:assert/strict'
import test from 'node:test'

const values = new Map()
globalThis.uni = {
  getStorageSync(key) { return values.has(key) ? values.get(key) : '' },
  setStorageSync(key, value) { values.set(key, value) },
  removeStorageSync(key) { values.delete(key) },
  getStorageInfoSync() { return { keys: [...values.keys()], currentSize: 1 } },
  saveFile({ tempFilePath, success }) { success({ savedFilePath: `/private/${tempFilePath.split('/').pop()}` }) },
  removeSavedFile({ filePath, complete }) { complete({ filePath }) },
}

const { photoRepository } = await import('../src/repositories/photoRepository.js')

test('photo repository moves selected image into private path and removes it after confirmation', async () => {
  const records = await photoRepository.saveForTaskItem(11, 22, ['/tmp/check.jpg'])
  assert.equal(records.length, 1)
  assert.equal(records[0].path, '/private/check.jpg')
  assert.equal(photoRepository.listForTaskItem(22).length, 1)

  await photoRepository.removeForTask(11)
  assert.equal(photoRepository.listForTask(11).length, 0)
})
