import { useState, useEffect, useCallback, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { Card } from 'primereact/card'
import { Button } from 'primereact/button'
import { InputText } from 'primereact/inputtext'
import { InputTextarea } from 'primereact/inputtextarea'
import { Dropdown } from 'primereact/dropdown'
import { Checkbox } from 'primereact/checkbox'
import { Toolbar } from 'primereact/toolbar'
import { Dialog } from 'primereact/dialog'
import { Tag } from 'primereact/tag'
import { Toast } from 'primereact/toast'
import { DataTable } from 'primereact/datatable'
import { Column } from 'primereact/column'
import { Paginator } from 'primereact/paginator'
import { confirmDialog } from 'primereact/confirmdialog'
import { useAuth } from '../store/useAuth'
import { listTemplates, createTemplate, applyTemplate, deleteTemplate } from '../services/template'
import type { ReportTemplate, ReportTemplateCreateRequest } from '../types'
import './TemplateMarket.scss'

const CATEGORY_OPTIONS = [
  { label: '全部分类', value: '' },
  { label: '销售', value: 'SALES' },
  { label: '财务', value: 'FINANCE' },
  { label: '人力', value: 'HR' },
]

const CATEGORY_LABEL: Record<string, string> = {
  SALES: '销售',
  FINANCE: '财务',
  HR: '人力',
}

const CATEGORY_SEVERITY: Record<string, 'info' | 'success' | 'warning'> = {
  SALES: 'info',
  FINANCE: 'success',
  HR: 'warning',
}

const emptyForm: ReportTemplateCreateRequest = {
  name: '',
  category: 'SALES',
  description: '',
  layoutJson: '',
  isPublic: true,
}

export default function TemplateMarket() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const toast = useRef<Toast>(null)

  const [templates, setTemplates] = useState<ReportTemplate[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(1)
  const [pageSize] = useState(12)
  const [categoryFilter, setCategoryFilter] = useState('')
  const [keyword, setKeyword] = useState('')
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid')

  const [createDialogVisible, setCreateDialogVisible] = useState(false)
  const [form, setForm] = useState<ReportTemplateCreateRequest>({ ...emptyForm })
  const [saving, setSaving] = useState(false)

  const [applyDialogVisible, setApplyDialogVisible] = useState(false)
  const [applyingTemplate, setApplyingTemplate] = useState<ReportTemplate | null>(null)
  const [reportName, setReportName] = useState('')
  const [applying, setApplying] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const result = await listTemplates({
        category: categoryFilter || undefined,
        keyword: keyword || undefined,
        includePublic: true,
        page,
        pageSize,
      })
      setTemplates(result.list)
      setTotal(result.total)
    } catch {
      toast.current?.show({ severity: 'error', summary: '加载失败', detail: '无法获取模板列表' })
    } finally {
      setLoading(false)
    }
  }, [categoryFilter, keyword, page, pageSize])

  useEffect(() => { load() }, [load])

  const resetForm = () => {
    setForm({ ...emptyForm })
  }

  const openCreate = () => {
    resetForm()
    setCreateDialogVisible(true)
  }

  const handleSave = async () => {
    if (!form.name || !form.category) {
      toast.current?.show({ severity: 'warn', summary: '表单不完整', detail: '请填写名称和分类' })
      return
    }
    if (form.layoutJson) {
      try {
        JSON.parse(form.layoutJson)
      } catch {
        toast.current?.show({ severity: 'warn', summary: 'JSON 格式错误', detail: '布局 JSON 不是有效的 JSON' })
        return
      }
    }
    setSaving(true)
    try {
      await createTemplate(form)
      toast.current?.show({ severity: 'success', summary: '已创建', detail: '模板已创建' })
      setCreateDialogVisible(false)
      resetForm()
      setPage(1)
      load()
    } catch {
      toast.current?.show({ severity: 'error', summary: '创建失败', detail: '无法创建模板' })
    } finally {
      setSaving(false)
    }
  }

  const openApply = (template: ReportTemplate) => {
    setApplyingTemplate(template)
    setReportName(`${template.name} 副本`)
    setApplyDialogVisible(true)
  }

  const handleApply = async () => {
    if (!applyingTemplate) return
    if (!reportName.trim()) {
      toast.current?.show({ severity: 'warn', summary: '请输入报表名称' })
      return
    }
    setApplying(true)
    try {
      const reportId = await applyTemplate(applyingTemplate.id, { name: reportName.trim() })
      toast.current?.show({ severity: 'success', summary: '应用成功', detail: '正在跳转到报表编辑器' })
      setApplyDialogVisible(false)
      setApplyingTemplate(null)
      setReportName('')
      navigate(`/reports/${reportId}/edit`)
    } catch {
      toast.current?.show({ severity: 'error', summary: '应用失败', detail: '无法应用模板' })
    } finally {
      setApplying(false)
    }
  }

  const handleDelete = (template: ReportTemplate) => {
    confirmDialog({
      message: `确定删除模板「${template.name}」？`,
      header: '确认删除',
      icon: 'pi pi-exclamation-triangle',
      accept: async () => {
        try {
          await deleteTemplate(template.id)
          toast.current?.show({ severity: 'success', summary: '已删除', detail: `模板「${template.name}」已删除` })
          load()
        } catch {
          toast.current?.show({ severity: 'error', summary: '删除失败', detail: '无法删除该模板' })
        }
      },
    })
  }

  const canDelete = (template: ReportTemplate) => {
    if (template.isSystem) return false
    if (!user) return false
    return template.createdBy === user.id
  }

  const categoryBody = (template: ReportTemplate) => (
    <Tag value={CATEGORY_LABEL[template.category] || template.category} severity={CATEGORY_SEVERITY[template.category] || 'info'} />
  )

  const tagsBody = (template: ReportTemplate) => (
    <div className="template-tags">
      {template.isSystem && <Tag value="系统" severity="warning" className="mr-2" />}
      <Tag value={template.isPublic ? '公开' : '私有'} severity={template.isPublic ? 'success' : 'info'} />
    </div>
  )

  const dateBody = (template: ReportTemplate) => template.createdAt ? new Date(template.createdAt).toLocaleString('zh-CN') : '-'

  const actionBody = (template: ReportTemplate) => (
    <div className="template-actions">
      <Button icon="pi pi-check" rounded text severity="success" tooltip="应用"
        onClick={() => openApply(template)} />
      {canDelete(template) && (
        <Button icon="pi pi-trash" rounded text severity="danger" tooltip="删除"
          onClick={() => handleDelete(template)} />
      )}
    </div>
  )

  const toolbarLeft = (
    <div className="template-toolbar">
      <InputText placeholder="搜索模板名称或描述..." value={keyword}
        onChange={e => { setKeyword(e.target.value); setPage(1) }} />
      <Dropdown options={CATEGORY_OPTIONS} value={categoryFilter}
        onChange={e => { setCategoryFilter(e.value); setPage(1) }}
        placeholder="分类筛选" className="category-dropdown" />
      <Button icon="pi pi-refresh" rounded text onClick={load} loading={loading} />
    </div>
  )

  const toolbarRight = (
    <div className="template-toolbar">
      <div className="view-toggle">
        <Button icon="pi pi-th-large" className={viewMode === 'grid' ? 'active' : ''}
          onClick={() => { setViewMode('grid'); setPage(1) }} tooltip="网格视图" />
        <Button icon="pi pi-list" className={viewMode === 'list' ? 'active' : ''}
          onClick={() => { setViewMode('list'); setPage(1) }} tooltip="列表视图" />
      </div>
      <Button label="创建模板" icon="pi pi-plus" onClick={openCreate} />
    </div>
  )

  const createDialogFooter = (
    <div>
      <Button label="取消" icon="pi pi-times" text onClick={() => setCreateDialogVisible(false)} />
      <Button label={saving ? '保存中...' : '保存'} icon="pi pi-check" onClick={handleSave} disabled={saving} />
    </div>
  )

  const applyDialogFooter = (
    <div>
      <Button label="取消" icon="pi pi-times" text onClick={() => setApplyDialogVisible(false)} />
      <Button label={applying ? '应用中...' : '应用'} icon="pi pi-check" onClick={handleApply} disabled={applying} />
    </div>
  )

  const renderGrid = () => (
    <>
      <div className="template-grid">
        {templates.map(template => (
          <Card key={template.id} className="template-card">
            <div className="template-card-header">
              <h3 className="template-name" title={template.name}>{template.name}</h3>
              {categoryBody(template)}
            </div>
            <div className="template-description" title={template.description}>
              {template.description || '暂无描述'}
            </div>
            <div className="template-meta">
              {tagsBody(template)}
              <span>{dateBody(template)}</span>
            </div>
            <div className="template-actions">
              <Button label="应用" icon="pi pi-check" size="small" severity="success"
                onClick={() => openApply(template)} />
              {canDelete(template) && (
                <Button label="删除" icon="pi pi-trash" size="small" severity="danger" outlined
                  onClick={() => handleDelete(template)} />
              )}
            </div>
          </Card>
        ))}
      </div>
      {!templates.length && !loading && (
        <div className="text-center p-4 text-secondary">暂无模板</div>
      )}
      <Paginator
        className="template-paginator"
        first={(page - 1) * pageSize}
        rows={pageSize}
        totalRecords={total}
        onPageChange={e => setPage((e.first ?? 0) / (e.rows ?? pageSize) + 1)}
      />
    </>
  )

  const renderList = () => (
    <DataTable
      className="template-list"
      value={templates}
      lazy
      loading={loading}
      totalRecords={total}
      paginator
      rows={pageSize}
      first={(page - 1) * pageSize}
      onPage={e => setPage((e.first ?? 0) / (e.rows ?? pageSize) + 1)}
      emptyMessage="暂无模板"
      stripedRows
      size="small"
    >
      <Column field="name" header="名称" sortable style={{ minWidth: '180px' }} />
      <Column header="分类" body={categoryBody} style={{ width: '120px' }} />
      <Column field="description" header="描述" style={{ minWidth: '200px' }} />
      <Column header="标签" body={tagsBody} style={{ width: '160px' }} />
      <Column field="createdAt" header="创建时间" body={dateBody} sortable style={{ width: '160px' }} />
      <Column header="操作" body={actionBody} style={{ width: '120px' }} />
    </DataTable>
  )

  return (
    <div className="template-market-page">
      <Toast ref={toast} />

      <h2 className="page-title">模板市场</h2>

      <Toolbar left={toolbarLeft} right={toolbarRight} />

      {viewMode === 'grid' ? renderGrid() : renderList()}

      <Dialog
        header="创建模板"
        visible={createDialogVisible}
        style={{ width: '520px' }}
        footer={createDialogFooter}
        onHide={() => setCreateDialogVisible(false)}
      >
        <div className="template-form">
          <div className="field">
            <label>名称 *</label>
            <InputText value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} placeholder="模板名称" />
          </div>
          <div className="field">
            <label>分类 *</label>
            <Dropdown options={CATEGORY_OPTIONS.filter(o => o.value)} value={form.category}
              onChange={e => setForm({ ...form, category: e.value })} placeholder="选择分类" />
          </div>
          <div className="field">
            <label>描述</label>
            <InputTextarea value={form.description || ''}
              onChange={e => setForm({ ...form, description: e.target.value })} rows={3} placeholder="模板描述" />
          </div>
          <div className="field">
            <label>布局 JSON</label>
            <InputTextarea value={form.layoutJson || ''}
              onChange={e => setForm({ ...form, layoutJson: e.target.value })} rows={6} placeholder={`{\n  \"version\": 1,\n  \"widgets\": []\n}`} />
          </div>
          <div className="field field-row">
            <Checkbox inputId="isPublic" checked={form.isPublic ?? false}
              onChange={e => setForm({ ...form, isPublic: e.checked ?? false })} />
            <label htmlFor="isPublic">公开模板（其他用户可见）</label>
          </div>
        </div>
      </Dialog>

      <Dialog
        header={`应用模板 - ${applyingTemplate?.name || ''}`}
        visible={applyDialogVisible}
        style={{ width: '450px' }}
        footer={applyDialogFooter}
        onHide={() => setApplyDialogVisible(false)}
      >
        <div className="template-form">
          <div className="field">
            <label>报表名称 *</label>
            <InputText value={reportName} onChange={e => setReportName(e.target.value)} placeholder="新报表名称" />
          </div>
        </div>
      </Dialog>
    </div>
  )
}
