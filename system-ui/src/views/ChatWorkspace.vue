<template>
  <div class="chat-page">
    <section class="chat-sidebar">
      <header class="sidebar-header">
        <div>
          <h2>会话</h2>
          <p class="hint">{{ sidebarHint }}</p>
        </div>
        <button class="ghost" type="button" @click="refreshSessions" :disabled="sessionLoading">
          <span v-if="sessionLoading">刷新中…</span>
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
        <p v-if="sessionLoading" class="placeholder">加载会话中…</p>
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

      <div ref="messageContainer" class="message-scroller" @scroll="onMessageScroll">
        <div v-if="!activeSession" class="thread-placeholder">
          <p>支持实时消息、未读提醒和订单侧边栏。请选择一个会话开始交流。</p>
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
              {{ messageLoading ? '加载中…' : '查看更多历史消息' }}
            </button>
          </transition>
          <transition-group name="list-fade" tag="div">
          <div
            v-for="(message, idx) in activeMessages"
            :key="message.id"
            :class="['message', message.senderRole === selfRole ? 'outgoing' : 'incoming', message.id < 0 ? 'pending' : '']"
          >
            <div class="avatar small" @click="openUserProfile(message)" :title="messageDisplayName(message)">
              <img v-if="messageAvatar(message)" :src="messageAvatar(message)" :alt="messageDisplayName(message)" />
              <span v-else>{{ initials(messageDisplayName(message)) }}</span>
            </div>
            <div class="message-body">
              <div :class="['name-line', message.senderRole === selfRole ? 'right' : 'left']">{{ messageDisplayName(message) }}</div>
              <div class="bubble" :data-index="idx">
                <p class="text">{{ text30(message.content) }}</p>
                <span class="timestamp">
                  {{ fullTime(message.createdAt) }}
                  <em v-if="message.id < 0" class="sending"> · 发送中</em>
                </span>
              </div>
            </div>
          </div>
          </transition-group>
          <p v-if="activeMessages.length === 0 && !messageLoading" class="thread-placeholder">
            暂无聊天记录，向 {{ activeSession.peerName }} 打招呼吧。
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
            {{ sending ? '发送中…' : '发送' }}
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
        <p>选择会话后展示对应订单详情，便于快速响应客户需求。</p>
      </div>
      </transition>
      <section class="tips">
        <h4>使用提示</h4>
        <ul>
          <li>实时消息通过 WebSocket 推送，右上角可查看连接状态。</li>
          <li>新消息会在左侧会话列表显示红点提醒。</li>
          <li>支持按订单分组的三方会话（客户、商家、骑手）。</li>
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
                    <div class="title"><strong>订单</strong> {{ o.orderNo || ('#' + o.orderId) }}
                      <span :class="['status-chip', statusClass(o.status)]">{{ zhStatus(o.status) }}</span>
                    </div>
                    <div class="meta">{{ o.remark || '无备注' }}</div>
                  </div>
                  <div class="meta">{{ shortTime(o.createdAt) }}</div>
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
import type { ChatHistoryPayload, ChatMessageDto, ChatOrderSnippet, ChatRole, ChatSessionSummary } from '../api/chat'
import { fetchChatHistory, fetchChatSessions, markChatRead, sendChatMessage } from '../api/chat'
import { useAuthStore } from '../stores/auth'
import { getOrderDetail } from '../api/order'

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
const messagesBySession = reactive<Record<string, ChatMessageDto[]>>({})
const historyState = reactive<Record<string, { hasMore: boolean; oldestId?: number }>>({})
const messageLoading = ref(false)
const showTopLoaderByScroll = ref(false)
const composerText = ref('')
const sending = ref(false)
const sendError = ref('')

const messagePageSize = 10
const connectionStatus = ref<'DISCONNECTED' | 'CONNECTING' | 'CONNECTED'>('DISCONNECTED')
let stompClient: Client | null = null
let refreshTimer: number | undefined

const selfRole = computed(() => auth.role)
const chatSelfRole = computed<ChatRole | null>(() => {
  if (selfRole.value === 'GUEST' || selfRole.value === 'RESTAURATEUR' || selfRole.value === 'DELIVERYMAN') {
    return selfRole.value
  }
  return null
})

