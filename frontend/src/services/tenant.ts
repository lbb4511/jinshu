import request from './request'
import type { Result, PageResult, Tenant, TenantCreateRequest, TenantUpdateRequest } from '../types'

export async function listTenants(params: {
  name?: string
  status?: string
  page?: number
  pageSize?: number
}): Promise<PageResult<Tenant>> {
  const res = await request.get<Result<PageResult<Tenant>>>('/admin/tenants', { params })
  return res.data.data
}

export async function getTenant(id: number): Promise<Tenant> {
  const res = await request.get<Result<Tenant>>(`/admin/tenants/${id}`)
  return res.data.data
}

export async function createTenant(data: TenantCreateRequest): Promise<Tenant> {
  const res = await request.post<Result<Tenant>>('/admin/tenants', data)
  return res.data.data
}

export async function updateTenant(id: number, data: TenantUpdateRequest): Promise<Tenant> {
  const res = await request.put<Result<Tenant>>(`/admin/tenants/${id}`, data)
  return res.data.data
}

export async function changeTenantStatus(id: number, status: string): Promise<Tenant> {
  const res = await request.patch<Result<Tenant>>(`/admin/tenants/${id}/status`, null, { params: { status } })
  return res.data.data
}

export async function archiveTenant(id: number): Promise<void> {
  await request.delete(`/admin/tenants/${id}`)
}
