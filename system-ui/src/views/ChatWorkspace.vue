<template>
  <div class="chat-page">
    <section class="chat-sidebar">
      <header class="sidebar-header">
        <div>
          <h2>会话</h2>
          <p class="hint">{{ sidebarHint }}</p>
        </div>
        <button class="ghost" type="button" @click="refreshSessions" :disabled="sessionLoading">
          <span v-if="sessionLoading">刷新中...</span>
          <span v-else>刷新</span>
        </button>
      </header>
      <div class="sidebar-controls">
        <input
          v-model="keyword"
          class="search"
          type="search"
          placeholder="搜索昵称或订单号"
        />
        <div class="filter-tabs" v-if="filterTabs.length > 1">
          <button
            v-for="tab in filterTabs"
            :key="tab.value"
            type="button"
            :class="['tab', { active: activeFilter === tab.value }]"
            @click="activateFilter(tab.value)"
          >
            {{ tab.label }}
            <span v-if="tab.value !== 'ALL' && filterUnread(tab.value)">
              {{ filterUnread(tab.value) }}
            </span>
          </button>
        </div>
      </div>
  <div class="session-list">
        <p v-if="sessionLoading" class="placeholder">加载会话中...</p>
        <p v-else-if="visibleSessions.length === 0" class="placeholder">暂无会话</p>
        <button
          v-for="session in visibleSessions"
          :key="session.sessionId"
          type="button"
          :class="['session-item', { active: session.sessionId === activeSessionId }]"
          @click="openSession(session)"
        >
          <div class="avatar">
            <img v-if="session.peerAvatar" :src="session.peerAvatar" :alt="session.peerName" />
            <span v-else>{{ initials(session.peerName) }}</span>
          </div>
          <div class="session-meta">
            <div class="row">
              <span class="name">{{ session.peerName }}</span>
              <span class="time">{{ shortTime(session.lastMessageTime) }}</span>
            </div>
            <div class="row">
              <span class="snippet">{{ session.lastMessage || '暂无消息' }}</span>
              <span v-if="Number(session.unreadCount) > 0" class="badge">{{ Number(session.unreadCount) > 10 ? '10+' : Number(session.unreadCount) }}</span>
            </div>
            <div class="order">
              <span>订单 {{ session.orderNo || ('#' + session.orderId) }}</span>
              <small v-if="session.orderStatus">{{ zhStatus(session.orderStatus) }}</small>
            </div>
          </div>
        </button>
      </div>
    </section>

    <section class="chat-thread">
      <header class="thread-header">
        <div v-if="activeSession" class="thread-title">
          <h3>{{ activeSession.peerName }}</h3>
          <div class="chip-group">
            <span class="chip">{{ roleLabel(activeSession.peerRole) }}</span>
            <span class="chip">订单 {{ activeSession.orderNo || ('#' + activeSession.orderId) }}</span>
          </div>
        </div>
        <div v-else class="thread-title">
          <h3>选择一个会话</h3>
          <p class="hint">从左侧选择客户或骑手以查看聊天记录</p>
        </div>
        <span :class="['ws-state', connectionStatusClass]">{{ connectionStatusLabel }}</span>
      </header>

      <transition name="fade">
        <div v-if="connectionNotice" class="connection-banner">{{ connectionNotice }}</div>
      </transition>

      <div ref="messageContainer" class="message-scroller" @scroll="onMessageScroll">
        <div v-if="!activeSession" class="thread-placeholder">
          <p>支持实时消息、未读提醒和订单侧边栏。请选择一个会话开始交流...</p>
        </div>
        <template v-else>
          <transition name="fade">
            <button
              v-if="showTopLoader"
              class="load-more"
              type="button"
              @click="loadMore"
              :disabled="messageLoading"
            >
              {{ messageLoading ? '加载中...' : '查看更多历史消息' }}
            </button>
          </transition>
        <transition-group name="list-fade" tag="div">
          <div
            v-for="(message, idx) in activeMessages"
            :key="message.id"
            :class="['message', message.senderRole === selfRole ? 'outgoing' : 'incoming', message.status === 'pending' ? 'pending' : '', message.status === 'failed' ? 'failed' : '']"
          >
            <div class="avatar small" @click="openUserProfile(message)" :title="messageDisplayName(message)">
              <img v-if="messageAvatar(message)" :src="messageAvatar(message)" :alt="messageDisplayName(message)" />
              <span v-else>{{ initials(messageDisplayName(message)) }}</span>
            </div>
            <div class="message-body">
              <div :class="['name-line', message.senderRole === selfRole ? 'right' : 'left']">{{ messageDisplayName(message) }}</div>
              <div class="bubble" :data-index="idx">
                <p class="text">{{ text30(message.content) }}</p>
                <span class="timestamp">{{ fullTime(message.createdAt) }}</span>
              </div>
              <div
                v-if="message.senderRole === selfRole && messageStatusLabel(message)"
                class="bubble-status"
              >
                <span
                  :class="['status-flag', message.status]"
                  :title="message.status === 'failed' && message.error ? message.error : ''"
                >
                  {{ messageStatusLabel(message) }}
                </span>
                <button
                  v-if="message.status === 'failed'"
                  type="button"
                  class="link retry-send"
                  @click="retryMessage(message)"
                  :disabled="retryingMessages.has(message.clientTempId ?? 0)"
                >
                  重试
                </button>
              </div>
            </div>
          </div>
          </transition-group>
          <p v-if="activeMessages.length === 0 && !messageLoading" class="thread-placeholder">
            暂无聊天记录，向 {{ activeSession.peerName }} 打招呼吧
          </p>
        </template>
      </div>

      <footer v-if="activeSession" class="composer">
        <textarea
          ref="composerArea"
          v-model="composerText"
          placeholder="输入消息，Enter 换行，Ctrl + Enter 发送"
          @keydown="onComposerKey"
        ></textarea>
        <div class="composer-actions">
          <span class="tip" v-if="sendError">{{ sendError }}</span>
          <button type="button" class="primary" @click="handleSend" :disabled="sendDisabled">
            {{ sending ? '发送中...' : '发送' }}
          </button>
        </div>
      </footer>
      <footer v-else class="composer disabled">
        <p>选择会话以启用输入框</p>
      </footer>
    </section>

    <aside class="chat-details">
      <transition name="slide-fade">
      <div v-if="activeOrder" key="order" class="order-card">
        <div class="card-header">
          <h3>订单信息</h3>
          <span class="status">{{ activeOrder.status || '未知状态' }}</span>
        </div>
        <dl>
          <div>
            <dt>订单号</dt>
            <dd>{{ activeOrder.orderNo || ('#' + activeOrder.orderId) }}</dd>
          </div>
          <div>
            <dt>备注</dt>
            <dd>{{ activeOrder.remark || '无' }}</dd>
          </div>
          <div>
            <dt>创建时间</dt>
            <dd>{{ fullTime(activeOrder.createdAt) }}</dd>
          </div>
        </dl>
        <div v-if="relatedOrders.length > 1" class="mini-orders">
          <h4>最近相关订单</h4>
          <ul>
            <li v-for="o in relatedOrders" :key="o.orderId">
              <span class="ono">{{ o.orderNo || ('#' + o.orderId) }}</span>
              <small class="ostat">{{ zhStatus(o.status) }}</small>
              <time class="otime">{{ shortTime(o.createdAt) }}</time>
            </li>
          </ul>
        </div>
        <div class="more-orders">
          <button type="button" class="link" @click="openOrdersModal()">... 查看更多订单</button>
        </div>
      </div>
      <div v-else key="empty" class="order-card muted">
        <h3>订单信息</h3>
        <p>选择会话后展示对应订单详情，便于快速响应客户需求...</p>
      </div>
      </transition>
      <section class="tips">
        <h4>使用提示</h4>
        <ul>
          <li>实时消息通过 WebSocket 推送，右上角可查看连接状态...</li>
          <li>新消息会在左侧会话列表显示红点提醒...</li>
          <li>支持按订单分组的三方会话（客户、商家、骑手）...</li>
        </ul>
      </section>
    </aside>
    <!-- Modal: 全部相关订单 -->
    <Teleport to="body">
      <transition name="fade">
        <div v-if="ordersModalOpen" class="modal-backdrop" @click.self="closeOrdersModal()">
          <div class="modal-panel">
            <div class="modal-header">
              <h3 class="modal-title">全部相关订单</h3>
              <button class="ghost" type="button" @click="closeOrdersModal()">关闭</button>
            </div>
            <div class="modal-body">
              <ul class="order-list">
                <li v-for="o in ordersModalItems" :key="o.orderId" class="order-item">
                  <div>
                    <div class="title"><strong>订单号</strong> {{ o.orderNo || ('#' + o.orderId) }}
                      <span :class="['status-chip', statusClass(o.status)]">{{ zhStatus(o.status) }}</span>
                    </div>
                    <div class="meta">备注：{{ o.remark || '无' }}</div>
                  </div>
                  <div class="meta">下单时间：{{ shortTime(o.createdAt) }}</div>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { Client } from '@stomp/stompjs'
