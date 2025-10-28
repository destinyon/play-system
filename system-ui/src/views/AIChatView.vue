<template>
  <div class="ai-chat-container" :class="'mode-' + currentMode">
    <!-- 头部 -->
    <div class="ai-chat-header">
      <div class="header-left">
        <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
          <circle cx="9" cy="10" r="1" fill="currentColor"></circle>
          <circle cx="12" cy="10" r="1" fill="currentColor"></circle>
          <circle cx="15" cy="10" r="1" fill="currentColor"></circle>
        </svg>
        <div class="header-title">
          <h2>AI 助手</h2>
          <div class="mode-indicator">
            <span v-if="currentMode === 'chat'" class="mode-badge mode-chat">快速模式</span>
            <span v-else class="mode-badge mode-reasoner">深思模式</span>
          </div>
        </div>
      </div>
      <div class="header-right">
        <button class="context-btn" :class="{ active: includeProjectContext }" @click="toggleIncludeContext" :disabled="loading" :title="includeProjectContext ? '已加入项目上下文' : '点击加入项目上下文'">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
            <path d="M4 4v15.5"></path>
            <path d="M6.5 2H20v15H6.5A2.5 2.5 0 0 0 4 19.5V4.5A2.5 2.5 0 0 1 6.5 2z"></path>
          </svg>
          <span>{{ includeProjectContext ? '已加入项目上下文' : '加入项目上下文' }}</span>
        </button>
        <button class="mode-toggle-btn" @click="toggleMode" :title="currentMode === 'chat' ? '切换到深思模式' : '切换到快速模式'">
          <svg v-if="currentMode === 'chat'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"></circle>
            <path d="M12 16v-4M12 8h.01"></path>
          </svg>
          <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"></path>
          </svg>
        </button>
        <button class="clear-btn" @click="showClearConfirm = true" title="清空对话">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="3 6 5 6 21 6"></polyline>
            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
          </svg>
        </button>
      </div>
    </div>

    <!-- 消息容器 -->
    <div class="messages-container" ref="messagesContainer">
      <div v-if="messages.length === 0" class="empty-state">
        <svg width="80" height="80" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
        </svg>
        <p class="empty-title">{{ currentMode === 'chat' ? '快速响应，即问即答' : '深度思考，精准解答' }}</p>
        <p class="empty-desc">开始与AI助手对话吧！</p>
      </div>

      <div v-for="(msg, index) in messages" :key="index" class="message-wrapper" :class="msg.role">
        <div class="message-content">
          <div class="avatar">
            <svg v-if="msg.role === 'assistant'" width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2z"></path>
              <path d="M9 10h.01M15 10h.01M8 15c.5 1 1.5 2 4 2s3.5-1 4-2"></path>
            </svg>
            <svg v-else width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
              <circle cx="12" cy="7" r="4"></circle>
            </svg>
          </div>
          <div class="message-info">
            <div class="message-meta">
              <span class="sender-name">
                {{ msg.role === 'assistant' ? 'AI 助手' : '我' }}
                <span v-if="msg.role === 'assistant' && msg.mode" class="msg-mode-badge" :class="'mode-' + msg.mode">
                  [{{ msg.mode === 'chat' ? '快速模式' : '深思模式' }}]
                </span>
              </span>
              <span v-if="msg.thinkingTime" class="thinking-time">思考了 {{ msg.thinkingTime }}秒</span>
            </div>
            <div class="bubble">
              <!-- 思考过程 -->
              <div v-if="msg.reasoning" class="reasoning-section">
                <div class="reasoning-header">思考过程</div>
                <div class="reasoning-content" v-html="renderMarkdown(msg.reasoning)"></div>
              </div>
              <!-- 主要内容 -->
              <div class="text" v-html="renderMarkdown(msg.content)"></div>
              <!-- 附件预览 -->
              <div v-if="msg.files && msg.files.length > 0" class="file-attachments">
                <div v-for="(file, fIdx) in msg.files" :key="fIdx" class="file-item">
                  <img v-if="file.type?.startsWith('image/')" :src="file.dataUrl" :alt="file.name" />
                  <div v-else class="file-doc">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                      <polyline points="14 2 14 8 20 8"></polyline>
                    </svg>
                    <span>{{ file.name }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 正在加载 -->
      <div v-if="loading" class="message-wrapper assistant">
        <div class="message-content">
          <div class="avatar">
            <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2z"></path>
            </svg>
          </div>
          <div class="message-info">
            <div class="message-meta">
              <span class="sender-name">
                AI 助手
                <span class="msg-mode-badge" :class="'mode-' + currentMode">
                  [{{ currentMode === 'chat' ? '快速模式' : '深思模式' }}]
                </span>
              </span>
              <span v-if="thinkingElapsed > 0" class="thinking-time">已思考 {{ thinkingElapsed }}秒</span>
            </div>
            <div class="bubble loading-bubble">
              <div v-if="currentMode === 'reasoner' && streamingReasoning" class="reasoning-section">
                <div class="reasoning-header">💭 思考中...</div>
                <div class="reasoning-content streaming" v-html="renderMarkdown(streamingReasoning)"></div>
              </div>
              <div v-if="streamingContent" class="text streaming" v-html="renderMarkdown(streamingContent)"></div>
              <div v-else class="loading-dots">
                <span></span>
                <span></span>
                <span></span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="input-container">
      <!-- 错误提示 -->
      <div v-if="error" class="error-banner">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"></circle>
          <line x1="12" y1="8" x2="12" y2="12"></line>
          <line x1="12" y1="16" x2="12.01" y2="16"></line>
        </svg>
        <span>{{ error }}</span>
        <button @click="error = ''" class="close-error">×</button>
      </div>

      <!-- 文件预览 -->
      <div v-if="attachedFiles.length > 0" class="file-preview-zone">
        <div v-for="(file, idx) in attachedFiles" :key="idx" class="preview-item">
          <img v-if="file.type.startsWith('image/')" :src="file.dataUrl" :alt="file.name" />
          <div v-else class="preview-doc">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
              <polyline points="14 2 14 8 20 8"></polyline>
            </svg>
            <span>{{ file.name }}</span>
          </div>
          <button class="remove-file-btn" @click="removeFile(idx)" title="移除">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
        </div>
      </div>

      <!-- 输入框 -->
      <div class="input-wrapper">
        <button class="attach-btn" @click="fileInput?.click()" :disabled="loading" title="上传文件/图片">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48"></path>
          </svg>
        </button>
        <textarea
          v-model="inputText"
          @keydown.enter.exact.prevent="sendMessage"
          @keydown.enter.shift.exact="inputText += '\n'"
          @input="adjustTextareaHeight"
          placeholder="输入您的问题... (Enter发送, Shift+Enter换行)"
          ref="textarea"
          :disabled="loading"
        ></textarea>
        <button class="send-btn" @click="sendMessage" :disabled="loading || (!inputText.trim() && attachedFiles.length === 0)">
          <svg v-if="!loading" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="22" y1="2" x2="11" y2="13"></line>
            <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
          </svg>
          <svg v-else width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="spin">
            <path d="M21 12a9 9 0 1 1-6.219-8.56"></path>
          </svg>
        </button>
        <input type="file" ref="fileInput" @change="handleFileSelect" multiple accept="image/*,.pdf,.txt,.doc,.docx,.xls,.xlsx,.ppt,.pptx" style="display: none;" />
      </div>
    </div>

    <!-- 自定义确认弹窗 -->
    <div v-if="showClearConfirm" class="custom-modal-overlay" @click="showClearConfirm = false">
      <div class="custom-modal" @click.stop>
        <div class="modal-icon">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="12" y1="8" x2="12" y2="12"></line>
            <line x1="12" y1="16" x2="12.01" y2="16"></line>
          </svg>
        </div>
        <h3>清空对话记录</h3>
        <p>确定要清空所有对话记录吗？<br />此操作无法撤销。</p>
        <div class="modal-actions">
          <button class="btn-cancel" @click="showClearConfirm = false">取消</button>
          <button class="btn-confirm" @click="confirmClearHistory">确定清空</button>
        </div>
      </div>
    </div>

    <!-- 模式切换警告弹窗 -->
    <div v-if="showModeWarning" class="custom-modal-overlay" @click="showModeWarning = false">
      <div class="custom-modal" @click.stop>
        <div class="modal-icon warning-icon">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
            <line x1="12" y1="9" x2="12" y2="13"></line>
            <line x1="12" y1="17" x2="12.01" y2="17"></line>
          </svg>
        </div>
        <h3>无法切换模式</h3>
        <p>AI正在思考中，请等待回答完成后再切换模式。</p>
        <div class="modal-actions">
          <button class="btn-cancel" @click="showModeWarning = false">知道了</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted, onUnmounted } from 'vue'
