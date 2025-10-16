export type Result<T = any> = {
  status: number
  message?: string | null
  data?: T
}

export async function postDataRequest<T = any>(url: string, payload: Record<string, any>): Promise<Result<T>> {
  const requestPayload = { ...payload }

  // Attach username from local storage when the caller did not provide one
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
  
  const res = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ data: requestPayload }),
  })
  if (!res.ok) {
    return { status: res.status, message: res.statusText } as Result<T>
  }
  const json = await res.json()
  return json as Result<T>
}
