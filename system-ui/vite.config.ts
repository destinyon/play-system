import { fileURLToPath, URL } from 'node:url'

import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const securityJsCode = env.VITE_AMAP_SECURITY_JSCODE || '3b0d644aa49fcb13d08e0693933c6767'

  return {
    plugins: [vue(), vueDevTools()],
    resolve: {
      alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
    },
    define: {
      global: 'globalThis',
    },
    server: {
      proxy: {
        // Backend API proxy
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
        '/ws': {
          target: 'http://localhost:8080',
          changeOrigin: true,
          ws: true,
        },
        '/uploads': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
        // Map styles (optional; if you use custom styles)
        '/_AMapService/v4/map/styles': {
          target: 'https://webapi.amap.com',
          changeOrigin: true,
          secure: true,
          rewrite: (path) => path.replace(/^\/_AMapService/, ''),
          configure: (proxy) => {
            proxy.on('proxyReq', (proxyReq) => {
              try {
                const url = new URL((proxyReq as any).path, 'http://amap')
                url.searchParams.set('jscode', securityJsCode)
                ;(proxyReq as any).path = url.pathname + url.search
              } catch {}
            })
          },
        },
        // REST API
        '/_AMapService': {
          target: 'https://restapi.amap.com',
          changeOrigin: true,
          secure: true,
          rewrite: (path) => path.replace(/^\/_AMapService/, ''),
          configure: (proxy) => {
            proxy.on('proxyReq', (proxyReq) => {
              try {
                const url = new URL((proxyReq as any).path, 'http://amap')
                url.searchParams.set('jscode', securityJsCode)
                ;(proxyReq as any).path = url.pathname + url.search
              } catch {}
            })
          },
        },
      },
    },
  }
})