import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'
import { buildAuthHeaders } from '@/api/http'

interface Message {
  role: 'user' | 'assistant'
  content: string
  mode?: 'chat' | 'reasoner'
  reasoning?: string
  thinkingTime?: number
  files?: Array<{ name: string; type: string; dataUrl?: string; size?: number }>
}

interface AttachedFile {
  name: string
  type: string
  dataUrl: string
  size: number
}

const md = new MarkdownIt({
  html: false,
  linkify: true,
  typographer: true,
  breaks: true
})

const messages = ref<Message[]>([])
const inputText = ref('')
const loading = ref(false)
const error = ref('')
const messagesContainer = ref<HTMLElement | null>(null)
const textarea = ref<HTMLTextAreaElement | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)
const attachedFiles = ref<AttachedFile[]>([])
const currentMode = ref<'chat' | 'reasoner'>('chat')
const showClearConfirm = ref(false)
const showModeWarning = ref(false)
const includeProjectContext = ref(false)

// 流式输出状态
const streamingContent = ref('')
const streamingReasoning = ref('')
const thinkingElapsed = ref(0)
let thinkingTimer: number | null = null

// Markdown渲染
function renderMarkdown(text: string): string {
  if (!text) return ''
  const rendered = md.render(text)
  return DOMPurify.sanitize(rendered)
}