import type { IFrame, IMessage } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { ChatHistoryPayload, ChatMessageDto, ChatOrderSnippet, ChatRole, ChatSessionSummary, SendMessagePayload } from '../api/chat'
import { fetchChatHistory, fetchChatSessions, markChatRead, sendChatMessage } from '../api/chat'
import { useAuthStore } from '../stores/auth'
import { getOrderDetail } from '../api/order'

type ChatMessageViewModel = ChatMessageDto & {
  clientTempId?: number
  status?: 'pending' | 'sent' | 'failed'
  error?: string | null
  attempts?: number
}

type PendingMessageMeta = {
  payload: SendMessagePayload
  sessionId: string
  attempts: number
}


const auth = useAuthStore()
auth.load?.()
const route = useRoute()
const router = useRouter()

const sessions = ref<ChatSessionSummary[]>([])
const sessionLoading = ref(false)
const keyword = ref('')
const activeFilter = ref<'ALL' | 'GUEST' | 'DELIVERYMAN' | 'RESTAURATEUR'>('ALL')
const activeSessionId = ref<string | null>(null)
const messageContainer = ref<HTMLDivElement | null>(null)
const composerArea = ref<HTMLTextAreaElement | null>(null)
const messagesBySession = reactive<Record<string, ChatMessageViewModel[]>>({})
const historyState = reactive<Record<string, { hasMore: boolean; oldestId?: number }>>({})
const messageLoading = ref(false)
const showTopLoaderByScroll = ref(false)
const composerText = ref('')
const sending = ref(false)
const sendError = ref('')
const connectionNotice = ref('')
const retryingMessages = ref<Set<number>>(new Set())

const pendingMessages = new Map<number, PendingMessageMeta>()
let tempIdSeed = 0

const messagePageSize = 10
const connectionStatus = ref<'DISCONNECTED' | 'CONNECTING' | 'CONNECTED'>('DISCONNECTED')
let stompClient: Client | null = null
let refreshTimer: number | undefined
let reconnectTimer: number | undefined

const selfRole = computed(() => auth.role)
const chatSelfRole = computed<ChatRole | null>(() => {
  if (selfRole.value === 'GUEST' || selfRole.value === 'RESTAURATEUR' || selfRole.value === 'DELIVERYMAN') {
    return selfRole.value
  }
  return null
})

