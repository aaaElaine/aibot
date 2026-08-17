<template>
  <div class="chat-container">
    <!-- 顶部导航栏 -->
    <van-nav-bar title="姜姜AI助手" fixed placeholder>
      <template #left>
        <van-icon name="service-o" size="20" />
      </template>
      <template #right>
        <van-icon name="delete-o" size="20" @click="clearHistory" />
      </template>
    </van-nav-bar>

    <!-- 消息列表区域 -->
    <div class="message-list" ref="messageListRef">
      <div class="message-wrapper" v-for="msg in messages" :key="msg.id">
        <!-- AI 消息 -->
        <div v-if="msg.role === 'assistant'" class="message-item assistant">
          <div class="avatar">
            <van-icon name="service" size="20" color="#fff" />
          </div>
          <div class="message-content">
            <!-- AI 文本内容：有卡片时只显示简短引导，避免重复 -->
            <div class="message-text" v-if="!msg.toolResult && !msg.products?.length" v-html="formatText(msg.content)"></div>
            
            <!-- 有工具结果或商品卡片时，显示简短引导语 -->
            <div class="message-text card-intro" v-if="msg.toolResult || msg.products?.length" v-html="formatCardIntro(msg)"></div>
            
            <!-- 思考中指示器 -->
            <div v-if="msg.content === 'AI 正在思考中...'" class="typing-indicator">
              <span></span><span></span><span></span>
            </div>
            
            <!-- 商品卡片推荐 -->
            <div v-if="msg.products && msg.products.length > 0" class="product-cards">
              <div class="product-cards-title">为您推荐以下商品：</div>
              <div 
                class="product-card" 
                v-for="product in msg.products" 
                :key="product.id"
                @click="goToProduct(product)"
              >
                <div class="product-info">
                  <div class="product-name">{{ product.name }}</div>
                  <div class="product-desc">{{ product.description }}</div>
                  <div class="product-price-row">
                    <span class="product-price">¥{{ product.price }}</span>
                    <span v-if="product.originalPrice" class="product-original-price">¥{{ product.originalPrice }}</span>
                    <span v-if="product.salesCount" class="product-sales">已售{{ product.salesCount }}</span>
                  </div>
                </div>
                <div class="product-action">
                  <van-button type="primary" size="mini" round>查看详情</van-button>
                </div>
              </div>
            </div>
            
            <!-- 工具结果展示：天气 -->
            <div v-if="msg.toolResult && msg.toolResult.type === 'weather'" class="tool-card weather-card">
              <div class="tool-card-header">
                <van-icon name="cloud-o" size="18" />
                <span>
                  <span v-if="msg.toolResult.data?.dateLabel">{{ msg.toolResult.data.dateLabel }}</span>
                  <span v-else>天气查询结果</span>
                </span>
              </div>
              <div class="weather-info" v-if="msg.toolResult.data">
                <div class="weather-main">
                  <span class="weather-city">{{ msg.toolResult.data.city }}</span>
                  <span class="weather-desc">{{ msg.toolResult.data.weather }}</span>
                  <span v-if="msg.toolResult.data.date" class="weather-date">{{ msg.toolResult.data.date }}</span>
                </div>
                <div class="weather-detail">
                  <span>🌡 {{ msg.toolResult.data.low }}℃ ~ {{ msg.toolResult.data.high }}℃</span>
                  <span>💨 {{ msg.toolResult.data.wind }}</span>
                  <span>💧 {{ msg.toolResult.data.humidity }}</span>
                  <span v-if="msg.toolResult.data.airQuality">🌿 {{ msg.toolResult.data.airQuality }}</span>
                </div>
                <div class="weather-tip" v-if="msg.toolResult.data.tip">{{ msg.toolResult.data.tip }}</div>
              </div>
            </div>

            <!-- 工具结果展示：新闻 -->
            <div v-if="msg.toolResult && msg.toolResult.type === 'news'" class="tool-card news-card">
              <div class="tool-card-header">
                <van-icon name="orders-o" size="18" />
                <span>{{ msg.toolResult.scope || '全国' }}新闻 Top{{ msg.toolResult.data?.length || 10 }}</span>
              </div>
              <div class="news-list" v-if="msg.toolResult.data">
                <div 
                  class="news-item" 
                  v-for="item in msg.toolResult.data" 
                  :key="item.rank"
                  @click="handleNewsClick(item)"
                >
                  <span class="news-rank">{{ item.rank }}</span>
                  <div class="news-content">
                    <div class="news-title" :title="item.title">{{ item.title }}</div>
                    <div class="news-summary" v-if="item.summary">{{ item.summary }}</div>
                    <div class="news-meta">
                      <span class="news-tag" v-if="item.tag">{{ item.tag }}</span>
                      <span class="news-time" v-if="item.time">{{ formatNewsTime(item.time) }}</span>
                    </div>
                  </div>
                  <van-icon v-if="item.url" name="arrow" class="news-arrow" />
                </div>
              </div>
            </div>
            
            <div class="message-time">{{ formatTime(msg.timestamp) }}</div>
          </div>
        </div>

        <!-- 用户消息 -->
        <div v-else class="message-item user">
          <div class="message-content">
            <div class="message-text">{{ msg.content }}</div>
            <div class="message-time">{{ formatTime(msg.timestamp) }}</div>
          </div>
          <div class="avatar user-avatar">
            <van-icon name="user-o" size="20" color="#fff" />
          </div>
        </div>
      </div>
    </div>

    <!-- 底部输入区域 -->
    <div class="input-area">
      <van-field
        v-model="inputMessage"
        placeholder="请输入您的问题..."
        type="textarea"
        rows="1"
        autosize
        maxlength="500"
        show-word-limit
        @keydown.enter.prevent="handleEnter"
      >
        <template #button>
          <transition name="fade">
            <!-- 发送按钮 -->
            <van-button
              v-if="!isStreaming"
              type="primary"
              size="small"
              :disabled="!inputMessage.trim() || isLoading"
              @click="sendMessage"
            >
              发送
            </van-button>
            <!-- 停止按钮 -->
            <div
              v-else
              class="stop-btn"
              :class="{ 'stop-btn--active': isStopAnimating }"
              @click="handleStopClick"
            >
              <van-icon name="pause" size="14" />
            </div>
          </transition>
        </template>
      </van-field>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { streamChatWithProducts, generateId, resetSessionChatId } from '@/api/chat'
