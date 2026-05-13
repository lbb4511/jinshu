import request from './request'

/**
 * 健康检查响应结构
 */
export interface HealthStatus {
  code: number
  message: string
  data: {
    status: string
    service: string
    database?: string | boolean
  }
}

/**
 * 检查后端健康状态
 *
 * @returns 健康检查结果
 */
export const checkHealth = async (): Promise<HealthStatus> => {
  const response = await request.get<HealthStatus>('/health')
  return response.data
}

/**
 * 检查后端就绪状态
 *
 * @returns 是否就绪
 */
export const checkReady = async (): Promise<boolean> => {
  try {
    await request.get('/ready')
    return true
  } catch {
    return false
  }
}

/**
 * 检查后端存活状态
 *
 * @returns 是否存活
 */
export const checkLive = async (): Promise<boolean> => {
  try {
    await request.get('/live')
    return true
  } catch {
    return false
  }
}
