import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Button } from 'primereact/button'
import { previewReport } from '../services/report'

export default function ReportPreview() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [html, setHtml] = useState<string>('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!id) return
    setLoading(true)
    previewReport(Number(id))
      .then(setHtml)
      .catch((err) => {
        setError(err.response?.data?.message || '加载失败')
      })
      .finally(() => setLoading(false))
  }, [id])

  if (loading) {
    return (
      <div style={{ height: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        加载中...
      </div>
    )
  }

  if (error) {
    return (
      <div style={{ height: '100vh', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 16 }}>
        <div style={{ color: '#ff4d4f' }}>{error}</div>
        <Button icon="pi pi-arrow-left" label="返回" onClick={() => navigate(-1)} />
      </div>
    )
  }

  return (
    <div style={{ height: '100vh', display: 'flex', flexDirection: 'column' }}>
      <div style={{
        padding: '12px 20px',
        background: '#fff',
        borderBottom: '1px solid #eee',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        position: 'sticky',
        top: 0,
        zIndex: 100,
      }}>
        <Button icon="pi pi-arrow-left" label="返回" text onClick={() => navigate(-1)} />
        <Button icon="pi pi-print" label="打印" text onClick={() => window.print()} />
      </div>
      <div style={{ flex: 1, overflow: 'auto', background: '#f5f5f5' }}>
        <iframe
          title="report-preview"
          srcDoc={html}
          style={{ width: '100%', height: '100%', border: 'none', display: 'block' }}
        />
      </div>
    </div>
  )
}
