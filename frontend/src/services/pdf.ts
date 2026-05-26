import request from './request'
import type { Result } from '../types'

export async function estimatePdf(reportId: number): Promise<{
  reportId: number
  maxPages: number
  supportedColorSpaces: string[]
}> {
  const res = await request.get<Result<any>>('/pdf/estimate', { params: { reportId } })
  return res.data.data
}

export async function submitPdf(data: {
  reportId: number
  colorSpace?: string
  watermarkEnabled?: boolean
}): Promise<{ taskId: number }> {
  const res = await request.post<Result<{ taskId: number }>>('/pdf/submit', data)
  return res.data.data
}

export async function getPdfTask(taskId: number): Promise<{
  taskId: number
  status: string
  progress: number
  segments?: any[]
  colorSpace?: string
  startedAt?: string
  completedAt?: string
}> {
  const res = await request.get<Result<any>>(`/pdf/${taskId}`)
  return res.data.data
}
