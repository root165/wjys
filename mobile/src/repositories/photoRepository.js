import { storageRepository } from './storageRepository.js'

function uuid() {
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function saveFile(tempFilePath) {
  return new Promise((resolve, reject) => {
    uni.saveFile({ tempFilePath, success: resolve, fail: reject })
  })
}

function removeFile(filePath) {
  return new Promise((resolve) => {
    uni.removeSavedFile({ filePath, complete: resolve })
  })
}

export const photoRepository = {
  async saveForTaskItem(taskId, taskItemId, tempFilePaths) {
    const saved = []
    for (const tempFilePath of tempFilePaths) {
      const result = await saveFile(tempFilePath)
      const record = {
        id: uuid(), taskId, taskItemId, path: result.savedFilePath,
        createdTime: new Date().toISOString(), uploaded: false,
      }
      storageRepository.set(`wjys:photo:v1:${record.id}`, record)
      saved.push(record)
    }
    storageRepository.setPhotoIndex([...new Set([...storageRepository.getPhotoIndex(), ...saved.map((item) => item.id)])])
    return saved
  },
  listForTaskItem(taskItemId) {
    return storageRepository.getPhotoIndex()
      .map((id) => storageRepository.get(`wjys:photo:v1:${id}`))
      .filter((item) => item && String(item.taskItemId) === String(taskItemId))
  },
  listForTask(taskId) {
    return storageRepository.getPhotoIndex()
      .map((id) => storageRepository.get(`wjys:photo:v1:${id}`))
      .filter((item) => item && String(item.taskId) === String(taskId))
  },
  async remove(id) {
    const record = storageRepository.get(`wjys:photo:v1:${id}`)
    if (!record) return
    await removeFile(record.path)
    storageRepository.remove(`wjys:photo:v1:${id}`)
    storageRepository.setPhotoIndex(storageRepository.getPhotoIndex().filter((item) => item !== id))
  },
  async removeForTask(taskId) {
    for (const record of this.listForTask(taskId)) await this.remove(record.id)
  },
}