// 从localStorage加载历史记录
onMounted(() => {
  const stored = localStorage.getItem('ai-chat-history')
  const storedMode = localStorage.getItem('ai-chat-mode')
  const storedCtx = localStorage.getItem('ai-include-project-context')
  if (stored) {
    try {
      messages.value = JSON.parse(stored)
      scrollToBottom()
    } catch (e) {
      console.error('Failed to load chat history', e)
    }
  }
  if (storedMode === 'reasoner' || storedMode === 'chat') {
    currentMode.value = storedMode
  }
  if (storedCtx === 'true' || storedCtx === 'false') {
    includeProjectContext.value = storedCtx === 'true'
  }
})

onUnmounted(() => {
  if (thinkingTimer) window.clearInterval(thinkingTimer)
})

// 保存历史记录
function saveHistory() {
  try {
    localStorage.setItem('ai-chat-history', JSON.stringify(messages.value))
    localStorage.setItem('ai-chat-mode', currentMode.value)
    localStorage.setItem('ai-include-project-context', String(includeProjectContext.value))
  } catch (e) {
    console.error('Failed to save chat history', e)
  }
}

// 滚动到底部
function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

// 切换模式
function toggleMode() {
  if (loading.value) {
    showModeWarning.value = true
    return
  }
  currentMode.value = currentMode.value === 'chat' ? 'reasoner' : 'chat'
  saveHistory()
}

// 切换是否注入项目上下文
function toggleIncludeContext() {
  if (loading.value) return
  includeProjectContext.value = !includeProjectContext.value
  saveHistory()
}

// 清空对话历史
function confirmClearHistory() {
  messages.value = []
  attachedFiles.value = []
  localStorage.removeItem('ai-chat-history')
  error.value = ''
  showClearConfirm.value = false
}

// 调整输入框高度
function adjustTextareaHeight() {
  nextTick(() => {
    if (textarea.value) {
      textarea.value.style.height = 'auto'
      const lineHeight = 24
      const maxLines = 5
      const maxHeight = lineHeight * maxLines
      const newHeight = Math.min(textarea.value.scrollHeight, maxHeight)
      textarea.value.style.height = newHeight + 'px'
    }
  })
}

// 处理文件选择
async function handleFileSelect(event: Event) {
  const target = event.target as HTMLInputElement
  if (!target.files || target.files.length === 0) return

  const files = Array.from(target.files)
  for (const file of files) {
    if (file.size > 20 * 1024 * 1024) {
      error.value = `文件 ${file.name} 超过20MB限制`
      continue
    }

    try {
      const dataUrl = await readFileAsDataURL(file)
      attachedFiles.value.push({
        name: file.name,
        type: file.type,
        dataUrl,
        size: file.size
      })
    } catch (err) {
      error.value = `读取文件 ${file.name} 失败`
    }
  }
  target.value = ''
}

// 移除文件
function removeFile(index: number) {
  attachedFiles.value.splice(index, 1)
}

// 读取文件为DataURL
function readFileAsDataURL(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result as string)
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

// 逐字打字效果
async function typewriterEffect(text: string, targetRef: { value: string }, speed = 20) {
  targetRef.value = ''
  for (let i = 0; i < text.length; i++) {
    targetRef.value += text[i]
    scrollToBottom()
    await new Promise(resolve => setTimeout(resolve, speed))
  }
}

