<template>
  <div class="main-layout">
    <!-- Top App Bar -->
    <header class="app-bar">
      <div class="app-bar-left">
        <button class="menu-toggle" @click="toggleSidebar" aria-label="Toggle menu">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="3" y1="12" x2="21" y2="12"></line>
            <line x1="3" y1="6" x2="21" y2="6"></line>
            <line x1="3" y1="18" x2="21" y2="18"></line>
          </svg>
        </button>
        <div class="app-logo">
          <svg width="32" height="32" viewBox="0 0 32 32" fill="none">
            <circle cx="16" cy="16" r="14" fill="#3b82f6" opacity="0.2"/>
            <path d="M16 8L18.5 13L24 14L20 18L21 24L16 21L11 24L12 18L8 14L13.5 13L16 8Z" fill="#3b82f6"/>
          </svg>
          <h1 class="app-title">高德导航系统</h1>
        </div>

        <!-- Avatar & Nickname on the left -->
        <div class="user-menu" @mouseleave="menuOpen=false">
          <button class="avatar-btn" @click="menuOpen=!menuOpen" title="用户中心">
            <img v-if="avatarUrl" :src="avatarUrl" alt="avatar" />
            <div v-else class="avatar-fallback">{{ initials }}</div>
            <span class="nickname">{{ nickname || username }}</span>
          </button>
          <div class="menu" v-if="menuOpen">
            <router-link class="menu-item" to="/app/profile">个人信息</router-link>
            <button class="menu-item" @click="changePassword">修改密码</button>
            <button class="menu-item danger" @click="logout">退出登录</button>
          </div>
        </div>
      </div>
      <div class="app-bar-right">
        <button class="icon-btn" @click="refreshMap" title="刷新地图">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="23 4 23 10 17 10"></polyline>
            <polyline points="1 20 1 14 7 14"></polyline>
            <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"></path>
          </svg>
        </button>
      </div>
    </header>

    <!-- Main Container -->
    <div class="main-container">
      <!-- Sidebar -->
      <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
        <nav class="sidebar-nav">
          <!-- Admin Menu -->
          <router-link v-if="role==='ADMIN'" to="/app/admin/dashboard" class="nav-item" active-class="active">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="3" width="7" height="9"/><rect x="14" y="3" width="7" height="5"/><rect x="14" y="10" width="7" height="11"/><rect x="3" y="14" width="7" height="7"/>
            </svg>
            <span class="nav-text">管理员控制台</span>
          </router-link>
          <router-link v-if="role==='ADMIN'" to="/app/profile" class="nav-item" active-class="active">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
              <circle cx="12" cy="7" r="4"></circle>
            </svg>
            <span class="nav-text">个人信息</span>
          </router-link>

          <!-- Guest/User Menu -->
          <router-link v-if="role==='GUEST' || !role" to="/app/guest/home" class="nav-item" active-class="active">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
            </svg>
            <span class="nav-text">首页</span>
          </router-link>
          <router-link v-if="role==='GUEST' || !role" to="/app/guest/orders" class="nav-item" active-class="active">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="6" cy="19" r="3"></circle>
              <path d="M9 19h8.5a3.5 3.5 0 0 0 0-7h-11a3.5 3.5 0 0 1 0-7H15"></path>
            </svg>
            <span class="nav-text">我的订单</span>
          </router-link>
          <router-link v-if="role==='GUEST' || !role" to="/app/guest/cart" class="nav-item" active-class="active">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M6 6h15l-1.5 9h-12z"></path>
            </svg>
            <span class="nav-text">购物车</span>
          </router-link>
          <router-link v-if="role==='GUEST' || !role" to="/app/guest/chat" class="nav-item" active-class="active">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15a4 4 0 0 1-4 4H7l-4 4V5a4 4 0 0 1 4-4h10a4 4 0 0 1 4 4z"></path>
            </svg>
            <span class="nav-text">聊天</span>
          </router-link>
          <router-link v-if="role==='GUEST' || !role" to="/app/profile" class="nav-item" active-class="active">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
              <circle cx="12" cy="7" r="4"></circle>
            </svg>
            <span class="nav-text">我的信息</span>
          </router-link>

          <!-- Restaurateur Menu -->
          <router-link v-if="role==='RESTAURATEUR'" to="/app/restaurateur/home" class="nav-item" active-class="active">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
            </svg>
            <span class="nav-text">首页</span>
          </router-link>
          <router-link v-if="role==='RESTAURATEUR'" to="/app/restaurateur/dashboard" class="nav-item" active-class="active">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="9"/><rect x="14" y="3" width="7" height="5"/><rect x="14" y="10" width="7" height="11"/><rect x="3" y="14" width="7" height="7"/></svg>
            <span class="nav-text">运营概览</span>
          </router-link>
          <router-link v-if="role==='RESTAURATEUR'" to="/app/restaurateur/orders" class="nav-item" active-class="active">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 7h18M3 12h18M3 17h18"/></svg>
            <span class="nav-text">接收订单</span>
          </router-link>
          <router-link v-if="role==='RESTAURATEUR'" to="/app/restaurateur/dishes" class="nav-item" active-class="active">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 21v-7m8 7V3m8 18v-7"/></svg>
            <span class="nav-text">菜品管理</span>
          </router-link>
          <router-link v-if="role==='RESTAURATEUR'" to="/app/restaurateur/chat" class="nav-item" active-class="active">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a4 4 0 0 1-4 4H7l-4 4V5a4 4 0 0 1 4-4h10a4 4 0 0 1 4 4z"/></svg>
            <span class="nav-text">聊天</span>
          </router-link>
          <router-link v-if="role==='RESTAURATEUR'" to="/app/restaurateur/restaurant" class="nav-item" active-class="active">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 11l9-9 9 9v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/></svg>
            <span class="nav-text">餐馆信息</span>
          </router-link>

          <!-- Deliveryman Menu -->
          <router-link v-if="role==='DELIVERYMAN'" to="/app/deliveryman/home" class="nav-item" active-class="active">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
            </svg>
            <span class="nav-text">首页</span>
          </router-link>
          <router-link v-if="role==='DELIVERYMAN'" to="/app/deliveryman/dashboard" class="nav-item" active-class="active">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 17l-1 2h16l-1-2zM6 6h11l3 7H3z"/></svg>
            <span class="nav-text">工作台</span>
          </router-link>
          <router-link v-if="role==='DELIVERYMAN'" to="/app/deliveryman/deliver" class="nav-item" active-class="active">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 12h18M3 6h18M3 18h18"/></svg>
            <span class="nav-text">送单</span>
          </router-link>
          <router-link v-if="role==='DELIVERYMAN'" to="/app/deliveryman/chat" class="nav-item" active-class="active">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a4 4 0 0 1-4 4H7l-4 4V5a4 4 0 0 1 4-4h10a4 4 0 0 1 4 4z"/></svg>
            <span class="nav-text">聊天</span>
          </router-link>
          <router-link v-if="role==='DELIVERYMAN'" to="/app/deliveryman/profile" class="nav-item" active-class="active">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
            <span class="nav-text">我的信息</span>
          </router-link>
          
          <div class="nav-divider"></div>
          
          <router-link to="/app/settings" class="nav-item" active-class="active">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="3"></circle>
              <path d="M12 1v6m0 6v6m-6-6h6m6 0h6M4.93 4.93l4.24 4.24m5.66 5.66l4.24 4.24M4.93 19.07l4.24-4.24m5.66-5.66l4.24-4.24"></path>
            </svg>
            <span class="nav-text">设置</span>
          </router-link>
        </nav>
        
        <div class="sidebar-footer" v-if="!sidebarCollapsed">
          <div class="version-info">
            <small>Version 1.0.0</small>
          </div>
        </div>
      </aside>

      <!-- Main Content Area -->
      <main class="content-area">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const sidebarCollapsed = ref(false)
