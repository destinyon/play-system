<template>
  <div class="ai-chat-container">
    <div class="ai-chat-header">
      <div class="header-left">
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
          <circle cx="9" cy="10" r="1" fill="currentColor"></circle>
          <circle cx="12" cy="10" r="1" fill="currentColor"></circle>
          <circle cx="15" cy="10" r="1" fill="currentColor"></circle>
        </svg>
        <h2>AI 助手</h2>
      </div>
      <div class="header-right">
        <!-- 模型选择 -->
        <div class="model-switch" role="group" aria-label="模型选择">
          <button :class="['switch-btn', selectedModel === 'deepseek-chat' && 'active']" @click="selectedModel = 'deepseek-chat'" title="快速模式">
            快速
          </button>
          <button :class="['switch-btn', selectedModel === 'deepseek-reasoner' && 'active']" @click="selectedModel = 'deepseek-reasoner'" title="深度思考">
            深思
          </button>
        </div>
        <button class="clear-btn" @click="showClearConfirm = true" title="清空对话">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="3 6 5 6 21 6"></polyline>
            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
          </svg>
        </button>
      </div>
    </div>

    <div class="messages-container" ref="messagesContainer">
      <div v-if="messages.length === 0" class="empty-state">
        <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="#cbd5e1" stroke-width="1.5">
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
        </svg>
        <p>开始与AI助手对话吧！</p>
      </div>

      <div v-for="(msg, index) in messages" :key="index" class="message-wrapper" :class="msg.role">
        <div class="message-content">
          <div class="avatar">
            <svg v-if="msg.role === 'assistant'" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2z"></path>
              <path d="M9 10h.01M15 10h.01M8 15c.5 1 1.5 2 4 2s3.5-1 4-2"></path>
            </svg>
            <svg v-else width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
              <circle cx="12" cy="7" r="4"></circle>
            </svg>
          </div>
          <div class="bubble">
            <div class="text markdown" v-html="renderMarkdown(msg.content)"></div>
            <div v-if="msg.parts && msg.parts.length" class="attachments">
              <template v-for="(p,i) in msg.parts" :key="i">
                <img v-if="p.type==='image' && p.url" class="thumb" :src="p.url" :alt="p.name || 'image'" />
                <a v-else class="file-pill" :href="p.url || '#'" target="_blank" :download="p.name" @click.prevent>
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline></svg>
                  <span class="name">{{ p.name || '附件' }}</span>
                </a>
              </template>
            </div>
          </div>
        </div>
      </div>

      <div v-if="loading" class="message-wrapper assistant">
        <div class="message-content">
          <div class="avatar">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2z"></path>
            </svg>
          </div>
          <div class="bubble loading-bubble">
            <template v-if="selectedModel === 'deepseek-reasoner'">
              <div class="thinking">
                <div class="thinking-title">深度思考中…</div>
                <div class="thinking-sub">已思考 {{ thinkingSeconds }} 秒</div>
              </div>
            </template>
            <template v-else>
              <div class="loading-dots"><span></span><span></span><span></span></div>
            </template>
          </div>
        </div>
      </div>
    </div>

    <div class="input-container">
      <div v-if="error" class="error-banner">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"></circle>
          <line x1="12" y1="8" x2="12" y2="12"></line>
          <line x1="12" y1="16" x2="12.01" y2="16"></line>
        </svg>
        <span>{{ error }}</span>
        <button @click="error = ''" class="close-error">×</button>
      </div>
      <!-- 附件预览与上传 -->
      <div class="attachments-bar">
        <div class="previews" v-if="attachments.length">
          <div class="preview-item" v-for="(att, idx) in attachments" :key="idx">
            <img v-if="att.type==='image'" :src="att.url" :alt="att.name" />
            <div v-else class="file-pill small">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline></svg>
              <span class="name">{{ att.name }}</span>
            </div>
            <button class="remove" @click="removeAttachment(idx)">×</button>
          </div>
        </div>
        <label class="upload-btn">
          <input type="file" multiple @change="onFilesSelected" />
          添加附件
        </label>
      </div>
      <div class="input-wrapper">
        <textarea
          v-model="inputText"
          @keydown.enter.exact.prevent="sendMessage"
          @keydown.enter.shift.exact="inputText += '\n'"
          placeholder="输入您的问题... (Enter发送, Shift+Enter换行)"
          rows="1"
          ref="textarea"
          :disabled="loading"
        ></textarea>
        <button class="send-btn" @click="sendMessage" :disabled="loading || !inputText.trim()">
          <svg v-if="!loading" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="22" y1="2" x2="11" y2="13"></line>
            <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
          </svg>
          <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="spin">
            <path d="M21 12a9 9 0 1 1-6.219-8.56"></path>
          </svg>
        </button>
      </div>
    </div>

    <div v-if="showClearConfirm" class="modal-mask">
      <div class="modal" role="dialog" aria-modal="true">
        <div class="modal-header">清空对话</div>
        <div class="modal-body">确定要清空所有对话记录吗？此操作无法撤销。</div>
        <div class="modal-footer">
          <button class="btn cancel" @click="showClearConfirm = false">取消</button>
          <button class="btn danger" @click="doClearHistory">清空</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue'
