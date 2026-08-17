// 全局状态
const state = {
    conversations: [],
    currentConvId: null,
    mode: 'chat',
    isLoading: false
};

// DOM 元素
const $ = (id) => document.getElementById(id);
const chatMessages = $('chatMessages');
const messageInput = $('messageInput');
const sendBtn = $('sendBtn');
const conversationList = $('conversationList');
const newChatBtn = $('newChatBtn');
const chatTitle = $('chatTitle');
const productPanel = $('productPanel');
const productInput = $('productInput');
const extractBtn = $('extractBtn');
const productResult = $('productResult');
const chatInputArea = $('chatInputArea');
const modeBtns = document.querySelectorAll('.mode-btn');

// 初始化
function init() {
    loadConversations();
    createNewConversation();
    setupEventListeners();
    autoResizeTextarea();
}

// 设置事件监听
function setupEventListeners() {
    newChatBtn.addEventListener('click', createNewConversation);
    sendBtn.addEventListener('click', sendMessage);
    messageInput.addEventListener('keydown', handleKeyDown);
    extractBtn.addEventListener('click', extractProductInfo);
    
    modeBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            modeBtns.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            state.mode = btn.dataset.mode;
            toggleMode();
        });
    });

    // 快捷问题
    document.querySelectorAll('.prompt-item').forEach(item => {
        item.addEventListener('click', () => {
            messageInput.value = item.dataset.text;
            updateSendButton();
        });
    });
}

// 自动调整输入框高度
function autoResizeTextarea() {
    messageInput.addEventListener('input', () => {
        messageInput.style.height = 'auto';
        messageInput.style.height = Math.min(messageInput.scrollHeight, 120) + 'px';
    });
    
    productInput.addEventListener('input', () => {
        productInput.style.height = 'auto';
        productInput.style.height = Math.min(productInput.scrollHeight, 120) + 'px';
    });
}

// 处理键盘事件
function handleKeyDown(e) {
    if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendMessage();
    }
}

// 切换模式
function toggleMode() {
    if (state.mode === 'product') {
        productPanel.style.display = 'block';
        chatInputArea.style.display = 'none';
    } else {
        productPanel.style.display = 'none';
        chatInputArea.style.display = 'block';
    }
}

// 更新发送按钮状态
function updateSendButton() {
    sendBtn.disabled = messageInput.value.trim().length === 0 || state.isLoading;
}

// 创建新对话
function createNewConversation() {
    const convId = Date.now().toString();
    const conversation = {
        id: convId,
        title: '新对话',
        messages: [],
        createdAt: new Date().toISOString()
    };
    
    state.conversations.unshift(conversation);
    state.currentConvId = convId;
    saveConversations();
    renderConversationList();
    renderCurrentConversation();
}

// 加载会话列表
function loadConversations() {
    const saved = localStorage.getItem('chatbot_conversations');
    if (saved) {
        try {
            state.conversations = JSON.parse(saved);
        } catch (e) {
            state.conversations = [];
        }
    }
}

// 保存会话列表
function saveConversations() {
    localStorage.setItem('chatbot_conversations', JSON.stringify(state.conversations));
}

// 渲染会话列表
function renderConversationList() {
    conversationList.innerHTML = '';
    
    if (state.conversations.length === 0) {
        return;
    }
    
    state.conversations.forEach(conv => {
        const item = document.createElement('div');
        item.className = 'conversation-item' + (conv.id === state.currentConvId ? ' active' : '');
        item.innerHTML = `
            <span class="conv-icon">💬</span>
            <span class="conv-text">${conv.title}</span>
            <span class="conv-delete" data-id="${conv.id}">🗑️</span>
        `;
        item.addEventListener('click', (e) => {
            if (e.target.classList.contains('conv-delete')) {
                e.stopPropagation();
                deleteConversation(conv.id);
            } else {
                state.currentConvId = conv.id;
                renderConversationList();
                renderCurrentConversation();
            }
        });
        conversationList.appendChild(item);
    });
}