import type { ChatMessage, ProductCard, ToolResult } from '@/api/chat'

const router = useRouter()
const messages = ref<ChatMessage[]>([])
const inputMessage = ref('')
const isLoading = ref(false)
// 流式输出中状态
const isStreaming = ref(false)
// 停止按钮动画状态
const isStopAnimating = ref(false)
// 消息列表容器引用
const messageListRef = ref<HTMLElement>()
// 中断控制器
let abortController: AbortController | null = null

// 初始化欢迎消息
onMounted(() => {
  loadHistory()

  if (messages.value.length === 0) {
    messages.value.push({
      id: generateId(),
      role: 'assistant',
      content: '您好呀～我是姜姜😊 这个平台的AI助手~\n\n我能回答的问题取决于平台维护的知识库内容。另外，我还支持：\n\n🌤 查询天气（说"北京天气"即可查询）\n📰 查看新闻资讯（说"最新新闻"即可浏览）\n\n有什么想了解的，尽管问我吧！',
      timestamp: Date.now()
    })
  }
})

onUnmounted(() => {
  if (abortController) {
    abortController.abort()
  }
})

// 加载历史消息
function loadHistory() {
  const history = localStorage.getItem('chatHistory')
  if (history) {
    try {
      messages.value = JSON.parse(history)
    } catch {
      messages.value = []
    }
  }
}

// 保存历史消息
function saveHistory() {
  localStorage.setItem('chatHistory', JSON.stringify(messages.value))
}

