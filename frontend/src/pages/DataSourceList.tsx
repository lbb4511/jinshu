import { useState, useEffect, useCallback, useRef } from 'react'
import { DataTable } from 'primereact/datatable'
import { Column } from 'primereact/column'
import { Tag } from 'primereact/tag'
import { Button } from 'primereact/button'
import { InputText } from 'primereact/inputtext'
import { Dropdown } from 'primereact/dropdown'
import { Toolbar } from 'primereact/toolbar'
import { Dialog } from 'primereact/dialog'
import { InputTextarea } from 'primereact/inputtextarea'
import { Toast } from 'primereact/toast'
import { confirmDialog } from 'primereact/confirmdialog'
import {
  listDataSources, createDataSource, updateDataSource,
  deleteDataSource, testConnection,
} from '../services/datasource'
import type { DataSource, DataSourceCreateRequest, DataSourceUpdateRequest } from '../types'
import './DataSourceList.scss'

const TYPE_OPTIONS = [
  { label: '全部', value: '' },
  { label: 'PostgreSQL', value: 'POSTGRESQL' },
  { label: 'MySQL', value: 'MYSQL' },
  { label: 'SQL Server', value: 'SQLSERVER' },
  { label: 'Oracle', value: 'ORACLE' },
]

const TYPE_LABEL: Record<string, string> = {
  POSTGRESQL: 'PostgreSQL',
  MYSQL: 'MySQL',
  SQLSERVER: 'SQL Server',
  ORACLE: 'Oracle',
}

const TYPE_SEVERITY: Record<string, 'info' | 'success' | 'warning' | 'danger'> = {
  POSTGRESQL: 'info',
  MYSQL: 'success',
  SQLSERVER: 'warning',
  ORACLE: 'danger',
}

const emptyForm: DataSourceCreateRequest = {
  name: '',
  type: 'POSTGRESQL',
  host: '',
  port: 5432,
  databaseName: '',
  username: '',
  password: '',
  description: '',
}

