import type { Result } from './http'
import { postDataRequest, postDataRequestWithPage } from './http'

export type ChatRole = 'GUEST' | 'RESTAURATEUR' | 'DELIVERYMAN'

export interface ChatSessionSummary {
  sessionId: string
  orderId: number
  peerId: number
  peerRole: ChatRole
  peerName: string
  peerAvatar?: string | null
  orderNo?: string | null
  orderStatus?: string | null
  orderRemark?: string | null
  lastMessage?: string | null
  lastMessageTime?: string | null
  unreadCount?: number | null
}

export interface ChatMessageDto {
  id: number
  sessionId?: string
  orderId: number
  senderId: number
  senderRole: ChatRole
  receiverId: number
  receiverRole: ChatRole
  content: string
  read: boolean
  createdAt: string
}

export interface ChatOrderSnippet {
  orderId: number
  orderNo?: string | null
  status?: string | null
  remark?: string | null
  createdAt?: string | null
}

export interface ChatHistoryPayload {
  orderId: number
  peerId: number
  peerRole: ChatRole
  beforeId?: number | null
  size?: number
  username?: string
}

export interface ChatHistoryResponse {
  messages: ChatMessageDto[]
  order?: ChatOrderSnippet
}

export interface SendMessagePayload {
  orderId: number
  receiverId: number
  receiverRole: ChatRole
  content: string
  username?: string
}

export function fetchChatSessions(extra: Record<string, any> = {}): Promise<Result<ChatSessionSummary[]>> {
  return postDataRequest('/api/chat/sessions', extra)
}

export function fetchChatHistory(payload: ChatHistoryPayload): Promise<Result<ChatHistoryResponse>> {
  const { size, ...data } = payload
  return postDataRequestWithPage('/api/chat/history', data, undefined, size)
}

export function markChatRead(payload: ChatHistoryPayload): Promise<Result<unknown>> {
  return postDataRequest('/api/chat/markRead', payload)
}

export function sendChatMessage(payload: SendMessagePayload): Promise<Result<ChatMessageDto>> {
  return postDataRequest('/api/chat/send', payload)
}