const menuOpen = ref(false)
const auth = useAuthStore()
auth.load()

const username = computed(() => auth.username)
const nickname = computed(() => auth.nickname)
const role = computed(() => auth.role)
const avatarUrl = computed(() => auth.avatarUrl)
const initials = computed(() => (nickname.value || username.value || 'U').slice(0,2).toUpperCase())

const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

const refreshMap = () => {
  // Emit event or use event bus to refresh map component
  window.dispatchEvent(new CustomEvent('refresh-map'))
}

function changePassword() {
  alert('修改密码功能待实现')
}
function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.main-layout {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #f5f7fa;
}

/* App Bar */
.app-bar {
  height: 60px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  z-index: 100;
}

.app-bar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.menu-toggle {
  background: transparent;
  border: none;
  color: white;
  cursor: pointer;
  padding: 8px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.menu-toggle:hover {
  background: rgba(255, 255, 255, 0.1);
}

.app-logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.app-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.app-bar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.icon-btn {
  background: transparent;
  border: none;
  color: white;
  cursor: pointer;
  padding: 10px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.icon-btn:hover {
  background: rgba(255, 255, 255, 0.15);
}

.user-menu { position: relative; }
.avatar-btn { display:flex; align-items:center; gap:8px; background:transparent; border:none; color:white; cursor:pointer; padding:6px 8px; border-radius:8px; }
.avatar-btn:hover { background: rgba(255,255,255,.15); }
.avatar-btn img { width:28px; height:28px; border-radius:50%; object-fit:cover; border:2px solid rgba(255,255,255,.5); }
.avatar-fallback { width:28px; height:28px; border-radius:50%; background:#fff; color:#4f46e5; display:grid; place-items:center; font-weight:800; }
.nickname { font-weight:700; }
.menu { position:absolute; right:0; top:40px; background:#fff; color:#111827; border:1px solid #e5e7eb; border-radius:10px; box-shadow:0 10px 30px rgba(0,0,0,.1); min-width:160px; overflow:hidden; z-index:200; }
.menu-item { display:block; width:100%; text-align:left; padding:10px 12px; border:none; background:#fff; color:#111827; text-decoration:none; font-weight:600; cursor:pointer; }
.menu-item:hover { background:#f3f4f6; }
.menu-item.danger { color:#dc2626; }

/* Main Container */
.main-container {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* Sidebar */
.sidebar {
  width: 240px;
  background: white;
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease, transform 0.3s ease;
  z-index: 50;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.05);
}

.sidebar.collapsed {
  width: 64px;
}

.sidebar.collapsed .nav-text {
  opacity: 0;
  width: 0;
}

.sidebar.collapsed .sidebar-footer {
  display: none;
}

.sidebar-nav {
  flex: 1;
  padding: 16px 8px;
  overflow-y: auto;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  margin-bottom: 8px;
  border-radius: 8px;
  color: #6b7280;
  text-decoration: none;
  transition: all 0.2s;
  font-size: 15px;
  font-weight: 500;
  white-space: nowrap;
}

.nav-item:hover {
  background: #f3f4f6;
  color: #3b82f6;
}

.nav-item.active {
  background: linear-gradient(135deg, #667eea15 0%, #764ba215 100%);
  color: #667eea;
  font-weight: 600;
}

.nav-text {
  transition: opacity 0.3s, width 0.3s;
}

.nav-divider {
  height: 1px;
  background: #e5e7eb;
  margin: 16px 12px;
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid #e5e7eb;
  background: #f9fafb;
}

.version-info {
  text-align: center;
  color: #9ca3af;
}

/* Content Area */
.content-area {
  flex: 1;
  position: relative;
  overflow: hidden;
}

/* Route Transition */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* Responsive */
@media (max-width: 768px) {
  .app-title {
    font-size: 16px;
  }
  
  .sidebar {
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    transform: translateX(0);
  }
  
  .sidebar.collapsed {
    transform: translateX(-100%);
    width: 240px;
  }
}
</style>
