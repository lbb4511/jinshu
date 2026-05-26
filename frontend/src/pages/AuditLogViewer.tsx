import { useState, useEffect, useCallback, useRef } from 'react'
import { DataTable } from 'primereact/datatable'
import { Column } from 'primereact/column'
import { Tag } from 'primereact/tag'
import { Button } from 'primereact/button'
import { InputText } from 'primereact/inputtext'
import { Dropdown } from 'primereact/dropdown'
import { Toolbar } from 'primereact/toolbar'
import { Toast } from 'primereact/toast'
import { Calendar } from 'primereact/calendar'
import { Dialog } from 'primereact/dialog'
import { Divider } from 'primereact/divider'
import { listAuditLogs, exportAuditLogs, verifyIntegrity, listRootHashes } from '../services/audit'
import type { AuditLogEntry, AuditRootHash, IntegrityCheckResult } from '../types'
import './AuditLogViewer.scss'

const OPERATION_OPTIONS = [
  { label: '全部操作', value: '' },
  { label: '登录', value: 'LOGIN' },
  { label: '登录失败', value: 'LOGIN_FAILED' },
  { label: '登出', value: 'LOGOUT' },
  { label: '创建报表', value: 'CREATE_REPORT' },
  { label: '编辑报表', value: 'UPDATE_REPORT' },
  { label: '删除报表', value: 'DELETE_REPORT' },
  { label: '提交审查', value: 'SUBMIT_REVIEW' },
  { label: '审查通过', value: 'APPROVE_REPORT' },
  { label: '审查驳回', value: 'REJECT_REPORT' },
  { label: '发布报表', value: 'PUBLISH_REPORT' },
  { label: '导入报表', value: 'IMPORT_REPORT' },
  { label: '导出报表', value: 'EXPORT_REPORT' },
  { label: '生成PDF', value: 'PDF_GENERATE' },
  { label: '创建用户', value: 'CREATE_USER' },
  { label: '编辑用户', value: 'UPDATE_USER' },
  { label: '禁用用户', value: 'DISABLE_USER' },
  { label: '创建数据源', value: 'CREATE_DATASOURCE' },
  { label: '编辑数据源', value: 'UPDATE_DATASOURCE' },
  { label: '取消任务', value: 'CANCEL_TASK' },
  { label: '编辑租户', value: 'UPDATE_TENANT' },
]

const OPERATION_LABEL: Record<string, string> = {
  LOGIN: '登录', LOGIN_FAILED: '登录失败', LOGOUT: '登出',
  CREATE_REPORT: '创建报表', UPDATE_REPORT: '编辑报表', DELETE_REPORT: '删除报表',
  SUBMIT_REVIEW: '提交审查', APPROVE_REPORT: '审查通过', REJECT_REPORT: '审查驳回',
  PUBLISH_REPORT: '发布报表', IMPORT_REPORT: '导入报表', EXPORT_REPORT: '导出报表',
  PDF_GENERATE: '生成PDF',
  CREATE_USER: '创建用户', UPDATE_USER: '编辑用户', DISABLE_USER: '禁用用户',
  CREATE_DATASOURCE: '创建数据源', UPDATE_DATASOURCE: '编辑数据源', DELETE_DATASOURCE: '删除数据源',
  CANCEL_TASK: '取消任务', UPDATE_TENANT: '编辑租户',
}

const OPERATION_SEVERITY: Record<string, 'info' | 'success' | 'warning' | 'danger'> = {
  LOGIN: 'info', LOGIN_FAILED: 'danger', LOGOUT: 'info',
  CREATE_REPORT: 'success', UPDATE_REPORT: 'info', DELETE_REPORT: 'danger',
  SUBMIT_REVIEW: 'warning', APPROVE_REPORT: 'success', REJECT_REPORT: 'danger',
  PUBLISH_REPORT: 'success',
  CREATE_USER: 'success', UPDATE_USER: 'info', DISABLE_USER: 'danger',
  CANCEL_TASK: 'warning',
}

