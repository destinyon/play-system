import { defineStore } from 'pinia'

export type Role = 'GUEST' | 'RESTAURATEUR' | 'DELIVERYMAN' | 'ADMIN'

export interface RestaurantInfo {
  id?: number | string
  name?: string
  address?: string
  lng?: number
  lat?: number
  photoUrl?: string
}

export interface UserState {
  username: string
  nickname: string
  role: Role | null
  avatarUrl?: string
  restaurant?: RestaurantInfo | null
}

export const useAuthStore = defineStore('auth', {
  state: (): UserState => ({
    username: '',
    nickname: '',
    role: null,
    avatarUrl: '',
    restaurant: null,
  }),
  actions: {
    setUser(payload: Partial<UserState>) {
      Object.assign(this, payload)
      // Persist lightweight info
      localStorage.setItem('auth.user', JSON.stringify({
        username: this.username,
        nickname: this.nickname,
        role: this.role,
        avatarUrl: this.avatarUrl,
      }))
    },
    setRestaurant(info: RestaurantInfo | null) {
      this.restaurant = info
    },
    load() {
      try {
        const raw = localStorage.getItem('auth.user')
        if (raw) {
          const data = JSON.parse(raw)
          this.username = data.username || ''
          this.nickname = data.nickname || ''
          this.role = data.role || null
          this.avatarUrl = data.avatarUrl || ''
        }
      } catch {}
    },
    logout() {
      this.username = ''
      this.nickname = ''
      this.role = null
      this.avatarUrl = ''
      this.restaurant = null
      localStorage.removeItem('auth.user')
    }
  }
})
