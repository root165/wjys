import { request } from './http.js'

export function login(username, password) {
  return request({ url: '/auth/login', method: 'POST', data: { username, password } })
}


export function logout() {
  return request({ url: '/auth/logout', method: 'POST' })
}

export function info() {
  return request({ url: '/auth/info' })
}

export function updateProfile(payload) {
  return request({ url: '/auth/profile', method: 'PUT', data: payload })
}

export function changePassword(payload) {
  return request({ url: '/auth/password', method: 'PUT', data: payload })
}
