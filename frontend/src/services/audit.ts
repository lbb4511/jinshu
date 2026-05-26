import request from './request'
import type { Result, PageResult, AuditLogEntry, AuditRootHash, IntegrityCheckResult } from '../types'

export async function listAuditLogs(params: {
  userId?: number
  operation?: string
  targetType?: string
  status?: string
  dateFrom?: string
  dateTo?: string
  page?: number
  pageSize?: number
}): Promise<PageResult<AuditLogEntry>> {
  const res = await request.get<Result<PageResult<AuditLogEntry>>>('/audit/logs', { params })
  return res.data.data
}

export async function exportAuditLogs(params: {
  userId?: number
  operation?: string
  targetType?: string
  status?: string
  dateFrom?: string
  dateTo?: string
}): Promise<Blob> {
  const res = await request.get('/audit/logs/export', {
    params,
    responseType: 'blob',
  })
  return res.data
}

export async function verifyIntegrity(hourStart: string): Promise<IntegrityCheckResult> {
  const res = await request.get<Result<IntegrityCheckResult>>('/audit/integrity', {
    params: { hourStart },
  })
  return res.data.data
}

export async function listRootHashes(params: {
  dateFrom?: string
  dateTo?: string
}): Promise<AuditRootHash[]> {
  const res = await request.get<Result<AuditRootHash[]>>('/audit/root-hash', { params })
  return res.data.data
}
