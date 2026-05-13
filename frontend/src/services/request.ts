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

/**
 * 响应拦截器
 *
 * 统一处理响应数据
 * TODO: 可扩展添加错误处理、token过期跳转登录等
 */
request.interceptors.response.use(
  (response: AxiosResponse<ResponseData>) => {
    return response
  },
  (error) => {
    console.error('Request Error:', error)
    return Promise.reject(error)
  }
)

export default request
export type { ResponseData }
