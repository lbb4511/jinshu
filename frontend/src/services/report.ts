import request from './request'
import type { Result, PageResult, Report, ReportCreateRequest, ReportUpdateRequest } from '../types'

export async function listReports(params: {
  name?: string
  status?: string
  page?: number
  pageSize?: number
}): Promise<PageResult<Report>> {
  const res = await request.get<Result<PageResult<Report>>>('/reports', { params })
  return res.data.data
}

export async function getReport(id: number): Promise<Report> {
  const res = await request.get<Result<Report>>(`/reports/${id}`)
  return res.data.data
}

export async function deleteReport(id: number): Promise<void> {
  await request.delete(`/reports/${id}`)
}

export async function createReport(data: ReportCreateRequest): Promise<Report> {
  const res = await request.post<Result<Report>>('/reports', data)
  return res.data.data
}

export async function updateReport(id: number, data: ReportUpdateRequest): Promise<Report> {
  const res = await request.put<Result<Report>>(`/reports/${id}`, data)
  return res.data.data
}

export async function submitReport(id: number, reviewerId: number): Promise<Report> {
  const res = await request.post<Result<Report>>(`/reports/${id}/submit`, { reviewerId })
  return res.data.data
}

export async function approveReport(id: number, comment?: string): Promise<Report> {
  const res = await request.post<Result<Report>>(`/reports/${id}/approve`, { comment })
  return res.data.data
}

export async function rejectReport(id: number, comment?: string): Promise<Report> {
  const res = await request.post<Result<Report>>(`/reports/${id}/reject`, { comment })
  return res.data.data
}

export async function publishReport(id: number): Promise<Report> {
  const res = await request.post<Result<Report>>(`/reports/${id}/publish`)
  return res.data.data
}

export async function previewReport(id: number): Promise<string> {
  const res = await request.get<string>(`/reports/${id}/preview`, {
    responseType: 'text',
    headers: { Accept: 'text/html' },
  })
  return res.data
}
