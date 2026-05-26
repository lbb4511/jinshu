import axios, { InternalAxiosRequestConfig, AxiosResponse } from 'axios'

/**
 * API响应数据结构
 */
interface ResponseData<T = any> {
  code: number
  message: string
  data: T
}

/**
 * Axios HTTP客户端实例
 *
 * 配置说明：
 * - baseURL: /api（通过Vite代理到后端）
 * - timeout: 300秒（支持大文件上传下载）
 *
 * 拦截器：
 * - 请求拦截：自动添加JWT Token
 * - 响应拦截：统一响应处理
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 300000
})

/**
 * 请求拦截器
 *
 * 自动从localStorage读取token并添加到请求头
 */
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

let redirecting = false

request.interceptors.response.use(
  (response: AxiosResponse<ResponseData>) => {
    return response
  },
  (error) => {
    if (error.response?.status === 401 && !redirecting) {
      redirecting = true
      localStorage.removeItem('jinshu_token')
      localStorage.removeItem('jinshu_refresh_token')
      localStorage.removeItem('jinshu_user')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default request
export type { ResponseData }