const sidebarHint = computed(() => {
  if (!selfRole.value) {
    return '登录后加载专属会话列表'
  }
  if (selfRole.value === 'RESTAURATEUR') {
    return '按订单整理的客户与骑手会话'
  }
  if (selfRole.value === 'DELIVERYMAN') {
    return '与商家及客户保持实时沟通'
  }
  return '查看订单相关会话'
})

const filterTabs = computed(() => {
  if (selfRole.value === 'RESTAURATEUR') {
    return [
      { value: 'ALL' as const, label: '全部' },
      { value: 'GUEST' as const, label: '顾客' },
      { value: 'DELIVERYMAN' as const, label: '骑手' },
    ]
  }
  if (selfRole.value === 'DELIVERYMAN') {
    return [
      { value: 'ALL' as const, label: '全部' },
      { value: 'RESTAURATEUR' as const, label: '商家' },
      { value: 'GUEST' as const, label: '顾客' },
    ]
  }
  return [
    { value: 'ALL' as const, label: '全部' },
    { value: 'RESTAURATEUR' as const, label: '商家' },
    { value: 'DELIVERYMAN' as const, label: '骑手' },
  ]
})

const visibleSessions = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return sessions.value.filter((item) => {
    const matchKeyword =
      !kw ||
      item.peerName.toLowerCase().includes(kw) ||
      (item.orderNo?.toLowerCase().includes(kw) ?? false) ||
      String(item.orderId).includes(kw)
    const matchFilter = activeFilter.value === 'ALL' || item.peerRole === activeFilter.value
    return matchKeyword && matchFilter
  })
})

const activeSession = computed(() => {
  if (!activeSessionId.value) return null
  return sessions.value.find((item) => item.sessionId === activeSessionId.value) ?? null
})

const activeMessages = computed(() => {
  if (!activeSessionId.value) {
    return []
  }
  return messagesBySession[activeSessionId.value] ?? []
})

const showTopLoader = computed(() => {
  const key = activeSession.value?.sessionId
  if (!key) return false
  const hasMore = historyState[key]?.hasMore
  return !!hasMore && showTopLoaderByScroll.value
})

const orderBySession = reactive<Record<string, ChatOrderSnippet>>({})

const activeOrder = computed<ChatOrderSnippet | null>(() => {
  if (!activeSession.value) return null
  const stored = orderBySession[activeSession.value.sessionId]
  return stored ?? null
})

// 最近相关订单（最多 3 条）：基于消息中的 orderId 去重并按最近消息时间排序
const relatedOrders = computed(() => {
  const sess = activeSession.value
  const msgs = activeMessages.value
  if (!sess || !msgs.length) return [] as Array<{orderId:number; orderNo?:string|null; status?:string|null; createdAt?:string|null}>
  const latestByOrder = new Map<number, string>()
  for (let i = msgs.length - 1; i >= 0; i--) {
    const m = msgs[i]
    if (!m) continue
    if (!latestByOrder.has(m.orderId)) {
      latestByOrder.set(m.orderId, m.createdAt)
    }
  }
  const items = Array.from(latestByOrder.entries())
    .sort((a,b) => new Date(b[1]).getTime() - new Date(a[1]).getTime())
    .slice(0,3)
    .map(([oid, time]) => {
      const base = orderBySession[sess.sessionId]
      if (base) {
        return { orderId: oid, orderNo: base.orderNo, status: base.status, createdAt: base.createdAt ?? time }
      }else {
        return { orderId: oid }
      }
    })
  return items
})

const sendDisabled = computed(() => {
  if (sending.value) return true
  if (!activeSession.value) return true
  return !composerText.value.trim()
})

const connectionStatusLabel = computed(() => {
  switch (connectionStatus.value) {
    case 'CONNECTED':
      return '已连接'
    case 'CONNECTING':
      return '连接中...'
    default:
      return '未连接'
  }
})

const connectionStatusClass = computed(() => {
  return {
    connected: connectionStatus.value === 'CONNECTED',
    connecting: connectionStatus.value === 'CONNECTING',
    disconnected: connectionStatus.value === 'DISCONNECTED',
  }
})

function roleLabel(role: string) {
  if (role === 'GUEST') return '顾客'
  if (role === 'DELIVERYMAN') return '骑手'
  return '商家'
}

function activateFilter(value: typeof activeFilter.value) {
  activeFilter.value = value
}

function initials(name?: string | null) {
  if (!name) return '?'
  return name.trim().slice(0, 1).toUpperCase()
}

function shortTime(value?: string | null) {
  if (!value) return ''
  try {
    const date = new Date(value)
    return new Intl.DateTimeFormat('zh-CN', {
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    }).format(date)
  } catch (err) {
    console.warn('无法格式化时间', err)
    return value
  }
}
function text30(s?: string | null, n = 30) {
  if (!s) return ''
  const arr = Array.from(s)
  return arr.length > n ? arr.slice(0, n).join('') + '...' : s
}

function zhStatus(code?: string | null) {
  const c = (code || '').toUpperCase()
  switch (c) {
    case 'PENDING': return '待处理'
    case 'PROCESSING': return '制作中'
    case 'IN_PROGRESS': return '进行中'
    case 'READY': return '待取件'
    case 'COMPLETED': return '已完成'
    case 'CANCELED': return '已取消'
    default: return '未知'
  }
}

