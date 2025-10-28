export type Result<T = any> = {
  status: number
  message?: string | null
  data?: T
}

type StoredAuth = {
  username?: string
  nickname?: string
  role?: string
  avatarUrl?: string
  token?: string
  tokenType?: string
  tokenExpiresAt?: string | null
  tokenIssuedAt?: string | null
}

export function getStoredAuth(): StoredAuth | null {
  const storedAuth = localStorage.getItem('auth.user') ?? localStorage.getItem('auth')
  if (!storedAuth) return null
  try {
    return JSON.parse(storedAuth) as StoredAuth
  } catch (e) {
    console.warn('Failed to parse stored auth data')
    return null
  }
}

export function buildAuthHeaders(initial: Record<string, string> = {}) {
  const headers = { ...initial }
  const auth = getStoredAuth()
  if (auth?.token) {
    const scheme = auth.tokenType || 'Bearer'
    headers.Authorization = `${scheme} ${auth.token}`
  }
  return headers
}

function enrichPayload(payload: Record<string, any>) {
  const requestPayload = { ...payload }
  const auth = getStoredAuth()
  if (!('username' in requestPayload) || !requestPayload.username) {
    if (auth?.username) {
      requestPayload.username = auth.username
    }
  }
  if (!('role' in requestPayload) || !requestPayload.role) {
    if (auth?.role) {
      requestPayload.role = auth.role
    }
  }
  return requestPayload
}

export function buildDataRequestPayload(payload: Record<string, any> = {}) {
  return { data: enrichPayload(payload) }
}

export function buildDataRequestBlob(payload: Record<string, any> = {}) {
  return new Blob([JSON.stringify(buildDataRequestPayload(payload))], { type: 'application/json' })
}

export async function postDataRequest<T = any>(url: string, payload: Record<string, any>): Promise<Result<T>> {
  const body = buildDataRequestPayload(payload)
  const headers = buildAuthHeaders({ 'Content-Type': 'application/json' })
  const res = await fetch(url, {
    method: 'POST',
    headers,
    body: JSON.stringify(body),
  })
  if (!res.ok) {
    return { status: res.status, message: res.statusText } as Result<T>
  }
  const json = await res.json()
  return json as Result<T>
}

export async function postDataRequestWithPage<T = any>(
  url: string,
  payload: Record<string, any>,
  page?: number,
  size?: number,
): Promise<Result<T>> {
  const requestPayload = enrichPayload(payload)
  const body: Record<string, any> = { data: requestPayload }
  if (typeof page === 'number') body.page = page
  if (typeof size === 'number') body.size = size

  const headers = buildAuthHeaders({ 'Content-Type': 'application/json' })
  const res = await fetch(url, {
    method: 'POST',
    headers,
    body: JSON.stringify(body),
  })
  if (!res.ok) {
    return { status: res.status, message: res.statusText } as Result<T>
  }
  const json = await res.json()
  return json as Result<T>
}
