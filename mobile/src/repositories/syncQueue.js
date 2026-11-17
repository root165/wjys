import { storageRepository } from './storageRepository'

function uuid() {
  return `${Date.now()}-${Math.random().toString(16).slice(2)}-${Math.random().toString(16).slice(2)}`
}

function requestHash(payload) {
  return JSON.stringify(payload)
}

export const syncQueue = {
  enqueueInspectionSubmit({ taskId, payload, photoPaths = [] }) {
    const record = {
      submitNo: payload.submitNo || uuid(),
      bizType: 'inspection_submit',
      bizId: taskId,
      requestHash: requestHash(payload),
      createdTime: new Date().toISOString(),
      retryCount: 0,
      status: 'pending',
      payload: { ...payload, submitNo: payload.submitNo || undefined },
      photoPaths
    }
    record.payload.submitNo = record.submitNo
    storageRepository.setSyncRecord(record)
    const index = storageRepository.getSyncIndex()
    if (!index.includes(record.submitNo)) storageRepository.setSyncIndex([...index, record.submitNo])
    return record
  },
  list() {
    return storageRepository.getSyncIndex().map((submitNo) => storageRepository.getSyncRecord(submitNo)).filter(Boolean)
  },
  pending() {
    return this.list().filter((record) => record.status === 'pending' || record.status === 'retry')
  },
  markConfirmed(submitNo, response) {
    const record = storageRepository.getSyncRecord(submitNo)
    if (!record) return
    storageRepository.setSyncRecord({ ...record, status: 'confirmed', confirmedTime: new Date().toISOString(), response })
  },
  markConflict(submitNo, message) {
    const record = storageRepository.getSyncRecord(submitNo)
    if (!record) return
    storageRepository.setSyncRecord({ ...record, status: 'conflict', lastError: message })
  },
  async retryAll(push) {
    const records = this.pending().sort((a, b) => a.createdTime.localeCompare(b.createdTime))
    const results = []
    for (const record of records) {
      try {
        const response = await push(record)
        this.markConfirmed(record.submitNo, response)
        results.push({ submitNo: record.submitNo, status: 'confirmed' })
      } catch (error) {
        const conflict = error?.statusCode === 409 || error?.status === 409
        const current = storageRepository.getSyncRecord(record.submitNo)
        storageRepository.setSyncRecord({ ...current, status: conflict ? 'conflict' : 'retry', retryCount: current.retryCount + 1, lastError: error?.message || '网络暂不可用' })
        results.push({ submitNo: record.submitNo, status: conflict ? 'conflict' : 'retry' })
      }
    }
    return results
  }
}