function statusClass(code?: string | null) {
  const c = (code || '').toLowerCase()
  if (c === 'pending') return 'pending'
  if (c === 'processing') return 'processing'
  if (c === 'in_progress' || c === 'in-progress') return 'in-progress'
  if (c === 'ready') return 'ready'
  if (c === 'completed') return 'completed'
  if (c === 'canceled' || c === 'cancelled') return 'canceled'
  return 'unknown'
}

// 规范化未读计数，防止后端返回 '10+' 字符串或 null 造成前端显示异常
function normalizeUnread(x: unknown): number {
  if (typeof x === 'number' && Number.isFinite(x)) return x
  if (typeof x === 'string') {
    const n = parseInt(x, 10)
    return Number.isFinite(n) && n >= 0 ? n : 0
  }
  return 0
}


function fullTime(value?: string | null) {
  if (!value) return ''
  try {
    const date = new Date(value)
    return new Intl.DateTimeFormat('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    }).format(date)
  } catch (err) {
    return value
  }
}

function filterUnread(role: string) {
  return sessions.value
    .filter((item) => item.peerRole === role)
    .reduce((sum, item) => sum + (item.unreadCount || 0), 0)
}

async function refreshSessions() {
  if (!auth.username) return
  sessionLoading.value = true
  try {
    const res = await fetchChatSessions({ username: auth.username })
    if (res.status === 200 && Array.isArray(res.data)) {
      const sorted = [...res.data].sort((a, b) => {
        const ta = a.lastMessageTime ? new Date(a.lastMessageTime).getTime() : 0
        const tb = b.lastMessageTime ? new Date(b.lastMessageTime).getTime() : 0
        return tb - ta
      })
      // 统一 unreadCount 规范为数字，避免出现 '10+' 字符串在前端被直接渲染
      const normalized = sorted.map((s: any) => ({
        ...s,
        unreadCount: normalizeUnread(s?.unreadCount),
      }))
      sessions.value = normalized
      // 若当前会话处于打开状态，则本地强制清零其未读，避免刷新闪红点
      if (activeSessionId.value) {
        const cur = sessions.value.find(s => s.sessionId === activeSessionId.value)
        if (cur) cur.unreadCount = 0
      }
      syncActiveSession()
      // 路由直达：若带有 orderId 查询参数，则打开该订单的首个会话
      const orderIdParam = route.query.orderId ? Number(route.query.orderId) : undefined
      if (orderIdParam && Number.isFinite(orderIdParam)) {
        const target = sessions.value.find((s) => s.orderId === orderIdParam)
        if (target) {
          openSession(target)
        }
      }
    }
  } catch (err) {
    console.error('加载会话失败', err)
  } finally {
    sessionLoading.value = false
  }
}

function syncActiveSession() {
  if (!activeSessionId.value) return
  const exists = sessions.value.some((item) => item.sessionId === activeSessionId.value)
  if (!exists) {
    const first = sessions.value[0]
    activeSessionId.value = first ? first.sessionId : null
  }
}

function sessionKey(message: ChatMessageDto) {
  if (message.sessionId) {
    return message.sessionId
  }
  const currentRole = chatSelfRole.value
  if (!currentRole) {
    return `${message.orderId}:${message.senderRole}:${message.senderId}`
  }
  const isSelfSender = message.senderRole === currentRole
  const peerRole = isSelfSender ? message.receiverRole : message.senderRole
  const peerId = isSelfSender ? message.receiverId : message.senderId
  return `${message.orderId}:${peerRole}:${peerId}`
}

async function openSession(session: ChatSessionSummary) {
  session.unreadCount = 0
  const sync = sessions.value.find((s) => s.sessionId === session.sessionId)
  if (sync) sync.unreadCount = 0
  if (activeSessionId.value === session.sessionId) {
    markSessionAsRead(session)
    return
  }
  activeSessionId.value = session.sessionId
  const cached = messagesBySession[session.sessionId]
  if (!cached || cached.length === 0) {
    await loadHistory({
      orderId: session.orderId,
      peerId: session.peerId,
      peerRole: session.peerRole,
    })
  }
  markSessionAsRead(session)
  // 本地把对方的消息标记为已读
  markLocalIncomingAsRead(session.sessionId)
  await nextTick(() => scrollToLast(true))
  focusComposer()
}

async function loadHistory(payload: ChatHistoryPayload, prepend = false) {
  if (messageLoading.value) return
  messageLoading.value = true
  try {
    const beforeScrollHeight = messageContainer.value?.scrollHeight || 0
    const beforeScrollTop = messageContainer.value?.scrollTop || 0
    const request: ChatHistoryPayload = { ...payload, size: messagePageSize }
    if (auth.username) {
      request.username = auth.username
    }
    const res = await fetchChatHistory(request)
    if (res.status === 200 && res.data) {
      const { messages = [], order } = res.data
      const ordered = [...messages].sort((a, b) => a.id - b.id)
      const key = activeSessionId.value ?? `${payload.orderId}:${payload.peerRole}:${payload.peerId}`
      const existing = messagesBySession[key] ?? []
      const updated = prepend ? [...ordered, ...existing] : [...existing, ...ordered]
      messagesBySession[key] = updated
      const oldest = updated[0]
      historyState[key] = {
        hasMore: messages.length >= messagePageSize,
        oldestId: oldest ? oldest.id : undefined,
      }
      if (order) {
        orderBySession[key] = order
      } else {
        // 回退：至少填充基本订单信息以显示右侧卡片
        const ss = sessions.value.find((s) => s.sessionId === key)
        if (ss) {
          orderBySession[key] = {
            orderId: ss.orderId,
            orderNo: ss.orderNo,
            status: ss.orderStatus ?? undefined,
            remark: ss.orderRemark ?? undefined,
          }
        }
      }
      await nextTick()
      if (prepend && messageContainer.value) {
        const afterScrollHeight = messageContainer.value.scrollHeight
        messageContainer.value.scrollTop = beforeScrollTop + (afterScrollHeight - beforeScrollHeight)
      } else if (!prepend) {
        scrollToLast()
      }
    }
  } catch (err) {
    console.error('加载聊天记录失败', err)
  } finally {
    messageLoading.value = false
  }
}

async function loadMore() {
  const session = activeSession.value
  if (!session) return
  const key = session.sessionId
  const oldestId = historyState[key]?.oldestId
  if (!oldestId) return
  await loadHistory(
    {
      orderId: session.orderId,
      peerId: session.peerId,
      peerRole: session.peerRole,
      beforeId: oldestId,
    },
    true,
  )
}

function markSessionAsRead(session: ChatSessionSummary) {
  const target = sessions.value.find((item) => item.sessionId === session.sessionId)
  if (target) {
    target.unreadCount = 0
  }
  const payload: ChatHistoryPayload = {
    orderId: session.orderId,
    peerId: session.peerId,
    peerRole: session.peerRole,
  }
  if (auth.username) {
    payload.username = auth.username
  }
  markChatRead(payload).catch((err) => console.warn('标记已读失败', err))
}

function markLocalIncomingAsRead(sessionId: string) {
  const list = messagesBySession[sessionId]
  if (!list || !chatSelfRole.value) return
  const self = chatSelfRole.value
  const cloned = [...list]
  for (let i = 0; i < cloned.length; i++) {
    const item = cloned[i]
    if (item && item.senderRole !== self) item.read = true
  }
  messagesBySession[sessionId] = cloned
}

function onComposerKey(event: KeyboardEvent) {
  sendError.value = ''
  if (event.key === 'Enter' && event.ctrlKey) {
    event.preventDefault()
    handleSend()
  }
}

async function handleSend() {
  const session = activeSession.value
  const text = composerText.value.trim()
  if (!session || !text) return
  if (!auth.username || !chatSelfRole.value) {
    sendError.value = '请先登录'
    return
  }
  sendError.value = ''
  sending.value = true

  const payload: SendMessagePayload = {
    orderId: session.orderId,
    receiverId: session.peerId,
    receiverRole: session.peerRole,
    content: text,
    username: auth.username,
  }

  const tempId = generateTempId()
  const pendingMessage: ChatMessageViewModel = {
    id: tempId,
    sessionId: session.sessionId,
    orderId: session.orderId,
    senderId: 0,
    senderRole: chatSelfRole.value,
    receiverId: session.peerId,
    receiverRole: session.peerRole,
    content: text,
    read: false,
    createdAt: new Date().toISOString(),
    clientTempId: tempId,
    status: 'pending',
    attempts: 1,
    error: null,
  }

  appendMessage(session.sessionId, pendingMessage)
  upsertSessionMeta(session.sessionId, pendingMessage)

  const sessionRecord = sessions.value.find((item) => item.sessionId === session.sessionId)
  if (sessionRecord) {
    sessionRecord.unreadCount = 0
    sessionRecord.lastMessage = pendingMessage.content
    sessionRecord.lastMessageTime = pendingMessage.createdAt
  }

  const meta: PendingMessageMeta = {
    payload: { ...payload },
    sessionId: session.sessionId,
    attempts: 1,
  }
  pendingMessages.set(tempId, meta)

  composerText.value = ''
  await nextTick(() => scrollToLast(true))
  focusComposer()

  try {
    await sendViaTransport(tempId, meta)
  } catch (err: any) {
    const reason = err?.message || '发送失败'
    sendError.value = reason
    markMessageFailed(tempId, reason)
    refreshSessions()
    console.error('发送消息失败', err)
  } finally {
    sending.value = false
  }
}

function appendMessage(key: string, message: ChatMessageViewModel) {
  const list = messagesBySession[key] ?? []
  if (message.id > 0 && list.some((item) => item.id === message.id)) {
    return
  }
  const enriched: ChatMessageViewModel = {
    ...message,
    status: message.status ?? (message.id > 0 ? 'sent' : 'pending'),
    error: message.error ?? null,
  }
  if (enriched.clientTempId == null && enriched.id < 0) {
    enriched.clientTempId = enriched.id
  }
  const updated = [...list, enriched]
  messagesBySession[key] = updated
  const oldest = updated[0]
  historyState[key] = {
    hasMore: historyState[key]?.hasMore ?? false,
    oldestId: oldest ? oldest.id : undefined,
  }
  if (key === activeSessionId.value) {
    nextTick(() => scrollToLast(true))
  }
}


function generateTempId(): number {
  tempIdSeed += 1
  return -(Date.now() + tempIdSeed)
}

async function sendViaTransport(tempId: number, meta: PendingMessageMeta) {
  const payloadWithId: SendMessagePayload = {
    ...meta.payload,
    clientMessageId: String(tempId),
  }
  const client = stompClient
  const useWebSocket = !!client && connectionStatus.value === 'CONNECTED'
  if (useWebSocket && client) {
    client.publish({
      destination: wsSendDestination(),
      body: JSON.stringify(payloadWithId),
    })
    return
  }
  const res = await sendChatMessage(payloadWithId)
  if (res.status !== 200 || !res.data) {
    throw new Error(res.message || '发送失败')
  }
  replacePendingWithDelivered(tempId, meta.sessionId, res.data)
}

function replacePendingWithDelivered(tempId: number, sessionId: string, delivered: ChatMessageDto) {
  const list = messagesBySession[sessionId] ?? []
  const idx = list.findIndex((m) => (m.clientTempId ?? m.id) === tempId)
  const normalized: ChatMessageViewModel = {
    ...delivered,
    status: 'sent',
    error: null,
    clientTempId: tempId,
  }
  if (idx >= 0) {
    const next = [...list]
    next[idx] = normalized
    messagesBySession[sessionId] = next
  } else {
    appendMessage(sessionId, normalized)
  }
  pendingMessages.delete(tempId)
  removeRetryingFlag(tempId)
  upsertSessionMeta(sessionId, normalized)
}

function markMessageFailed(tempId: number, reason: string) {
  for (const [key, value] of Object.entries(messagesBySession)) {
    const idx = value.findIndex((m) => (m.clientTempId ?? m.id) === tempId)
    if (idx >= 0) {
      const next = [...value]
      const original = next[idx]!
      const target: ChatMessageViewModel = {
        ...original,
        status: 'failed',
        error: reason,
        clientTempId: tempId,
        attempts: original?.attempts ?? 1,
        id: tempId,
      }
      next[idx] = target
      messagesBySession[key] = next
      break
    }
  }
}

function messageStatusLabel(message: ChatMessageViewModel) {
  if (message.status === 'pending') {
    return '发送中'
  }
  if (message.status === 'failed') {
    return '发送失败'
  }
  return ''
}

async function retryMessage(message: ChatMessageViewModel) {
  const tempId = message.clientTempId ?? message.id
  if (tempId == null) return
  const meta = pendingMessages.get(tempId)
  if (!meta) return
  const list = messagesBySession[meta.sessionId] ?? []
  const idx = list.findIndex((m) => (m.clientTempId ?? m.id) === tempId)
  if (idx < 0) return
  const next = [...list]
  const current = next[idx]!
  const refreshed: ChatMessageViewModel = {
    ...current,
    status: 'pending',
    error: null,
    attempts: (current.attempts ?? 1) + 1,
    createdAt: new Date().toISOString(),
    clientTempId: tempId,
    id: tempId,
  }
  next[idx] = refreshed
  messagesBySession[meta.sessionId] = next

  meta.attempts += 1
  pendingMessages.set(tempId, meta)

  setRetryingFlag(tempId)
  try {
    await sendViaTransport(tempId, meta)
  } catch (err: any) {
    const reason = err?.message || '发送失败'
    sendError.value = reason
    markMessageFailed(tempId, reason)
    console.error('重试发送失败', err)
  } finally {
    removeRetryingFlag(tempId)
  }
}

function setRetryingFlag(tempId: number) {
  const next = new Set(retryingMessages.value)
  next.add(tempId)
  retryingMessages.value = next
}

function removeRetryingFlag(tempId: number) {
  if (!retryingMessages.value.has(tempId)) {
    return
  }
  const next = new Set(retryingMessages.value)
  next.delete(tempId)
  retryingMessages.value = next
}

function scheduleReconnect() {
  if (reconnectTimer || !auth.username) {
    return
  }
  reconnectTimer = window.setTimeout(() => {
    reconnectTimer = undefined
    if (auth.username) {
      connectWebSocket()
    }
  }, 3000)
}

function clearReconnectTimer() {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = undefined
  }
}