// 格式化AI回复文本
function formatText(text: string): string {
  if (!text || text === 'AI 正在思考中...') return ''
  return text
    .replace(/\n/g, '<br>')
    .replace(/【步骤(\d+)】/g, '<strong class="step-title">【步骤$1】</strong>')
}

// 有卡片时显示简短引导语，避免与卡片内容重复
function formatCardIntro(msg: ChatMessage): string {
  if (msg.toolResult?.type === 'weather') {
    const city = msg.toolResult.data?.city || '您所在的位置'
    const dateLabel = msg.toolResult.data?.dateLabel ? ` ${msg.toolResult.data.dateLabel}` : ''
    return `为您查询了<strong>${city}</strong>${dateLabel}的天气情况👇`
  }
  if (msg.toolResult?.type === 'news') {
    const scope = msg.toolResult.scope || '全国'
    return `为您整理了<strong>${scope}</strong>最新资讯👇`
  }
  if (msg.products?.length) {
    return `为您推荐了${msg.products.length}款相关商品，点击卡片可查看详情👇`
  }
  return ''
}

// 格式化新闻时间
function formatNewsTime(time: string): string {
  if (!time) return ''
  // 显示简化的日期时间
  return time
}

// 点击新闻项跳转
function handleNewsClick(item: any) {
  if (item.url) {
    window.open(item.url, '_blank')
  }
}

// 跳转到商品详情页
function goToProduct(product: ProductCard) {
  router.push({
    name: 'ProductDetail',
    query: {
      id: product.id,
      name: product.name,
      price: product.price,
      description: product.description,
      imageUrl: product.imageUrl || '',
      category: product.category || ''
    }
  })
}

// 处理停止按钮点击（带动画反馈）
function handleStopClick() {
  if (isStopAnimating.value) return
  isStopAnimating.value = true
  stopGeneration()
  setTimeout(() => {
    isStopAnimating.value = false
  }, 300)
}

// 停止生成
function stopGeneration() {
  if (abortController) {
    abortController.abort()
    abortController = null
  }
  isStreaming.value = false
  isLoading.value = false
  
  // 找到正在生成的消息，添加中断提示
  const lastAssistantMsg = [...messages.value].reverse().find(m => m.role === 'assistant')
  if (lastAssistantMsg && !lastAssistantMsg.content.includes('（已中断）')) {
    if (lastAssistantMsg.content === 'AI 正在思考中...') {
      lastAssistantMsg.content = '好的，已为您停止生成~ 有什么其他问题可以继续问我哦😊'
    } else {
      lastAssistantMsg.content += '\n\n---\n（已中断生成，您可以继续提问）'
    }
  }
  
  saveHistory()
}

