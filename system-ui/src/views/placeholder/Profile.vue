<template>
  <div class="profile-page">
    <div class="profile-container">
      <div class="profile-header">
        <h1>个人信息</h1>
        <p class="subtitle">管理您的个人资料和账户设置</p>
      </div>

      <!-- Profile Card -->
      <div class="profile-card">
        <div class="avatar-section">
          <div class="avatar-wrapper">
            <img v-if="avatarUrl" :src="avatarUrl" alt="avatar" class="avatar-img" />
            <div v-else class="avatar-placeholder">{{ initials }}</div>
            <label class="avatar-upload">
              <input type="file" accept="image/*" @change="handleAvatarChange" hidden />
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"></path>
                <circle cx="12" cy="13" r="4"></circle>
              </svg>
            </label>
          </div>
          <div class="avatar-info">
            <h2>{{ userInfo.nickname || userInfo.username }}</h2>
            <span class="role-badge" :class="roleClass">{{ roleText }}</span>
          </div>
        </div>

        <form class="profile-form" @submit.prevent="saveProfile">
          <div class="form-grid">
            <div class="form-group">
              <label>用户名</label>
              <input type="text" :value="userInfo.username" disabled class="form-input disabled" />
            </div>

            <div class="form-group">
              <label>昵称</label>
              <input v-model="userInfo.nickname" type="text" class="form-input" placeholder="请输入昵称" />
            </div>

            <div class="form-group">
              <label>邮箱</label>
              <input v-model="userInfo.email" type="email" class="form-input" placeholder="请输入邮箱" />
            </div>

            <div class="form-group">
              <label>手机号</label>
              <input v-model="userInfo.phone" type="tel" class="form-input" placeholder="请输入手机号" />
            </div>

            <div class="form-group full-width">
              <label>地址</label>
              <input v-model="userInfo.address" type="text" class="form-input" placeholder="请输入地址" />
            </div>
          </div>

          <div class="form-actions">
            <button type="button" class="btn btn-secondary" @click="resetForm">重置</button>
            <button type="submit" class="btn btn-primary" :disabled="saving">
              {{ saving ? '保存中...' : '保存更改' }}
            </button>
          </div>
        </form>
      </div>

      <!-- Password Change Card -->
      <div class="profile-card">
        <h2 class="card-title">修改密码</h2>
        <form class="profile-form" @submit.prevent="changePasswordSubmit">
          <div class="form-grid">
            <div class="form-group full-width">
              <label>当前密码</label>
              <input v-model="passwordForm.oldPassword" type="password" class="form-input" placeholder="请输入当前密码" />
            </div>

            <div class="form-group">
              <label>新密码</label>
              <input v-model="passwordForm.newPassword" type="password" class="form-input" placeholder="请输入新密码" />
            </div>

            <div class="form-group">
              <label>确认新密码</label>
              <input v-model="passwordForm.confirmPassword" type="password" class="form-input" placeholder="请再次输入新密码" />
            </div>
          </div>

          <div class="form-actions">
            <button type="button" class="btn btn-secondary" @click="resetPasswordForm">重置</button>
            <button type="submit" class="btn btn-primary" :disabled="changingPassword">
              {{ changingPassword ? '修改中...' : '修改密码' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Toast Notification -->
    <div v-if="toast.show" class="toast" :class="toast.type">
      {{ toast.message }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { getUserInfo, updateUserInfo, changePassword, uploadAvatar } from '@/api/user'
import type { UploadAvatarResponse } from '@/api/user'

type Role = 'GUEST' | 'RESTAURATEUR' | 'DELIVERYMAN'

const auth = useAuthStore()
const saving = ref(false)
const changingPassword = ref(false)

const userInfo = reactive({
  username: '',
  nickname: '',
  email: '',
  phone: '',
  address: '',
  role: '' as Role | '',
  avatarUrl: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const avatarUrl = computed(() => auth.avatarUrl)
const initials = computed(() => (userInfo.nickname || userInfo.username || 'U').slice(0, 2).toUpperCase())

const roleText = computed(() => {
  switch (userInfo.role) {
    case 'RESTAURATEUR': return '商家'
    case 'DELIVERYMAN': return '骑手'
    case 'GUEST': return '用户'
    default: return '未知'
  }
})

const roleClass = computed(() => {
  switch (userInfo.role) {
    case 'RESTAURATEUR': return 'role-owner'
    case 'DELIVERYMAN': return 'role-rider'
    case 'GUEST': return 'role-user'
    default: return ''
  }
})

const toast = reactive({
  show: false,
  message: '',
  type: 'success'
})

const showToast = (message: string, type: 'success' | 'error' = 'success') => {
  toast.message = message
  toast.type = type
  toast.show = true
  setTimeout(() => { toast.show = false }, 3000)
}

const loadUserInfo = async () => {
  try {
    // 首先从auth store获取基本信息
    if (auth.username) {
      userInfo.username = auth.username
      userInfo.role = auth.role as Role || ''
      userInfo.avatarUrl = auth.avatarUrl || ''
    }
    
    const res = await getUserInfo()
    if (res.status === 200 && res.data) {
      // 合并后端返回的完整数据
      const data = res.data as Record<string, any>
      userInfo.username = data.username || userInfo.username
      userInfo.nickname = data.nickname || ''
      userInfo.email = res.data.email || ''
      userInfo.phone = res.data.phone || ''
      userInfo.address = res.data.address || ''
      userInfo.role = (data.role as Role) || userInfo.role
      userInfo.avatarUrl = data.avatarUrl || userInfo.avatarUrl
      commitAuthState()
    }
  } catch (error) {
    console.error('Failed to load user info:', error)
    showToast('加载用户信息失败', 'error')
  }
}

const commitAuthState = (avatarOverride?: string, nicknameOverride?: string) => {
  const finalAvatar = avatarOverride ?? userInfo.avatarUrl ?? ''
  if (avatarOverride !== undefined) {
    userInfo.avatarUrl = finalAvatar
  }
  auth.setUser({
    username: userInfo.username,
    nickname: nicknameOverride ?? (userInfo.nickname || userInfo.username),
    role: (userInfo.role as Role) || null,
    avatarUrl: finalAvatar
  })
}

const pendingAvatarFile = ref<File | null>(null)

const saveProfile = async () => {
  saving.value = true
  try {
    // if there's a pending avatar file, upload it first
    if (pendingAvatarFile.value) {
      const upRes = await uploadAvatar(pendingAvatarFile.value)
      if (upRes.status === 200 && upRes.data) {
        const payload = upRes.data as UploadAvatarResponse
        userInfo.avatarUrl = payload.url
        pendingAvatarFile.value = null
      } else {
        showToast('头像上传失败，已使用本地预览', 'error')
      }
    }

    const res = await updateUserInfo({
      nickname: userInfo.nickname,
      email: userInfo.email,
      phone: userInfo.phone,
      address: userInfo.address,
      avatarUrl: userInfo.avatarUrl || undefined
    })
    if (res.status === 200) {
      commitAuthState(undefined, userInfo.nickname)
      showToast('个人信息更新成功！')
    } else {
      showToast('更新失败，请稍后重试', 'error')
    }
  } catch (error) {
    console.error('Failed to update profile:', error)
    showToast('更新失败，请稍后重试', 'error')
  } finally {
    saving.value = false
  }
}

const changePasswordSubmit = async () => {
  if (!passwordForm.oldPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
    showToast('请填写所有密码字段', 'error')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    showToast('两次输入的新密码不一致', 'error')
    return
  }
  if (passwordForm.newPassword.length < 6) {
    showToast('新密码长度不能少于6位', 'error')
    return
  }

  changingPassword.value = true
  try {
    const res = await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    if (res.data === true || res.status === 200) {
      showToast('密码修改成功！')
      resetPasswordForm()
    } else {
      showToast(res.message || '密码修改失败', 'error')
    }
  } catch (error) {
    console.error('Failed to change password:', error)
    showToast('密码修改失败，请检查当前密码是否正确', 'error')
  } finally {
    changingPassword.value = false
  }
}

const handleAvatarChange = (e: Event) => {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  if (file.size > 5 * 1024 * 1024) { showToast('图片大小不能超过5MB', 'error'); return }

  // immediate preview
  const reader = new FileReader()
  reader.onload = (ev) => {
    const tempUrl = ev.target?.result as string
    commitAuthState(tempUrl)
  }
  reader.readAsDataURL(file)
  // store pending file to upload on save
  pendingAvatarFile.value = file
  if (target) target.value = ''
}

const resetForm = () => {
  loadUserInfo()
}

const resetPasswordForm = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}

onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped>
.profile-page {
  height: 100%;
  background: #f5f7fa;
  padding: 24px;
  overflow-y: auto;
  overflow-x: hidden;
}

.profile-container {
  max-width: 900px;
  margin: 0 auto;
}

.profile-header {
  margin-bottom: 32px;
}

.profile-header h1 {
  font-size: 32px;
  font-weight: 700;
  color: #111827;
  margin: 0 0 8px 0;
}

.subtitle {
  font-size: 16px;
  color: #6b7280;
  margin: 0;
}

.profile-card {
  background: white;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  margin-bottom: 24px;
}

.avatar-section {
  display: flex;
  align-items: center;
  gap: 24px;
  padding-bottom: 32px;
  border-bottom: 2px solid #f3f4f6;
  margin-bottom: 32px;
}

.avatar-wrapper {
  position: relative;
}

.avatar-img,
.avatar-placeholder {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-placeholder {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: grid;
  place-items: center;
  font-size: 36px;
  font-weight: 700;
}

.avatar-upload {
  position: absolute;
  bottom: 0;
  right: 0;
  background: #3b82f6;
  color: white;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.4);
  transition: transform 0.2s;
}

.avatar-upload:hover {
  transform: scale(1.1);
}

.avatar-info h2 {
  font-size: 24px;
  font-weight: 700;
  color: #111827;
  margin: 0 0 8px 0;
}

.role-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
}

.role-owner {
  background: #fef3c7;
  color: #92400e;
}

.role-rider {
  background: #dbeafe;
  color: #1e40af;
}

.role-user {
  background: #e0e7ff;
  color: #3730a3;
}

.card-title {
  font-size: 20px;
  font-weight: 700;
  color: #111827;
  margin: 0 0 24px 0;
}

.profile-form {
  width: 100%;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
  margin-bottom: 24px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group.full-width {
  grid-column: span 2;
}

.form-group label {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.form-input {
  padding: 12px 16px;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  font-size: 15px;
  transition: all 0.2s;
}

.form-input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.form-input.disabled {
  background: #f3f4f6;
  color: #9ca3af;
  cursor: not-allowed;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.btn {
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-secondary {
  background: #f3f4f6;
  color: #6b7280;
}

.btn-secondary:hover {
  background: #e5e7eb;
}

.toast {
  position: fixed;
  bottom: 24px;
  right: 24px;
  padding: 16px 24px;
  border-radius: 12px;
  font-weight: 600;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
  animation: slideIn 0.3s ease;
  z-index: 1000;
}

.toast.success {
  background: #10b981;
  color: white;
}

.toast.error {
  background: #ef4444;
  color: white;
}

@keyframes slideIn {
  from {
    transform: translateX(400px);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
  
  .form-group.full-width {
    grid-column: span 1;
  }

  .avatar-section {
    flex-direction: column;
    text-align: center;
  }
}
</style>