function scrollToBottom() {
  if (!messageContainer.value) return
  messageContainer.value.scrollTop = messageContainer.value.scrollHeight
}

function scrollToLast(center = false) {
  if (!messageContainer.value) return
  try {
    const el = messageContainer.value.querySelector('.message:last-child') as HTMLElement | null
    if (el) {
      el.scrollIntoView({ behavior: 'auto', block: center ? 'center' : 'end' })
    } else {
      scrollToBottom()
    }
  } catch {
    scrollToBottom()
  }
}

function focusComposer() {
  nextTick(() => composerArea.value?.focus())
}

function messageAvatar(message: ChatMessageDto) {
  if (!activeSession.value) return ''
  const isSelf = message.senderRole === chatSelfRole.value
  return isSelf ? (auth.avatarUrl || '') : (activeSession.value.peerAvatar || '')
}

function messageDisplayName(message: ChatMessageDto) {
  if (!activeSession.value) return ''
  const isSelf = message.senderRole === chatSelfRole.value
  return isSelf ? (auth.nickname || auth.username || '我') : (activeSession.value.peerName || '对方')
}

function openUserProfile(message: ChatMessageDto) {
  const userId = message.senderId
  router.push({ name: 'user-public-profile', params: { identifier: 'id' }, query: { userId: String(userId ?? '') } })
}