const sidebarHint = computed(() => {
  if (!selfRole.value) {
    return '登录后加载专属会话'
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

// 最近相关订单（最多3条）：基于消息内的 orderId 去重并按最近消息时间排序
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
      // 当前订单用详细信息，其它订单尽力用已知片段（若未来历史接口返回更多订单片段，可在此合并）
      if (base) {
        return { orderId: oid, orderNo: base.orderNo, status: base.status, createdAt: base.createdAt ?? time }
      }
      // return { orderId: oid, orderNo: undefined, status: undefined, createdAt: time }
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
      return '连接中…'
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
  return arr.length > n ? arr.slice(0, n).join('') + '…' : s
}

function zhStatus(code?: string | null) {
  const c = (code || '').toUpperCase()
  switch (c) {
    case 'PENDING': return '待处理'
    case 'PROCESSING': return '制作中'
    case 'IN_PROGRESS': return '进行中'
    case 'READY': return '待取餐'
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
      // 统一将 unreadCount 规范为数字，避免出现 '10+' 字符串在前端被直接渲染
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
  sending.value = true
  sendError.value = ''
  const payload = {
    orderId: session.orderId,
    receiverId: session.peerId,
    receiverRole: session.peerRole,
    content: text,
    username: auth.username,
  }
  try {
    const client = stompClient
    const useWebSocket = !!client && connectionStatus.value === 'CONNECTED'
    let snippetSource: ChatMessageDto | null = null
    if (useWebSocket && client) {
      client.publish({
        destination: wsSendDestination(),
        body: JSON.stringify(payload),
      })
      snippetSource = {
        id: -Date.now(),
        sessionId: session.sessionId,
        orderId: session.orderId,
        senderId: 0,
        senderRole: chatSelfRole.value,
        receiverId: session.peerId,
        receiverRole: session.peerRole,
        content: text,
        read: false,
        createdAt: new Date().toISOString(),
      }
      // 先本地插入一条“发送中”的消息，等待服务端回推后替换
      appendMessage(session.sessionId, { ...snippetSource })
    } else {
      const res = await sendChatMessage(payload)
      if (res.status !== 200) {
        throw new Error(res.message || '发送失败')
      }
      if (res.data) {
        const delivered: ChatMessageDto = {
          ...res.data,
          sessionId: res.data.sessionId ?? session.sessionId,
        }
        appendMessage(session.sessionId, delivered)
        snippetSource = delivered
      } else {
        snippetSource = {
          id: -Date.now(),
          sessionId: session.sessionId,
          orderId: session.orderId,
          senderId: 0,
          senderRole: chatSelfRole.value,
          receiverId: session.peerId,
          receiverRole: session.peerRole,
          content: text,
          read: false,
          createdAt: new Date().toISOString(),
        }
        appendMessage(session.sessionId, snippetSource)
      }
    }
    if (snippetSource) {
      upsertSessionMeta(session.sessionId, snippetSource)
    }
    const sessionRecord = sessions.value.find((item) => item.sessionId === session.sessionId)
    if (sessionRecord) {
      sessionRecord.unreadCount = 0
    }
    composerText.value = ''
    await nextTick(() => scrollToLast(true))
    focusComposer()
  } catch (err: any) {
    sendError.value = err?.message || '发送失败'
    refreshSessions()
    console.error('发送消息失败', err)
  } finally {
    sending.value = false
  }
}

function appendMessage(key: string, message: ChatMessageDto) {
  const list = messagesBySession[key] ?? []
  if (list.some((item) => item.id === message.id)) {
    return
  }
  const updated = [...list, message]
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
  // - 其它角色退化为调用 fetchChatHistory 拿 order snippet
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
  disconnectWebSocket()
  connectionStatus.value = 'CONNECTING'
  const endpoint = resolveWsUrl()
  const client = new Client({
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    connectHeaders: { username: auth.username },
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

function disconnectWebSocket() {
  if (stompClient) {
    try {
      stompClient.deactivate()
    } catch (err) {
      console.warn('断开 WebSocket 失败', err)
    }
    stompClient = null
  }
  connectionStatus.value = 'DISCONNECTED'
}

function handleSocketFrame(frame: IMessage) {
  if (!frame.body) return
  try {
    const payload = JSON.parse(frame.body)
    if (payload?.type === 'ERROR') {
      sendError.value = payload.message ?? '发送失败'
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
  // 若存在同内容且临近时间的“临时负ID”消息，视作送达并用服务端消息替换
  const list = messagesBySession[key] ?? []
  const idx = list.findIndex((m) => m.id < 0 && m.content === message.content && Math.abs(new Date(m.createdAt).getTime() - new Date(message.createdAt).getTime()) < 15_000 && m.senderRole === message.senderRole)
  if (idx >= 0) {
    const next = [...list]
    next[idx] = message
    messagesBySession[key] = next
  } else {
    appendMessage(key, message)
  }
  upsertSessionMeta(key, message)
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
      // 本地也立即将该条消息置为已读
      const list = messagesBySession[key] ?? []
      const idx2 = list.findIndex((m) => m.id === message.id)
      if (idx2 >= 0) {
        const next = [...list]
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
    // 等 refreshSessions 完成后会尝试自动打开；这里先做一次关键字设置以便用户视觉聚焦
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

<style scoped>
.chat-page {
  display: grid;
  grid-template-columns: 320px 1fr 280px;
  height: calc(100vh - 64px);
  background: #fff9f0;
  color: #502600;
}

.chat-sidebar {
  border-right: 1px solid rgba(104, 64, 18, 0.2);
  display: flex;
  flex-direction: column;
  padding: 16px;
  gap: 16px;
  background: rgba(255, 241, 222, 0.9);
  /* 允许中栏滚动时自身不撑高 */
  min-height: 0;
}

.sidebar-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.sidebar-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.hint {
  margin: 2px 0 0;
  font-size: 12px;
  color: rgba(80, 38, 0, 0.6);
}

.ghost {
  background: transparent;
  border: 1px solid rgba(80, 38, 0, 0.2);
  border-radius: 8px;
  padding: 6px 12px;
  cursor: pointer;
}

.ghost:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.sidebar-controls {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.search {
  width: 100%;
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid rgba(80, 38, 0, 0.2);
  background: #fff;
}

.filter-tabs {
  display: flex;
  gap: 8px;
}

.tab {
  flex: 1;
  border: none;
  border-radius: 999px;
  padding: 6px 12px;
  background: rgba(255, 210, 150, 0.5);
  cursor: pointer;
  font-size: 13px;
}

.tab.active {
  background: #ff9f55;
  color: #fff;
  font-weight: 600;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-right: 4px;
}

.session-item {
  display: grid;
  grid-template-columns: 48px 1fr;
  gap: 12px;
  padding: 12px;
  border-radius: 16px;
  border: none;
  background: rgba(255, 255, 255, 0.8);
  cursor: pointer;
  text-align: left;
  transition: background 0.2s ease;
}

.session-item:hover {
  background: rgba(255, 222, 179, 0.8);
}

.session-item.active {
  background: #ffb97a;
  color: #4b2300;
  box-shadow: 0 6px 16px rgba(255, 145, 0, 0.15);
}

.session-item.active .snippet {
  color: rgba(75, 35, 0, 0.8);
}

.avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: rgba(255, 176, 82, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  font-weight: 600;
  color: #7f3b00;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.session-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.session-meta .row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.session-meta .name {
  font-weight: 600;
}

.session-meta .snippet {
  font-size: 13px;
  color: rgba(80, 38, 0, 0.65);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-meta .time {
  font-size: 12px;
  color: rgba(80, 38, 0, 0.55);
}

.session-meta .order {
  font-size: 12px;
  color: rgba(80, 38, 0, 0.6);
  display: flex;
  justify-content: space-between;
  gap: 8px;
}

.badge {
  min-width: 24px;
  padding: 2px 6px;
  border-radius: 999px;
  background: #ff6d1b;
  color: #fff;
  font-size: 12px;
  text-align: center;
}

.placeholder {
  text-align: center;
  color: rgba(80, 38, 0, 0.5);
}

.chat-thread {
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.9);
  /* 关键：允许内部滚动区域收缩，否则子元素无法滚动 */
  min-height: 0;
}

.thread-header {
  padding: 16px 20px;
  border-bottom: 1px solid rgba(80, 38, 0, 0.1);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.thread-title h3 {
  margin: 0;
  font-size: 18px;
}

.chip-group {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.chip {
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(255, 176, 82, 0.3);
  font-size: 12px;
  color: #7f3b00;
}

.ws-state {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid currentColor;
}

.ws-state.connected {
  color: #2f9d52;
  background: rgba(64, 199, 110, 0.18);
}

.ws-state.connecting {
  color: #ff8c37;
  background: rgba(255, 178, 81, 0.18);
}

.ws-state.disconnected {
  color: #c0392b;
  background: rgba(224, 102, 102, 0.2);
}

.message-scroller {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden; /* 禁止水平滚动条，长内容向中间收拢换行 */
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  /* 关键：与父 flex 配合，确保可滚动 */
  min-height: 0;
  scrollbar-gutter: stable both-edges;
}

/* Visible custom scrollbar */
.message-scroller::-webkit-scrollbar {
  width: 10px;
}
.message-scroller::-webkit-scrollbar-track {
  background: rgba(255, 237, 213, 0.7);
  border-radius: 8px;
}
.message-scroller::-webkit-scrollbar-thumb {
  background: rgba(249, 115, 22, 0.45);
  border-radius: 8px;
}
.message-scroller {
  scrollbar-color: rgba(249,115,22,0.45) rgba(255,237,213,0.7);
  scrollbar-width: thin;
}

.message {
  display: flex;
  align-items: flex-start;
  gap: 5px;
  width: 50%; /* 单条消息占满一行，避免挤压头像/名称 */
}

.message.incoming {
  justify-content: flex-start;
}

.message.outgoing {
  margin-left: auto;
  justify-content: flex-end;
  flex-direction: row-reverse;
}

.message-body {
  display: flex;
  flex-direction: column;
  gap: 5px;
  max-width: 70%; /* 参照示例：限制文本区域宽度，长消息不外溢 */
  flex: 0 0 70%; /* 保持占比，避免被压缩过度 */
}
.message.outgoing .message-body { align-items: flex-end; }
.message.incoming .message-body { align-items: flex-start; }

.name-line {
  font-size: 12px;
  color: rgba(80, 38, 0, 0.7);
}
.name-line.left { text-align: left; padding-left: 4px; }
.name-line.right { text-align: right; padding-right: 4px; }

.bubble {
  background: rgba(210, 114, 18, 0.5);
  padding: 12px 16px;
  border-radius: 16px;
  position: relative;
  line-height: 1.5;
  box-shadow: 0 4px 12px rgba(255, 145, 0, 0.1);
  max-width: 100%; /* 由父容器 message-body 控制最大宽度 */
}

.outgoing .bubble {
  background: linear-gradient(135deg, #ff9f55, #ff7b3a);
  color: #fff6ec;
}

.timestamp {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  opacity: 0.65;
}
.bubble .text {
  white-space: pre-wrap; /* 多行显示，保留换行 */
  overflow-wrap: anywhere; /* 超长英文/URL 也能断行 */
  word-break: break-word;
}

.message.pending .bubble {
  opacity: 0.75;
}

.sending {
  font-style: normal;
}

.avatar.small {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(255, 176, 82, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  font-weight: 600;
  color: #7f3b00;
  margin: 0 8px 0 0;
}


.message.outgoing .avatar.small { margin: 0 0 0 8px; }

.avatar.small img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 响应式：小屏时适当放宽文本区域占比，进一步减少被压缩的概率 */
@media (max-width: 1024px) {
  .message-body { max-width: 75%; flex-basis: 75%; }
}
@media (max-width: 640px) {
  .message-body { max-width: 85%; flex-basis: 85%; }
}



.read-flag {
  font-style: normal;
  color: rgba(80, 38, 0, 0.7);
}

.load-more {
  align-self: center;
  padding: 6px 12px;
  border-radius: 12px;
  border: 1px solid rgba(80, 38, 0, 0.2);
  background: rgba(255, 231, 199, 0.7);
  cursor: pointer;
}

.fade-enter-active, .fade-leave-active { transition: opacity .2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

.slide-fade-enter-active, .slide-fade-leave-active { transition: all .24s ease; }
.slide-fade-enter-from { opacity: 0; transform: translateY(6px); }
.slide-fade-leave-to { opacity: 0; transform: translateY(6px); }

.list-fade-enter-active, .list-fade-leave-active { transition: all .16s ease; }
.list-fade-enter-from, .list-fade-leave-to { opacity: 0; transform: translateY(6px); }

.thread-placeholder {
  text-align: center;
  color: rgba(80, 38, 0, 0.55);
  margin: 0 auto;
}

.composer {
  padding: 16px 20px;
  border-top: 1px solid rgba(80, 38, 0, 0.1);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.composer textarea {
  width: 100%;
  min-height: 80px;
  resize: vertical;
  padding: 12px;
  border-radius: 12px;
  border: 1px solid rgba(80, 38, 0, 0.25);
}

.composer-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tip {
  font-size: 12px;
  color: #c0392b;
}

.primary {
  padding: 8px 18px;
  border-radius: 999px;
  border: none;
  background: #ff8137;
  color: #fff;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 8px 16px rgba(255, 129, 55, 0.3);
}

.primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.composer.disabled {
  align-items: center;
  justify-content: center;
  color: rgba(80, 38, 0, 0.5);
}

.chat-details {
  border-left: 1px solid rgba(80, 38, 0, 0.2);
  padding: 20px;
  background: rgba(255, 250, 240, 0.95);
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-height: 0;
}

.order-card {
  padding: 16px;
  border-radius: 18px;
  background: #fffef8;
  box-shadow: 0 12px 28px rgba(120, 65, 0, 0.12);
}

.mini-orders { margin-top: 10px; }
.mini-orders h4 { margin: 0 0 6px; font-size: 13px; color: rgba(80,38,0,.8); }
.mini-orders ul { margin: 0; padding: 0; list-style: none; display: flex; flex-direction: column; gap: 6px; }
.mini-orders li { display: grid; grid-template-columns: 1fr auto auto; gap: 8px; font-size: 12px; align-items: center; }
.mini-orders .ono { font-weight: 600; color: #7f3b00; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.mini-orders .ostat { color: rgba(80,38,0,.65); }
.mini-orders .otime { color: rgba(80,38,0,.55); }

.order-card.muted {
  background: rgba(255, 255, 255, 0.6);
  color: rgba(80, 38, 0, 0.6);
  box-shadow: none;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.card-header h3 {
  margin: 0;
  font-size: 16px;
}

.status {
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(255, 167, 81, 0.25);
  font-size: 12px;
}

.more-orders {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}


.link {
  border: none;
  background: transparent;
  color: #ff8137;
  cursor: pointer;
  font-size: 13px;
  position: relative;
  overflow: hidden;
}
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 60;
}
.modal-panel {
  width: 720px;
  max-width: calc(100vw - 40px);
  background: #fffef8;
  border-radius: 16px;
  box-shadow: 0 20px 50px rgba(0,0,0,0.25);
  display: flex;
  flex-direction: column;
  max-height: 80vh;
  overflow: hidden;
}
.modal-header { display:flex; align-items:center; justify-content:space-between; padding:12px 16px; border-bottom:1px solid rgba(80,38,0,.1); }
.modal-title { margin:0; font-size:16px; }
.modal-body { padding:0; overflow-y:auto; }
.order-list { list-style:none; margin:0; padding:0; }
.order-item { padding:12px 16px; border-bottom:1px solid rgba(80,38,0,.06); display:grid; grid-template-columns: 1fr auto; gap:8px; }
.order-item .meta { font-size:12px; color: rgba(80,38,0,.85); }
.order-item .title { color:#4b2300; font-weight:600; }
.status-chip { display:inline-block; padding:2px 8px; border-radius:999px; font-size:12px; margin-left:6px; }
.status-chip.pending { background:rgba(255,196,0,.18); color:#9a6b00; }
.status-chip.processing { background:rgba(255,159,85,.2); color:#7f3b00; }
.status-chip.in-progress { background:rgba(255,159,85,.2); color:#7f3b00; }
.status-chip.ready { background:rgba(64,199,110,.18); color:#2f9d52; }
.status-chip.completed { background:rgba(64,199,110,.18); color:#2f9d52; }
.status-chip.canceled { background:rgba(224,102,102,.18); color:#c0392b; }
.modal-body::-webkit-scrollbar { width: 10px; }
.modal-body::-webkit-scrollbar-track { background: rgba(255, 237, 213, 0.7); border-radius: 8px; }
.modal-body::-webkit-scrollbar-thumb { background: rgba(249, 115, 22, 0.45); border-radius: 8px; }


.link:after {
  content: '';
  position: absolute;
  left: 50%;
  top: 50%;
  width: 0;
  height: 0;
  background: rgba(255, 129, 55, 0.15);
  border-radius: 50%;
  transform: translate(-50%, -50%);
  transition: width .3s ease, height .3s ease;
}
.link:active:after {
  width: 160px;
  height: 160px;
}

dl {
  margin: 0;
}

dl div {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  gap: 12px;
}

.tips {
  padding: 16px;
  border-radius: 16px;
  background: rgba(255, 235, 208, 0.7);
}

.tips h4 {
  margin: 0 0 12px;
  font-size: 14px;
}

.tips ul {
  margin: 0;
  padding-left: 18px;
  font-size: 13px;
}

@media (max-width: 1024px) {
  .chat-page {
    grid-template-columns: 280px 1fr;
  }
  .chat-details {
    display: none;
  }
}

@media (max-width: 640px) {
  .chat-page {
    grid-template-columns: 1fr;
  }
}

/* Orders Modal Root */
</style>
