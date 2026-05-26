import { useState, useEffect, useCallback, useRef } from 'react'
import { DataTable } from 'primereact/datatable'
import { Column } from 'primereact/column'
import { Tag } from 'primereact/tag'
import { Button } from 'primereact/button'
import { Dropdown } from 'primereact/dropdown'
import { Toolbar } from 'primereact/toolbar'
import { ProgressBar } from 'primereact/progressbar'
import { Toast } from 'primereact/toast'
import { ConfirmDialog, confirmDialog } from 'primereact/confirmdialog'
import { listTasks, cancelTask } from '../services/task'
import type { Task } from '../types'
import './TaskCenter.scss'

const STATUS_OPTIONS = [
  { label: '全部', value: '' },
  { label: '等待中', value: 'PENDING' },
  { label: '处理中', value: 'PROCESSING' },
  { label: '已完成', value: 'SUCCESS' },
  { label: '已失败', value: 'FAILED' },
  { label: '已取消', value: 'CANCELLED' },
]

const TYPE_LABEL: Record<string, string> = {
  IMPORT: '导入',
  EXPORT: '导出',
  PDF: 'PDF渲染',
}

const TYPE_SEVERITY: Record<string, 'info' | 'success' | 'warning'> = {
  IMPORT: 'info',
  EXPORT: 'success',
  PDF: 'warning',
}

const STATUS_SEVERITY: Record<string, 'info' | 'success' | 'danger' | 'warning'> = {
  PENDING: 'info',
  PROCESSING: 'warning',
  SUCCESS: 'success',
  FAILED: 'danger',
  CANCELLED: 'info',
}

const STATUS_LABEL: Record<string, string> = {
  PENDING: '等待中',
  PROCESSING: '处理中',
  SUCCESS: '已完成',
  FAILED: '已失败',
  CANCELLED: '已取消',
}

export default function TaskCenter() {
  const [tasks, setTasks] = useState<Task[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(1)
  const [pageSize] = useState(20)
  const [statusFilter, setStatusFilter] = useState('')
  const toast = useRef<Toast>(null)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const result = await listTasks({
        status: statusFilter || undefined,
        page,
        pageSize,
      })
      setTasks(result.list)
      setTotal(result.total)
    } catch {
      toast.current?.show({ severity: 'error', summary: '加载失败', detail: '无法获取任务列表' })
    } finally {
      setLoading(false)
    }
  }, [statusFilter, page, pageSize])

  useEffect(() => { load() }, [load])

  useEffect(() => {
    const interval = setInterval(load, 15000)
    return () => clearInterval(interval)
  }, [load])

  const handleCancel = (task: Task) => {
    confirmDialog({
      message: `确定取消该任务？`,
      header: '确认取消',
      icon: 'pi pi-exclamation-triangle',
      accept: async () => {
        try {
          await cancelTask(task.id)
          toast.current?.show({ severity: 'success', summary: '已取消' })
          load()
        } catch {
          toast.current?.show({ severity: 'error', summary: '取消失败' })
        }
      },
    })
  }

  const typeBody = (row: Task) => (
    <Tag value={TYPE_LABEL[row.taskType] || row.taskType} severity={TYPE_SEVERITY[row.taskType] || 'info'} />
  )

  const statusBody = (row: Task) => (
    <Tag value={STATUS_LABEL[row.status] || row.status} severity={STATUS_SEVERITY[row.status] || 'info'} />
  )

  const progressBody = (row: Task) => (
    <ProgressBar value={row.progress} displayValueTemplate={() => `${row.progress}%`}
      style={{ height: '16px' }} />
  )

  const actionBody = (row: Task) => {
    const cancellable = row.status === 'PENDING' || row.status === 'PROCESSING'
    return (
      <div className="task-actions">
        {cancellable && (
          <Button icon="pi pi-times" rounded text severity="danger" tooltip="取消"
            onClick={() => handleCancel(row)} />
        )}
      </div>
    )
  }

  const dateBody = (row: Task) => row.createdAt ? new Date(row.createdAt).toLocaleString('zh-CN') : ''

  const toolbarLeft = (
    <div className="task-toolbar">
      <Dropdown options={STATUS_OPTIONS} value={statusFilter} onChange={e => { setStatusFilter(e.value); setPage(1) }}
        placeholder="状态筛选" className="status-dropdown" />
      <Button icon="pi pi-refresh" rounded text onClick={load} />
    </div>
  )

  return (
    <div className="task-center-page">
      <Toast ref={toast} />
      <ConfirmDialog />

      <h2 className="page-title">任务中心</h2>

      <Toolbar left={toolbarLeft} />

      <DataTable
        value={tasks}
        lazy
        loading={loading}
        totalRecords={total}
        paginator
        rows={pageSize}
        first={(page - 1) * pageSize}
        onPage={e => setPage((e.first ?? 0) / (e.rows ?? pageSize) + 1)}
        emptyMessage="暂无任务"
        stripedRows
        size="small"
        sortField="createdAt"
        sortOrder={-1}
      >
        <Column field="id" header="ID" style={{ width: '80px' }} />
        <Column header="类型" body={typeBody} style={{ width: '100px' }} />
        <Column header="状态" body={statusBody} style={{ width: '100px' }} />
        <Column header="进度" body={progressBody} style={{ width: '160px' }} />
        <Column field="createdAt" header="创建时间" body={dateBody} sortable style={{ width: '160px' }} />
        <Column field="startedAt" header="开始时间" body={(r: Task) => r.startedAt ? new Date(r.startedAt).toLocaleString('zh-CN') : '-'} style={{ width: '160px' }} />
        <Column field="errorMessage" header="错误信息" style={{ minWidth: '150px' }} />
        <Column header="操作" body={actionBody} style={{ width: '100px' }} />
      </DataTable>
    </div>
  )
}
