import { storageRepository } from '../repositories/storageRepository.js'

const BASE_URL = import.meta.env?.VITE_API_BASE_URL || 'http://localhost:8080/api'
let refreshPromise = null

function headers(extra = {}) {
  const token = storageRepository.getAuth()?.accessToken
  return { ...(token ? { Authorization: `Bearer ${token}` } : {}), ...extra }
}

function requestOnce({ url, method = 'GET', data, header = {} }) {
  return new Promise((resolve, reject) => {
    uni.request({ url: `${BASE_URL}${url}`, method, data, header: headers(header), success: (response) => {
      const statusCode = response.statusCode
      if (statusCode >= 200 && statusCode < 300) resolve(response.data)
      else reject(Object.assign(new Error(response.data?.message || '请求失败'), { statusCode }))
    }, fail: reject })
  })
}

async function refreshAccessToken() {
  const auth = storageRepository.getAuth()
  if (!auth?.refreshToken) throw new Error('登录已失效，请重新登录')
  if (!refreshPromise) {
    refreshPromise = requestOnce({ url: '/auth/refresh', method: 'POST', data: { refreshToken: auth.refreshToken } })
      .then((response) => {
        const nextAuth = response.data
        storageRepository.setAuth(nextAuth)
        return nextAuth
      })
      .finally(() => { refreshPromise = null })
  }
  return refreshPromise
}

function expireSession() {
  storageRepository.clearAuth()
  if (typeof uni !== 'undefined' && typeof uni.reLaunch === 'function') {
    uni.reLaunch({ url: '/pages/login/login' })
  }
}

export async function request(options, retried = false) {
  try {
    return await requestOnce(options)
  } catch (error) {
    if (error.statusCode === 401 && !retried && options.url !== '/auth/login' && options.url !== '/auth/refresh') {
      try { await refreshAccessToken() } catch (refreshError) { expireSession(); throw refreshError }
      return request(options, true)
    }
    throw error
  }
}

function uploadOnce(filePath) {
  return new Promise((resolve, reject) => {
    uni.uploadFile({ url: `${BASE_URL}/files/upload`, filePath, name: 'file', header: headers(), success: (response) => {
      if (response.statusCode >= 200 && response.statusCode < 300) resolve(JSON.parse(response.data))
      else reject(Object.assign(new Error('照片上传失败'), { statusCode: response.statusCode }))
    }, fail: reject })
  })
}

export async function upload(filePath, retried = false) {
  try {
    return await uploadOnce(filePath)
  } catch (error) {
    if (error.statusCode === 401 && !retried) {
      try { await refreshAccessToken() } catch (refreshError) { expireSession(); throw refreshError }
      return upload(filePath, true)
    }
    throw error
  }
}

export function apiBaseUrl() { return BASE_URL }
