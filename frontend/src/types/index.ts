/**
 * 租户类型定义
 */
export interface Tenant {
  id: number
  name: string
  code: string
  status: string
  quotaConfig?: string
  createdAt: string
  updatedAt: string
}

/**
 * 用户类型定义
 */
export interface User {
  id: number
  tenantId: number
  username: string
  email?: string
  role: string
  status: string
  createdAt: string
  updatedAt: string
}

/**
 * 报表元数据类型定义
 */
export interface ReportMetadata {
  id: number
  tenantId: number
  name: string
  description?: string
  schemaVersion: number
  dataSourceId?: number
  templateConfig?: string
  createdBy?: number
  createdAt: string
  updatedAt: string
}

/**
 * 任务类型定义
 */
export interface Task {
  id: number
  tenantId: number
  type: string
  status: string
  priority: number
  config?: string
  progress: number
  errorMessage?: string
  createdBy?: number
  createdAt: string
  startedAt?: string
  completedAt?: string
}

/**
 * 分页结果类型定义
 */
export interface PageResult<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
}

/**
 * 统一API响应类型定义
 */
export interface Result<T = any> {
  code: number
  message: string
  data: T
}
