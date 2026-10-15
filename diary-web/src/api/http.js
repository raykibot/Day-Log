import axios from 'axios'

export const http = axios.create({
  baseURL: '/api/v1',
  timeout: 10000,
})

http.interceptors.response.use((response) => response.data?.data)

export function apiErrorMessage(error) {
  if (error.response?.data?.message) return error.response.data.message
  if (error.code === 'ECONNABORTED') return '请求超时，请稍后重试。'
  if (!error.response) return '无法连接后端，请确认 Spring Boot 服务正在运行。'
  return `请求失败（HTTP ${error.response.status}），请稍后重试。`
}