// 删除对话
function deleteConversation(convId) {
    if (!confirm('确定要删除这个对话吗？')) return;
    
    state.conversations = state.conversations.filter(c => c.id !== convId);
    saveConversations();
    
    if (state.currentConvId === convId) {
        createNewConversation();
    } else {
        renderConversationList();
    }
}

// 获取当前对话
function getCurrentConversation() {
    return state.conversations.find(c => c.id === state.currentConvId);
}

// 渲染当前对话
function renderCurrentConversation() {
    const conv = getCurrentConversation();
    if (!conv) return;
    
    chatTitle.textContent = conv.title;
    chatMessages.innerHTML = '';
    
    if (conv.messages.length === 0) {
        showWelcome();
    } else {
        conv.messages.forEach(msg => appendMessageToUI(msg, false));
    }
}

// 显示欢迎消息
function showWelcome() {
    chatMessages.innerHTML = `
        <div class="welcome-message">
            <div class="welcome-icon">👋</div>
            <div class="welcome-title">你好，我是智能客服助手</div>
            <div class="welcome-desc">我可以帮你解答商品信息、订单查询、售后服务等问题</div>
            <div class="quick-prompts">
                <div class="quick-title">试试这些问题：</div>
                <div class="prompt-list">
                    <button class="prompt-item" data-text="如何查询我的订单？">🔍 如何查询订单？</button>
                    <button class="prompt-item" data-text="退货政策是什么？">📋 退货政策是什么？</button>
                    <button class="prompt-item" data-text="你们支持哪些付款方式？">💳 支持哪些付款方式？</button>
                    <button class="prompt-item" data-text="如何联系人工客服？">☎️ 如何联系人工客服？</button>
                </div>
            </div>
        </div>
    `;
    
    document.querySelectorAll('.prompt-item').forEach(item => {
        item.addEventListener('click', () => {
            messageInput.value = item.dataset.text;
            updateSendButton();
        });
    });
}

