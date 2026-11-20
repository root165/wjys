const AUTH_KEY = 'wjys:auth:v1'
const USER_KEY = 'wjys:user:v1'
const SYNC_INDEX_KEY = 'wjys:sync:v1:index'
const SYNC_PREFIX = 'wjys:sync:v1:'
const DRAFT_PREFIX = 'wjys:draft:v1:'
const PHOTO_PREFIX = 'wjys:photo:v1:'
const SYNC_CURSOR_KEY = 'wjys:sync:v1:cursor'
const TASK_PREFIX = 'wjys:task:v1:'
const PHOTO_INDEX_KEY = 'wjys:photo:v1:index'
const MESSAGE_CACHE_KEY = 'wjys:message:v1:list'
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
  keys: { AUTH_KEY, USER_KEY, SYNC_INDEX_KEY, SYNC_PREFIX, DRAFT_PREFIX, PHOTO_PREFIX, SYNC_CURSOR_KEY, TASK_PREFIX, PHOTO_INDEX_KEY, MESSAGE_CACHE_KEY },
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
  getUser() { return read(USER_KEY, null) },
  setUser(value) { write(USER_KEY, value) },
  clearAuth() { uni.removeStorageSync(AUTH_KEY); uni.removeStorageSync(USER_KEY) },
  getSyncIndex() { return read(SYNC_INDEX_KEY, []) },
  setSyncIndex(value) { write(SYNC_INDEX_KEY, value) },
  getSyncCursor() { return read(SYNC_CURSOR_KEY, null) },
  setSyncCursor(value) { write(SYNC_CURSOR_KEY, value) },
  getDraft(taskId) { return read(`${DRAFT_PREFIX}${taskId}`, {}) },
  setDraft(taskId, value) { write(`${DRAFT_PREFIX}${taskId}`, value) },
  removeDraft(taskId) { uni.removeStorageSync(`${DRAFT_PREFIX}${taskId}`) },
  setTask(task) { write(`${TASK_PREFIX}${task.id}`, task) },
  getTask(taskId) { return read(`${TASK_PREFIX}${taskId}`, null) },
  getTasks() {
    return uni.getStorageInfoSync().keys
      .filter((key) => key.startsWith(TASK_PREFIX))
      .map((key) => read(key, null))
      .filter(Boolean)
      .sort((a, b) => String(a.planTime).localeCompare(String(b.planTime)) || Number(a.id) - Number(b.id))
  },
  getMessages() { return read(MESSAGE_CACHE_KEY, []) },
  setMessages(messages) { write(MESSAGE_CACHE_KEY, messages) },
  mergeMessages(messages) {
    const merged = new Map(this.getMessages().map((item) => [String(item.id), item]))
    messages.forEach((item) => merged.set(String(item.id), item))
    const sorted = [...merged.values()].sort((a, b) => String(b.createdTime).localeCompare(String(a.createdTime)) || Number(b.id) - Number(a.id))
    this.setMessages(sorted)
    return sorted
  },
  setPhotoIndex(value) { write(PHOTO_INDEX_KEY, value) },
  getPhotoIndex() { return read(PHOTO_INDEX_KEY, []) },
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