const ordersModalOpen = ref(false)
const ordersModalItems = ref<Array<{orderId:number; orderNo?:string|null; status?:string|null; remark?:string|null; createdAt?:string|null}>>([])

function openOrdersModal() {
  const sess = activeSession.value
  if (!sess) return
  const msgs = activeMessages.value
  const latestByOrder = new Map<number, {createdAt:string, orderNo?:string|null; status?:string|null; remark?:string|null}>()
  for (let i = msgs.length - 1; i >= 0; i--) {
    const m = msgs[i]
    if (!m) continue
    const existed = latestByOrder.get(m.orderId)
    if (!existed) {
      const base = orderBySession[sess.sessionId]
      latestByOrder.set(m.orderId, {
        createdAt: m.createdAt,
        orderNo: base && base.orderId === m.orderId ? base.orderNo : undefined,
        status: base && base.orderId === m.orderId ? base.status : undefined,
        remark: base && base.orderId === m.orderId ? base.remark : undefined,
      })
    }
  }
  // 补齐订单编号与状态：
  // - 商家端优先调用 getOrderDetail(orderId, restaurateurId)
  // - 其它角色退化为调用 fetchChatHistory 获取 order snippet
  const entries = Array.from(latestByOrder.entries())
  const restaurateurIdMaybe = auth.restaurant?.id ? Number(auth.restaurant.id) : undefined
  const tasks = entries.map(async ([oid, meta]) => {
    let orderNo = meta.orderNo
    let status = meta.status
    let remark = meta.remark
    let createdAt = meta.createdAt
    try {
      if (selfRole.value === 'RESTAURATEUR' && restaurateurIdMaybe && Number.isFinite(restaurateurIdMaybe)) {
        const d = await getOrderDetail(oid, restaurateurIdMaybe)
        if (d.status === 200 && d.data) {
          orderNo = d.data.orderNo || orderNo
          status = d.data.status || status
          remark = d.data.remark || remark
          createdAt = d.data.createdAt || createdAt
        }
      } else {
        const req: ChatHistoryPayload = { orderId: oid, peerId: sess.peerId, peerRole: sess.peerRole, size: 1 }
        if (auth.username) req.username = auth.username
        const r = await fetchChatHistory(req)
        if (r.status === 200 && r.data?.order) {
          orderNo = r.data.order.orderNo || orderNo
          status = r.data.order.status || status
          remark = r.data.order.remark || remark
          createdAt = r.data.order.createdAt || createdAt
        }
      }
    } catch {}
    return { orderId: oid, orderNo, status, remark, createdAt }
  })
  Promise.all(tasks).then((list) => {
    ordersModalItems.value = list.sort((a,b) => new Date(b.createdAt ?? 0).getTime() - new Date(a.createdAt ?? 0).getTime())
    ordersModalOpen.value = true
  })
}
function closeOrdersModal() { ordersModalOpen.value = false }