// 发送消息
async function sendMessage() {
  const text = inputText.value.trim()
  if ((!text && attachedFiles.value.length === 0) || loading.value) return

  // 构建用户消息
  const userMessage: Message = { 
    role: 'user', 
    content: text || '(附件)',
    files: attachedFiles.value.map(f => ({ name: f.name, type: f.type, dataUrl: f.dataUrl }))
  }
  messages.value.push(userMessage)
  inputText.value = ''
  error.value = ''
  scrollToBottom()
  saveHistory()

  // 构建API消息格式
  const apiMessages = []
  for (const msg of messages.value) {
    if (msg.role === 'user') {
      if (msg.files && msg.files.length > 0) {
        // 有附件的消息 - DeepSeek当前不支持vision，将文件内容转换为文本描述
        let contentText = msg.content && msg.content !== '(附件)' ? msg.content + '\n\n' : ''
        contentText += '=== 附件信息 ===\n'
        
        for (const file of msg.files) {
          if (file.type.startsWith('image/') && file.dataUrl) {
            contentText += `\n[图片文件: ${file.name}]\n`
            contentText += `类型: ${file.type}\n`
            contentText += `大小: ${((file.size || 0) / 1024).toFixed(2)} KB\n`
            contentText += `Base64数据前100字符: ${file.dataUrl.substring(0, 100)}...\n`
            contentText += `完整Base64数据:\n${file.dataUrl}\n`
            contentText += `注意: 这是一张图片的完整base64编码。虽然当前模型可能不支持直接识别图片，但数据已提供。请根据用户的文字描述来理解图片内容。\n`
          } else if (file.dataUrl) {
            contentText += `\n[文件: ${file.name}]\n`
            contentText += `类型: ${file.type}\n`
            contentText += `大小: ${((file.size || 0) / 1024).toFixed(2)} KB\n`
            
            // 尝试解析文本文件内容
            if (file.type.includes('text') || file.type.includes('json') || 
                file.name.endsWith('.txt') || file.name.endsWith('.md') || 
                file.name.endsWith('.json') || file.name.endsWith('.xml') ||
                file.name.endsWith('.csv') || file.name.endsWith('.log')) {
              try {
                const base64Data = file.dataUrl.split(',')[1]
                if (base64Data) {
                  const textContent = atob(base64Data)
                  contentText += `文件内容:\n${textContent}\n`
                }
              } catch (e) {
                contentText += `无法读取文件文本内容\n`
                contentText += `Base64数据: ${file.dataUrl}\n`
              }
            } else {
              contentText += `完整Base64数据:\n${file.dataUrl}\n`
            }
          }
        }
        
        apiMessages.push({ role: 'user', content: contentText })
      } else {
        apiMessages.push({ role: 'user', content: msg.content })
      }
    } else {
      apiMessages.push({ role: 'assistant', content: msg.content })
    }
  }

  // 清空附件
  attachedFiles.value = []

  // 调用API
  loading.value = true
  streamingContent.value = ''
  streamingReasoning.value = ''
  thinkingElapsed.value = 0

  const thinkingStartTime = Date.now()
  thinkingTimer = window.setInterval(() => {
    thinkingElapsed.value = Math.floor((Date.now() - thinkingStartTime) / 1000)
  }, 1000)

  try {
    const modelName = currentMode.value === 'reasoner' ? 'deepseek-reasoner' : 'deepseek-chat'
    const response = await fetch('/api/ai/chat', {
      method: 'POST',
      headers: buildAuthHeaders({
        'Content-Type': 'application/json',
      }),
      body: JSON.stringify({
        data: {
          model: modelName,
          messages: apiMessages,
          includeProjectContext: includeProjectContext.value
        }
      })
    })

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`)
    }

    const result = await response.json()
    
    if (result.status === 200 && result.data) {
      const thinkingTime = Math.floor((Date.now() - thinkingStartTime) / 1000)
      if (thinkingTimer) {
        window.clearInterval(thinkingTimer)
        thinkingTimer = null
      }

      const assistantMessage: Message = {
        role: 'assistant',
        content: '',
        mode: currentMode.value,
        thinkingTime
      }

      // 深思模式：先显示思考过程
      if (currentMode.value === 'reasoner' && result.data.reasoning_content) {
        await typewriterEffect(result.data.reasoning_content, streamingReasoning, 15)
        assistantMessage.reasoning = result.data.reasoning_content
      }

      // 显示主要内容
      await typewriterEffect(result.data.content || '抱歉,我没有理解您的问题。', streamingContent, 20)
      assistantMessage.content = result.data.content || '抱歉,我没有理解您的问题。'

      messages.value.push(assistantMessage)
      streamingContent.value = ''
      streamingReasoning.value = ''
      saveHistory()
      scrollToBottom()
    } else {
      throw new Error(result.message || '服务器返回错误')
    }

  } catch (err: any) {
    console.error('AI调用失败:', err)
    error.value = `请求失败: ${err.message || '未知错误'}`
    messages.value.pop()
    saveHistory()
  } finally {
    loading.value = false
    thinkingElapsed.value = 0
    if (thinkingTimer) {
      window.clearInterval(thinkingTimer)
      thinkingTimer = null
    }
  }
}
</script>
<style scoped src="./styles/AIChatView.css"></style>

