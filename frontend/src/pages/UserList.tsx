import { useState, useEffect, useCallback, useRef } from 'react'
import { DataTable } from 'primereact/datatable'
import { Column } from 'primereact/column'
import { Tag } from 'primereact/tag'
import { Button } from 'primereact/button'
import { InputText } from 'primereact/inputtext'
import { Dropdown } from 'primereact/dropdown'
import { Toolbar } from 'primereact/toolbar'
import { Dialog } from 'primereact/dialog'
import { Password } from 'primereact/password'
import { Toast } from 'primereact/toast'
import { confirmDialog } from 'primereact/confirmdialog'
import {
  listUsers, createUser, updateUser, changeUserStatus, resetPassword,
} from '../services/user'
import type { User } from '../types'
import type { CreateUserRequest, UpdateUserRequest } from '../services/user'
import './UserList.scss'

const ROLE_OPTIONS = [
  { label: '全部', value: '' },
  { label: '管理员', value: 'ADMIN' },
  { label: '普通用户', value: 'USER' },
  { label: '只读用户', value: 'VIEWER' },
]

const ROLE_LABEL: Record<string, string> = {
  ADMIN: '管理员',
  USER: '普通用户',
  VIEWER: '只读用户',
}

const ROLE_SEVERITY: Record<string, 'info' | 'success' | 'warning' | 'danger'> = {
  ADMIN: 'danger',
  USER: 'success',
  VIEWER: 'info',
}

const STATUS_LABEL: Record<string, string> = {
  ACTIVE: '启用',
  DISABLED: '禁用',
}

const emptyForm: CreateUserRequest = {
  username: '',
  password: '',
  displayName: '',
  email: '',
  role: 'USER',
}

