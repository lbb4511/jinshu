import { useState, useEffect, useCallback } from 'react'
import { DataTable } from 'primereact/datatable'
import { Column } from 'primereact/column'
import { Tag } from 'primereact/tag'
import { Button } from 'primereact/button'
import { InputText } from 'primereact/inputtext'
import { Dropdown } from 'primereact/dropdown'
import { Toolbar } from 'primereact/toolbar'
import { ConfirmDialog, confirmDialog } from 'primereact/confirmdialog'
import { Toast } from 'primereact/toast'
import { useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { listReports, deleteReport } from '../services/report'
import type { Report } from '../types'
import './ReportList.scss'

const STATUS_OPTIONS = [
  { label: '全部', value: '' },
  { label: '草稿', value: 'DRAFT' },
  { label: '待审批', value: 'PENDING_REVIEW' },
  { label: '已批准', value: 'APPROVED' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '已拒绝', value: 'REJECTED' },
]

export default function ReportList() {
  const [reports, setReports] = useState<Report[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(1)
  const [pageSize] = useState(20)
  const [nameFilter, setNameFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const toast = useRef<Toast>(null)
  const navigate = useNavigate()

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const result = await listReports({
        name: nameFilter || undefined,
        status: statusFilter || undefined,
        page,
        pageSize,
      })
      setReports(result.list)
      setTotal(result.total)
    } catch {
      toast.current?.show({ severity: 'error', summary: '加载失败', detail: '无法获取报表列表' })
    } finally {
      setLoading(false)
    }
  }, [nameFilter, statusFilter, page, pageSize])

  useEffect(() => { load() }, [load])

  const handleDelete = (report: Report) => {
    confirmDialog({
      message: `确定删除报表「${report.name}」？`,
      header: '确认删除',
      icon: 'pi pi-exclamation-triangle',
      accept: async () => {
        try {
          await deleteReport(report.id)
          toast.current?.show({ severity: 'success', summary: '已删除', detail: `报表「${report.name}」已删除` })
          load()
        } catch {
          toast.current?.show({ severity: 'error', summary: '删除失败', detail: '无法删除该报表' })
        }
      },
    })
  }

  const statusBody = (row: Report) => {
    const severityMap: Record<string, 'success' | 'info' | 'warning' | 'danger'> = {
      DRAFT: 'info',
      PENDING_REVIEW: 'warning',
      APPROVED: 'success',
      PUBLISHED: 'success',
      REJECTED: 'danger',
    }
    const labelMap: Record<string, string> = {
      DRAFT: '草稿',
      PENDING_REVIEW: '待审批',
      APPROVED: '已批准',
      PUBLISHED: '已发布',
      REJECTED: '已拒绝',
    }
    return <Tag value={labelMap[row.status] || row.status} severity={severityMap[row.status] || 'info'} />
  }

  const actionBody = (row: Report) => (
    <div className="report-actions">
      <Button icon="pi pi-eye" rounded text severity="info" tooltip="查看"
        onClick={() => navigate(`/reports/${row.id}`)} />
      <Button icon="pi pi-pencil" rounded text severity="warning" tooltip="编辑"
        onClick={() => navigate(`/reports/${row.id}/edit`)} />
      <Button icon="pi pi-trash" rounded text severity="danger" tooltip="删除"
        onClick={() => handleDelete(row)} />
    </div>
  )

  const dateBody = (row: Report) => row.createdAt ? new Date(row.createdAt).toLocaleString('zh-CN') : ''

  const toolbarLeft = (
    <div className="report-toolbar">
      <InputText placeholder="搜索报表名称..." value={nameFilter}
        onChange={e => { setNameFilter(e.target.value); setPage(1) }} />
      <Dropdown options={STATUS_OPTIONS} value={statusFilter} onChange={e => { setStatusFilter(e.value); setPage(1) }}
        placeholder="状态筛选" className="status-dropdown" />
      <Button icon="pi pi-refresh" rounded text onClick={load} />
    </div>
  )

  const toolbarRight = (
    <Button label="新建报表" icon="pi pi-plus" onClick={() => navigate('/reports/new')} />
  )

  return (
    <div className="report-list-page">
      <Toast ref={toast} />
      <ConfirmDialog />

      <h2 className="page-title">报表列表</h2>

      <Toolbar left={toolbarLeft} right={toolbarRight} />

      <DataTable
        value={reports}
        lazy
        loading={loading}
        totalRecords={total}
        paginator
        rows={pageSize}
        first={(page - 1) * pageSize}
        onPage={e => setPage((e.first ?? 0) / (e.rows ?? pageSize) + 1)}
        emptyMessage="暂无报表"
        stripedRows
        size="small"
        sortField="createdAt"
        sortOrder={-1}
      >
        <Column field="name" header="名称" sortable style={{ minWidth: '180px' }} />
        <Column field="description" header="描述" style={{ minWidth: '200px' }} />
        <Column field="status" header="状态" body={statusBody} style={{ width: '100px' }} />
        <Column field="schemaVersion" header="版本" style={{ width: '70px' }} />
        <Column field="createdAt" header="创建时间" body={dateBody} sortable style={{ width: '160px' }} />
        <Column header="操作" body={actionBody} style={{ width: '120px' }} />
      </DataTable>
    </div>
  )
}
