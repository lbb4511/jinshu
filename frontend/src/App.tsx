import { Routes, Route } from 'react-router-dom'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import ReportList from './pages/ReportList'
import ReportEditor from './pages/ReportEditor'
import ReportDetail from './pages/ReportDetail'
import ReportPreview from './pages/ReportPreview'
import TaskCenter from './pages/TaskCenter'
import DataSourceList from './pages/DataSourceList'
import UserList from './pages/UserList'
import AuditLogViewer from './pages/AuditLogViewer'
import TenantList from './pages/TenantList'
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
        <Route path="/reports/new" element={<ReportEditor />} />
        <Route path="/reports/:id/edit" element={<ReportEditor />} />
        <Route path="/reports/:id" element={<ReportDetail />} />
        <Route path="/tasks" element={<TaskCenter />} />
        <Route path="/datasources" element={<DataSourceList />} />
        <Route path="/users" element={<UserList />} />
        <Route path="/audit" element={<AuditLogViewer />} />
        <Route path="/tenants" element={<TenantList />} />
      </Route>
      <Route path="/reports/:id/preview" element={
        <AuthGuard>
          <ReportPreview />
        </AuthGuard>
      } />
    </Routes>
  )
}
