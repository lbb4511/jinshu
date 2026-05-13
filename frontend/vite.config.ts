import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

/**
 * Vite配置文件
 *
 * 代理配置：
 * - /api -> http://localhost:8080/api（后端服务）
 *
 * 开发说明：
 * - 前端运行在 http://localhost:3000
 * - 后端请求通过代理转发，避免CORS问题
 */
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: false
  }
})
