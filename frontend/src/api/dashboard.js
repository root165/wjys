import http from './http'

export const getOverview = (params) => http.get('/dashboard/overview', { params })
export const getFaultPareto = (params) => http.get('/dashboard/fault/pareto', { params })
export const getEquipmentRanking = (params) => http.get('/dashboard/equipment/ranking', { params })
export const getInspectionSummary = (params) => http.get('/dashboard/inspection/summary', { params })
export const getTrend = (params) => http.get('/dashboard/trend', { params })
