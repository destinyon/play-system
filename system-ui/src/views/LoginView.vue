<template>
  <div class="auth-page">
    <div class="auth-container">
      <!-- Intro / Branding -->
      <section class="intro">
        <div class="brand">
          <div class="logo">
            <svg width="40" height="40" viewBox="0 0 32 32" fill="none">
              <circle cx="16" cy="16" r="14" fill="#3b82f6" opacity="0.15"/>
              <path d="M16 8L18.5 13L24 14L20 18L21 24L16 21L11 24L12 18L8 14L13.5 13L16 8Z" fill="#3b82f6"/>
            </svg>
          </div>
          <h1>Celeste's Takeout Project</h1>
          <p class="tagline">一个基于 Spring + Vue 的外卖演示项目</p>
        </div>

        <ul class="intro-points">
          <li>
            <strong>三种角色</strong>
            <span>用户、商家、骑手，完整流程体验</span>
          </li>
          <li>
            <strong>地图与路线</strong>
            <span>集成高德地图，路线规划与标记管理</span>
          </li>
          <li>
            <strong>现代化前端</strong>
            <span>Vite + Vue 3 + TypeScript</span>
          </li>
        </ul>

        <!-- 注册为“商家”时，在介绍卡片底部放置地图（作为选点区域） -->
        <div class="intro-map-wrap" v-show="activeTab==='register' && registerForm.role==='RESTAURATEUR'">
          <div id="register-intro-map" class="intro-map"></div>
        </div>
      </section>

      <!-- Auth Card -->
      <section class="auth-card">
        <div class="tabs">
          <button :class="['tab', { active: activeTab === 'login' }]" @click="activeTab = 'login'">登录</button>
          <button :class="['tab', { active: activeTab === 'register' }]" @click="activeTab = 'register'">注册</button>
          <button :class="['tab', { active: activeTab === 'forgot' }]" @click="activeTab = 'forgot'">忘记密码</button>
        </div>

        <!-- Login -->
        <form v-if="activeTab === 'login'" class="form" @submit.prevent="onLogin">
          <div class="preset-grid">
            <button
              v-for="preset in loginPresets"
              :key="preset.label"
              type="button"
              class="preset-btn"
              :class="preset.className"
              @click="applyPreset(preset)"
            >
              {{ preset.label }}
            </button>
          </div>
          <div class="form-item">
              <label>用户名</label>
            <input v-model.trim="loginForm.username" placeholder="请输入用户名" required />
          </div>
          <div class="form-item">
            <label>密码</label>
              <input v-model.trim="loginForm.password" type="password" placeholder="请输入密码" required />
          </div>
          <div class="form-actions">
            <button class="btn btn--primary" type="submit" :disabled="!loginValid">登录</button>
          </div>
        </form>

        <!-- Register -->
        <form v-else-if="activeTab === 'register'" class="form" @submit.prevent="onRegister">
          <div class="role-select">
            <span class="role-label">角色</span>
            <div class="role-options">
              <label :class="['role', { selected: registerForm.role==='GUEST' }]">
                <input type="radio" value="GUEST" v-model="registerForm.role" /> 用户
              </label>
              <label :class="['role', { selected: registerForm.role==='RESTAURATEUR' }]">
                <input type="radio" value="RESTAURATEUR" v-model="registerForm.role" /> 商家
              </label>
              <label :class="['role', { selected: registerForm.role==='DELIVERYMAN' }]">
                <input type="radio" value="DELIVERYMAN" v-model="registerForm.role" /> 骑手
              </label>
            </div>
          </div>

          <div class="form-item">
              <label>用户名</label>
            <input v-model.trim="registerForm.username" placeholder="请输入用户名" required />
          </div>

          <div class="form-row">
            <div class="form-item">
              <label>密码</label>
                <input v-model.trim="registerForm.password" type="password" placeholder="至少 6 位密码" minlength="6" required />
            </div>
            <div class="form-item">
              <label>再次输入</label>
              <input v-model.trim="registerForm.confirm" type="password" placeholder="再次输入密码" minlength="6" required />
            </div>
          </div>
            <p v-if="registerForm.confirm && registerForm.password !== registerForm.confirm" class="error">两次输入的密码不一致</p>

          <div class="form-row">
            <div class="form-item">
              <label>邮箱（可选）</label>
              <input v-model.trim="registerForm.email" type="email" placeholder="example@domain.com" />
              <small class="hint">后端会验证邮箱格式</small>
            </div>
            <div class="form-item">
              <label>电话（可选）</label>
              <input v-model.trim="registerForm.phone" placeholder="11位手机号" />
              <small class="hint">后端会验证手机号格式</small>
            </div>
          </div>

          <!-- Merchant extra panel -->
          <RestaurantRegisterPanel v-if="registerForm.role==='RESTAURATEUR'" v-model="restaurantModel" :map-target="'#register-intro-map'" />

          <div class="form-actions">
            <button class="btn btn--primary" type="submit" :disabled="!registerValid">注册</button>
          </div>
        </form>

        <!-- Forgot -->
        <form v-else class="form" @submit.prevent="onForgot">
          <div class="form-item">
            <label>用户名或邮箱</label>
              <input v-model.trim="forgotForm.account" placeholder="请输入用户名或邮箱" required />
          </div>
          <div class="form-actions">
              <button class="btn btn--secondary" type="submit" :disabled="!forgotValid">发送重置请求</button>
          </div>
        </form>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { apiLogin, apiRegister } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import { postDataRequest } from '@/api/http'
