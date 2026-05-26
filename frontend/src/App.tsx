import { Routes, Route } from 'react-router-dom'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import ReportList from './pages/ReportList'
import ReportDetail from './pages/ReportDetail'
import TaskCenter from './pages/TaskCenter'
import DataSourceList from './pages/DataSourceList'
import AuditLogViewer from './pages/AuditLogViewer'
import AuthGuard from './components/AuthGuard'
import AppLayout from './components/AppLayout'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route
        element={
          <AuthGuard>
            <AppLayout />
          </AuthGuard>
        }
      >
        <Route path="/" element={<Dashboard />} />
        <Route path="/reports" element={<ReportList />} />
        <Route path="/reports/:id" element={<ReportDetail />} />
        <Route path="/tasks" element={<TaskCenter />} />
        <Route path="/datasources" element={<DataSourceList />} />
        <Route path="/audit" element={<AuditLogViewer />} />
      </Route>
    </Routes>
  )
}
