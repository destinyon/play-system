import type { Result } from './http'

export interface MenuItemDto {
  id: number
  name: string
  category?: string
  price: number
  imageUrl?: string
  description?: string
  status: 'ON_SHELF' | 'OFF_SHELF' | string
}

export interface MenuPageRequest {
  restaurateurId: number
  keyword?: string
  category?: string
  status?: string
  page?: number
  size?: number
}

interface PageResult<T> {
  total: number
  records: T[]
}

const BASE_URL = '/api/restaurateur/menu'

export async function fetchMenuPage(request: MenuPageRequest): Promise<Result<PageResult<MenuItemDto>>> {
  const res = await fetch(`${BASE_URL}/list`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      data: {
        restaurateurId: request.restaurateurId,
        keyword: request.keyword,
        category: request.category,
        status: request.status
      },
      page: request.page || 1,
      size: request.size || 8
    })
  })
  if (!res.ok) {
    return { status: res.status, message: res.statusText }
  }
  return await res.json()
}

export async function fetchMenuCategories(restaurateurId: number): Promise<Result<string[]>> {
  const res = await fetch(`${BASE_URL}/categories`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      data: { restaurateurId }
    })
  })
  if (!res.ok) {
    return { status: res.status, message: res.statusText }
  }
  return await res.json()
}

export async function fetchMenuDetail(restaurateurId: number, id: number): Promise<Result<MenuItemDto>> {
  const res = await fetch(`${BASE_URL}/detail`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      data: { restaurateurId, id }
    })
  })
  if (!res.ok) {
    return { status: res.status, message: res.statusText }
  }
  return await res.json()
}

export async function createMenuItem(restaurateurId: number, payload: Partial<MenuItemDto>): Promise<Result> {
  const res = await fetch(`${BASE_URL}/create`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      data: {
        restaurateurId,
        ...payload
      }
    })
  })
  if (!res.ok) {
    return { status: res.status, message: res.statusText }
  }
  return await res.json()
}

export async function updateMenuItem(restaurateurId: number, id: number, payload: Partial<MenuItemDto>): Promise<Result> {
  const res = await fetch(`${BASE_URL}/update`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      data: {
        restaurateurId,
        id,
        ...payload
      }
    })
  })
  if (!res.ok) {
    return { status: res.status, message: res.statusText }
  }
  return await res.json()
}

export async function deleteMenuItem(restaurateurId: number, id: number): Promise<Result> {
  const res = await fetch(`${BASE_URL}/delete`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      data: { restaurateurId, id }
    })
  })
  if (!res.ok) {
    return { status: res.status, message: res.statusText }
  }
  return await res.json()
}