function resolveWsUrl() {
  // Always use HTTP(S) URL for SockJS endpoint; proxy will handle it in dev
  const raw = (import.meta.env.VITE_WS_CHAT_ENDPOINT as string | undefined) || '/ws/chat'
  if (raw.startsWith('http')) return raw
  const base = (import.meta.env.VITE_API_BASE_URL as string | undefined) || window.location.origin
  const normalizedBase = base.endsWith('/') ? base.slice(0, -1) : base
  const normalizedPath = raw.startsWith('/') ? raw : `/${raw}`
  return normalizedBase + normalizedPath
}

function wsSendDestination() {
  return (import.meta.env.VITE_WS_CHAT_SEND_DEST as string | undefined) || '/app/chat/send'
}

function wsSubscriptionPath() {
  return (import.meta.env.VITE_WS_CHAT_SUB as string | undefined) || '/user/queue/chat'
}

function connectWebSocket() {
  if (!auth.username) return
  clearReconnectTimer()
  connectionStatus.value = 'CONNECTING'
  if (stompClient) {
    disconnectWebSocket(true)
  }
  const endpoint = resolveWsUrl()
  const headers: Record<string, string> = { username: auth.username }
  if (auth.token) {
    const scheme = auth.tokenType || 'Bearer'
    headers.Authorization = `${scheme} ${auth.token}`
  }
  const client = new Client({
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    connectHeaders: headers,
  })
  // Always use SockJS for compatibility with the server endpoint configured with withSockJS()
  client.webSocketFactory = () => new SockJS(endpoint)
  client.onConnect = () => {
    connectionStatus.value = 'CONNECTED'
    // Primary user-destination subscription
    client.subscribe(wsSubscriptionPath(), (message: IMessage) => handleSocketFrame(message))
    // Fallback topic channel for environments where user-destination mapping fails
    const uname = auth.username
    if (uname) {
      client.subscribe(`/topic/chat/${uname}`, (message: IMessage) => handleSocketFrame(message))
    }
  }
  client.onStompError = (frame: IFrame) => {
    console.error('STOMP 错误', frame)
    connectionStatus.value = 'DISCONNECTED'
  }
  client.onWebSocketClose = () => {
    connectionStatus.value = 'DISCONNECTED'
  }
  client.activate()
  stompClient = client
}

function disconnectWebSocket(silent = false) {
  if (stompClient) {
    try {
      stompClient.deactivate()
    } catch (err) {
      console.warn('断开 WebSocket 失败', err)
    }
    stompClient = null
  }
  clearReconnectTimer()
  if (!silent) {
    connectionStatus.value = 'DISCONNECTED'
  }
}

function handleSocketFrame(frame: IMessage) {
  if (!frame.body) return
  try {
    const payload = JSON.parse(frame.body)
    if (payload?.type === 'ERROR') {
      const reason = payload.message ?? '发送失败'
      sendError.value = reason
      const ref = payload.clientMessageId ? Number(payload.clientMessageId) : NaN
      if (!Number.isNaN(ref)) {
        markMessageFailed(ref, reason)
        removeRetryingFlag(ref)
      }
      return
    }
    if (payload?.type === 'SESSION_REFRESH') {
      refreshSessions()
      return
    }
    // 通用已读回执（后端若实现）
    if (payload?.type === 'READ_RECEIPT' || payload?.receipt) {
      applyReadReceipt(payload.receipt || payload)
    }
    if (payload?.session) {
      upsertSession(payload.session as ChatSessionSummary)
    }
    if (payload?.message) {
      integrateIncoming(payload.message as ChatMessageDto)
    }
  } catch (err) {
    console.warn('解析消息失败', err)
  }
}

function applyReadReceipt(receipt: any) {
  try {
    const key = activeSessionId.value
    if (!key || !messagesBySession[key]) return
    const mine = messagesBySession[key]
    const ids: number[] | undefined = receipt?.ids
    const untilId: number | undefined = receipt?.untilId
    if (Array.isArray(ids)) {
      for (const id of ids) {
        const idx = mine.findIndex((m) => m && m.id === id)
        if (idx >= 0 && mine[idx] && mine[idx]!.senderRole === chatSelfRole.value) mine[idx]!.read = true
      }
    } else if (typeof untilId === 'number') {
      for (let i = 0; i < mine.length; i++) {
        const m = mine[i]
        if (m && m.id > 0 && m.id <= untilId && m.senderRole === chatSelfRole.value) {
          m.read = true
        }
      }
    }
    messagesBySession[key] = [...mine]
  } catch {}
}

