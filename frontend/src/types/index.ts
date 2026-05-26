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

export interface Report {
  id: number
  tenantId: number
  name: string
  description?: string
  schemaVersion: number
  dataSourceId?: number
  templateConfig?: string
  status: string
  createdBy?: number
  updatedBy?: number
  createdAt: string
  updatedAt: string
  deletedAt?: string
  isDeleted?: boolean
}

export interface Task {
  id: number
  tenantId: number
  taskType: string
  status: string
  priority: number
  config?: string
  progress: number
  errorMessage?: string
  reportId?: number
  parentTaskId?: number
  shardSeq?: number
  shardTotal?: number
  createdBy?: number
  createdAt: string
  startedAt?: string
  completedAt?: string
  cancelledAt?: string
}

export interface DataSource {
  id: number
  tenantId: number
  name: string
  type: string
  host: string
  port: number
  databaseName: string
  username: string
  connectionConfig?: string
  status: string
  lastTestTime?: string
  lastTestResult?: string
  description?: string
  createdBy?: number
  updatedBy?: number
  createdAt: string
  updatedAt: string
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

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  refreshToken: string
  expiresIn: number
}

export interface AuthState {
  user: User | null
  token: string | null
  loading: boolean
  isAuthenticated: boolean
}

export interface DataSourceCreateRequest {
  name: string
  type: string
  host: string
  port: number
  databaseName: string
  username: string
  password?: string
  sslEnabled?: boolean
  description?: string
}

export interface DataSourceUpdateRequest {
  name?: string
  type?: string
  host?: string
  port?: number
  databaseName?: string
  username?: string
  password?: string
  description?: string
}

export interface AuditLogEntry {
  id: number
  tenantId: number
  userId: number
  username: string
  operation: string
  targetType?: string
  targetId?: number
  targetName?: string
  ipAddress?: string
  userAgent?: string
  requestParams?: string
  status: string
  errorMessage?: string
  logHash?: string
  previousHash?: string
  createdAt: string
}

export interface AuditRootHash {
  id: number
  tenantId: number
  hourStart: string
  rootHash: string
  logCount: number
  createdAt: string
}

export interface IntegrityCheckResult {
  tenantId: number
  hourStart: string
  hourEnd: string
  logCount: number
  consistent: boolean
  message: string
}
