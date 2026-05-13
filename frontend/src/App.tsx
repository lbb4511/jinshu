import { Routes, Route } from 'react-router-dom'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'

/**
 * 应用根组件
 *
 * 配置路由规则：
 * - /login: 登录页面
 * - /: 仪表盘（首页）
 *
 * 后续可扩展路由：
 * - /reports: 报表列表
 * - /reports/:id: 报表详情
 * - /tasks: 任务中心
 * - /datasources: 数据源管理
 * - /audit: 审计日志
 */
function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/" element={<Dashboard />} />
    </Routes>
  )
}

export default App