export default function UserList() {
  const [users, setUsers] = useState<User[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(1)
  const [pageSize] = useState(20)
  const [usernameFilter, setUsernameFilter] = useState('')
  const [roleFilter, setRoleFilter] = useState('')
  const [dialogVisible, setDialogVisible] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [form, setForm] = useState<CreateUserRequest>({ ...emptyForm })
  const [saving, setSaving] = useState(false)
  const [resetDialogVisible, setResetDialogVisible] = useState(false)
  const [resettingUser, setResettingUser] = useState<User | null>(null)
  const [newPassword, setNewPassword] = useState('')
  const [resetting, setResetting] = useState(false)
  const toast = useRef<Toast>(null)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const result = await listUsers({
        username: usernameFilter || undefined,
        role: roleFilter || undefined,
        page,
        pageSize,
      })
      setUsers(result.list)
      setTotal(result.total)
    } catch {
      toast.current?.show({ severity: 'error', summary: '加载失败', detail: '无法获取用户列表' })
    } finally {
      setLoading(false)
    }
  }, [usernameFilter, roleFilter, page, pageSize])

  useEffect(() => { load() }, [load])

  const openCreate = () => {
    setEditingId(null)
    setForm({ ...emptyForm })
    setDialogVisible(true)
  }

  const openEdit = (user: User) => {
    setEditingId(user.id)
    setForm({
      username: user.username,
      password: '',
      displayName: user.displayName || '',
      email: user.email || '',
      role: user.role,
    })
    setDialogVisible(true)
  }

  const handleSave = async () => {
    if (!form.username || (!editingId && !form.password)) {
      toast.current?.show({ severity: 'warn', summary: '表单不完整', detail: '请填写必填字段' })
      return
    }
    setSaving(true)
    try {
      if (editingId) {
        const update: UpdateUserRequest = {
          displayName: form.displayName,
          email: form.email,
          role: form.role,
        }
        await updateUser(editingId, update)
        toast.current?.show({ severity: 'success', summary: '已更新' })
      } else {
        await createUser(form)
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

  const handleToggleStatus = (user: User) => {
    const nextStatus = user.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
    const actionLabel = nextStatus === 'ACTIVE' ? '启用' : '禁用'
    confirmDialog({
      message: `确定${actionLabel}用户「${user.username}」？`,
      header: `确认${actionLabel}`,
      icon: 'pi pi-exclamation-triangle',
      accept: async () => {
        try {
          await changeUserStatus(user.id, nextStatus)
          toast.current?.show({ severity: 'success', summary: `已${actionLabel}` })
          load()
        } catch {
          toast.current?.show({ severity: 'error', summary: `${actionLabel}失败` })
        }
      },
    })
  }

  const openResetPassword = (user: User) => {
    setResettingUser(user)
    setNewPassword('')
    setResetDialogVisible(true)
  }

  const handleResetPassword = async () => {
    if (!resettingUser || !newPassword) {
      toast.current?.show({ severity: 'warn', summary: '请输入新密码' })
      return
    }
    setResetting(true)
    try {
      await resetPassword(resettingUser.id, newPassword)
      toast.current?.show({ severity: 'success', summary: '密码已重置' })
      setResetDialogVisible(false)
      setNewPassword('')
    } catch {
      toast.current?.show({ severity: 'error', summary: '重置失败' })
    } finally {
      setResetting(false)
    }
  }

  const roleBody = (row: User) => (
    <Tag value={ROLE_LABEL[row.role] || row.role} severity={ROLE_SEVERITY[row.role] || 'info'} />
  )

  const statusBody = (row: User) => {
    const severity: Record<string, 'success' | 'danger'> = { ACTIVE: 'success', DISABLED: 'danger' }
    return <Tag value={STATUS_LABEL[row.status] || row.status} severity={severity[row.status] || 'info'} />
  }

  const dateBody = (row: User, field: keyof User) => {
    const value = row[field]
    return value ? new Date(String(value)).toLocaleString('zh-CN') : '-'
  }

  const actionBody = (row: User) => (
    <div className="user-actions">
      <Button icon="pi pi-pencil" rounded text severity="info" tooltip="编辑"
        onClick={() => openEdit(row)} />
      <Button
        icon={row.status === 'ACTIVE' ? 'pi pi-lock' : 'pi pi-lock-open'}
        rounded text
        severity={row.status === 'ACTIVE' ? 'warning' : 'success'}
        tooltip={row.status === 'ACTIVE' ? '禁用' : '启用'}
        onClick={() => handleToggleStatus(row)}
      />
      <Button icon="pi pi-key" rounded text severity="help" tooltip="重置密码"
        onClick={() => openResetPassword(row)} />
    </div>
  )

  const toolbarLeft = (
    <div className="user-toolbar">
      <InputText placeholder="搜索用户名..." value={usernameFilter}
        onChange={e => { setUsernameFilter(e.target.value); setPage(1) }} />
      <Dropdown options={ROLE_OPTIONS} value={roleFilter} onChange={e => { setRoleFilter(e.value); setPage(1) }}
        placeholder="角色筛选" className="role-dropdown" />
      <Button icon="pi pi-refresh" rounded text onClick={load} />
    </div>
  )

  const dialogFooter = (
    <div>
      <Button label="取消" icon="pi pi-times" text onClick={() => setDialogVisible(false)} />
      <Button label={saving ? '保存中...' : '保存'} icon="pi pi-check" onClick={handleSave} disabled={saving} />
    </div>
  )

  const resetDialogFooter = (
    <div>
      <Button label="取消" icon="pi pi-times" text onClick={() => setResetDialogVisible(false)} />
      <Button label={resetting ? '重置中...' : '重置'} icon="pi pi-check" onClick={handleResetPassword} disabled={resetting} />
    </div>
  )

  return (
    <div className="user-list-page">
      <Toast ref={toast} />

      <h2 className="page-title">用户管理</h2>

      <Toolbar left={toolbarLeft} right={<Button label="添加用户" icon="pi pi-plus" onClick={openCreate} />} />

      <DataTable
        value={users}
        lazy
        loading={loading}
        totalRecords={total}
        paginator
        rows={pageSize}
        first={(page - 1) * pageSize}
        onPage={e => setPage((e.first ?? 0) / (e.rows ?? pageSize) + 1)}
        emptyMessage="暂无用户"
        stripedRows
        size="small"
      >
        <Column field="username" header="用户名" sortable style={{ minWidth: '140px' }} />
        <Column field="displayName" header="显示名" style={{ minWidth: '140px' }} />
        <Column field="email" header="邮箱" style={{ minWidth: '180px' }} />
        <Column header="角色" body={roleBody} style={{ width: '110px' }} />
        <Column header="状态" body={statusBody} style={{ width: '80px' }} />
        <Column field="createdAt" header="创建时间" body={row => dateBody(row, 'createdAt')} sortable style={{ width: '160px' }} />
        <Column field="updatedAt" header="更新时间" body={row => dateBody(row, 'updatedAt')} sortable style={{ width: '160px' }} />
        <Column header="操作" body={actionBody} style={{ width: '150px' }} />
      </DataTable>

      <Dialog
        header={editingId ? '编辑用户' : '添加用户'}
        visible={dialogVisible}
        style={{ width: '500px' }}
        footer={dialogFooter}
        onHide={() => setDialogVisible(false)}
      >
        <div className="user-form">
          <div className="field">
            <label>用户名 *</label>
            <InputText value={form.username} disabled={!!editingId}
              onChange={e => setForm({ ...form, username: e.target.value })} placeholder="用户名" />
          </div>
          {!editingId && (
            <div className="field">
              <label>密码 *</label>
              <Password value={form.password} onChange={e => setForm({ ...form, password: e.target.value })}
                placeholder="至少8位，含大小写字母、数字和特殊字符" toggleMask feedback={false} style={{ width: '100%' }} inputStyle={{ width: '100%' }} />
            </div>
          )}
          <div className="field">
            <label>显示名</label>
            <InputText value={form.displayName} onChange={e => setForm({ ...form, displayName: e.target.value })} placeholder="显示名" />
          </div>
          <div className="field">
            <label>邮箱</label>
            <InputText value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} placeholder="邮箱" />
          </div>
          <div className="field">
            <label>角色 *</label>
            <Dropdown options={ROLE_OPTIONS.filter(o => o.value)} value={form.role}
              onChange={e => setForm({ ...form, role: e.value })} placeholder="选择角色" style={{ width: '100%' }} />
          </div>
        </div>
      </Dialog>

      <Dialog
        header={`重置密码 - ${resettingUser?.username || ''}`}
        visible={resetDialogVisible}
        style={{ width: '400px' }}
        footer={resetDialogFooter}
        onHide={() => setResetDialogVisible(false)}
      >
        <div className="user-form">
          <div className="field">
            <label>新密码 *</label>
            <Password value={newPassword} onChange={e => setNewPassword(e.target.value)}
              placeholder="至少8位，含大小写字母、数字和特殊字符" toggleMask feedback={false} style={{ width: '100%' }} inputStyle={{ width: '100%' }} />
          </div>
        </div>
      </Dialog>
    </div>
  )
}