// 添加消息到 UI
function appendMessageToUI(msg, save = true) {
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${msg.role}`;
    
    const avatar = msg.role === 'user' ? '👤' : '🤖';
    
    messageDiv.innerHTML = `
        <div class="message-avatar">${avatar}</div>
        <div class="message-content">
            <div class="message-text"></div>
            <div class="message-time">${formatTime(msg.time)}</div>
        </div>
    `;
    
    chatMessages.appendChild(messageDiv);
    chatMessages.scrollTop = chatMessages.scrollHeight;
    
    const textElement = messageDiv.querySelector('.message-text');
    const timeElement = messageDiv.querySelector('.message-time');
    
    if (save) {
        textElement.textContent = msg.content;
    } else {
        typeText(textElement, msg.content);
    }
    
    return { messageDiv, textElement, timeElement };
}

// 打字机效果
function typeText(element, text) {
    let index = 0;
    const timer = setInterval(() => {
        if (index < text.length) {
            element.textContent += text[index];
            chatMessages.scrollTop = chatMessages.scrollHeight;
            index++;
        } else {
            clearInterval(timer);
        }
    }, 20);
}

// 格式化时间
function formatTime(isoString) {
    const date = new Date(isoString);
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
}

// 添加消息到对话
function addMessageToConversation(role, content) {
    const conv = getCurrentConversation();
    if (!conv) return;
    
    const msg = {
        role,
        content,
        time: new Date().toISOString()
    };
    
    conv.messages.push(msg);
    
    // 更新对话标题
    if (conv.messages.length === 2 && role === 'assistant') {
        conv.title = conv.messages[0].content.substring(0, 20) + '...';
    }
    
    saveConversations();
    renderConversationList();
    
    return msg;
}

// 发送消息
async function sendMessage() {
    const text = messageInput.value.trim();
    if (!text || state.isLoading) return;
    
    const conv = getCurrentConversation();
    if (!conv) return;
    
    // 添加用户消息
    const userMsg = addMessageToConversation('user', text);
    appendMessageToUI(userMsg, true);
    
    // 清空输入
    messageInput.value = '';
    updateSendButton();
    
    // 显示正在输入
    state.isLoading = true;
    updateSendButton();
    
    const loadingDiv = document.createElement('div');
    loadingDiv.className = 'message assistant typing';
    loadingDiv.innerHTML = `
        <div class="message-avatar">🤖</div>
        <div class="message-content">
            <div class="message-text">
                <div class="typing-dot"></div>
                <div class="typing-dot"></div>
                <div class="typing-dot"></div>
            </div>
        </div>
    `;
    chatMessages.appendChild(loadingDiv);
    chatMessages.scrollTop = chatMessages.scrollHeight;
    
    try {
        // 调用后端 SSE 接口
        const response = await fetch(
            `/ai/assistant_app/chat/server_sent_event?message=${encodeURIComponent(text)}&chatId=${conv.id}`
        );
        
        // 移除加载动画
        loadingDiv.remove();
        
        if (!response.ok) {
            throw new Error(`请求失败: ${response.status}`);
        }
        
        // 处理 SSE 流
        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let fullContent = '';
        const chatId = conv.id;
        
        // 创建消息元素
        const aiMsg = { role: 'assistant', content: '', time: new Date().toISOString() };
        const { textElement } = appendMessageToUI(aiMsg, true);
        textElement.textContent = '';
        
        while (true) {
            const { done, value } = await reader.read();
            if (done) break;
            
            const chunk = decoder.decode(value);
            const lines = chunk.split('\n');
            
            for (const line of lines) {
                if (line.startsWith('data: ')) {
                    const data = line.slice(6);
                    if (data && data !== '[DONE]') {
                        fullContent += data;
                        textElement.textContent = fullContent;
                        chatMessages.scrollTop = chatMessages.scrollHeight;
                    }
                }
            }
        }
        
        // 保存完整消息
        aiMsg.content = fullContent;
        conv.messages.push(aiMsg);
        saveConversations();
        renderConversationList();
        
    } catch (error) {
        // 移除加载动画
        loadingDiv.remove();
        
        // 显示错误消息
        const errorMsg = {
            role: 'assistant',
            content: '😅 抱歉，出现了一些问题：' + error.message,
            time: new Date().toISOString()
        };
        appendMessageToUI(errorMsg, true);
        conv.messages.push(errorMsg);
        saveConversations();
        
    } finally {
        state.isLoading = false;
        updateSendButton();
    }
}

// 提取商品信息
async function extractProductInfo() {
    const text = productInput.value.trim();
    if (!text || state.isLoading) return;
    
    state.isLoading = true;
    extractBtn.disabled = true;
    extractBtn.textContent = '⏳ 提取中...';
    productResult.classList.remove('show');
    
    try {
        const response = await fetch(
            `/ai/product_info_app/chat/sync?message=${encodeURIComponent(text)}`
        );
        
        if (!response.ok) {
            throw new Error(`请求失败: ${response.status}`);
        }
        
        const data = await response.json();
        
        // 显示结果
        displayProductResult(data);
        
    } catch (error) {
        productResult.innerHTML = `<div style="color: #f5576c;">❌ 提取失败: ${error.message}</div>`;
        productResult.classList.add('show');
    } finally {
        state.isLoading = false;
        extractBtn.disabled = false;
        extractBtn.textContent = '🔍 提取商品信息';
    }
}

// 显示商品提取结果
function displayProductResult(data) {
    const fields = [
        { key: 'title', label: '商品名称' },
        { key: 'brand', label: '品牌' },
        { key: 'category', label: '分类' },
        { key: 'price', label: '价格' },
        { key: 'rating', label: '评分' },
        { key: 'reviewCount', label: '评论数' }
    ];
    
    let html = '<h3>📦 提取结果</h3>';
    fields.forEach(field => {
        const value = data[field.key];
        if (value !== undefined && value !== null && value !== '' && value !== 0) {
            html += `
                <div class="result-field">
                    <span class="field-label">${field.label}</span>
                    <span class="field-value">${value}</span>
                </div>
            `;
        }
    });
    
    productResult.innerHTML = html;
    productResult.classList.add('show');
}

// 启动
init();