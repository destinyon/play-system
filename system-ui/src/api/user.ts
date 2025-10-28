import { postDataRequest, buildDataRequestBlob } from './http'
import type { Result } from './http'

export type LoginPayload = { username: string; password: string }
export type RegisterPayload = {
  // Align with backend enum: GUEST, RESTAURATEUR, DELIVERYMAN
  role: 'GUEST' | 'RESTAURATEUR' | 'DELIVERYMAN'
  username: string
  password: string
  email?: string
  phone?: string
}

export function apiLogin(payload: LoginPayload) {
  return postDataRequest('/api/user/login', payload)
}

export function apiRegister(payload: RegisterPayload) {
  return postDataRequest('/api/user/register', payload)
}

// Optional helper if backend supports getting by username in forgot-password flow
export function apiGetByUsername(username: string) {
  return postDataRequest('/api/user/getByUsername', { username })
}

// 获取当前或指定用户信息
export const getUserInfo = (username?: string) => {
  const payload = username ? { username } : {}
  return postDataRequest('/api/user/getByUsername', payload)
}

// 对外展示的用户资料
export const getUserPublicProfile = (payload: { username?: string; userId?: number | string }) => {
  if (payload.userId != null && payload.userId !== '') {
    return postDataRequest('/api/user/getById', { id: payload.userId } as Record<string, any>)
  }
  return postDataRequest('/api/user/getByUsername', payload as Record<string, any>)
}

// 获取所有用户（管理员）
export const getAllUsers = () => {
  return postDataRequest('/api/user/getAllUsers', {})
}

// 更新用户信息
export const updateUserInfo = (data: any) => {
  return postDataRequest('/api/user/update', data)
}

// 修改密码
export const changePassword = (data: { oldPassword: string; newPassword: string }) => {
  return postDataRequest('/api/user/changePassword', data)
}

export type UploadAvatarResponse = {
  filename: string
  url: string
}

// 上传头像
export const uploadAvatar = async (file: File): Promise<Result<UploadAvatarResponse>> => {
  const formData = new FormData()
  const requestPayload = { data: {} }
  formData.append('request', buildDataRequestBlob({}))
  formData.append('file', file)
  const res = await fetch('/api/user/uploadAvatar', {
    method: 'POST',
    body: formData
  })
  if (!res.ok) {
    return { status: res.status, message: res.statusText }
  }
  return await res.json()
}

// 获取商家收入统计
export const getRestaurateurStats = async () => {
  return postDataRequest('/api/restaurateur/stats', {})
}

// 获取骑手收入统计
export const getDeliverymanStats = async () => {
  return postDataRequest('/api/deliveryman/stats', {})
}

// 获取用户统计（管理员）
export const getUserStats = async () => {
  return postDataRequest('/api/user/stats', {})
}


