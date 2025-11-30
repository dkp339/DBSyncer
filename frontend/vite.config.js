import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173, // 前端运行端口
    proxy: {
      // 代理配置：凡是 '/api' 开头的请求，都转发给后端
      '/api': {
        target: 'http://localhost:8080', // Spring Boot 后端地址
        changeOrigin: true,
        // rewrite: (path) => path.replace(/^\/api/, '') // 如果你后端接口本身包含 /api，则不需要这行
      }
    }
  }
})