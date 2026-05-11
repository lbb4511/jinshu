export interface Tenant {
  id: number
  name: string
  code: string
  status: string
  quotaConfig?: string
  createdAt: string
  updatedAt: string
}

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

export interface PageResult<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
}

export interface Result<T = any> {
  code: number
  message: string
  data: T
}