import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'

interface Message {
  role: 'user' | 'assistant'
  content: string
  parts?: Attachment[]
}

interface Attachment {
  type: 'image' | 'file'
  url?: string
  name?: string
  mime?: string
  size?: number
}

const messages = ref<Message[]>([])
const inputText = ref('')
const loading = ref(false)
const error = ref('')
const messagesContainer = ref<HTMLElement | null>(null)
const textarea = ref<HTMLTextAreaElement | null>(null)
const selectedModel = ref<'deepseek-chat' | 'deepseek-reasoner'>('deepseek-chat')
const thinkingSeconds = ref(0)
let thinkingTimer: number | undefined
const attachments = ref<Attachment[]>([])

const md = new MarkdownIt({ linkify: true, breaks: true })
function renderMarkdown(text: string) {
  const dirty = md.render(text || '')
  return DOMPurify.sanitize(dirty)
}

// 从localStorage加载历史记录
onMounted(() => {
  const stored = localStorage.getItem('ai-chat-history')
  if (stored) {
    try {
      messages.value = JSON.parse(stored)
      scrollToBottom()
    } catch (e) {
      console.error('Failed to load chat history', e)
    }
  }
})

// 保存历史记录
function saveHistory() {
  try {
    localStorage.setItem('ai-chat-history', JSON.stringify(messages.value))
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

// 清空对话历史
const showClearConfirm = ref(false)
function doClearHistory() {
  messages.value = []
  localStorage.removeItem('ai-chat-history')
  error.value = ''
  showClearConfirm.value = false
}

function onFilesSelected(e: Event) {
  const input = e.target as HTMLInputElement
  if (!input.files) return
  const files = Array.from(input.files)
  for (const file of files) {
    const isImage = file.type.startsWith('image/')
    const att: Attachment = { type: isImage ? 'image' : 'file', name: file.name, mime: file.type, size: file.size }
    if (isImage) {
      const reader = new FileReader()
      reader.onload = () => {
        att.url = String(reader.result)
        attachments.value.push(att)
      }
      reader.readAsDataURL(file)
    } else {
      // 暂不上传到服务器，先仅保留名称预览
      attachments.value.push(att)
    }
  }
  // 重置选择器
  input.value = ''
}

function removeAttachment(idx: number) {
  attachments.value.splice(idx, 1)
}

// 发送消息
async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || loading.value) return

  // 添加用户消息
  const userMessage: Message = { role: 'user', content: text, parts: attachments.value.length ? [...attachments.value] : undefined }
  messages.value.push(userMessage)
  inputText.value = ''
  error.value = ''
  attachments.value = []
  scrollToBottom()
  saveHistory()

  // 调用API
  loading.value = true
  if (selectedModel.value === 'deepseek-reasoner') {
    thinkingSeconds.value = 0
    thinkingTimer = window.setInterval(() => {
      thinkingSeconds.value += 1
    }, 1000)
  }
  try {
    const response = await fetch('/api/ai/chat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        data: {
          model: selectedModel.value,
          stream: false,
          messages: messages.value.map(m => {
            // 发送给后端时，带上富内容
            if (m.parts && m.parts.length) {
              const parts: any[] = []
              if (m.content) parts.push({ type: 'text', text: m.content })
              for (const p of m.parts) {
                if (p.type === 'image' && p.url) {
                  parts.push({ type: 'image_url', image_url: { url: p.url } })
                } else {
                  parts.push({ type: 'text', text: `[附件] ${p.name || 'file'}` })
                }
              }
              return { role: m.role, content: parts }
            }
            return { role: m.role, content: m.content }
          })
        }
      })
    })

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`)
    }

    const result = await response.json()
    console.log('AI响应结果:', result)
    
    if (result.status === 200 && result.data) {
      // 添加AI响应
      const assistantMessage: Message = {
        role: 'assistant',
        content: result.data.content || '抱歉,我没有理解您的问题。'
      }
      messages.value.push(assistantMessage)
      saveHistory()
      scrollToBottom()
    } else {
      throw new Error(result.message || '服务器返回错误')
    }

  } catch (err: any) {
    console.error('AI调用失败:', err)
    error.value = `请求失败: ${err.message || '未知错误'}`
    // 移除最后一条用户消息
    messages.value.pop()
    saveHistory()
  } finally {
    loading.value = false
    if (thinkingTimer) {
      clearInterval(thinkingTimer)
      thinkingTimer = undefined
    }
  }
}
</script>

<style scoped>
.ai-chat-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: linear-gradient(135deg, #fff7ed 0%, #fef3c7 100%);
  overflow: hidden;
}

.ai-chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: linear-gradient(135deg, #fff7ed 0%, #fde68a 45%, #f97316 100%);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  color: #7c2d12;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-left h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
}

.header-right {
  display: flex;
  gap: 8px;
}

.model-switch {
  display: inline-flex;
  background: rgba(255,255,255,0.5);
  padding: 4px;
  border-radius: 999px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}
.switch-btn {
  border: none;
  background: transparent;
  color: #7c2d12;
  font-weight: 700;
  padding: 6px 12px;
  border-radius: 999px;
  cursor: pointer;
}
.switch-btn.active { background: #fff; }

.clear-btn {
  background: rgba(255, 255, 255, 0.4);
  border: none;
  color: #7c2d12;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.2s;
}

.clear-btn:hover {
  background: rgba(255, 255, 255, 0.7);
  box-shadow: 0 4px 12px rgba(124, 45, 18, 0.15);
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  scroll-behavior: smooth;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #94a3b8;
  gap: 16px;
}

.empty-state p {
  font-size: 16px;
  margin: 0;
}

.message-wrapper {
  display: flex;
  margin-bottom: 16px;
  animation: fadeIn 0.3s ease;
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

.message-wrapper.user {
  justify-content: flex-end;
}

.message-wrapper.assistant {
  justify-content: flex-start;
}

.message-content {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  max-width: 70%;
}

.message-wrapper.user .message-content {
  flex-direction: row-reverse;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.message-wrapper.user .avatar {
  background: linear-gradient(135deg, #fde68a 0%, #f97316 100%);
  color: #7c2d12;
}

.message-wrapper.assistant .avatar {
  background: linear-gradient(135deg, #e0e7ff 0%, #818cf8 100%);
  color: #3730a3;
}

.bubble {
  padding: 12px 16px;
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  word-wrap: break-word;
  overflow-wrap: anywhere;
}

.markdown :deep(p) { margin: 0 0 0.6em; }
.markdown :deep(code) { background: rgba(0,0,0,0.06); padding: 2px 6px; border-radius: 6px; font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace; }
.markdown :deep(pre) { background: #0b1020; color: #e5e7eb; padding: 12px; border-radius: 8px; overflow: auto; }
.markdown :deep(h1), .markdown :deep(h2), .markdown :deep(h3) { margin: 0.6em 0 0.4em; }
.markdown :deep(ul), .markdown :deep(ol) { padding-left: 1.2em; margin: 0.4em 0 0.6em; }
.markdown :deep(a) { color: #2563eb; text-decoration: underline; }

.attachments { display: flex; gap: 8px; margin-top: 8px; flex-wrap: wrap; }
.attachments .thumb { width: 120px; height: 80px; object-fit: cover; border-radius: 8px; box-shadow: 0 2px 6px rgba(0,0,0,0.08); }
.file-pill { display: inline-flex; align-items: center; gap: 6px; background: #fff7ed; border:1px solid #fed7aa; color:#7c2d12; padding: 6px 8px; border-radius: 999px; font-size: 12px; }
.file-pill.small { padding: 4px 6px; }
.file-pill .name { max-width: 140px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.message-wrapper.user .bubble {
  background: linear-gradient(135deg, #fed7aa 0%, #fb923c 100%);
  color: #7c2d12;
  border-bottom-right-radius: 4px;
}

.message-wrapper.assistant .bubble {
  background: white;
  color: #1e293b;
  border-bottom-left-radius: 4px;
}

.text {
  white-space: pre-wrap;
  line-height: 1.6;
  font-size: 15px;
}

.loading-bubble {
  background: white;
  padding: 16px 20px;
}

.thinking-title { font-size: 14px; color:#0f172a; font-weight: 700; }
.thinking-sub { font-size: 12px; color: #64748b; margin-top: 4px; }

.loading-dots {
  display: flex;
  gap: 6px;
}

.loading-dots span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #cbd5e1;
  animation: bounce 1.4s infinite ease-in-out both;
}

.loading-dots span:nth-child(1) {
  animation-delay: -0.32s;
}

.loading-dots span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes bounce {
  0%, 80%, 100% {
    transform: scale(0.8);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

.input-container {
  padding: 16px 20px;
  background: white;
  border-top: 1px solid #e5e7eb;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.05);
}

.attachments-bar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; gap: 10px; }
.previews { display: flex; gap: 8px; flex-wrap: wrap; }
.preview-item { position: relative; }
.preview-item img { width: 56px; height: 56px; object-fit: cover; border-radius: 8px; box-shadow: 0 1px 4px rgba(0,0,0,0.1); }
.preview-item .remove { position: absolute; top: -6px; right: -6px; width: 20px; height: 20px; border: none; background: #ef4444; color: #fff; border-radius: 50%; cursor: pointer; }
.upload-btn { border:1px dashed #f59e0b; color:#b45309; background: #fffbeb; padding: 6px 10px; border-radius: 8px; cursor: pointer; font-weight: 700; }
.upload-btn input { display: none; }

.error-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  margin-bottom: 12px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  color: #dc2626;
  font-size: 14px;
}

.error-banner svg {
  flex-shrink: 0;
}

.close-error {
  margin-left: auto;
  background: transparent;
  border: none;
  color: #dc2626;
  font-size: 24px;
  cursor: pointer;
  padding: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
}

.close-error:hover {
  background: #fee2e2;
}

.input-wrapper {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

textarea {
  flex: 1;
  min-height: 44px;
  max-height: 120px;
  padding: 12px 16px;
  border: 2px solid #fed7aa;
  border-radius: 12px;
  font-size: 15px;
  font-family: inherit;
  resize: none;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
  background: #fffbeb;
}

textarea:focus {
  border-color: #fb923c;
  box-shadow: 0 0 0 3px rgba(251, 146, 60, 0.1);
}

textarea:disabled {
  background: #f1f5f9;
  cursor: not-allowed;
}

.send-btn {
  width: 44px;
  height: 44px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #fde68a 0%, #f97316 100%);
  color: #7c2d12;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
}

.send-btn:hover:not(:disabled) {
  box-shadow: 0 6px 16px rgba(249, 115, 22, 0.3);
  transform: translateY(-2px);
}

.send-btn:disabled {
  background: #e5e7eb;
  color: #9ca3af;
  cursor: not-allowed;
  opacity: 0.6;
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  100% {
    transform: rotate(360deg);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .message-content {
    max-width: 85%;
  }
  
  .ai-chat-header h2 {
    font-size: 18px;
  }
}

/* 自定义清空确认弹窗 */
.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.25); display: grid; place-items: center; z-index: 2000; }
.modal { background: #fff; width: 92%; max-width: 420px; border-radius: 16px; box-shadow: 0 20px 60px rgba(0,0,0,0.2); overflow: hidden; }
.modal-header { background: linear-gradient(135deg, #fff7ed 0%, #fde68a 45%, #f97316 100%); color:#7c2d12; padding: 14px 16px; font-weight: 800; }
.modal-body { padding: 16px; color:#0f172a; }
.modal-footer { display:flex; justify-content:flex-end; gap: 8px; padding: 12px 16px; background:#f9fafb; }
.btn { border:none; border-radius: 10px; padding: 8px 12px; font-weight: 700; cursor: pointer; }
.btn.cancel { background:#fff; border:1px solid #e5e7eb; color:#334155; }
.btn.danger { background: linear-gradient(135deg, #fecaca 0%, #ef4444 100%); color:#fff; }
</style>
