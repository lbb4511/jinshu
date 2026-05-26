import request from './request'
import type { Result, PageResult, Task } from '../types'

export async function listTasks(params: {
  status?: string
  page?: number
  pageSize?: number
}): Promise<PageResult<Task>> {
  const res = await request.get<Result<PageResult<Task>>>('/tasks', { params })
  return res.data.data
}

export async function getTask(id: number): Promise<Task> {
  const res = await request.get<Result<Task>>(`/tasks/${id}`)
  return res.data.data
}

export async function cancelTask(id: number): Promise<Task> {
  const res = await request.post<Result<Task>>(`/tasks/${id}/cancel`)
  return res.data.data
}