function upsertSession(session: ChatSessionSummary) {
  const idx = sessions.value.findIndex((item) => item.sessionId === session.sessionId)
  if (idx >= 0) {
    const merged: any = { ...sessions.value[idx], ...session }
    merged.unreadCount = normalizeUnread(merged.unreadCount)
    sessions.value[idx] = merged
  } else {
    const inserted: any = { ...session, unreadCount: normalizeUnread((session as any)?.unreadCount) }
    sessions.value = [inserted, ...sessions.value]
  }
}

function integrateIncoming(message: ChatMessageDto) {
  const key = sessionKey(message)
  const list = messagesBySession[key] ?? []
  const clientRef = message.clientMessageId ? Number(message.clientMessageId) : NaN
  let idx = -1
  if (!Number.isNaN(clientRef)) {
    idx = list.findIndex((m) => (m.clientTempId ?? m.id) === clientRef)
  }
  if (idx < 0) {
    idx = list.findIndex(
      (m) =>
        m.status === 'pending' &&
        m.content === message.content &&
        Math.abs(new Date(m.createdAt).getTime() - new Date(message.createdAt).getTime()) < 15_000 &&
        m.senderRole === message.senderRole,
    )
  }
  const normalized: ChatMessageViewModel = {
    ...message,
    status: 'sent',
    error: null,
    clientTempId: !Number.isNaN(clientRef) ? clientRef : undefined,
  }
  if (idx >= 0) {
    const next = [...list]
    next[idx] = normalized
    messagesBySession[key] = next
    if (!Number.isNaN(clientRef)) {
      pendingMessages.delete(clientRef)
      removeRetryingFlag(clientRef)
    }
  } else {
    appendMessage(key, normalized)
  }
  upsertSessionMeta(key, normalized)
  if (activeSessionId.value !== key) {
    const target = sessions.value.find((item) => item.sessionId === key)
    if (target) {
      target.unreadCount = (target.unreadCount || 0) + 1
    }
  } else {
    const session = sessions.value.find((item) => item.sessionId === key)
    if (session) {
      session.unreadCount = 0
    }
    if (message.senderRole !== chatSelfRole.value) {
      const payload: ChatHistoryPayload = {
        orderId: session?.orderId ?? message.orderId,
        peerId: session?.peerId ?? message.senderId,
        peerRole: session?.peerRole ?? message.senderRole,
      }
      if (auth.username) {
        payload.username = auth.username
      }
      markChatRead(payload).catch((err) => console.warn('实时标记已读失败', err))
      const listForRead = messagesBySession[key] ?? []
      const idx2 = listForRead.findIndex((m) => m.id === message.id)
      if (idx2 >= 0) {
        const next = [...listForRead]
        if (next[idx2]) {
          next[idx2].read = true
        }
        messagesBySession[key] = next
      }
    }
  }
}

function upsertSessionMeta(key: string, message: ChatMessageDto) {
  const target = sessions.value.find((item) => item.sessionId === key)
  if (!target) return
  target.lastMessage = message.content
  target.lastMessageTime = message.createdAt
}

function startAutoRefresh() {
  stopAutoRefresh()
  refreshTimer = window.setInterval(() => refreshSessions(), 60_000)
}

function stopAutoRefresh() {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = undefined
  }
}

watch(
  () => auth.username,
  (value) => {
    if (value) {
      refreshSessions()
      connectWebSocket()
      startAutoRefresh()
    } else {
      stopAutoRefresh()
      disconnectWebSocket()
    }
  },
  { immediate: true },
)

watch(
  connectionStatus,
  (status) => {
    if (status === 'CONNECTED') {
      connectionNotice.value = ''
      clearReconnectTimer()
    } else if (status === 'CONNECTING') {
      connectionNotice.value = '聊天服务连接中...'
      clearReconnectTimer()
    } else if (status === 'DISCONNECTED') {
      if (!auth.username) {
        connectionNotice.value = ''
        clearReconnectTimer()
        return
      }
      connectionNotice.value = '聊天连接已断开，正在尝试重新连接...'
      scheduleReconnect()
    }
  },
)

watch(selfRole, () => {
  activeFilter.value = 'ALL'
})

onMounted(() => {
  if (auth.username) {
    refreshSessions()
    connectWebSocket()
    startAutoRefresh()
  }
  // 若直接从订单详情跳入，尝试根据 query 预设筛选与光标
  const orderIdParam = route.query.orderId ? Number(route.query.orderId) : undefined
  if (orderIdParam && Number.isFinite(orderIdParam)) {
    // 待 refreshSessions 完成后会尝试自动打开；这里先做一次关键字设置以便用户视觉聚焦
    keyword.value = String(orderIdParam)
  }
})

onBeforeUnmount(() => {
  stopAutoRefresh()
  disconnectWebSocket()
})

function onMessageScroll() {
  const el = messageContainer.value
  if (!el) return
  const nearTop = el.scrollTop <= 24
  showTopLoaderByScroll.value = nearTop
  // 自动触顶加载
  const key = activeSession.value?.sessionId
  const hasMore = key ? historyState[key]?.hasMore : false
  if (nearTop && hasMore && !messageLoading.value) {
    loadMore()
  }
}
</script>
<style scoped src='./styles/ChatWorkspace.css'></style>


