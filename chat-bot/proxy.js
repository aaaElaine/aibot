// Simple HTTP proxy to bypass SSL verification for DashScope API
const https = require('https');
const http = require('http');
const { URL } = require('url');

const DASHSCOPE_BASE = 'https://dashscope.aliyuncs.com';
const PORT = process.argv[2] || 8081;

const server = http.createServer((req, res) => {
    const targetUrl = new URL(req.url, DASHSCOPE_BASE);
    
    console.log(`[PROXY] ${req.method} ${req.url} -> ${targetUrl.href}`);

    // Read request body
    let body = [];
    req.on('data', chunk => body.push(chunk));
    req.on('end', () => {
        const bodyData = Buffer.concat(body);

        // Build headers (exclude hop-by-hop headers)
        const headers = {};
        for (const [key, value] of Object.entries(req.headers)) {
            if (!['host', 'connection', 'transfer-encoding'].includes(key.toLowerCase())) {
                headers[key] = value;
            }
        }

        const options = {
            method: req.method,
            headers,
            rejectUnauthorized: false,
            timeout: 30000
        };

        const proxyReq = https.request(targetUrl, options, (proxyRes) => {
            res.writeHead(proxyRes.statusCode, proxyRes.headers);
            proxyRes.pipe(res);
        });

        proxyReq.on('error', (err) => {
            console.error(`[PROXY] Error: ${err.message}`);
            res.writeHead(502, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ error: err.message }));
        });

        proxyReq.on('timeout', () => {
            proxyReq.destroy();
        });

        if (bodyData.length > 0) {
            proxyReq.write(bodyData);
        }
        proxyReq.end();
    });
});

server.listen(PORT, '0.0.0.0', () => {
    console.log(`[PROXY] Server running on port ${PORT} (no SSL verify)`);
});
