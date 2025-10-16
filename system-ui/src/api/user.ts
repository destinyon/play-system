import { postDataRequest } from './http'
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

// 获取当前用户信息
export const getUserInfo = () => {
  return postDataRequest('/api/user/getByUsername', {})
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
  const res = await fetch('/api/restaurateur/stats')
  if (!res.ok) {
    return { status: res.status, message: res.statusText }
  }
  return await res.json()
}

// 获取骑手收入统计
export const getDeliverymanStats = async () => {
  const res = await fetch('/api/deliveryman/stats')
  if (!res.ok) {
    return { status: res.status, message: res.statusText }
  }
  return await res.json()
}

// 获取用户统计（管理员）
export const getUserStats = async () => {
  const res = await fetch('/api/user/stats')
  if (!res.ok) {
    return { status: res.status, message: res.statusText }
  }
  return await res.json()
}


