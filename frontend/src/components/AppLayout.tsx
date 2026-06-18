import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { Menubar } from 'primereact/menubar'
import { Button } from 'primereact/button'
import { Avatar } from 'primereact/avatar'
import { ConfirmDialog } from 'primereact/confirmdialog'
import type { MenuItem } from 'primereact/menuitem'
import { useAuth } from '../store/useAuth'
import './AppLayout.scss'

export default function AppLayout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const isActive = (path: string) => {
    if (path === '/') return location.pathname === '/'
    return location.pathname.startsWith(path)
  }

  const items: MenuItem[] = [
    {
      label: '仪表盘',
      icon: 'pi pi-home',
      command: () => navigate('/'),
      className: isActive('/') ? 'p-menuitem-active' : '',
    },
    {
      label: '报表管理',
      icon: 'pi pi-file',
      className: isActive('/reports') ? 'p-menuitem-active' : '',
      items: [
        {
          label: '报表列表',
          icon: 'pi pi-list',
          command: () => navigate('/reports'),
        },
        {
          label: '新建报表',
          icon: 'pi pi-plus',
          command: () => navigate('/reports/new'),
        },
      ],
    },
    {
      label: '数据源',
      icon: 'pi pi-database',
      command: () => navigate('/datasources'),
      className: isActive('/datasources') ? 'p-menuitem-active' : '',
    },
    {
      label: '任务中心',
      icon: 'pi pi-tasks',
      command: () => navigate('/tasks'),
      className: isActive('/tasks') ? 'p-menuitem-active' : '',
    },
    {
      label: '用户管理',
      icon: 'pi pi-users',
      command: () => navigate('/users'),
      className: isActive('/users') ? 'p-menuitem-active' : '',
    },
    {
      label: '审计日志',
      icon: 'pi pi-history',
      command: () => navigate('/audit'),
      className: isActive('/audit') ? 'p-menuitem-active' : '',
    },
    ...(user?.role === 'ADMIN'
      ? [{
          label: '租户管理',
          icon: 'pi pi-building',
          command: () => navigate('/tenants'),
          className: isActive('/tenants') ? 'p-menuitem-active' : '',
        }]
      : []),
  ]

  const end = (
    <div className="app-layout-user">
      <Avatar
        label={user?.username?.charAt(0).toUpperCase()}
        shape="circle"
        size="normal"
        className="p-mr-2"
      />
      <span className="app-layout-username">{user?.username || '用户'}</span>
      <Button
        label="退出"
        icon="pi pi-sign-out"
        className="p-button-text"
        onClick={async () => {
          await logout()
          navigate('/login')
        }}
      />
    </div>
  )

  return (
    <div className="app-layout">
      <ConfirmDialog />
      <Menubar model={items} end={end} className="app-layout-header" />
      <div className="app-layout-content">
        <Outlet />
      </div>
    </div>
  )
}
