import type { Result } from './http'

export interface OrderListItem {
  id: number
  orderNo: string
  customerName: string
  totalAmount: number
  status: string
  deliveryAddress: string
  createdAt: string
  remark?: string
}

export interface OrderItem {
  id: number
  dishId: number
  dishName: string
  dishImage?: string
  unitPrice: number
  quantity: number
}

export interface OrderDetail {
  id: number
  orderNo: string
  customerName?: string
  customerPhone?: string
  deliveryAddress: string
  status: string
  remark?: string
  totalAmount: number
  createdAt: string
  updatedAt: string
  items: OrderItem[]
}

export interface PageResult<T> {
  total: number
  records: T[]
}

export async function getPendingOrders(
  restaurateurId: number,
  keyword?: string,
  page = 1,
  size = 10
): Promise<Result<PageResult<OrderListItem>>> {
  const res = await fetch('/api/order/pending', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      data: { restaurateurId, keyword: keyword || '' },
      page,
      size
    })
  })
  return res.json()
}

export async function getOrderList(
  restaurateurId: number,
  status?: string,
  keyword?: string,
  page = 1,
  size = 10
): Promise<Result<PageResult<OrderListItem>>> {
  const res = await fetch('/api/order/list', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      data: { restaurateurId, status: status || '', keyword: keyword || '' },
      page,
      size
    })
  })
  return res.json()
}

export async function getOrderDetail(
  orderId: number,
  restaurateurId: number
): Promise<Result<OrderDetail>> {
  const res = await fetch('/api/order/detail', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      data: { orderId, restaurateurId }
    })
  })
  return res.json()
}

export async function acceptOrder(
  orderId: number,
  restaurateurId: number
): Promise<Result<string>> {
  const res = await fetch('/api/order/accept', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      data: { orderId, restaurateurId }
    })
  })
  return res.json()
}

export async function startCooking(
  orderId: number,
  restaurateurId: number
): Promise<Result<string>> {
  const res = await fetch('/api/order/start-cooking', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      data: { orderId, restaurateurId }
    })
  })
  return res.json()
}

export async function markOrderReady(
  orderId: number,
  restaurateurId: number
): Promise<Result<string>> {
  const res = await fetch('/api/order/ready', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      data: { orderId, restaurateurId }
    })
  })
  return res.json()
}

export async function completeOrder(orderId: number): Promise<Result<string>> {
  const res = await fetch('/api/order/complete', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      data: { orderId }
    })
  })
  return res.json()
}

export interface MetricsData {
  timeSeries: Array<{
    date: string
    income: number
    orders: number
    dishes: number
  }>
  granularity: string
  from: string
  to: string
}

export interface StatsData {
  totalIncome: number
  pendingOrders: number
  todayOrders: number
  todayOrdersGrowth: number
  dishCount: number
}

export interface TopDish {
  name: string
  salesCount: number
  revenue: number
}

export async function getRestaurateurStats(
  restaurateurId: number
): Promise<Result<StatsData>> {
  const res = await fetch('/api/restaurateur/stats', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      data: { restaurateurId }
    })
  })
  return res.json()
}

export async function getRestaurateurMetrics(
  from?: string,
  to?: string,
  granularity = 'day'
): Promise<Result<MetricsData>> {
  const res = await fetch('/api/restaurateur/metrics', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      data: { from, to, granularity }
    })
  })
  return res.json()
}

export async function getTopDishes(
  from?: string,
  to?: string,
  limit = 10
): Promise<Result<TopDish[]>> {
  const res = await fetch('/api/restaurateur/menu/tops', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      data: { from, to, limit }
    })
  })
  return res.json()
}