// 发送消息
async function sendMessage() {
  const message = inputMessage.value.trim()
  if (!message || isLoading.value || isStreaming.value) return

  abortController = new AbortController()

  const userMessage: ChatMessage = {
    id: generateId(),
    role: 'user',
    content: message,
    timestamp: Date.now()
  }
  messages.value.push(userMessage)
  
  inputMessage.value = ''
  await scrollToBottom()

  isLoading.value = true
  isStreaming.value = true

  const assistantMessage: ChatMessage = {
    id: generateId(),
    role: 'assistant',
    content: 'AI 正在思考中...',
    timestamp: Date.now()
  }
  messages.value.push(assistantMessage)
  
  // 记录助手消息的索引，用于后续更新
  const assistantIndex = messages.value.length - 1
  
  try {
    // 暂时不传递 signal，简化调试
    await streamChatWithProducts(
      message,
      (text: string) => {
        // 通过数组索引更新，确保 Vue 响应式能检测到变化
        const msg = messages.value[assistantIndex]
        if (msg) {
          messages.value[assistantIndex] = { ...msg, content: text }
        }
        scrollToBottom()
      },
      (products?: ProductCard[], toolResult?: ToolResult) => {
        const msg = messages.value[assistantIndex]
        if (msg) {
          const updatedMsg: ChatMessage = { ...msg }
          if (products && products.length > 0) {
            updatedMsg.products = products
          }
          if (toolResult) {
            updatedMsg.toolResult = toolResult
          }
          messages.value[assistantIndex] = updatedMsg
          scrollToBottom()
        }
      }
    )
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      // 用户主动中断
    } else {
      console.error('Chat error:', error)
      // 如果 SSE 失败，尝试同步接口降级
      const currentMsg = messages.value[assistantIndex]
      if (currentMsg && (!currentMsg.content || currentMsg.content === 'AI 正在思考中...')) {
        try {
          const syncResult = await fetch(
            `/ai/assistant_app/chat/sync?message=${encodeURIComponent(message)}&chatId=fallback_${Date.now()}`,
            { signal: abortController.signal }
          )
          if (syncResult.ok) {
            const text = await syncResult.text()
            if (text) {
              const msg = messages.value[assistantIndex]
              if (msg) {
                messages.value[assistantIndex] = { ...msg, content: text }
              }
            } else {
              const msg = messages.value[assistantIndex]
              if (msg) {
                messages.value[assistantIndex] = { ...msg, content: '抱歉，我遇到了一些问题，请稍后再试。' }
              }
            }
          } else {
            const msg = messages.value[assistantIndex]
            if (msg) {
              messages.value[assistantIndex] = { ...msg, content: '抱歉，我遇到了一些问题，请稍后再试。' }
            }
          }
        } catch {
          const msg = messages.value[assistantIndex]
          if (msg && (!msg.content || msg.content === 'AI 正在思考中...')) {
            messages.value[assistantIndex] = { ...msg, content: '抱歉，网络开小差了，请稍后再试。' }
          }
        }
      }
    }
  } finally {
    isLoading.value = false
    isStreaming.value = false
    abortController = null
    saveHistory()
  }
}

// 按回车发送（Ctrl+回车换行）
function handleEnter(event: KeyboardEvent) {
  if (event.ctrlKey || event.metaKey) {
    inputMessage.value += '\n'
  } else {
    sendMessage()
  }
}

