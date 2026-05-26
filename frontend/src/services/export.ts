import request from './request'
import type { Result } from '../types'

export async function estimateExport(params: {
  reportId: number
  dateFrom?: string
  dateTo?: string
}): Promise<{
  reportId: number
  totalRows: number
  estimatedFileSize: string
  estimatedTime: string
  suggestFormat: string
}> {
  const res = await request.get<Result<any>>('/export/estimate', { params })
  return res.data.data
}

export async function submitExport(data: {
  reportId: number
  format: string
  filters?: Record<string, any>
}): Promise<{ taskId: number }> {
  const res = await request.post<Result<{ taskId: number }>>('/export', data)
  return res.data.data
}

export async function getExportTask(taskId: number): Promise<{
  taskId: number
  status: string
  progress: number
  startedAt?: string
  completedAt?: string
}> {
  const res = await request.get<Result<any>>(`/export/${taskId}`)
  return res.data.data
}