import RestaurantRegisterPanel from '@/components/RestaurantRegisterPanel.vue'

const activeTab = ref<'login'|'register'|'forgot'>('login')
const router = useRouter()
const auth = useAuthStore()

// login
const loginForm = reactive({ username: '', password: '' })
const loginPresets = [
  { label: '管理员', username: 'admin', password: 'admin123', className: 'preset-btn--admin' },
  { label: '用户', username: 'guest', password: 'guest123', className: 'preset-btn--guest' },
  { label: '商家', username: 'restaurateur', password: 'restaurateur123', className: 'preset-btn--restaurateur' },
  { label: '骑手', username: 'deliveryman', password: 'deliveryman123', className: 'preset-btn--deliveryman' }
]
const applyPreset = (preset: typeof loginPresets[number]) => {
  activeTab.value = 'login'
  loginForm.username = preset.username
  loginForm.password = preset.password
}
const loginValid = computed(() => !!loginForm.username && !!loginForm.password)
async function onLogin() {
  const res = await apiLogin({ username: loginForm.username, password: loginForm.password })
  if (res.status === 200) {
    // 假设后端返回 data = { username, nickname, role, avatarUrl }
    const data: any = res.data || {}
    auth.setUser({
      username: data.username || loginForm.username,
      nickname: data.nickname || data.username || loginForm.username,
      role: data.role || 'GUEST',
      avatarUrl: data.avatarUrl || '',
      token: data.token || data.accessToken || '',
      tokenType: data.tokenType || data.token_type || 'Bearer',
      tokenExpiresAt: data.tokenExpiresAt || data.expiresAt || null,
      tokenIssuedAt: data.tokenIssuedAt || data.issuedAt || null,
    })
    // 角色路由跳转
    if (auth.role === 'ADMIN') router.push('/app/admin/dashboard')
    else if (auth.role === 'RESTAURATEUR') router.push('/app/restaurateur/home')
    else if (auth.role === 'DELIVERYMAN') router.push('/app/deliveryman/home')
    else router.push('/app/guest/home')
  } else {
    alert(res.message || '登录失败')
  }
}

// register
const registerForm = reactive({
  role: 'GUEST', // GUEST / RESTAURATEUR / DELIVERYMAN (align with backend)
  username: '',
  password: '',
  confirm: '',
  email: '',
  phone: '',
})
// Merchant model collected from panel; photoFile holds the selected File until submission
const restaurantModel = ref<{ name?: string; address?: string; lng?: number; lat?: number; photoUrl?: string; photoFile?: File }>({})
const registerValid = computed(() => {
  if (!registerForm.username || !registerForm.password || !registerForm.confirm) return false
  if (registerForm.password !== registerForm.confirm) return false
  if (registerForm.role === 'RESTAURATEUR') {
    const m = restaurantModel.value
    if (!m.name || !m.address || !m.lng || !m.lat) return false
  }
  return true
})

// When user switches role, clear merchant panel state to avoid leftover File objects or open map
import { watch } from 'vue'
watch(() => registerForm.role, (val, oldVal) => {
  if (val !== 'RESTAURATEUR' && oldVal === 'RESTAURATEUR') {
    // clear merchant model to collapse panel and avoid dangling File references
    restaurantModel.value = {}
  }
})
async function onRegister() {
  if (registerForm.password !== registerForm.confirm) return
  const payload = {
    role: registerForm.role as any,
    username: registerForm.username,
    password: registerForm.password,
    email: registerForm.email || undefined,
    phone: registerForm.phone || undefined,
  }
  const res = await apiRegister(payload)
  if (res.status === 200) {
    // Merchant: if the merchant panel selected a File but didn't upload yet, upload it now
    if (registerForm.role === 'RESTAURATEUR') {
      let serverPhotoUrl = restaurantModel.value.photoUrl
      if ((restaurantModel.value as any).photoFile) {
        try {
          const { uploadRestaurantPhoto } = await import('@/api/restaurant')
          const upRes = await uploadRestaurantPhoto((restaurantModel.value as any).photoFile)
          if (upRes.status === 200 && upRes.data?.url) {
            serverPhotoUrl = upRes.data.url
          } else {
            console.warn('restaurant upload during register failed', upRes)
          }
        } catch (err) {
          console.warn('upload error', err)
        }
      }

      const createRes = await postDataRequest('/api/restaurant/createOrUpdate', {
        name: restaurantModel.value.name,
        address: restaurantModel.value.address,
        photoUrl: serverPhotoUrl,
        lng: restaurantModel.value.lng,
        lat: restaurantModel.value.lat,
        restaurateur: { username: registerForm.username }
      })
      if (createRes.status !== 200) {
        alert(createRes.message || '创建餐馆信息失败，请稍后在“餐馆信息”中补充')
      }
    }
    alert('注册成功，请登录')
    activeTab.value = 'login'
    loginForm.username = registerForm.username
  } else {
    alert(res.message || '注册失败')
  }
}

// forgot
const forgotForm = reactive({ account: '' })
const forgotValid = computed(() => !!forgotForm.account)
function onForgot() {
  // 后端未提供忘记密码接口示例，这里仅做占位提示
  alert('已发送重置请求（示例）。请在后端实现实际的邮件/短信流程')
}
</script>

<style scoped src="./styles/LoginView.css"></style>


