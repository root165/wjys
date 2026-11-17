import http from './http'

export const getMessages = (params) => http.get('/messages', { params })
export const getUnreadCount = () => http.get('/messages/unread-count')
export const markMessageRead = (id) => http.put(`/messages/${id}/read`)
