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
