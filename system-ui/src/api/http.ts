export type Result<T = any> = {
  status: number
  message?: string | null
  data?: T
}

function enrichPayload(payload: Record<string, any>) {
  const requestPayload = { ...payload }
  if (!('username' in requestPayload) || !requestPayload.username) {
    const storedAuth = localStorage.getItem('auth.user') ?? localStorage.getItem('auth')
    if (storedAuth) {
      try {
        const auth = JSON.parse(storedAuth)
        if (auth?.username) {
          requestPayload.username = auth.username
        }
      } catch (e) {
        console.warn('Failed to parse stored auth data')
      }
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
  const res = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
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

  const res = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!res.ok) {
    return { status: res.status, message: res.statusText } as Result<T>
  }
  const json = await res.json()
  return json as Result<T>
}
