import type { Result } from './http'

// Upload restaurant photo. Backend expected endpoint: /api/restaurant/uploadPhoto
export async function uploadRestaurantPhoto(file: File): Promise<Result<{ url: string }>> {
  const form = new FormData()
  form.append('file', file)
  try {
    const res = await fetch('/api/restaurant/uploadPhoto', {
      method: 'POST',
      body: form,
    })
    if (!res.ok) return { status: res.status, message: res.statusText }
    const json = await res.json()
    return json as Result<{ url: string }>
  } catch (e: any) {
    return { status: 500, message: e?.message || 'upload error' }
  }
}

// List restaurants for user map. Expect backend to return minimal info.
export async function listRestaurants(): Promise<Result<Array<{
  id: number | string
  name: string
  address: string
  lng: number
  lat: number
  photoUrl?: string
}>>> {
  try {
    const res = await fetch('/api/restaurant/list', { method: 'GET' })
    if (!res.ok) return { status: res.status, message: res.statusText }
    const json = await res.json()
    // 后端返回的是实体字段：restaurantName/restaurantAddress/restaurantImageUrl
    const data = Array.isArray(json?.data)
      ? json.data.map((r: any) => ({
          id: r.id,
          name: r.restaurantName ?? r.name,
          address: r.restaurantAddress ?? r.address,
          lng: r.lng,
          lat: r.lat,
          photoUrl: r.restaurantImageUrl ?? r.photoUrl,
        }))
      : []
    return { status: 200, data } as Result<any>
  } catch (e: any) {
    return { status: 500, message: e?.message || 'fetch error' }
  }
}
