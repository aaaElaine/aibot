# -*- coding: utf-8 -*-
"""
简单的 HTTP 代理服务器，用于绕过 SSL 证书验证问题
使用 requests 库，禁用环境代理
"""
import http.server
import json
import sys
import urllib3

# 禁用 SSL 警告
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

try:
    import requests
except ImportError:
    print("Error: requests library not found. Install with: pip install requests")
    sys.exit(1)

DASHSCOPE_BASE_URL = "https://dashscope.aliyuncs.com"

# 创建一个禁用环境代理的 Session
session = requests.Session()
session.trust_env = False  # 禁用环境代理
session.verify = False  # 禁用 SSL 验证

class ProxyHandler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        print(f"[PROXY] GET {self.path}")
        self.proxy_request('GET')

    def do_POST(self):
        print(f"[PROXY] POST {self.path}")
        self.proxy_request('POST')

    def proxy_request(self, method):
        url = f"{DASHSCOPE_BASE_URL}{self.path}"
        
        content_length = int(self.headers.get('Content-Length', 0))
        body = self.rfile.read(content_length) if content_length > 0 else None
        
        # 构建请求头
        headers = {}
        for key, value in self.headers.items():
            if key.lower() not in ('host', 'connection', 'transfer-encoding'):
                headers[key] = value
        
        try:
            response = session.request(
                method=method,
                url=url,
                headers=headers,
                data=body,
                timeout=30
            )
            
            self.send_response(response.status_code)
            for key, value in response.headers.items():
                if key.lower() not in ('transfer-encoding', 'connection'):
                    self.send_header(key, value)
            self.end_headers()
            
            self.wfile.write(response.content)
            
        except requests.exceptions.HTTPError as e:
            print(f"[PROXY] HTTP Error: {e}")
            self.send_response(e.response.status_code)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            self.wfile.write(e.response.content)
            
        except Exception as e:
            print(f"[PROXY] Error: {e}")
            self.send_response(502)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            self.wfile.write(json.dumps({"error": str(e)}).encode())

if __name__ == '__main__':
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8081
    server = http.server.HTTPServer(('0.0.0.0', port), ProxyHandler)
    print(f"[PROXY] Server running on port {port} (no proxy, no SSL verify)")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\n[PROXY] Server stopped")
        server.server_close()