// 滚动到底部
async function scrollToBottom() {
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

// 格式化时间
function formatTime(timestamp?: number) {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  const now = new Date()
  const isToday = date.toDateString() === now.toDateString()
  
  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')
  
  if (isToday) {
    return `${hours}:${minutes}`
  } else {
    const month = (date.getMonth() + 1).toString().padStart(2, '0')
    const day = date.getDate().toString().padStart(2, '0')
    return `${month}-${day} ${hours}:${minutes}`
  }
}

// 清空历史
async function clearHistory() {
  try {
    await showConfirmDialog({
      title: '确认清空',
      message: '确定要清空所有对话记录吗？'
    })
    // 重置 chatId，让后端开启全新会话
    resetSessionChatId()
    messages.value = [
      {
        id: generateId(),
        role: 'assistant',
        content: '您好呀～我是姜姜😊 这个平台的AI助手~\n\n我能回答的问题取决于平台维护的知识库内容。另外，我还支持：\n\n🌤 查询天气（说"北京天气"即可查询）\n📰 查看新闻资讯（说"最新新闻"即可浏览）\n\n有什么想了解的，尽管问我吧！',
        timestamp: Date.now()
      }
    ]
    localStorage.removeItem('chatHistory')
    showToast('已清空对话记录')
  } catch {
    // 用户取消
  }
}
</script>

<style lang="scss" scoped>
.chat-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #f5f5f5;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px 16px;
  padding-bottom: 20px;

  .message-wrapper {
    margin-bottom: 16px;
    animation: fadeIn 0.3s ease-in;
  }

  .message-item {
    display: flex;
    align-items: flex-start;
    gap: 10px;

    &.assistant {
      .avatar {
        width: 36px;
        height: 36px;
        border-radius: 50%;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
      }

      .message-content {
        align-items: flex-start;

        .message-text {
          background: #fff;
          border-radius: 12px 12px 12px 4px;
          color: #333;
        }
      }
    }

    &.user {
      flex-direction: row-reverse;

      .message-content {
        align-items: flex-end;

        .message-text {
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          border-radius: 12px 12px 4px 12px;
          color: #fff;
        }
      }
    }

    .message-content {
      display: flex;
      flex-direction: column;
      gap: 4px;
      max-width: 85%;

      .message-text {
        padding: 12px 16px;
        word-wrap: break-word;
        word-break: break-word;
        line-height: 1.8;
        font-size: 15px;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
        white-space: pre-wrap;

        :deep(.step-title) {
          display: inline-block;
          color: #667eea;
          margin-top: 8px;
          margin-bottom: 4px;
          font-weight: 600;
        }

        &.card-intro {
          padding: 8px 4px 4px;
          background: transparent;
          box-shadow: none;
          color: #666;
          font-size: 14px;
          line-height: 1.6;

          strong {
            color: #667eea;
            font-weight: 600;
          }
        }
      }

      .message-time {
        font-size: 12px;
        color: #999;
        padding: 0 4px;
      }
    }

    .avatar {
      width: 36px;
      height: 36px;
      border-radius: 50%;
      background: #e8e8e8;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;

      &.user-avatar {
        background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
      }
    }
  }

  // 思考中指示器
  .typing-indicator {
    display: inline-flex;
    gap: 4px;
    padding: 12px 16px;
    background: #fff;
    border-radius: 12px 12px 12px 4px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    
    span {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: #667eea;
      animation: typing 1.4s infinite;
      
      &:nth-child(2) {
        animation-delay: 0.2s;
      }
      &:nth-child(3) {
        animation-delay: 0.4s;
      }
    }
  }

  // 商品卡片样式
  .product-cards {
    margin-top: 8px;
    width: 100%;

    .product-cards-title {
      font-size: 13px;
      color: #666;
      margin-bottom: 8px;
      padding-left: 4px;
    }

    .product-card {
      background: #fff;
      border-radius: 12px;
      padding: 12px 16px;
      margin-bottom: 10px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
      cursor: pointer;
      transition: all 0.2s ease;
      display: flex;
      flex-direction: column;
      gap: 8px;

      &:active {
        transform: scale(0.98);
        box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
      }

      .product-info {
        display: flex;
        flex-direction: column;
        gap: 6px;

        .product-name {
          font-size: 15px;
          font-weight: 600;
          color: #333;
          line-height: 1.4;
        }

        .product-desc {
          font-size: 13px;
          color: #888;
          line-height: 1.5;
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
          overflow: hidden;
        }

        .product-price-row {
          display: flex;
          align-items: baseline;
          gap: 8px;

          .product-price {
            font-size: 18px;
            font-weight: bold;
            color: #ff4d4f;
          }

          .product-original-price {
            font-size: 12px;
            color: #ccc;
            text-decoration: line-through;
          }

          .product-sales {
            font-size: 12px;
            color: #999;
            margin-left: auto;
          }
        }
      }

      .product-action {
        display: flex;
        justify-content: flex-end;
        padding-top: 4px;
        border-top: 1px solid #f5f5f5;
      }
    }
  }

  // 工具卡片样式
  .tool-card {
    margin-top: 8px;
    border-radius: 12px;
    padding: 12px 16px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    overflow: hidden;

    .tool-card-header {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 14px;
      font-weight: 600;
      margin-bottom: 10px;
      padding-bottom: 8px;
      border-bottom: 1px solid rgba(0, 0, 0, 0.06);
    }

    &.weather-card {
      background: linear-gradient(135deg, #e0f7fa 0%, #b2ebf2 100%);
      color: #00695c;

      .tool-card-header {
        color: #00695c;
      }

      .weather-info {
        .weather-main {
          display: flex;
          align-items: center;
          gap: 12px;
          margin-bottom: 10px;
          flex-wrap: wrap;

          .weather-city {
            font-size: 18px;
            font-weight: 700;
          }

          .weather-desc {
            font-size: 16px;
          }

          .weather-date {
            font-size: 13px;
            color: #00897b;
            background: rgba(255, 255, 255, 0.6);
            padding: 2px 8px;
            border-radius: 10px;
          }
        }

        .weather-detail {
          display: flex;
          flex-wrap: wrap;
          gap: 12px;
          font-size: 13px;
          color: #00897b;
        }

        .weather-tip {
          margin-top: 10px;
          padding-top: 8px;
          border-top: 1px dashed rgba(0, 137, 123, 0.3);
          font-size: 13px;
          color: #00695c;
          font-weight: 500;
        }
      }
    }

    &.news-card {
      background: linear-gradient(135deg, #fff3e0 0%, #ffe0b2 100%);
      color: #e65100;

      .tool-card-header {
        color: #e65100;
      }

      .news-list {
        .news-item {
          display: flex;
          gap: 10px;
          padding: 10px 4px;
          border-bottom: 1px dashed rgba(230, 81, 0, 0.2);
          cursor: pointer;
          transition: background 0.2s;
          border-radius: 6px;

          &:active {
            background: rgba(255, 255, 255, 0.6);
          }

          &:last-child {
            border-bottom: none;
          }

          .news-rank {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            width: 22px;
            height: 22px;
            background: #ff6d00;
            color: #fff;
            border-radius: 50%;
            font-size: 12px;
            font-weight: 600;
            flex-shrink: 0;
          }

          .news-content {
            flex: 1;
            min-width: 0;

            .news-title {
              font-size: 14px;
              font-weight: 500;
              color: #bf360c;
              line-height: 1.4;
              margin-bottom: 4px;
            }

            .news-summary {
              font-size: 12px;
              color: #6d4c41;
              line-height: 1.4;
              display: -webkit-box;
              -webkit-line-clamp: 2;
              -webkit-box-orient: vertical;
              overflow: hidden;
              margin-bottom: 4px;
            }

            .news-meta {
              display: flex;
              align-items: center;
              gap: 8px;
              flex-wrap: wrap;

              .news-tag {
                font-size: 11px;
                color: #fff;
                background: #ff6d00;
                padding: 1px 6px;
                border-radius: 8px;
              }

              .news-time {
                font-size: 11px;
                color: #8d6e63;
              }
            }
          }

          .news-arrow {
            color: #e65100;
            font-size: 12px;
            align-self: center;
            opacity: 0.6;
            flex-shrink: 0;
          }
        }
      }
    }
  }
}

.input-area {
  background: #fff;
  border-top: 1px solid #eee;
  padding: 8px;
  padding-bottom: env(safe-area-inset-bottom);

  :deep(.van-cell) {
    padding: 0;

    .van-field__control {
      background: #f5f5f5;
      border-radius: 20px;
      padding: 10px 16px;
    }
  }
  
  // 停止按钮
  .stop-btn {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background: linear-gradient(135deg, #ff4d4f 0%, #ff7875 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
    box-shadow: 0 2px 8px rgba(255, 77, 79, 0.3);
    
    &:hover {
      box-shadow: 0 4px 12px rgba(255, 77, 79, 0.4);
    }
    
    &:active, &--active {
      transform: scale(0.85);
      box-shadow: 0 1px 4px rgba(255, 77, 79, 0.3);
    }
    
    :deep(.van-icon) {
      color: #fff;
      transition: transform 0.25s ease;
    }
    
    &--active :deep(.van-icon) {
      transform: rotate(90deg);
    }
  }
}

// 过渡动画
.fade-enter-active,
.fade-leave-active {
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: scale(0.8);
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes typing {
  0%, 60%, 100% {
    transform: translateY(0);
    opacity: 0.4;
  }
  30% {
    transform: translateY(-8px);
    opacity: 1;
  }
}
</style>
