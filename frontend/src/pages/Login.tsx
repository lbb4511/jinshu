import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { InputText } from 'primereact/inputtext'
import { Password } from 'primereact/password'
import { Button } from 'primereact/button'
import { Card } from 'primereact/card'
import { Message } from 'primereact/message'
import { useAuth } from '../store/useAuth'
import './Login.scss'

function Login() {
  const [username, setUsername] = useState<string>('')
  const [password, setPassword] = useState<string>('')
  const [error, setError] = useState<string>('')
  const [submitting, setSubmitting] = useState(false)
  const { login } = useAuth()
  const navigate = useNavigate()

  const handleLogin = async () => {
    if (!username || !password) {
      setError('请输入用户名和密码')
      return
    }
    setError('')
    setSubmitting(true)
    try {
      await login({ username, password })
      navigate('/', { replace: true })
    } catch (e: any) {
      const msg = e?.response?.data?.message || '登录失败，请检查用户名和密码'
      setError(msg)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="login-container">
      <Card title="锦书报表系统" className="login-card">
        <div className="login-form">
          {error && <Message severity="error" text={error} className="error-message" />}

          <div className="form-group">
            <label>用户名</label>
            <InputText
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="请输入用户名"
            />
          </div>

          <div className="form-group">
            <label>密码</label>
            <Password
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="请输入密码"
              toggleMask
              feedback={false}
            />
          </div>

          <Button
            label={submitting ? '登录中...' : '登录'}
            icon="pi pi-sign-in"
            className="login-button"
            disabled={submitting}
            onClick={handleLogin}
          />
        </div>
      </Card>
    </div>
  )
}

export default Login
