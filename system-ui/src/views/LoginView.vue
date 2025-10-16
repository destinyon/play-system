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
            <span class="role-label">角色：</span>
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
              <input v-model.trim="registerForm.password" type="password" placeholder="至少 6 位" minlength="6" required />
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
      avatarUrl: data.avatarUrl || ''
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
        alert(createRes.message || '创建餐馆信息失败，请稍后在“餐馆信息”中补充。')
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
  alert('已发送重置请求（示例）。请在后端实现实际的邮件/短信流程。')
}
</script>

<style scoped>
.auth-page {
  position: fixed;
  inset: 0;
  background: radial-gradient(1200px 600px at 10% 0%, #dbeafe 0%, transparent 60%),
              radial-gradient(1000px 600px at 100% 100%, #e0e7ff 0%, transparent 60%),
              linear-gradient(135deg, #ffffff, #f8fafc);
  display: grid;
  place-items: center;
}
.auth-container {
  width: min(1100px, 92vw);
  display: grid;
  grid-template-columns: 1.1fr 1fr;
  gap: 24px;
  align-items: stretch;
}
.intro {
  background: linear-gradient(135deg, #ffffff 0%, #f0f9ff 100%);
  border: 2px solid rgba(147, 197, 253, 0.5);
  border-radius: 20px;
  padding: 28px;
  box-shadow: 0 10px 40px rgba(59, 130, 246, 0.12);
}
.brand { display: flex; flex-direction: column; gap: 10px; margin-bottom: 18px; }
.brand .logo { width: 44px; height: 44px; display: grid; place-items: center; background: #eff6ff; border-radius: 12px; }
.brand h1 { margin: 0; font-size: 26px; font-weight: 800; letter-spacing: 0.2px; color: #1e293b; }
.brand .tagline { margin: 0; color: #334155; font-weight: 600; }

.intro-points { list-style: none; padding: 0; margin: 10px 0 0; display: flex; flex-direction: column; gap: 12px; }
.intro-points li { background: #ffffff; border: 2px solid #e2e8f0; border-radius: 12px; padding: 14px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
.intro-points strong { display: block; color: #0ea5e9; font-size: 14px; margin-bottom: 4px; }
.intro-points span { color: #0f172a; font-weight: 600; }

.intro-map-wrap { margin-top: 20px; }
.intro-map { width: 450px; height: 400px; border-radius: 12px; border: 2px solid #93c5fd; overflow: hidden; background: #fff; box-shadow: 0 6px 24px rgba(59,130,246,.10); }

.auth-card {
  background: linear-gradient(135deg, #ffffff 0%, #f8fbff 100%);
  border: 2px solid rgba(147, 197, 253, 0.5);
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 10px 40px rgba(59, 130, 246, 0.12);
}
.tabs { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin-bottom: 16px; }
.tab { padding: 10px 12px; border-radius: 10px; border: 2px solid #dbeafe; background: #fff; cursor: pointer; font-weight: 700; color: #2563eb; transition: .2s; }
.tab.active { background: linear-gradient(135deg, #3b82f6 0%, #06b6d4 100%); color: #fff; border-color: #93c5fd; box-shadow: 0 4px 16px rgba(59,130,246,.3); }

.form { display: flex; flex-direction: column; gap: 12px; }
.preset-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; margin-bottom: 4px; }
.preset-btn { border: none; border-radius: 12px; padding: 10px 12px; font-weight: 700; color: #0f172a; cursor: pointer; transition: transform .2s, box-shadow .2s; background: #f1f5f9; }
.preset-btn:hover { transform: translateY(-2px); box-shadow: 0 6px 16px rgba(15,23,42,.12); }
.preset-btn:focus-visible { outline: 3px solid rgba(59,130,246,.35); outline-offset: 2px; }
.preset-btn--admin { background: linear-gradient(135deg, rgba(37,99,235,.12) 0%, rgba(59,130,246,.28) 100%); color: #1d4ed8; }
.preset-btn--guest { background: linear-gradient(135deg, rgba(16,185,129,.12) 0%, rgba(34,197,94,.28) 100%); color: #047857; }
.preset-btn--restaurateur { background: linear-gradient(135deg, rgba(249,115,22,.12) 0%, rgba(251,146,60,.3) 100%); color: #c2410c; }
.preset-btn--deliveryman { background: linear-gradient(135deg, rgba(6,182,212,.12) 0%, rgba(59,130,246,.3) 100%); color: #0f766e; }
.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.form-item { display: flex; flex-direction: column; gap: 6px; }
.form-item label { font-size: 13px; font-weight: 800; color: #334155; }
.form-item input, .form-item select {
  border: 2px solid #e2e8f0; border-radius: 10px; padding: 10px 12px; font-size: 14px; font-weight: 600; color: #0f172a; background: #f8fafc; transition: .2s;
}
.form-item input:focus { outline: none; border-color: #93c5fd; background: #fff; box-shadow: 0 0 0 4px rgba(147,197,253,.25); }
.form-actions { margin-top: 6px; }
.btn { display: inline-flex; align-items: center; justify-content: center; gap: 8px; border: none; padding: 10px 16px; border-radius: 10px; cursor: pointer; font-weight: 800; letter-spacing: .2px; }
.btn--primary { background: linear-gradient(135deg, #3b82f6 0%, #06b6d4 100%); color: #fff; box-shadow: 0 4px 14px rgba(59,130,246,.35); }
.btn--secondary { background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%); color: #0369a1; border: 2px solid #bae6fd; }
.btn:disabled { opacity: .5; cursor: not-allowed; }
.error { color: #dc2626; font-weight: 700; font-size: 12px; }
.hint { color: #64748b; font-size: 12px; font-weight: 600; }

.role-select { display: flex; align-items: center; gap: 10px; }
.role-label { font-size: 13px; font-weight: 800; color: #334155; }
.role-options { display: flex; gap: 8px; flex-wrap: wrap; }
.role { display: inline-flex; align-items: center; gap: 6px; padding: 8px 10px; border-radius: 10px; border: 2px solid #e2e8f0; cursor: pointer; font-weight: 700; color: #0f172a; background: #fff; }
.role input { display: none; }
.role.selected { border-color: #93c5fd; background: #eff6ff; color: #0369a1; box-shadow: 0 2px 10px rgba(59,130,246,.2); }

@media (max-width: 960px) {
  .auth-container { grid-template-columns: 1fr; }
}
</style>
