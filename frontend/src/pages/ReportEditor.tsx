import { useState, useEffect, useRef, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Card } from 'primereact/card'
import { Button } from 'primereact/button'
import { InputText } from 'primereact/inputtext'
import { InputTextarea } from 'primereact/inputtextarea'
import { Dropdown } from 'primereact/dropdown'
import { Toast } from 'primereact/toast'
import { getReport, createReport, updateReport } from '../services/report'
import { listDataSources } from '../services/datasource'
import type { Report, DataSource, ReportCreateRequest } from '../types'
import './ReportEditor.scss'

const emptyForm: ReportCreateRequest = {
  name: '',
  description: '',
  dataSourceId: undefined,
  templateConfig: '',
}

export default function ReportEditor() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const toast = useRef<Toast>(null)

  const isEdit = Boolean(id && id !== 'new')
  const reportId = isEdit ? Number(id) : undefined

  const [form, setForm] = useState<ReportCreateRequest>({ ...emptyForm })
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [dataSources, setDataSources] = useState<DataSource[]>([])
  const [dsLoading, setDsLoading] = useState(false)

  const loadDataSources = useCallback(async () => {
    setDsLoading(true)
    try {
      const result = await listDataSources({ page: 1, pageSize: 1000 })
      setDataSources(result.list)
    } catch {
      toast.current?.show({ severity: 'warn', summary: '提示', detail: '数据源列表加载失败' })
    } finally {
      setDsLoading(false)
    }
  }, [])

  const loadReport = useCallback(async () => {
    if (!reportId) return
    setLoading(true)
    try {
      const report: Report = await getReport(reportId)
      if (report.status !== 'DRAFT' && report.status !== 'REJECTED') {
        toast.current?.show({
          severity: 'warn',
          summary: '不可编辑',
          detail: '当前报表状态不允许编辑，仅草稿或被拒绝的报表可修改',
        })
        navigate(`/reports/${reportId}`)
        return
      }
      setForm({
        name: report.name || '',
        description: report.description || '',
        dataSourceId: report.dataSourceId,
        templateConfig: report.templateConfig || '',
      })
    } catch {
      toast.current?.show({ severity: 'error', summary: '加载失败', detail: '无法获取报表信息' })
      navigate('/reports')
    } finally {
      setLoading(false)
    }
  }, [reportId, navigate])

  useEffect(() => {
    loadDataSources()
    if (isEdit) {
      loadReport()
    } else {
      setForm({ ...emptyForm })
    }
  }, [isEdit, loadDataSources, loadReport])

  const updateField = <K extends keyof ReportCreateRequest>(field: K, value: ReportCreateRequest[K]) => {
    setForm(prev => ({ ...prev, [field]: value }))
  }

  const validate = (): boolean => {
    if (!form.name.trim()) {
      toast.current?.show({ severity: 'warn', summary: '表单不完整', detail: '请填写报表名称' })
      return false
    }
    if (form.templateConfig && form.templateConfig.trim()) {
      try {
        JSON.parse(form.templateConfig)
      } catch {
        toast.current?.show({ severity: 'warn', summary: '格式错误', detail: '模板配置不是有效的 JSON' })
        return false
      }
    }
    return true
  }

  const handleSave = async () => {
    if (!validate()) return
    setSaving(true)
    try {
      const payload: ReportCreateRequest = {
        ...form,
        description: form.description || undefined,
        templateConfig: form.templateConfig?.trim() || undefined,
      }
      if (isEdit && reportId) {
        await updateReport(reportId, payload)
        toast.current?.show({ severity: 'success', summary: '已更新' })
        navigate(`/reports/${reportId}`)
      } else {
        const created = await createReport(payload)
        toast.current?.show({ severity: 'success', summary: '已创建' })
        navigate(`/reports/${created.id}`)
      }
    } catch (err: any) {
      const message = err.response?.data?.message || (isEdit ? '更新失败' : '创建失败')
      toast.current?.show({ severity: 'error', summary: '保存失败', detail: message })
    } finally {
      setSaving(false)
    }
  }

  const handleCancel = () => {
    if (isEdit && reportId) {
      navigate(`/reports/${reportId}`)
    } else {
      navigate('/reports')
    }
  }

  const dsOptions = dataSources.map(ds => ({ label: ds.name, value: ds.id }))

  return (
    <div className="report-editor-page">
      <Toast ref={toast} />

      <div className="editor-header">
        <div className="editor-header-left">
          <Button icon="pi pi-arrow-left" text rounded onClick={handleCancel} tooltip="返回" />
          <h2 className="editor-title">{isEdit ? '编辑报表' : '新建报表'}</h2>
        </div>
        <div className="editor-header-actions">
          <Button label="取消" icon="pi pi-times" text onClick={handleCancel} />
          <Button label={saving ? '保存中...' : '保存'} icon="pi pi-check" onClick={handleSave} loading={saving} />
        </div>
      </div>

      <Card className="editor-card" title="基本信息">
        {loading ? (
          <div className="editor-loading">加载中...</div>
        ) : (
          <div className="editor-form">
            <div className="field">
              <label>报表名称 *</label>
              <InputText
                value={form.name}
                onChange={e => updateField('name', e.target.value)}
                placeholder="请输入报表名称"
                disabled={saving}
              />
            </div>
            <div className="field">
              <label>描述</label>
              <InputTextarea
                value={form.description || ''}
                onChange={e => updateField('description', e.target.value)}
                placeholder="请输入报表描述"
                rows={3}
                disabled={saving}
              />
            </div>
            <div className="field">
              <label>数据源</label>
              <Dropdown
                options={dsOptions}
                value={form.dataSourceId}
                onChange={e => updateField('dataSourceId', e.value)}
                placeholder={dsLoading ? '加载中...' : '请选择数据源'}
                loading={dsLoading}
                showClear
                disabled={saving}
                style={{ width: '100%' }}
              />
            </div>
            <div className="field">
              <label>模板配置（JSON）</label>
              <InputTextarea
                value={form.templateConfig || ''}
                onChange={e => updateField('templateConfig', e.target.value)}
                placeholder={`{\n  "columns": []\n}`}
                rows={10}
                disabled={saving}
              />
              <small className="field-hint">留空表示暂不配置；填写后须为合法 JSON。</small>
            </div>
          </div>
        )}
      </Card>
    </div>
  )
}
