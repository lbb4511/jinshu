import request from './request'
import type { Result, PageResult, Task } from '../types'

export interface TaskProgress {
  taskId: number
  status: string
  progress: number
  processedRows?: number
  totalRows?: number
  failedRows?: number
  message?: string
  totalShards?: number
  shards?: Array<{
    shardSeq: number
    processedRows: number
    failedRows: number
    status: string
  }>
  errorMessage?: string
  startedAt?: string
  completedAt?: string
}

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

export async function getImportTaskProgress(id: number): Promise<TaskProgress> {
  const res = await request.get<Result<TaskProgress>>(`/import-tasks/${id}/progress`)
  return res.data.data
}

export async function getExportTaskProgress(id: number): Promise<TaskProgress> {
  const res = await request.get<Result<TaskProgress>>(`/export/${id}`)
  return res.data.data
}

export async function getPdfTaskProgress(id: number): Promise<TaskProgress> {
  const res = await request.get<Result<TaskProgress>>(`/pdf/${id}`)
  return res.data.data
}

export async function getTaskProgress(type: string, id: number): Promise<TaskProgress> {
  switch (type) {
    case 'IMPORT':
      return getImportTaskProgress(id)
    case 'EXPORT':
      return getExportTaskProgress(id)
    case 'PDF':
      return getPdfTaskProgress(id)
    default:
      const res = await request.get<Result<TaskProgress>>(`/tasks/${id}`)
      return res.data.data
  }
}

export async function retryImportTask(id: number): Promise<number> {
  const res = await request.post<Result<{ retryTaskId: number }>>(`/import-tasks/${id}/retry`)
  return res.data.data.retryTaskId
}

export async function downloadTaskFile(id: number): Promise<{ blob: Blob; fileName?: string }> {
  const res = await request.get('/files/download', {
    params: { taskId: id },
    responseType: 'blob',
  })
  const disposition = res.headers['content-disposition'] as string | undefined
  let fileName: string | undefined
  if (disposition) {
    const match = disposition.match(/filename="([^"]+)"/)
    if (match) fileName = match[1]
  }
  return { blob: res.data as Blob, fileName }
}
