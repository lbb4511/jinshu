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
  listTenants, createTenant, updateTenant, archiveTenant, changeTenantStatus,
} from '../services/tenant'
import type { Tenant, TenantCreateRequest, TenantUpdateRequest } from '../types'
import './TenantList.scss'

const STATUS_OPTIONS = [
  { label: '全部', value: '' },
  { label: '启用', value: 'ACTIVE' },
  { label: '停用', value: 'SUSPENDED' },
  { label: '归档', value: 'ARCHIVED' },
]

const STATUS_LABEL: Record<string, string> = {
  ACTIVE: '启用',
  SUSPENDED: '停用',
  ARCHIVED: '归档',
}

const STATUS_SEVERITY: Record<string, 'success' | 'warning' | 'info' | 'danger'> = {
  ACTIVE: 'success',
  SUSPENDED: 'warning',
  ARCHIVED: 'info',
}

const emptyForm: TenantCreateRequest = {
  name: '',
  code: '',
  description: '',
  adminUsername: '',
  adminPassword: '',
  adminEmail: '',
}

export default function TenantList() {
  const [tenants, setTenants] = useState<Tenant[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(1)
  const [pageSize] = useState(20)
  const [nameFilter, setNameFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [dialogVisible, setDialogVisible] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [form, setForm] = useState<TenantCreateRequest>({ ...emptyForm })
  const [saving, setSaving] = useState(false)
  const [statusLoadingId, setStatusLoadingId] = useState<number | null>(null)
  const toast = useRef<Toast>(null)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const result = await listTenants({
        name: nameFilter || undefined,
        status: statusFilter || undefined,
        page,
        pageSize,
      })
      setTenants(result.list)
      setTotal(result.total)
    } catch {
      toast.current?.show({ severity: 'error', summary: '加载失败', detail: '无法获取租户列表' })
    } finally {
      setLoading(false)
    }
  }, [nameFilter, statusFilter, page, pageSize])

  useEffect(() => { load() }, [load])

  const openCreate = () => {
    setEditingId(null)
    setForm({ ...emptyForm })
    setDialogVisible(true)
  }

  const openEdit = (tenant: Tenant) => {
    setEditingId(tenant.id)
    setForm({
      name: tenant.name,
      code: tenant.code,
      description: tenant.description || '',
    })
    setDialogVisible(true)
  }

  const handleSave = async () => {
    if (!form.name || !form.code) {
      toast.current?.show({ severity: 'warn', summary: '表单不完整', detail: '请填写租户名称和编码' })
      return
    }
    setSaving(true)
    try {
      if (editingId) {
        const update: TenantUpdateRequest = {
          name: form.name,
          description: form.description,
        }
        await updateTenant(editingId, update)
        toast.current?.show({ severity: 'success', summary: '已更新' })
      } else {
        const create: TenantCreateRequest = {
          name: form.name,
          code: form.code,
          description: form.description,
        }
        if (form.adminUsername) create.adminUsername = form.adminUsername
        if (form.adminPassword) create.adminPassword = form.adminPassword
        if (form.adminEmail) create.adminEmail = form.adminEmail
        await createTenant(create)
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

  const handleArchive = (tenant: Tenant) => {
    confirmDialog({
      message: `确定归档租户「${tenant.name}」？归档后该租户将被置为归档状态。`,
      header: '确认归档',
      icon: 'pi pi-exclamation-triangle',
      accept: async () => {
        try {
          await archiveTenant(tenant.id)
          toast.current?.show({ severity: 'success', summary: '已归档' })
          load()
        } catch {
          toast.current?.show({ severity: 'error', summary: '归档失败' })
        }
      },
    })
  }

  const handleToggleStatus = async (tenant: Tenant) => {
    const nextStatus = tenant.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE'
    setStatusLoadingId(tenant.id)
    try {
      await changeTenantStatus(tenant.id, nextStatus)
      toast.current?.show({
        severity: 'success',
        summary: nextStatus === 'ACTIVE' ? '已启用' : '已停用',
      })
      load()
    } catch {
      toast.current?.show({ severity: 'error', summary: '状态变更失败' })
    } finally {
      setStatusLoadingId(null)
    }
  }

  const statusBody = (row: Tenant) => (
    <Tag value={STATUS_LABEL[row.status] || row.status} severity={STATUS_SEVERITY[row.status] || 'info'} />
  )

  const dateBody = (row: Tenant) => row.createdAt ? new Date(row.createdAt).toLocaleString('zh-CN') : '-'

  const actionBody = (row: Tenant) => (
    <div className="tenant-actions">
      <Button icon="pi pi-pencil" rounded text severity="info" tooltip="编辑"
        onClick={() => openEdit(row)} />
      <Button
        icon={row.status === 'ACTIVE' ? 'pi pi-pause' : 'pi pi-play'}
        rounded text
        severity={row.status === 'ACTIVE' ? 'warning' : 'success'}
        tooltip={row.status === 'ACTIVE' ? '停用' : '启用'}
        loading={statusLoadingId === row.id}
        onClick={() => handleToggleStatus(row)}
      />
      <Button icon="pi pi-folder" rounded text severity="danger" tooltip="归档"
        onClick={() => handleArchive(row)} />
    </div>
  )

  const toolbarLeft = (
    <div className="tenant-toolbar">
      <InputText placeholder="搜索租户名称..." value={nameFilter}
        onChange={e => { setNameFilter(e.target.value); setPage(1) }} />
      <Dropdown options={STATUS_OPTIONS} value={statusFilter} onChange={e => { setStatusFilter(e.value); setPage(1) }}
        placeholder="状态筛选" className="status-dropdown" />
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
    <div className="tenant-list-page">
      <Toast ref={toast} />

      <h2 className="page-title">租户管理</h2>

      <Toolbar left={toolbarLeft} right={<Button label="添加租户" icon="pi pi-plus" onClick={openCreate} />} />

      <DataTable
        value={tenants}
        lazy
        loading={loading}
        totalRecords={total}
        paginator
        rows={pageSize}
        first={(page - 1) * pageSize}
        onPage={e => setPage((e.first ?? 0) / (e.rows ?? pageSize) + 1)}
        emptyMessage="暂无租户"
        stripedRows
        size="small"
        sortField="createdAt"
        sortOrder={-1}
      >
        <Column field="name" header="租户名称" sortable style={{ minWidth: '160px' }} />
        <Column field="code" header="租户编码" style={{ minWidth: '120px' }} />
        <Column field="description" header="描述" style={{ minWidth: '200px' }} />
        <Column header="状态" body={statusBody} style={{ width: '100px' }} />
        <Column field="createdAt" header="创建时间" body={dateBody} sortable style={{ width: '160px' }} />
        <Column header="操作" body={actionBody} style={{ width: '150px' }} />
      </DataTable>

      <Dialog
        header={editingId ? '编辑租户' : '添加租户'}
        visible={dialogVisible}
        style={{ width: '520px' }}
        footer={dialogFooter}
        onHide={() => setDialogVisible(false)}
      >
        <div className="tenant-form">
          <div className="field">
            <label>租户名称 *</label>
            <InputText value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} placeholder="请输入租户名称" />
          </div>
          <div className="field">
            <label>租户编码 *</label>
            <InputText value={form.code} onChange={e => setForm({ ...form, code: e.target.value })} placeholder="唯一编码" disabled={!!editingId} />
          </div>
          <div className="field">
            <label>描述</label>
            <InputTextarea value={form.description || ''} onChange={e => setForm({ ...form, description: e.target.value })} rows={3} />
          </div>
          {!editingId && (
            <>
              <div className="field-row">
                <div className="field half">
                  <label>管理员账号</label>
                  <InputText value={form.adminUsername || ''} onChange={e => setForm({ ...form, adminUsername: e.target.value })} placeholder="admin" />
                </div>
                <div className="field half">
                  <label>管理员密码</label>
                  <InputText type="password" value={form.adminPassword || ''} onChange={e => setForm({ ...form, adminPassword: e.target.value })} placeholder="选填" />
                </div>
              </div>
              <div className="field">
                <label>管理员邮箱</label>
                <InputText value={form.adminEmail || ''} onChange={e => setForm({ ...form, adminEmail: e.target.value })} placeholder="admin@example.com" />
              </div>
            </>
          )}
        </div>
      </Dialog>
    </div>
  )
}
