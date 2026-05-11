import axios, { AxiosRequestConfig, AxiosResponse } from 'axios'

interface ResponseData<T = any> {
  code: number
  message: string
  data: T
}

const request = axios.create({
  baseURL: '/api',
  timeout: 300000
})

request.interceptors.request.use(
  (config: AxiosRequestConfig) => {
    const token = localStorage.getItem('token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response: AxiosResponse<ResponseData>) => response.data,
  (error) => {
    console.error('Request Error:', error)
    return Promise.reject(error)
  }
)

export default request
export type { ResponseData }
