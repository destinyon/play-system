// 高德地图代理服务器 - Node.js 版本
// 用于开发环境，解决跨域和安全码注入问题

const express = require('express');
const { createProxyMiddleware } = require('http-proxy-middleware');
const cors = require('cors');

const app = express();
const PORT = 80;

// 高德地图安全码
const AMAP_SECURITY_JSCODE = '3b0d644aa49fcb13d08e0693933c6767';

// 启用 CORS
app.use(cors({
  origin: '*',
  methods: ['GET', 'POST', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization']
}));

// 首页
app.get('/', (req, res) => {
  res.send(`
    <html>
      <head>
        <title>AMap Proxy Server</title>
        <style>
          body { font-family: system-ui; padding: 40px; max-width: 800px; margin: 0 auto; }
          h1 { color: #667eea; }
          code { background: #f3f4f6; padding: 4px 8px; border-radius: 4px; }
          .success { color: #10b981; font-weight: 600; }
        </style>
      </head>
      <body>
        <h1>✅ AMap Proxy Server is Running</h1>
        <p class="success">Proxy server is running on port ${PORT}</p>
        <h2>Configuration</h2>
        <ul>
          <li>Endpoint: <code>/_AMapService/</code></li>
          <li>Target: <code>https://restapi.amap.com</code></li>
          <li>Security JsCode: <code>${AMAP_SECURITY_JSCODE}</code></li>
        </ul>
        <h2>Test API</h2>
        <p>Try: <a href="/_AMapService/v3/config/district?keywords=北京" target="_blank">/_AMapService/v3/config/district?keywords=北京</a></p>
      </body>
    </html>
  `);
});

// 高德地图 API 代理
app.use('/_AMapService', createProxyMiddleware({
  target: 'https://restapi.amap.com',
  changeOrigin: true,
  pathRewrite: {
    '^/_AMapService': '', // 移除路径前缀
  },
  onProxyReq: (proxyReq, req, res) => {
    // 注入安全码到查询参数
    const url = new URL(req.url, 'https://restapi.amap.com');
    if (!url.searchParams.has('jscode')) {
      url.searchParams.append('jscode', AMAP_SECURITY_JSCODE);
    }
    
    // 更新代理请求路径
    proxyReq.path = url.pathname + url.search;
    
    console.log(`[PROXY] ${req.method} ${req.url} -> ${proxyReq.path}`);
  },
  onProxyRes: (proxyRes, req, res) => {
    // 添加 CORS 头
    proxyRes.headers['Access-Control-Allow-Origin'] = '*';
    proxyRes.headers['Access-Control-Allow-Methods'] = 'GET, POST, OPTIONS';
    proxyRes.headers['Access-Control-Allow-Headers'] = 'Content-Type, Authorization';
  },
  onError: (err, req, res) => {
    console.error(`[PROXY ERROR] ${err.message}`);
    res.status(502).json({
      error: 'Proxy Error',
      message: err.message,
      timestamp: new Date().toISOString()
    });
  },
  logLevel: 'debug',
}));

// 启动服务器
app.listen(PORT, () => {
  console.log(`
╔════════════════════════════════════════════════╗
║  🚀 AMap Proxy Server Started!                ║
╠════════════════════════════════════════════════╣
║  📍 Address:  http://localhost:${PORT.toString().padEnd(21)}  ║
║  🔗 Endpoint: /_AMapService/                   ║
║  🎯 Target:   https://restapi.amap.com         ║
║  🔒 Security: ${AMAP_SECURITY_JSCODE}  ║
╠════════════════════════════════════════════════╣
║  💡 Test:                                      ║
║     http://localhost/_AMapService/v3/config... ║
║                                                ║
║  ⚠️  Make sure .env.local has:                 ║
║     VITE_AMAP_PROXY_HOST=http://localhost      ║
╚════════════════════════════════════════════════╝
  `);
});

// 优雅关闭
process.on('SIGINT', () => {
  console.log('\n\n🛑 Shutting down proxy server...');
  process.exit(0);
});

process.on('SIGTERM', () => {
  console.log('\n\n🛑 Shutting down proxy server...');
  process.exit(0);
});
