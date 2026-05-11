import React from 'react'
import { Menubar } from 'primereact/menubar'
import { Button } from 'primereact/button'
import { MenuItem } from 'primereact/menuitem'
import './Dashboard.scss'

function Dashboard() {
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

  const end = <Button label="退出" icon="pi pi-sign-out" className="p-button-text" />

  return (
    <div className="dashboard">
      <Menubar model={items} end={end} className="dashboard-header" />
      <div className="dashboard-content">
        <h1>欢迎使用锦书企业级报表系统</h1>
        <p>高性能 · 多租户 · 安全合规</p>
      </div>
    </div>
  )
}

export default Dashboard
