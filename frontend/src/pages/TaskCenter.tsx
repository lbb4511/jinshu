import { useState, useEffect, useCallback, useRef } from 'react'
import { DataTable } from 'primereact/datatable'
import { Column } from 'primereact/column'
import { Tag } from 'primereact/tag'
import { Button } from 'primereact/button'
import { Dropdown } from 'primereact/dropdown'
import { Toolbar } from 'primereact/toolbar'
import { ProgressBar } from 'primereact/progressbar'
import { Dialog } from 'primereact/dialog'
import { Message } from 'primereact/message'
import { Toast } from 'primereact/toast'
import {
  listTasks,
  getTaskProgress,
  retryImportTask,
  downloadTaskFile,
  type TaskProgress,
} from '../services/task'
import { listReports } from '../services/report'
import type { Task } from '../types'
import './TaskCenter.scss'

const STATUS_OPTIONS = [
  { label: '全部', value: '' },
  { label: '等待中', value: 'PENDING' },
  { label: '处理中', value: 'PROCESSING' },
  { label: '已完成', value: 'SUCCESS' },
  { label: '已失败', value: 'FAILED' },
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

function resolveReportId(task: Task): number | undefined {
  if (task.reportId) return task.reportId
  const raw = task.parameters ?? task.config
  if (raw) {
    try {
      const parsed = JSON.parse(raw) as { reportId?: number }
      if (parsed.reportId) return parsed.reportId
    } catch {
      // ignore
    }
  }
  return undefined
}

export default function TaskCenter() {
  const [tasks, setTasks] = useState<Task[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(1)
  const [pageSize] = useState(20)
  const [statusFilter, setStatusFilter] = useState('')
  const [reportMap, setReportMap] = useState<Record<number, string>>({})
  const [dialogVisible, setDialogVisible] = useState(false)
  const [selectedTask, setSelectedTask] = useState<Task | null>(null)
  const [progress, setProgress] = useState<TaskProgress | null>(null)
  const [progressLoading, setProgressLoading] = useState(false)
  const [downloadingId, setDownloadingId] = useState<number | null>(null)
  const [retryingId, setRetryingId] = useState<number | null>(null)
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
    listReports({ page: 1, pageSize: 1000 })
      .then((result) => {
        const map: Record<number, string> = {}
        result.list.forEach((report) => {
          map[report.id] = report.name
        })
        setReportMap(map)
      })
      .catch(() => {
        // 报表名称加载失败不影响任务列表展示
      })
  }, [])

  useEffect(() => {
    const hasProcessing = tasks.some((t) => t.status === 'PROCESSING')
    if (!hasProcessing) return
    const interval = setInterval(load, 5000)
    return () => clearInterval(interval)
  }, [tasks, load])

  const fetchProgressFor = useCallback(async (task: Task) => {
    setProgressLoading(true)
    try {
      const p = await getTaskProgress(task.taskType, task.id)
      setProgress(p)
    } catch {
      toast.current?.show({ severity: 'error', summary: '加载失败', detail: '无法获取任务进度' })
    } finally {
      setProgressLoading(false)
    }
  }, [])

  useEffect(() => {
    if (!dialogVisible || !selectedTask) return
    void fetchProgressFor(selectedTask)
    if (selectedTask.status !== 'PROCESSING') return
    const interval = setInterval(() => {
      void fetchProgressFor(selectedTask)
    }, 5000)
    return () => clearInterval(interval)
  }, [dialogVisible, selectedTask, fetchProgressFor])

  const handleViewProgress = (task: Task) => {
    setSelectedTask(task)
    setProgress(null)
    setDialogVisible(true)
  }

  const handleRetry = async (task: Task) => {
    setRetryingId(task.id)
    try {
      const retryTaskId = await retryImportTask(task.id)
      toast.current?.show({
        severity: 'success',
        summary: '重试已提交',
        detail: `新任务 ID：${retryTaskId}`,
      })
      load()
    } catch {
      toast.current?.show({ severity: 'error', summary: '重试失败' })
    } finally {
      setRetryingId(null)
    }
  }

  const handleDownload = async (task: Task) => {
    setDownloadingId(task.id)
    try {
      const { blob, fileName } = await downloadTaskFile(task.id)
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = fileName || task.resultFileName || `task_${task.id}_result`
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(url)
      toast.current?.show({ severity: 'success', summary: '下载已开始' })
    } catch {
      toast.current?.show({ severity: 'error', summary: '下载失败', detail: '无法下载任务结果文件' })
    } finally {
      setDownloadingId(null)
    }
  }

  const typeBody = (row: Task) => (
    <Tag value={TYPE_LABEL[row.taskType] || row.taskType} severity={TYPE_SEVERITY[row.taskType] || 'info'} />
  )

  const statusBody = (row: Task) => (
    <Tag value={STATUS_LABEL[row.status] || row.status} severity={STATUS_SEVERITY[row.status] || 'info'} />
  )

  const reportNameBody = (row: Task) => {
    const reportId = resolveReportId(row)
    const name = reportId ? reportMap[reportId] : undefined
    return <span>{name || (reportId ? `报表 #${reportId}` : '-')}</span>
  }

  const progressBody = (row: Task) => (
    <ProgressBar value={row.progress} displayValueTemplate={() => `${row.progress}%`} style={{ height: '16px' }} />
  )

  const actionBody = (row: Task) => {
    const canRetry = row.taskType === 'IMPORT' && row.status === 'FAILED'
    const canDownload = row.status === 'SUCCESS' && (row.taskType === 'EXPORT' || row.taskType === 'PDF')
    return (
      <div className="task-actions">
        <Button
          icon="pi pi-chart-line"
          rounded
          text
          severity="info"
          tooltip="查看进度"
          onClick={() => handleViewProgress(row)}
        />
        {canRetry && (
          <Button
            icon="pi pi-replay"
            rounded
            text
            severity="warning"
            tooltip="重试"
            loading={retryingId === row.id}
            onClick={() => handleRetry(row)}
          />
        )}
        {canDownload && (
          <Button
            icon="pi pi-download"
            rounded
            text
            severity="success"
            tooltip="下载"
            loading={downloadingId === row.id}
            onClick={() => handleDownload(row)}
          />
        )}
      </div>
    )
  }

  const dateBody = (row: Task, field: keyof Task) => {
    const value = row[field]
    return value ? new Date(String(value)).toLocaleString('zh-CN') : '-'
  }

  const toolbarLeft = (
    <div className="task-toolbar">
      <Dropdown
        options={STATUS_OPTIONS}
        value={statusFilter}
        onChange={(e) => { setStatusFilter(e.value); setPage(1) }}
        placeholder="状态筛选"
        className="status-dropdown"
      />
      <Button icon="pi pi-refresh" rounded text onClick={load} />
    </div>
  )

  const currentProgress = progress?.progress ?? selectedTask?.progress ?? 0
  const currentStatus = progress?.status ?? selectedTask?.status ?? ''

  const dialogFooter = (
    <div>
      <Button label="关闭" icon="pi pi-times" text onClick={() => setDialogVisible(false)} />
    </div>
  )

  return (
    <div className="task-center-page">
      <Toast ref={toast} />

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
        onPage={(e) => setPage((e.first ?? 0) / (e.rows ?? pageSize) + 1)}
        emptyMessage="暂无任务"
        stripedRows
        size="small"
        sortField="createdAt"
        sortOrder={-1}
      >
        <Column field="id" header="ID" style={{ width: '80px' }} />
        <Column header="类型" body={typeBody} style={{ width: '100px' }} />
        <Column header="报表名" body={reportNameBody} style={{ minWidth: '160px' }} />
        <Column header="状态" body={statusBody} style={{ width: '100px' }} />
        <Column header="进度" body={progressBody} style={{ width: '160px' }} />
        <Column field="createdAt" header="创建时间" body={(row: Task) => dateBody(row, 'createdAt')} sortable style={{ width: '160px' }} />
        <Column field="startedAt" header="开始时间" body={(row: Task) => dateBody(row, 'startedAt')} style={{ width: '160px' }} />
        <Column field="errorMessage" header="错误信息" style={{ minWidth: '150px' }} />
        <Column header="操作" body={actionBody} style={{ width: '140px' }} />
      </DataTable>

      <Dialog
        header={selectedTask ? `任务进度 - #${selectedTask.id}` : '任务进度'}
        visible={dialogVisible}
        style={{ width: '560px' }}
        footer={dialogFooter}
        onHide={() => setDialogVisible(false)}
      >
        {selectedTask && (
          <div className="task-progress-dialog">
            <div className="task-progress-row">
              <span className="task-progress-label">任务类型</span>
              <Tag
                value={TYPE_LABEL[selectedTask.taskType] || selectedTask.taskType}
                severity={TYPE_SEVERITY[selectedTask.taskType] || 'info'}
              />
            </div>
            <div className="task-progress-row">
              <span className="task-progress-label">当前状态</span>
              <Tag
                value={STATUS_LABEL[currentStatus] || currentStatus}
                severity={STATUS_SEVERITY[currentStatus] || 'info'}
              />
            </div>
            <div className="task-progress-field">
              <label>总体进度</label>
              <ProgressBar
                value={currentProgress}
                displayValueTemplate={(value) => `${value}%`}
                style={{ height: '20px' }}
              />
            </div>
            {progress && (progress.processedRows !== undefined || progress.totalRows !== undefined) && (
              <div className="task-progress-row">
                <span className="task-progress-label">已处理 / 总行数</span>
                <span>{progress.processedRows ?? 0} / {progress.totalRows ?? '-'}</span>
              </div>
            )}
            {progress && progress.failedRows ? (
              <div className="task-progress-row">
                <span className="task-progress-label">失败行数</span>
                <span className="task-progress-error">{progress.failedRows}</span>
              </div>
            ) : null}
            {(selectedTask.errorMessage || progress?.errorMessage) && (
              <Message
                severity="error"
                text={progress?.errorMessage || selectedTask.errorMessage}
                className="w-full"
              />
            )}
            {selectedTask.taskType === 'IMPORT' && progress?.shards && progress.shards.length > 0 && (
              <div className="task-progress-shards">
                <label>分片进度</label>
                <DataTable value={progress.shards} size="small" stripedRows>
                  <Column field="shardSeq" header="分片" style={{ width: '70px' }} />
                  <Column field="processedRows" header="已处理" style={{ width: '90px' }} />
                  <Column field="failedRows" header="失败" style={{ width: '70px' }} />
                  <Column field="status" header="状态" style={{ width: '90px' }} />
                </DataTable>
              </div>
            )}
            {progressLoading && (
              <div className="task-progress-loading">加载进度中...</div>
            )}
          </div>
        )}
      </Dialog>
    </div>
  )
}