export default function DataSourceList() {
  const [dataSources, setDataSources] = useState<DataSource[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(1)
  const [pageSize] = useState(20)
  const [nameFilter, setNameFilter] = useState('')
  const [typeFilter, setTypeFilter] = useState('')
  const [dialogVisible, setDialogVisible] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [form, setForm] = useState<DataSourceCreateRequest>({ ...emptyForm })
  const [saving, setSaving] = useState(false)
  const [testingId, setTestingId] = useState<number | null>(null)
  const toast = useRef<Toast>(null)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const result = await listDataSources({
        name: nameFilter || undefined,
        type: typeFilter || undefined,
        page,
        pageSize,
      })
      setDataSources(result.list)
      setTotal(result.total)
    } catch {
      toast.current?.show({ severity: 'error', summary: '加载失败', detail: '无法获取数据源列表' })
    } finally {
      setLoading(false)
    }
  }, [nameFilter, typeFilter, page, pageSize])

  useEffect(() => { load() }, [load])

  const openCreate = () => {
    setEditingId(null)
    setForm({ ...emptyForm })
    setDialogVisible(true)
  }

  const openEdit = (ds: DataSource) => {
    setEditingId(ds.id)
    setForm({
      name: ds.name,
      type: ds.type,
      host: ds.host,
      port: ds.port,
      databaseName: ds.databaseName,
      username: ds.username,
      password: '',
      description: ds.description || '',
    })
    setDialogVisible(true)
  }

  const handleSave = async () => {
    if (!form.name || !form.host || !form.databaseName) {
      toast.current?.show({ severity: 'warn', summary: '表单不完整', detail: '请填写必填字段' })
      return
    }
    setSaving(true)
    try {
      if (editingId) {
        const update: DataSourceUpdateRequest = { ...form }
        if (!update.password) delete update.password
        await updateDataSource(editingId, update)
        toast.current?.show({ severity: 'success', summary: '已更新' })
      } else {
        await createDataSource(form)
        toast.current?.show({ severity: 'success', summary: '已创建' })
      }
      setDialogVisible(false)
      load()
    } catch {
      toast.current?.show({ severity: 'error', summary: '保存失败' })
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = (ds: DataSource) => {
    confirmDialog({
      message: `确定删除数据源「${ds.name}」？`,
      header: '确认删除',
      icon: 'pi pi-exclamation-triangle',
      accept: async () => {
        try {
          await deleteDataSource(ds.id)
          toast.current?.show({ severity: 'success', summary: '已删除' })
          load()
        } catch {
          toast.current?.show({ severity: 'error', summary: '删除失败' })
        }
      },
    })
  }

  const handleTest = async (id: number) => {
    setTestingId(id)
    try {
      const result = await testConnection(id)
      toast.current?.show({
        severity: result.success ? 'success' : 'error',
        summary: result.success ? '连接成功' : '连接失败',
        detail: result.message,
      })
    } catch {
      toast.current?.show({ severity: 'error', summary: '测试失败', detail: '无法连接到数据源' })
    } finally {
      setTestingId(null)
    }
  }

  const typeBody = (row: DataSource) => (
    <Tag value={TYPE_LABEL[row.type] || row.type} severity={TYPE_SEVERITY[row.type] || 'info'} />
  )

  const statusBody = (row: DataSource) => {
    const sev: Record<string, 'success' | 'danger'> = { ACTIVE: 'success', INACTIVE: 'danger' }
    const lab: Record<string, string> = { ACTIVE: '正常', INACTIVE: '异常' }
    return <Tag value={lab[row.status] || row.status} severity={sev[row.status] || 'info'} />
  }

  const actionBody = (row: DataSource) => (
    <div className="ds-actions">
      <Button icon="pi pi-pencil" rounded text severity="info" tooltip="编辑"
        onClick={() => openEdit(row)} />
      <Button icon="pi pi-trash" rounded text severity="danger" tooltip="删除"
        onClick={() => handleDelete(row)} />
      <Button icon="pi pi-check" rounded text severity="success" tooltip="测试连接"
        loading={testingId === row.id}
        onClick={() => handleTest(row.id)} />
    </div>
  )



  const toolbarLeft = (
    <div className="ds-toolbar">
      <InputText placeholder="搜索名称..." value={nameFilter}
        onChange={e => { setNameFilter(e.target.value); setPage(1) }} />
      <Dropdown options={TYPE_OPTIONS} value={typeFilter} onChange={e => { setTypeFilter(e.value); setPage(1) }}
        placeholder="类型筛选" className="type-dropdown" />
      <Button icon="pi pi-refresh" rounded text onClick={load} />
    </div>
  )

  const dialogFooter = (
    <div>
      <Button label="取消" icon="pi pi-times" text onClick={() => setDialogVisible(false)} />
      <Button label={saving ? '保存中...' : '保存'} icon="pi pi-check" onClick={handleSave} disabled={saving} />
    </div>
  )

  return (
    <div className="datasource-list-page">
      <Toast ref={toast} />

      <h2 className="page-title">数据源管理</h2>

      <Toolbar left={toolbarLeft} right={<Button label="添加数据源" icon="pi pi-plus" onClick={openCreate} />} />

      <DataTable
        value={dataSources}
        lazy
        loading={loading}
        totalRecords={total}
        paginator
        rows={pageSize}
        first={(page - 1) * pageSize}
        onPage={e => setPage((e.first ?? 0) / (e.rows ?? pageSize) + 1)}
        emptyMessage="暂无数据源"
        stripedRows
        size="small"
      >
        <Column field="name" header="名称" sortable style={{ minWidth: '140px' }} />
        <Column header="类型" body={typeBody} style={{ width: '120px' }} />
        <Column field="host" header="主机" style={{ width: '140px' }} />
        <Column field="port" header="端口" style={{ width: '70px' }} />
        <Column field="databaseName" header="数据库" style={{ width: '120px' }} />
        <Column header="状态" body={statusBody} style={{ width: '80px' }} />
        <Column field="lastTestTime" header="最后测试" body={(r: DataSource) => r.lastTestTime ? new Date(r.lastTestTime).toLocaleString('zh-CN') : '-'} style={{ width: '150px' }} />
        <Column header="操作" body={actionBody} style={{ width: '150px' }} />
      </DataTable>

      <Dialog
        header={editingId ? '编辑数据源' : '添加数据源'}
        visible={dialogVisible}
        style={{ width: '500px' }}
        footer={dialogFooter}
        onHide={() => setDialogVisible(false)}
      >
        <div className="ds-form">
          <div className="field">
            <label>名称 *</label>
            <InputText value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} placeholder="数据源名称" />
          </div>
          <div className="field">
            <label>类型 *</label>
            <Dropdown options={TYPE_OPTIONS.filter(o => o.value)} value={form.type}
              onChange={e => {
                const portMap: Record<string, number> = { POSTGRESQL: 5432, MYSQL: 3306, SQLSERVER: 1433, ORACLE: 1521 }
                setForm({ ...form, type: e.value, port: portMap[e.value] || form.port })
              }}
              placeholder="选择类型" style={{ width: '100%' }} />
          </div>
          <div className="field">
            <label>主机地址 *</label>
            <InputText value={form.host} onChange={e => setForm({ ...form, host: e.target.value })} placeholder="localhost" />
          </div>
          <div className="field-row">
            <div className="field half">
              <label>端口</label>
              <InputText value={String(form.port)} onChange={e => setForm({ ...form, port: parseInt(e.target.value) || 0 })} />
            </div>
            <div className="field half">
              <label>数据库名 *</label>
              <InputText value={form.databaseName} onChange={e => setForm({ ...form, databaseName: e.target.value })} />
            </div>
          </div>
          <div className="field">
            <label>用户名</label>
            <InputText value={form.username} onChange={e => setForm({ ...form, username: e.target.value })} />
          </div>
          <div className="field">
            <label>密码</label>
            <InputText type="password" value={form.password || ''} onChange={e => setForm({ ...form, password: e.target.value })} placeholder={editingId ? '留空则不修改' : ''} />
          </div>
          <div className="field">
            <label>描述</label>
            <InputTextarea value={form.description || ''} onChange={e => setForm({ ...form, description: e.target.value })} rows={3} />
          </div>
        </div>
      </Dialog>
    </div>
  )
}
