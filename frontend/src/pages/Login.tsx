import React, { useState } from 'react'
import { InputText } from 'primereact/inputtext'
import { Password } from 'primereact/password'
import { Button } from 'primereact/button'
import { Card } from 'primereact/card'
import { Message } from 'primereact/message'
import './Login.scss'

function Login() {
  const [username, setUsername] = useState<string>('')
  const [password, setPassword] = useState<string>('')
  const [error, setError] = useState<string>('')

  const handleLogin = () => {
    if (!username || !password) {
      setError('请输入用户名和密码')
      return
    }
    console.log('Login:', username)
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
            label="登录"
            icon="pi pi-sign-in"
            className="login-button"
            onClick={handleLogin}
          />
        </div>
      </Card>
    </div>
  )
}

export default Login
