<template>
  <div class="public-profile">
    <div class="public-profile__shell">
      <button class="back-btn" type="button" @click="goBack">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M15 18l-6-6 6-6" />
        </svg>
        返回
      </button>

      <div class="profile-card" v-if="!state.loading && !state.error && state.profile">
        <div class="profile-card__header">
          <div class="avatar" :style="avatarStyle">
            <span v-if="!state.profile.avatarUrl">{{ initials }}</span>
            <img v-else :src="state.profile.avatarUrl" alt="用户头像" />
          </div>
          <div class="profile-card__identity">
            <h1>{{ state.profile.nickname || state.profile.username }}</h1>
            <p class="tag">{{ roleText }}</p>
          </div>
        </div>

        <div class="profile-info">
          <div class="info-item">
            <span class="label">用户名</span>
            <span class="value">{{ state.profile.username || '—' }}</span>
          </div>
          <div class="info-item" v-if="state.profile.email">
            <span class="label">邮箱</span>
            <span class="value">{{ state.profile.email }}</span>
          </div>
          <div class="info-item" v-if="state.profile.phone">
            <span class="label">联系电话</span>
            <span class="value">{{ state.profile.phone }}</span>
          </div>
          <div class="info-item">
            <span class="label">联系地址</span>
            <span class="value">{{ state.profile.address || '暂无地址信息' }}</span>
          </div>
          <div class="info-item" v-if="state.profile.bio">
            <span class="label">个人介绍</span>
            <span class="value">{{ state.profile.bio }}</span>
          </div>
        </div>
      </div>

      <div v-else class="status-card">
        <div v-if="state.loading" class="status-card__text">正在加载用户主页…</div>
        <div v-else class="status-card__text status-card__text--error">
          {{ state.error || '没有找到该用户信息' }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getUserPublicProfile } from '@/api/user'

interface PublicProfile {
  id?: number | string
  username?: string
  nickname?: string
  email?: string
  phone?: string
  address?: string
  bio?: string
  avatarUrl?: string
  role?: string
}

const route = useRoute()
const router = useRouter()

const state = reactive({
  loading: true,
  error: '',
  profile: null as PublicProfile | null,
})

const identifier = computed(() => ((route.params.identifier as string | undefined) || '').trim())
const userIdQuery = computed(() => route.query.userId as string | undefined)

const API_FILE_BASE =
  import.meta.env.VITE_FILE_BASE ||
  (import.meta.env.DEV
    ? 'http://localhost:8080'
    : typeof window !== 'undefined'
      ? window.location.origin
      : '')

function normalizeImageUrl(url?: string | null) {
  if (!url) return null
  if (/^(data:|https?:)/i.test(url)) return url
  const path = url.startsWith('/') ? url : `/${url}`
  if (!API_FILE_BASE) return path
  return `${API_FILE_BASE}${path}`
}

const avatarStyle = computed(() => {
  if (!state.profile?.avatarUrl) return {}
  return { backgroundImage: `url(${state.profile.avatarUrl})` }
})

const initials = computed(() => {
  const source = state.profile?.nickname || state.profile?.username || 'U'
  return source.slice(0, 1).toUpperCase()
})

const roleText = computed(() => {
  switch (state.profile?.role) {
    case 'RESTAURATEUR':
      return '商家'
    case 'DELIVERYMAN':
      return '骑手'
    case 'GUEST':
      return '用户'
    default:
      return '访客'
  }
})

async function loadProfile() {
  if (!identifier.value && !userIdQuery.value) {
    state.error = '缺少用户标识'
    state.profile = null
    state.loading = false
    return
  }

  state.loading = true
  state.error = ''
  state.profile = null
  try {
    const payload: Record<string, any> = {}
    if (userIdQuery.value) {
      const numericId = Number(userIdQuery.value)
      payload.userId = Number.isNaN(numericId) ? userIdQuery.value : numericId
    } else if (identifier.value) {
      payload.username = identifier.value
    }

    const res = await getUserPublicProfile(payload)
    if (res.status === 200 && res.data) {
      const profile = { ...(res.data as Record<string, any>) }
      profile.avatarUrl = normalizeImageUrl(profile.avatarUrl)
      state.profile = profile
    } else {
      state.error = res.message || '用户信息暂不可用'
    }
  } catch (error: any) {
    state.error = error?.message || '加载用户信息失败'
  } finally {
    state.loading = false
  }
}

function goBack() {
  if (window.history.length > 1) router.back()
  else router.push('/app/restaurateur/restaurant')
}

watch(
  () => [identifier.value, userIdQuery.value],
  () => {
    loadProfile()
  },
  { immediate: true },
)
</script>

<style scoped>
.public-profile {
  min-height: 100%;
  background: linear-gradient(135deg, #fff7ed 0%, #fde68a 45%, #fef3c7 100%);
  padding: 32px;
  display: flex;
  justify-content: center;
}

.public-profile__shell {
  width: min(720px, 100%);
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.back-btn {
  align-self: flex-start;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  background: rgba(249, 115, 22, 0.15);
  border-radius: 999px;
  border: none;
  color: #c2410c;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.back-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 22px rgba(249, 115, 22, 0.28);
}

.profile-card {
  background: linear-gradient(155deg, rgba(255, 255, 255, 0.9), rgba(254, 215, 170, 0.92));
  border-radius: 24px;
  padding: 32px 36px;
  box-shadow: 0 24px 48px rgba(249, 115, 22, 0.2);
  border: 1px solid rgba(251, 191, 36, 0.35);
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.profile-card__header {
  display: flex;
  align-items: center;
  gap: 20px;
}

.avatar {
  width: 96px;
  height: 96px;
  border-radius: 28px;
  background: rgba(250, 204, 21, 0.25);
  display: grid;
  place-items: center;
  color: #7c2d12;
  font-size: 32px;
  font-weight: 800;
  overflow: hidden;
  box-shadow: inset 0 0 0 1px rgba(249, 115, 22, 0.32);
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-card__identity h1 {
  margin: 0 0 8px 0;
  font-size: 30px;
  font-weight: 800;
  color: #7c2d12;
}

.tag {
  margin: 0;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: 999px;
  background: rgba(249, 115, 22, 0.18);
  color: #c2410c;
  font-weight: 700;
}

.profile-info {
  display: grid;
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(251, 191, 36, 0.25);
  box-shadow: 0 12px 24px rgba(251, 191, 36, 0.16);
}

.label {
  font-size: 13px;
  font-weight: 700;
  color: #fb923c;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.value {
  font-size: 16px;
  font-weight: 700;
  color: #7c2d12;
}

.status-card {
  background: rgba(255, 255, 255, 0.85);
  border-radius: 20px;
  padding: 40px;
  text-align: center;
  font-weight: 700;
  color: #c2410c;
  box-shadow: 0 18px 40px rgba(249, 115, 22, 0.22);
  border: 1px solid rgba(251, 191, 36, 0.3);
}

.status-card__text {
  font-size: 18px;
}

.status-card__text--error {
  color: #b91c1c;
}

@media (max-width: 768px) {
  .public-profile {
    padding: 20px;
  }

  .profile-card {
    padding: 24px;
  }

  .profile-card__header {
    flex-direction: column;
    align-items: flex-start;
  }

  .avatar {
    width: 80px;
    height: 80px;
  }
}
</style>
