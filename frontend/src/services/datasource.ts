import request from './request'
import type { Result, PageResult, DataSource, DataSourceCreateRequest, DataSourceUpdateRequest } from '../types'

export async function listDataSources(params: {
  name?: string
  type?: string
  page?: number
  pageSize?: number
}): Promise<PageResult<DataSource>> {
  const res = await request.get<Result<PageResult<DataSource>>>('/datasources', { params })
  return res.data.data
}

export async function getDataSource(id: number): Promise<DataSource> {
  const res = await request.get<Result<DataSource>>(`/datasources/${id}`)
  return res.data.data
}

export async function createDataSource(data: DataSourceCreateRequest): Promise<DataSource> {
  const res = await request.post<Result<DataSource>>('/datasources', data)
  return res.data.data
}

export async function updateDataSource(id: number, data: DataSourceUpdateRequest): Promise<DataSource> {
  const res = await request.put<Result<DataSource>>(`/datasources/${id}`, data)
  return res.data.data
}

export async function deleteDataSource(id: number): Promise<void> {
  await request.delete(`/datasources/${id}`)
}

export async function testConnection(id: number): Promise<{ success: boolean; message: string }> {
  const res = await request.post<Result<{ success: boolean; message: string }>>(`/datasources/${id}/test`)
  return res.data.data
}
