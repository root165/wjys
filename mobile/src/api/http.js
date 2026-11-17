import { storageRepository } from '../repositories/storageRepository'

const BASE_URL = import.meta.env?.VITE_API_BASE_URL || 'http://localhost:8080/api'

function headers(extra = {}) {
  const token = storageRepository.getAuth()?.accessToken
  return { ...(token ? { Authorization: `Bearer ${token}` } : {}), ...extra }
}

export function request({ url, method = 'GET', data, header = {} }) {
  return new Promise((resolve, reject) => {
    uni.request({ url: `${BASE_URL}${url}`, method, data, header: headers(header), success: (response) => {
      const statusCode = response.statusCode
      if (statusCode >= 200 && statusCode < 300) resolve(response.data)
      else reject(Object.assign(new Error(response.data?.message || '请求失败'), { statusCode }))
    }, fail: reject })
  })
}

export function upload(filePath) {
  return new Promise((resolve, reject) => {
    uni.uploadFile({ url: `${BASE_URL}/files/upload`, filePath, name: 'file', header: headers(), success: (response) => {
      if (response.statusCode >= 200 && response.statusCode < 300) resolve(JSON.parse(response.data))
      else reject(Object.assign(new Error('照片上传失败'), { statusCode: response.statusCode }))
    }, fail: reject })
  })
}
