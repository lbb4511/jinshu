import { useState, useEffect, useRef, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Card } from 'primereact/card'
import { Tag } from 'primereact/tag'
import { Button } from 'primereact/button'
import { ProgressBar } from 'primereact/progressbar'
import { Toast } from 'primereact/toast'
import { InputTextarea } from 'primereact/inputtextarea'
import { Dialog } from 'primereact/dialog'
import { Dropdown } from 'primereact/dropdown'
import {
  getReport, submitReport, approveReport, rejectReport, publishReport,
} from '../services/report'
import { submitExport, getExportTask } from '../services/export'
import { submitPdf, getPdfTask } from '../services/pdf'
import type { Report } from '../types'
import './ReportDetail.scss'

const STATUS_SEVERITY: Record<string, 'info' | 'warning' | 'success' | 'danger'> = {
  DRAFT: 'info',
  PENDING_REVIEW: 'warning',
  APPROVED: 'success',
  PUBLISHED: 'success',
  REJECTED: 'danger',
}

const STATUS_LABEL: Record<string, string> = {
  DRAFT: '草稿',
  PENDING_REVIEW: '待审批',
  APPROVED: '已批准',
  PUBLISHED: '已发布',
  REJECTED: '已拒绝',
}

export default function ReportDetail() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const toast = useRef<Toast>(null)

  const [report, setReport] = useState<Report | null>(null)
  const [loading, setLoading] = useState(true)
  const [actionLoading, setActionLoading] = useState(false)

  const [submitDialogVisible, setSubmitDialogVisible] = useState(false)
  const [reviewerId, setReviewerId] = useState<number | null>(null)
  const [comment, setComment] = useState('')
  const [rejectDialogVisible, setRejectDialogVisible] = useState(false)

  const [, setExportTaskId] = useState<number | null>(null)
  const [exportStatus, setExportStatus] = useState<string | null>(null)
  const [exportProgress, setExportProgress] = useState(0)
  const [exportLoading, setExportLoading] = useState(false)

  const [, setPdfTaskId] = useState<number | null>(null)
  const [pdfStatus, setPdfStatus] = useState<string | null>(null)
  const [pdfProgress, setPdfProgress] = useState(0)
  const [pdfLoading, setPdfLoading] = useState(false)

  const loadReport = useCallback(async () => {
    if (!id) return
    setLoading(true)
    try {
      const r = await getReport(Number(id))
      setReport(r)
    } catch {
      toast.current?.show({ severity: 'error', summary: '加载失败', detail: '无法获取报表信息' })
    } finally {
      setLoading(false)
    }
  }, [id])

  useEffect(() => { loadReport() }, [loadReport])

  const handleSubmit = async () => {
    if (!report || !reviewerId) return
    setActionLoading(true)
    try {
      const r = await submitReport(report.id, reviewerId)
      setReport(r)
      setSubmitDialogVisible(false)
      setReviewerId(null)
      toast.current?.show({ severity: 'success', summary: '已提交审批' })
    } catch {
      toast.current?.show({ severity: 'error', summary: '提交失败' })
    } finally {
      setActionLoading(false)
    }
  }

  const handleApprove = async () => {
    if (!report) return
    setActionLoading(true)
    try {
      const r = await approveReport(report.id, comment || undefined)
      setReport(r)
      setComment('')
      toast.current?.show({ severity: 'success', summary: '已审批通过' })
    } catch {
      toast.current?.show({ severity: 'error', summary: '审批失败' })
    } finally {
      setActionLoading(false)
    }
  }

  const handleReject = async () => {
    if (!report) return
    setActionLoading(true)
    try {
      const r = await rejectReport(report.id, comment || undefined)
      setReport(r)
      setRejectDialogVisible(false)
      setComment('')
      toast.current?.show({ severity: 'info', summary: '已拒绝' })
    } catch {
      toast.current?.show({ severity: 'error', summary: '操作失败' })
    } finally {
      setActionLoading(false)
    }
  }

  const handlePublish = async () => {
    if (!report) return
    setActionLoading(true)
    try {
      const r = await publishReport(report.id)
      setReport(r)
      toast.current?.show({ severity: 'success', summary: '已发布' })
    } catch {
      toast.current?.show({ severity: 'error', summary: '发布失败' })
    } finally {
      setActionLoading(false)
    }
  }

  const handleExport = async (format: string) => {
    if (!report) return
    setExportLoading(true)
    setExportStatus('PENDING')
    setExportProgress(0)
    try {
      const { taskId } = await submitExport({ reportId: report.id, format })
      setExportTaskId(taskId)
      pollExportTask(taskId)
    } catch {
      toast.current?.show({ severity: 'error', summary: '导出任务提交失败' })
      setExportLoading(false)
      setExportStatus(null)
    }
  }

  const pollExportTask = (taskId: number) => {
    const poll = async () => {
      try {
        const task = await getExportTask(taskId)
        setExportStatus(task.status)
        setExportProgress(task.progress)
        if (task.status === 'SUCCESS' || task.status === 'FAILED') {
          setExportLoading(false)
          if (task.status === 'SUCCESS') {
            toast.current?.show({ severity: 'success', summary: '导出完成' })
          }
          return
        }
        setTimeout(() => poll(), 3000)
      } catch {
        setExportLoading(false)
      }
    }
    poll()
  }

  const handleGeneratePdf = async () => {
    if (!report) return
    setPdfLoading(true)
    setPdfStatus('PENDING')
    setPdfProgress(0)
    try {
      const { taskId } = await submitPdf({ reportId: report.id, watermarkEnabled: false })
      setPdfTaskId(taskId)
      pollPdfTask(taskId)
    } catch {
      toast.current?.show({ severity: 'error', summary: 'PDF 任务提交失败' })
      setPdfLoading(false)
      setPdfStatus(null)
    }
  }

  const pollPdfTask = (taskId: number) => {
    const poll = async () => {
      try {
        const task = await getPdfTask(taskId)
        setPdfStatus(task.status)
        setPdfProgress(task.progress)
        if (task.status === 'SUCCESS' || task.status === 'FAILED') {
          setPdfLoading(false)
          if (task.status === 'SUCCESS') {
            toast.current?.show({ severity: 'success', summary: 'PDF 生成完成' })
          }
          return
        }
        setTimeout(() => poll(), 3000)
      } catch {
        setPdfLoading(false)
      }
    }
    poll()
  }

  if (loading) {
    return <div className="report-detail-loading">加载中...</div>
  }

  if (!report) {
    return <div className="report-detail-error">报表不存在</div>
  }

  const isDraft = report.status === 'DRAFT'
  const isPendingReview = report.status === 'PENDING_REVIEW'
  const isApproved = report.status === 'APPROVED'
  const isPublished = report.status === 'PUBLISHED'
  const isRejected = report.status === 'REJECTED'

  return (
    <div className="report-detail-page">
      <Toast ref={toast} />

      <div className="detail-header">
        <div className="detail-header-left">
          <Button icon="pi pi-arrow-left" text rounded onClick={() => navigate('/reports')} tooltip="返回" />
          <h2 className="detail-title">{report.name}</h2>
          <Tag value={STATUS_LABEL[report.status] || report.status} severity={STATUS_SEVERITY[report.status] || 'info'} />
        </div>
        <div className="detail-header-actions">
          {isDraft && (
            <Button label="提交审批" icon="pi pi-send" onClick={() => setSubmitDialogVisible(true)} />
          )}
          {isPendingReview && (
            <>
              <Button label="通过" icon="pi pi-check" severity="success" className="p-mr-2" onClick={handleApprove} loading={actionLoading} />
              <Button label="拒绝" icon="pi pi-times" severity="danger" onClick={() => setRejectDialogVisible(true)} />
            </>
          )}
          {isApproved && (
            <Button label="发布" icon="pi pi-upload" severity="success" onClick={handlePublish} loading={actionLoading} />
          )}
          {isPublished && (
            <>
              <Button label="HTML 预览" icon="pi pi-eye" severity="info" className="p-mr-2"
                onClick={() => navigate(`/reports/${report.id}/preview`)} />
              <Button label="导出 Excel" icon="pi pi-file-excel" severity="info" className="p-mr-2"
                onClick={() => handleExport('EXCEL')} loading={exportLoading} disabled={exportLoading} />
              <Button label="导出 CSV" icon="pi pi-file" severity="info" className="p-mr-2"
                onClick={() => handleExport('CSV')} loading={exportLoading} disabled={exportLoading} />
              <Button label="生成 PDF" icon="pi pi-file-pdf" severity="danger"
                onClick={handleGeneratePdf} loading={pdfLoading} disabled={pdfLoading} />
            </>
          )}
          {isRejected && (
            <Button label="修改后重新提交" icon="pi pi-pencil" severity="warning"
              onClick={() => setSubmitDialogVisible(true)} />
          )}
        </div>
      </div>

      <div className="detail-grid">
        <Card title="基本信息" className="detail-info-card">
          <div className="info-row">
            <span className="info-label">报表 ID</span>
            <span className="info-value">{report.id}</span>
          </div>
          <div className="info-row">
            <span className="info-label">描述</span>
            <span className="info-value">{report.description || '-'}</span>
          </div>
          <div className="info-row">
            <span className="info-label">版本号</span>
            <span className="info-value">v{report.schemaVersion}</span>
          </div>
          <div className="info-row">
            <span className="info-label">数据源 ID</span>
            <span className="info-value">{report.dataSourceId || '-'}</span>
          </div>
          <div className="info-row">
            <span className="info-label">创建时间</span>
            <span className="info-value">{new Date(report.createdAt).toLocaleString('zh-CN')}</span>
          </div>
          <div className="info-row">
            <span className="info-label">更新时间</span>
            <span className="info-value">{new Date(report.updatedAt).toLocaleString('zh-CN')}</span>
          </div>
        </Card>

        <Card title="数据导出" className="detail-export-card">
          {isPublished ? (
            <div className="export-actions">
              <p className="export-hint">报表已发布，可执行以下操作：</p>
              <div className="export-buttons">
                <Button label="导出 Excel" icon="pi pi-file-excel" severity="info"
                  onClick={() => handleExport('EXCEL')} loading={exportLoading} disabled={exportLoading} />
                <Button label="导出 CSV" icon="pi pi-file" severity="info"
                  onClick={() => handleExport('CSV')} loading={exportLoading} disabled={exportLoading} />
                <Button label="生成 PDF" icon="pi pi-file-pdf" severity="danger"
                  onClick={handleGeneratePdf} loading={pdfLoading} disabled={pdfLoading} />
              </div>

              {exportStatus && (
                <div className="task-progress">
                  <h4>导出任务</h4>
                  <div className="task-status-row">
                    <Tag value={exportStatus} severity={exportStatus === 'SUCCESS' ? 'success' : exportStatus === 'FAILED' ? 'danger' : 'warning'} />
                    <ProgressBar value={exportProgress} displayValueTemplate={() => `${exportProgress}%`} style={{ height: '16px', flex: 1 }} />
                  </div>
                </div>
              )}

              {pdfStatus && (
                <div className="task-progress">
                  <h4>PDF 任务</h4>
                  <div className="task-status-row">
                    <Tag value={pdfStatus} severity={pdfStatus === 'SUCCESS' ? 'success' : pdfStatus === 'FAILED' ? 'danger' : 'warning'} />
                    <ProgressBar value={pdfProgress} displayValueTemplate={() => `${pdfProgress}%`} style={{ height: '16px', flex: 1 }} />
                  </div>
                </div>
              )}
            </div>
          ) : (
            <p className="export-hint">报表发布后方可导出数据和生成 PDF。</p>
          )}
        </Card>
      </div>

      <Dialog header="提交审批" visible={submitDialogVisible} style={{ width: '400px' }}
        onHide={() => setSubmitDialogVisible(false)}
        footer={
          <div>
            <Button label="取消" icon="pi pi-times" text onClick={() => setSubmitDialogVisible(false)} />
            <Button label="提交" icon="pi pi-send" onClick={handleSubmit} loading={actionLoading} disabled={!reviewerId} />
          </div>
        }
      >
        <div className="dialog-form">
          <div className="field">
            <label>审批人</label>
            <Dropdown
              options={[
                { label: '管理员', value: 1 },
              ]}
              value={reviewerId}
              onChange={e => setReviewerId(e.value)}
              placeholder="选择审批人"
              style={{ width: '100%' }}
            />
          </div>
        </div>
      </Dialog>

      <Dialog header="拒绝原因" visible={rejectDialogVisible} style={{ width: '400px' }}
        onHide={() => setRejectDialogVisible(false)}
        footer={
          <div>
            <Button label="取消" icon="pi pi-times" text onClick={() => { setRejectDialogVisible(false); setComment('') }} />
            <Button label="确认拒绝" icon="pi pi-times" severity="danger" onClick={handleReject} loading={actionLoading} />
          </div>
        }
      >
        <div className="dialog-form">
          <div className="field">
            <label>拒绝原因</label>
            <InputTextarea value={comment} onChange={e => setComment(e.target.value)} rows={4}
              placeholder="请输入拒绝原因" style={{ width: '100%' }} />
          </div>
        </div>
      </Dialog>
    </div>
  )
}
