export interface Tenant {
  id: number
  name: string
  code: string
  status: string
  quotaConfig?: string
  description?: string
  createdAt: string
  updatedAt: string
}

export interface TenantCreateRequest {
  name: string
  code: string
  description?: string
  adminUsername?: string
  adminPassword?: string
  adminEmail?: string
  quotaConfig?: Record<string, unknown>
}

export interface TenantUpdateRequest {
  name?: string
  description?: string
  quotaConfig?: Record<string, unknown>
}

export interface User {
  id: number
  tenantId: number
  username: string
  displayName?: string
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
  parameters?: string
  progress: number
  errorMessage?: string
  reportId?: number
  resultFileName?: string
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

export interface ReportCreateRequest {
  name: string
  description?: string
  dataSourceId?: number
  templateConfig?: string
}

export interface ReportUpdateRequest {
  name?: string
  description?: string
  dataSourceId?: number
  templateConfig?: string
}

export interface ReportTemplate {
  id: number
  tenantId: number
  name: string
  description?: string
  category: string
  thumbnailUrl?: string
  layoutJson?: string
  sampleData?: string
  isPublic?: boolean
  isSystem?: boolean
  status: string
  createdBy?: number
  createdAt: string
  updatedAt: string
}

export interface ReportTemplateCreateRequest {
  name: string
  category: string
  description?: string
  thumbnailUrl?: string
  layoutJson?: string
  sampleData?: string
  isPublic?: boolean
}

export interface ReportTemplateApplyRequest {
  name: string
  description?: string
  dataSourceId?: number
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
