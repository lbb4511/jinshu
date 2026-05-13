import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './App'
import './assets/styles/global.scss'
import 'primereact/resources/themes/lara-light-blue/theme.css'
import 'primereact/resources/primereact.min.css'
import 'primeicons/primeicons.css'

/**
 * 锦书企业级报表系统 - 前端应用入口
 *
 * 技术栈：
 * - React 19
 * - TypeScript
 * - Vite
 * - PrimeReact（UI组件库）
 * - React Router（路由）
 * - Axios（HTTP客户端）
 */
ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </React.StrictMode>,
)
