import type { Result } from './http'
import { postDataRequest, postDataRequestWithPage } from './http'

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

export interface RestaurantProfile {
  id: number | null
  name: string
  address: string
  description: string
  lng: number | null
  lat: number | null
  photoUrl: string | null
  restaurateurId: number | null
}

export interface RestaurantSummaryInfo {
  reviewCount: number
  avgRating: number
  likeCount: number
}

export interface RestaurantReviewItem {
  id: number
  rating: number
  content: string
  detail?: string | null
  likes: number
  liked: boolean
  createdAt: string
  user?: {
    id: number
    username: string
    nickname: string
    avatarUrl?: string | null
  }
  orderId?: number
}

export interface RestaurantProfileResponse {
  restaurant: RestaurantProfile
  summary: RestaurantSummaryInfo
  highlight?: RestaurantReviewItem
}

export async function getRestaurantProfile(): Promise<Result<RestaurantProfileResponse>> {
  return postDataRequest<RestaurantProfileResponse>('/api/restaurant/profile/get', {})
}

export async function saveRestaurantProfile(payload: Partial<RestaurantProfile>): Promise<Result<RestaurantProfile>> {
  return postDataRequest<RestaurantProfile>('/api/restaurant/profile/save', payload as Record<string, any>)
}

export interface ReviewListResponse {
  items: RestaurantReviewItem[]
  page: number
  size: number
  total: number
}

export async function getRestaurantReviews(restaurantId: number, page = 1, size = 8): Promise<Result<ReviewListResponse>> {
  return postDataRequestWithPage<ReviewListResponse>(
    '/api/restaurant/reviews/page',
    { restaurantId },
    page,
    size,
  )
}

export async function toggleRestaurantReviewLike(reviewId: number): Promise<Result<RestaurantReviewItem>> {
  return postDataRequest<RestaurantReviewItem>('/api/restaurant/reviews/like', { reviewId })
}
