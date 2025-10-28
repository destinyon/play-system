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
  token: string
  tokenType: string
  tokenExpiresAt?: string | null
  tokenIssuedAt?: string | null
  restaurant?: RestaurantInfo | null
}

export const useAuthStore = defineStore('auth', {
  state: (): UserState => ({
    username: '',
    nickname: '',
    role: null,
    avatarUrl: '',
    token: '',
    tokenType: 'Bearer',
    tokenExpiresAt: null,
    tokenIssuedAt: null,
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
        token: this.token,
        tokenType: this.tokenType,
        tokenExpiresAt: this.tokenExpiresAt,
        tokenIssuedAt: this.tokenIssuedAt,
      }))
    },
    setToken(token: string, options?: { tokenType?: string; expiresAt?: string | null; issuedAt?: string | null }) {
      this.token = token
      if (options?.tokenType) {
        this.tokenType = options.tokenType
      }
      if (Object.prototype.hasOwnProperty.call(options ?? {}, 'expiresAt')) {
        this.tokenExpiresAt = options?.expiresAt ?? null
      }
      if (Object.prototype.hasOwnProperty.call(options ?? {}, 'issuedAt')) {
        this.tokenIssuedAt = options?.issuedAt ?? null
      }
      // keep persisted state in sync
      this.setUser({})
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
          this.token = data.token || ''
          this.tokenType = data.tokenType || 'Bearer'
          this.tokenExpiresAt = data.tokenExpiresAt || null
          this.tokenIssuedAt = data.tokenIssuedAt || null
        }
      } catch {}
    },
    logout() {
      this.username = ''
      this.nickname = ''
      this.role = null
      this.avatarUrl = ''
       this.token = ''
       this.tokenExpiresAt = null
       this.tokenIssuedAt = null
      this.restaurant = null
      localStorage.removeItem('auth.user')
    }
  }
})
