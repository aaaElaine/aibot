// API 基础 URL - 使用 Vite 代理时为空字符串，直接使用相对路径
// new URL() 需要有效的 base URL，所以使用 window.location.origin 作为基础
const getBaseUrl = () => {
  // 如果需要直接连接后端（绕过代理），可以返回 'http://127.0.0.1:8081'
  return window.location.origin
}

// 商品卡片接口
export interface ProductCard {
  id: number
  name: string
  description: string
  price: string
  originalPrice?: string
  imageUrl?: string
  category?: string
  productUrl?: string
  salesCount?: number
}

// 工具结果接口
export interface ToolResult {
  type: 'weather' | 'news'
  data?: any
  formatted?: string
  scope?: string
}

// 聊天消息接口
export interface ChatMessage {
  id?: string
  role: 'user' | 'assistant'
  content: string
  products?: ProductCard[]
  toolResult?: ToolResult
  timestamp?: number
}

export interface ChatRequest {
  message: string
  conversationId?: string
  history?: ChatMessage[]
}

// AI响应接口
export interface AiResponse {
  text: string
  products: ProductCard[]
  toolResult?: ToolResult
}

// 同步对话（带商品卡片）
export async function syncChat(
  message: string,
  onMessage: (text: string, products?: ProductCard[]) => void
) {
  const token = localStorage.getItem('token')
  
  const url = new URL('/ai/assistant_app/chat/with-products', getBaseUrl())
  url.searchParams.append('message', message)
  if (token) {
    url.searchParams.append('token', token)
  }

  const response = await fetch(url.toString(), {
    method: 'GET',
    headers: {
      'Accept': 'application/json'
    }
  })

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`)
  }

  const result = await response.json()
  // 处理响应格式：Result<ChatResponseDTO>
  if (result.code === 200 || result.code === 0) {
    const data = result.data || result
    if (data.text) {
      onMessage(data.text, data.products || [])
    } else {
      onMessage(data.toString(), [])
    }
  } else if (typeof result === 'string') {
    onMessage(result, [])
  } else {
    const text = typeof result === 'object' ? (result.text || result.data || '') : result
    const products = result.products || []
    onMessage(text, products)
  }
}

// 流式对话 - 使用带工具的接口（天气/新闻 + 商品推荐）
export async function streamChatWithProducts(
  message: string,
  onChunk: (text: string) => void,
  onDone: (products?: ProductCard[], toolResult?: ToolResult) => void,
  signal?: AbortSignal
) {
  const token = localStorage.getItem('token')
  const chatId = getSessionChatId()

  // 如果被中止，直接返回
  if (signal?.aborted) {
    return
  }

  const url = new URL('/ai/assistant_app/chat/with-tools', getBaseUrl())
  url.searchParams.append('message', message)
  url.searchParams.append('chatId', chatId)
  if (token) {
    url.searchParams.append('token', token)
  }
  try {
    // 创建带超时的请求
    const controller = new AbortController()
    const timeoutId = setTimeout(() => {
      controller.abort()
    }, 30000) // 30秒超时
    
    // 如果外部 signal 被中止，也中止内部请求
    if (signal) {
      signal.addEventListener('abort', () => controller.abort(), { once: true })
    }

    const response = await fetch(url.toString(), { 
      method: 'GET',
      headers: {
        'Accept': 'application/json'
      },
      signal: controller.signal
    })

    clearTimeout(timeoutId)

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }

    const result = await response.json()
    
    if (result.code === 200 || result.code === 0) {
      const data = result.data || result
      if (data.text) {
        onChunk(data.text)
      } else {
        onChunk('抱歉，我暂时无法回答您的问题，请稍后再试~')
      }
      onDone(data.products || [], data.toolResult || undefined)
    } else {
      onChunk('抱歉，我暂时无法回答您的问题，请稍后再试~')
      onDone([])
    }
  } catch (error: any) {
    // 如果被用户中止或超时
    if (signal?.aborted || error?.name === 'AbortError') {
      if (error?.name === 'AbortError' && !signal?.aborted) {
        // 超时导致的中止
        onChunk('抱歉，回复超时了，请稍后再试~')
        onDone([])
      }
      return
    }
    
    console.warn('请求失败:', error?.message)
    // 区分不同错误类型，给出更友好的提示
    if (error?.message?.includes('Failed to fetch') || error?.message?.includes('NetworkError') || error?.message?.includes('Load failed')) {
      onChunk('抱歉，网络连接不稳定，请检查网络后重试~')
    } else if (error?.message?.includes('HTTP')) {
      onChunk('抱歉，服务暂时不可用，请稍后再试~')
    } else {
      onChunk('抱歉，我理解了您的意思，但暂时无法回复，请换种方式问我哦~')
    }
    onDone([])
  }
}

// 获取商品推荐
async function fetchProductsForMessage(message: string, signal?: AbortSignal): Promise<ProductCard[]> {
  const token = localStorage.getItem('token')
  const url = new URL('/api/products/search', getBaseUrl())
  url.searchParams.append('keyword', message)
  url.searchParams.append('limit', '3')
  if (token) {
    url.searchParams.append('token', token)
  }

  const response = await fetch(url.toString(), {
    method: 'GET',
    headers: {
      'Accept': 'application/json'
    },
    signal
  })

  if (!response.ok) return []

  const result = await response.json()
  if (result.code === 200 || result.code === 0) {
    return (result.data || []).map((p: any) => ({
      id: p.id,
      name: p.name,
      description: p.description,
      price: p.price?.toString(),
      originalPrice: p.originalPrice?.toString(),
      imageUrl: p.imageUrl,
      category: p.category,
      productUrl: p.productUrl
    }))
  }
  return []
}

// SSE 流式对话（保留旧版接口兼容）
export function streamChat(message: string, onMessage: (text: string) => void, onError?: (error: Error) => void) {
  const token = localStorage.getItem('token')
  
  const url = new URL('/ai/assistant_app/chat/sse', window.location.origin)
  url.searchParams.append('message', message)
  if (token) {
    url.searchParams.append('token', token)
  }

  const eventSource = new EventSource(url.toString())
  
  eventSource.onmessage = (event) => {
    const data = event.data
    if (data === '[DONE]') {
      eventSource.close()
      return
    }
    try {
      const parsed = JSON.parse(data)
      if (parsed.content) {
        onMessage(parsed.content)
      }
    } catch {
      onMessage(data)
    }
  }

  eventSource.onerror = (error) => {
    eventSource.close()
    if (onError) {
      onError(new Error('SSE connection error'))
    }
  }

  return () => {
    eventSource.close()
  }
}

// 使用 fetch API 处理 SSE 流式响应
export async function fetchStreamChat(
  message: string,
  onMessage: (text: string) => void,
  onError?: (error: Error) => void
) {
  const token = localStorage.getItem('token')
  
  try {
    const url = new URL('/ai/assistant_app/chat/sse', window.location.origin)
    url.searchParams.append('message', message)
    if (token) {
      url.searchParams.append('token', token)
    }

    const response = await fetch(url.toString(), {
      method: 'GET',
      headers: {
        'Accept': 'text/event-stream'
      }
    })

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    const reader = response.body?.getReader()
    const decoder = new TextDecoder()

    if (!reader) {
      throw new Error('Response body is null')
    }

    let buffer = ''
    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (line.startsWith('data:')) {
          const data = line.slice(5).trim()
          if (data === '[DONE]') {
            return
          }
          if (data) {
            try {
              const parsed = JSON.parse(data)
              if (parsed.content) {
                onMessage(parsed.content)
              }
            } catch {
              onMessage(data)
            }
          }
        }
      }
    }
    
    if (!buffer) {
      await syncChat(message, (text) => onMessage(text))
    }
  } catch (error) {
    await syncChat(message, (text) => onMessage(text))
  }
}

// 生成唯一 ID
export function generateId(): string {
  return Date.now().toString(36) + Math.random().toString(36).substr(2)
}

// 获取当前会话的 chatId（持久化到 localStorage，整个会话周期内不变）
export function getSessionChatId(): string {
  let chatId = localStorage.getItem('chat_session_id')
  if (!chatId) {
    chatId = 'chat_' + Date.now() + '_' + Math.random().toString(36).substr(2, 6)
    localStorage.setItem('chat_session_id', chatId)
  }
  return chatId
}

// 重置会话（新对话时调用）
export function resetSessionChatId(): void {
  localStorage.removeItem('chat_session_id')
}
