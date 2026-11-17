const AUTH_KEY = 'wjys:auth:v1'
const SYNC_INDEX_KEY = 'wjys:sync:v1:index'
const SYNC_PREFIX = 'wjys:sync:v1:'
const DRAFT_PREFIX = 'wjys:draft:v1:'
const PHOTO_PREFIX = 'wjys:photo:v1:'
const STORAGE_WARN_BYTES = 8 * 1024 * 1024

function read(key, fallback = null) {
  const value = uni.getStorageSync(key)
  return value === '' || value === undefined ? fallback : value
}

function estimateBytes(value) {
  return new Blob([JSON.stringify(value)]).size
}

function cleanupReloadableCache() {
  const info = uni.getStorageInfoSync()
  const removable = info.keys.filter((key) => key.startsWith('wjys:cache:v1:'))
  removable.forEach((key) => uni.removeStorageSync(key))
}

function write(key, value) {
  try {
    uni.setStorageSync(key, value)
  } catch (error) {
    cleanupReloadableCache()
    uni.setStorageSync(key, value)
  }
}

export const storageRepository = {
  keys: { AUTH_KEY, SYNC_INDEX_KEY, SYNC_PREFIX, DRAFT_PREFIX, PHOTO_PREFIX },
  get(key, fallback = null) { return read(key, fallback) },
  set(key, value) { write(key, value) },
  remove(key) { uni.removeStorageSync(key) },
  estimateBytes,
  isNearCapacity() {
    const info = uni.getStorageInfoSync()
    return Number(info.currentSize || 0) * 1024 + estimateBytes({}) >= STORAGE_WARN_BYTES
  },
  getAuth() { return read(AUTH_KEY, {}) },
  setAuth(value) { write(AUTH_KEY, value) },
  getSyncIndex() { return read(SYNC_INDEX_KEY, []) },
  setSyncIndex(value) { write(SYNC_INDEX_KEY, value) },
  syncKey(submitNo) { return `${SYNC_PREFIX}${submitNo}` },
  draftKey(taskId) { return `${DRAFT_PREFIX}${taskId}` },
  photoKey(taskItemId) { return `${PHOTO_PREFIX}${taskItemId}` },
  getSyncRecord(submitNo) { return read(`${SYNC_PREFIX}${submitNo}`) },
  setSyncRecord(record) { write(`${SYNC_PREFIX}${record.submitNo}`, record) },
  removeSyncRecord(submitNo) {
    const index = read(SYNC_INDEX_KEY, []).filter((item) => item !== submitNo)
    write(SYNC_INDEX_KEY, index)
    uni.removeStorageSync(`${SYNC_PREFIX}${submitNo}`)
  }
}
