import request from './request'
import type { Result, PageResult, ReportTemplate, ReportTemplateCreateRequest, ReportTemplateApplyRequest } from '../types'

export async function listTemplates(params: {
  category?: string
  keyword?: string
  includePublic?: boolean
  page?: number
  pageSize?: number
}): Promise<PageResult<ReportTemplate>> {
  const res = await request.get<Result<PageResult<ReportTemplate>>>('/templates', { params })
  return res.data.data
}

export async function createTemplate(data: ReportTemplateCreateRequest): Promise<ReportTemplate> {
  const res = await request.post<Result<ReportTemplate>>('/templates', data)
  return res.data.data
}

export async function applyTemplate(id: number, data: ReportTemplateApplyRequest): Promise<number> {
  const res = await request.post<Result<number>>(`/templates/${id}/apply`, data)
  return res.data.data
}

export async function deleteTemplate(id: number): Promise<void> {
  await request.delete(`/templates/${id}`)
}
