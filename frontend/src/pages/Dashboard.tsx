import { useEffect, useState } from 'react'
import { Menubar } from 'primereact/menubar'
import { Button } from 'primereact/button'
import { MenuItem } from 'primereact/menuitem'
import { Card } from 'primereact/card'
import { Tag } from 'primereact/tag'
import './Dashboard.scss'
import { checkHealth } from '../services/health'

/**
 * 健康检查数据结构
 */
interface HealthData {
  status: string
  service: string
  database?: boolean | string
}

/**
 * 仪表盘页面
 *
 * 功能说明：
 * - 显示系统导航菜单
 * - 展示系统健康状态（后端、数据库）
 * - 定期刷新健康检查（30秒）
 *
 * 菜单模块：
 * - 报表管理：报表列表、新建报表
 * - 数据源：数据源列表、添加数据源
 * - 任务中心：导入导出任务
 * - 审计日志：操作审计
 */
function Dashboard() {
  /**
   * 健康检查数据
   */
  const [health, setHealth] = useState<HealthData | null>(null)

  /**
   * 加载状态
   */
  const [loading, setLoading] = useState(true)

  /**
   * 页面加载时获取健康检查，定时刷新
   */
  useEffect(() => {
    const loadHealth = async () => {
      try {
        const result = await checkHealth()
        setHealth(result.data)
      } catch (error) {
        console.error('健康检查失败:', error)
      } finally {
        setLoading(false)
      }
    }

    loadHealth()
    // 每30秒刷新一次健康检查
    const interval = setInterval(loadHealth, 30000)
    return () => clearInterval(interval)
  }, [])

  /**
   * 导航菜单项配置
   */
  const items: MenuItem[] = [
    {
      label: '报表管理',
      icon: 'pi pi-file',
      items: [
        { label: '报表列表', icon: 'pi pi-list' },
        { label: '新建报表', icon: 'pi pi-plus' },
      ]
    },
    {
      label: '数据源',
      icon: 'pi pi-database',
      items: [
        { label: '数据源列表', icon: 'pi pi-list' },
        { label: '添加数据源', icon: 'pi pi-plus' },
      ]
    },
    {
      label: '任务中心',
      icon: 'pi pi-tasks',
    },
    {
      label: '审计日志',
      icon: 'pi pi-history',
    }
  ]

  /**
   * 右侧退出按钮
   */
  const end = <Button label="退出" icon="pi pi-sign-out" className="p-button-text" />

  /**
   * 获取状态颜色
   *
   * @param status 状态值
   * @returns PrimeReact Tag的severity
   */
  const getSeverity = (status?: string | boolean) => {
    if (typeof status === 'boolean') {
      return status ? 'success' : 'danger'
    }
    if (!status) return 'warning' as const
    return status.toUpperCase() === 'UP' ? 'success' : 'danger'
  }

  /**
   * 获取状态文本
   *
   * @param status 状态值
   * @returns 状态文本
   */
  const getStatusText = (status?: string | boolean) => {
    if (typeof status === 'boolean') {
      return status ? 'UP' : 'DOWN'
    }
    return status || '未知'
  }

  return (
    <div className="dashboard">
      <Menubar model={items} end={end} className="dashboard-header" />

      <div className="dashboard-content">
        <div className="welcome-section">
          <h1>欢迎使用锦书企业级报表系统</h1>
          <p>高性能 · 多租户 · 安全合规</p>
        </div>

        <div className="health-section">
          <Card title="系统状态">
            {loading ? (
              <div className="loading-status">检查中...</div>
            ) : health ? (
              <div className="health-status">
                <div className="status-item">
                  <span className="status-label">后端服务</span>
                  <Tag value={health.status} severity={getSeverity(health.status)} />
                </div>
                <div className="status-item">
                  <span className="status-label">数据库</span>
                  <Tag value={getStatusText(health.database)} severity={getSeverity(health.database)} />
                </div>
              </div>
            ) : (
              <div className="error-status">无法连接后端</div>
            )}
          </Card>
        </div>
      </div>
    </div>
  )
}

export default Dashboard