export default function AuditLogViewer() {
  const [logs, setLogs] = useState<AuditLogEntry[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(1)
  const [pageSize] = useState(20)

  const [userIdFilter, setUserIdFilter] = useState('')
  const [operationFilter, setOperationFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [dateFrom, setDateFrom] = useState<Date | undefined>()
  const [dateTo, setDateTo] = useState<Date | undefined>()

  const [detailDialogVisible, setDetailDialogVisible] = useState(false)
  const [selectedLog, setSelectedLog] = useState<AuditLogEntry | null>(null)

  const [integrityDialogVisible, setIntegrityDialogVisible] = useState(false)
  const [integrityDate, setIntegrityDate] = useState<Date>(new Date())
  const [integrityResult, setIntegrityResult] = useState<IntegrityCheckResult | null>(null)
  const [checkingIntegrity, setCheckingIntegrity] = useState(false)

  const [rootHashes, setRootHashes] = useState<AuditRootHash[]>([])
  const [rootHashDialogVisible, setRootHashDialogVisible] = useState(false)
  const [loadingRootHashes, setLoadingRootHashes] = useState(false)

  const toast = useRef<Toast>(null)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const result = await listAuditLogs({
        userId: userIdFilter ? Number(userIdFilter) : undefined,
        operation: operationFilter || undefined,
        status: statusFilter || undefined,
        dateFrom: dateFrom?.toISOString(),
        dateTo: dateTo?.toISOString(),
        page,
        pageSize,
      })
      setLogs(result.list)
      setTotal(result.total)
    } catch {
      toast.current?.show({ severity: 'error', summary: '加载失败', detail: '无法获取审计日志' })
    } finally {
      setLoading(false)
    }
  }, [userIdFilter, operationFilter, statusFilter, dateFrom, dateTo, page, pageSize])

  useEffect(() => { load() }, [load])

  const handleExport = async () => {
    try {
      const blob = await exportAuditLogs({
        userId: userIdFilter ? Number(userIdFilter) : undefined,
        operation: operationFilter || undefined,
        status: statusFilter || undefined,
        dateFrom: dateFrom?.toISOString(),
        dateTo: dateTo?.toISOString(),
      })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `audit_logs_${new Date().toISOString().slice(0, 10)}.csv`
      a.click()
      URL.revokeObjectURL(url)
      toast.current?.show({ severity: 'success', summary: '导出成功' })
    } catch {
      toast.current?.show({ severity: 'error', summary: '导出失败' })
    }
  }

  const handleVerifyIntegrity = async () => {
    const hourStart = new Date(integrityDate)
    hourStart.setMinutes(0, 0, 0)
    setCheckingIntegrity(true)
    try {
      const result = await verifyIntegrity(hourStart.toISOString())
      setIntegrityResult(result)
    } catch {
      toast.current?.show({ severity: 'error', summary: '校验失败', detail: '无法完成完整性校验' })
    } finally {
      setCheckingIntegrity(false)
    }
  }

  const openRootHashDialog = async () => {
    setRootHashDialogVisible(true)
    setLoadingRootHashes(true)
    try {
      const result = await listRootHashes({})
      setRootHashes(result)
    } catch {
      toast.current?.show({ severity: 'error', summary: '加载失败', detail: '无法获取根哈希记录' })
    } finally {
      setLoadingRootHashes(false)
    }
  }

  const showDetail = (log: AuditLogEntry) => {
    setSelectedLog(log)
    setDetailDialogVisible(true)
  }

  const operationBody = (row: AuditLogEntry) => (
    <Tag value={OPERATION_LABEL[row.operation] || row.operation}
      severity={OPERATION_SEVERITY[row.operation] || 'info'} />
  )

  const statusBody = (row: AuditLogEntry) => {
    const sev: Record<string, 'success' | 'danger'> = { SUCCESS: 'success', FAILED: 'danger' }
    const lab: Record<string, string> = { SUCCESS: '成功', FAILED: '失败' }
    return <Tag value={lab[row.status] || row.status} severity={sev[row.status] || 'info'} />
  }

  const timeBody = (row: AuditLogEntry) => {
    return new Date(row.createdAt).toLocaleString('zh-CN')
  }

  const actionBody = (row: AuditLogEntry) => (
    <Button icon="pi pi-search" rounded text severity="info" tooltip="详情"
      onClick={() => showDetail(row)} />
  )

  const toolbarLeft = (
    <div className="audit-toolbar">
      <InputText placeholder="用户ID..." value={userIdFilter}
        onChange={e => { setUserIdFilter(e.target.value); setPage(1) }}
        style={{ width: '100px' }} />
      <Dropdown options={OPERATION_OPTIONS} value={operationFilter}
        onChange={e => { setOperationFilter(e.value); setPage(1) }}
        placeholder="操作类型" style={{ width: '150px' }} />
      <Dropdown options={[
        { label: '全部状态', value: '' },
        { label: '成功', value: 'SUCCESS' },
        { label: '失败', value: 'FAILED' },
      ]} value={statusFilter}
        onChange={e => { setStatusFilter(e.value); setPage(1) }}
        placeholder="状态" style={{ width: '110px' }} />
      <Calendar value={dateFrom} onChange={e => { setDateFrom(e.value as Date); setPage(1) }}
        placeholder="开始时间" showTime hourFormat="24" />
      <Calendar value={dateTo} onChange={e => { setDateTo(e.value as Date); setPage(1) }}
        placeholder="结束时间" showTime hourFormat="24" />
      <Button icon="pi pi-refresh" rounded text onClick={load} />
    </div>
  )

  const toolbarRight = (
    <div className="audit-toolbar-right">
      <Button label="完整性校验" icon="pi pi-shield" severity="warning" text
        onClick={() => setIntegrityDialogVisible(true)} />
      <Button label="根哈希记录" icon="pi pi-qrcode" severity="info" text
        onClick={openRootHashDialog} />
      <Button label="导出CSV" icon="pi pi-download" text onClick={handleExport} />
    </div>
  )

  return (
    <div className="audit-log-viewer">
      <Toast ref={toast} />

      <h2 className="page-title">审计日志</h2>

      <Toolbar left={toolbarLeft} right={toolbarRight} />

      <DataTable
        value={logs}
        lazy
        loading={loading}
        totalRecords={total}
        paginator
        rows={pageSize}
        first={(page - 1) * pageSize}
        onPage={e => setPage((e.first ?? 0) / (e.rows ?? pageSize) + 1)}
        emptyMessage="暂无审计日志"
        stripedRows
        size="small"
        sortField="createdAt"
        sortOrder={-1}
      >
        <Column field="id" header="ID" style={{ width: '70px' }} />
        <Column header="操作" body={operationBody} style={{ width: '110px' }} />
        <Column field="username" header="用户" style={{ width: '90px' }} />
        <Column field="targetType" header="目标类型" style={{ width: '90px' }} />
        <Column field="targetName" header="目标名称" style={{ minWidth: '120px' }} />
        <Column field="ipAddress" header="IP地址" style={{ width: '120px' }} />
        <Column header="状态" body={statusBody} style={{ width: '70px' }} />
        <Column header="时间" body={timeBody} style={{ minWidth: '150px' }} sortable />
        <Column header="操作" body={actionBody} style={{ width: '60px' }} />
      </DataTable>

      <Dialog header="审计日志详情" visible={detailDialogVisible}
        style={{ width: '550px' }} onHide={() => setDetailDialogVisible(false)}>
        {selectedLog && (
          <div className="audit-detail">
            <div className="detail-row"><span className="label">ID</span><span>{selectedLog.id}</span></div>
            <div className="detail-row"><span className="label">操作</span><span>{OPERATION_LABEL[selectedLog.operation] || selectedLog.operation}</span></div>
            <div className="detail-row"><span className="label">用户</span><span>{selectedLog.username} (ID: {selectedLog.userId})</span></div>
            <div className="detail-row"><span className="label">目标类型</span><span>{selectedLog.targetType || '-'}</span></div>
            <div className="detail-row"><span className="label">目标名称</span><span>{selectedLog.targetName || '-'}</span></div>
            <div className="detail-row"><span className="label">目标ID</span><span>{selectedLog.targetId ?? '-'}</span></div>
            <div className="detail-row"><span className="label">IP地址</span><span>{selectedLog.ipAddress || '-'}</span></div>
            <div className="detail-row"><span className="label">状态</span><span>{selectedLog.status === 'SUCCESS' ? '成功' : '失败'}</span></div>
            <div className="detail-row"><span className="label">错误信息</span><span>{selectedLog.errorMessage || '-'}</span></div>
            <div className="detail-row"><span className="label">日志哈希</span><span className="hash">{selectedLog.logHash || '-'}</span></div>
            <div className="detail-row"><span className="label">前一哈希</span><span className="hash">{selectedLog.previousHash || '-'}</span></div>
            <div className="detail-row"><span className="label">创建时间</span><span>{new Date(selectedLog.createdAt).toLocaleString('zh-CN')}</span></div>
          </div>
        )}
      </Dialog>

      <Dialog header="完整性校验" visible={integrityDialogVisible}
        style={{ width: '500px' }} onHide={() => { setIntegrityDialogVisible(false); setIntegrityResult(null) }}>
        <div className="integrity-form">
          <div className="field">
            <label>选择校验小时</label>
            <Calendar value={integrityDate} onChange={e => setIntegrityDate(e.value as Date)}
              showTime hourFormat="24" dateFormat="yy-mm-dd" stepMinute={60} />
          </div>
          <Button label={checkingIntegrity ? '校验中...' : '开始校验'} icon="pi pi-shield"
            onClick={handleVerifyIntegrity} disabled={checkingIntegrity} />
          {integrityResult && (
            <div className={`integrity-result ${integrityResult.consistent ? 'consistent' : 'tampered'}`}>
              <Divider />
              <div className="result-icon">
                <i className={`pi ${integrityResult.consistent ? 'pi-check-circle' : 'pi-exclamation-triangle'}`} />
              </div>
              <div className="result-message">{integrityResult.message}</div>
              <div className="result-detail">
                <div>时间范围: {new Date(integrityResult.hourStart).toLocaleString('zh-CN')} ~ {new Date(integrityResult.hourEnd).toLocaleString('zh-CN')}</div>
                <div>日志数量: {integrityResult.logCount}</div>
                {!integrityResult.consistent && <div className="hash-warn">根哈希不一致，日志可能已被篡改！</div>}
              </div>
            </div>
          )}
        </div>
      </Dialog>

      <Dialog header="Merkle 根哈希记录" visible={rootHashDialogVisible}
        style={{ width: '600px' }} onHide={() => setRootHashDialogVisible(false)}>
        {loadingRootHashes ? (
          <div className="loading-text">加载中...</div>
        ) : rootHashes.length === 0 ? (
          <div className="loading-text">暂无根哈希记录</div>
        ) : (
          <DataTable value={rootHashes} stripedRows size="small">
            <Column header="小时" body={(r: AuditRootHash) => new Date(r.hourStart).toLocaleString('zh-CN')} />
            <Column field="rootHash" header="根哈希" body={(r: AuditRootHash) => (
              <span className="hash">{r.rootHash}</span>
            )} />
            <Column field="logCount" header="日志数" style={{ width: '80px' }} />
          </DataTable>
        )}
      </Dialog>
    </div>
  )
}
