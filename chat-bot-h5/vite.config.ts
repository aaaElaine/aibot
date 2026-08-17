import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import Components from 'unplugin-vue-components/vite'
import { VantResolver } from 'unplugin-vue-components/resolvers'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    Components({
      resolvers: [VantResolver()]
    })
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 5173,
    host: '127.0.0.1',
    allowedHosts: ['h5.example.com'],
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8081',
        changeOrigin: true,
        timeout: 60000,
        proxyTimeout: 60000
      },
      '/ai': {
        target: 'http://127.0.0.1:8081',
        changeOrigin: true,
        ws: true,
        timeout: 60000,
        proxyTimeout: 60000,
        configure: (proxy, options) => {
          // 禁用压缩，避免缓冲问题
          proxy.on('proxyReq', (proxyReq, req, res) => {
            proxyReq.setHeader('Accept-Encoding', 'identity')
          })
          proxy.on('proxyRes', (proxyRes, req, res) => {
            // 禁用所有代理响应的缓冲
            res.setHeader('Cache-Control', 'no-cache, no-transform')
            res.setHeader('X-Accel-Buffering', 'no')
            
            if (proxyRes.headers['content-type']?.includes('text/event-stream')) {
              res.setHeader('Connection', 'keep-alive')
            }
          })
        }
      }
    }
  }
})
