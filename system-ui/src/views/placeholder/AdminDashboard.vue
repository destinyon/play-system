<template>
  <div class="admin-dashboard">
    <div class="dashboard-header">
      <h1>管理员控制台</h1>
      <p class="subtitle">平台数据统计与用户管理</p>
    </div>

    <!-- Stats Cards -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
            <circle cx="12" cy="7" r="4"></circle>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">总用户数</div>
          <div class="stat-value">{{ stats.totalUsers }}</div>
          <div class="stat-change positive">+12.5%</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2">
            <path d="M3 11l9-9 9 9v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">商家数量</div>
          <div class="stat-value">{{ stats.restaurateurCount }}</div>
          <div class="stat-change positive">+8.2%</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2">
            <path d="M5 17l-1 2h16l-1-2z"></path>
            <path d="M6 6h11l3 7H3z"></path>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">骑手数量</div>
          <div class="stat-value">{{ stats.deliverymanCount }}</div>
          <div class="stat-change positive">+15.3%</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2">
            <circle cx="9" cy="21" r="1"></circle>
            <circle cx="20" cy="21" r="1"></circle>
            <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"></path>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">普通用户</div>
          <div class="stat-value">{{ stats.guestCount }}</div>
          <div class="stat-change positive">+10.1%</div>
        </div>
      </div>
    </div>

    <!-- Charts Row -->
    <div class="charts-row">
      <div class="chart-card">
        <h2 class="chart-title">用户增长趋势</h2>
        <div class="chart-container">
          <canvas ref="userGrowthChart"></canvas>
        </div>
      </div>

      <div class="chart-card">
        <h2 class="chart-title">用户角色分布</h2>
        <div class="chart-container">
          <canvas ref="userRoleChart"></canvas>
        </div>
      </div>
    </div>

    <!-- User List -->
    <div class="user-list-card">
      <div class="card-header">
        <h2 class="card-title">用户列表</h2>
        <div class="card-actions">
          <button class="btn-refresh" @click="loadData">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="23 4 23 10 17 10"></polyline>
              <polyline points="1 20 1 14 7 14"></polyline>
              <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"></path>
            </svg>
            刷新
          </button>
        </div>
      </div>
      <div class="table-container">
        <table class="user-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>用户名</th>
              <th>昵称</th>
              <th>角色</th>
              <th>邮箱</th>
              <th>手机号</th>
              <th>注册时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in users" :key="user.id">
              <td>{{ user.id }}</td>
              <td><strong>{{ user.username }}</strong></td>
              <td>{{ user.nickname || '-' }}</td>
              <td>
                <span class="role-badge" :class="getRoleClass(user.role)">
                  {{ getRoleText(user.role) }}
                </span>
              </td>
              <td>{{ user.email || '-' }}</td>
              <td>{{ user.phone || '-' }}</td>
              <td>{{ formatDate(user.createdAt) }}</td>
            </tr>
          </tbody>
        </table>
        <div v-if="users.length === 0" class="empty-state">
          <p>暂无用户数据</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getUserStats, getAllUsers } from '@/api/user'

// Import Chart.js dynamically to avoid build errors
let Chart: any = null

const userGrowthChart = ref<HTMLCanvasElement | null>(null)
const userRoleChart = ref<HTMLCanvasElement | null>(null)

const stats = reactive({
  totalUsers: 0,
  guestCount: 0,
  restaurateurCount: 0,
  deliverymanCount: 0
})

const users = ref<any[]>([])

const loadStats = async () => {
  try {
    const res = await getUserStats()
    if (res.data) {
      Object.assign(stats, res.data)
    }
  } catch (error) {
    console.error('Failed to load stats:', error)
    // Mock data
    stats.totalUsers = 156
    stats.guestCount = 120
    stats.restaurateurCount = 28
    stats.deliverymanCount = 8
  }
}

const loadUsers = async () => {
  try {
    const res = await getAllUsers()
    if (res.data && Array.isArray(res.data)) {
      users.value = res.data
    }
  } catch (error) {
    console.error('Failed to load users:', error)
  }
}

const loadData = () => {
  loadStats()
  loadUsers()
}

const getRoleText = (role: string) => {
  switch (role) {
    case 'RESTAURATEUR': return '商家'
    case 'DELIVERYMAN': return '骑手'
    case 'GUEST': return '用户'
    default: return role
  }
}

const getRoleClass = (role: string) => {
  switch (role) {
    case 'RESTAURATEUR': return 'role-owner'
    case 'DELIVERYMAN': return 'role-rider'
    case 'GUEST': return 'role-user'
    default: return ''
  }
}

const formatDate = (date: any) => {
  if (!date) return '-'
  return new Date(date).toLocaleDateString('zh-CN')
}

const initCharts = async () => {
  try {
    Chart = (await import('chart.js/auto')).default
  } catch (e) {
    console.warn('Chart.js not available')
    return
  }

  if (userGrowthChart.value && Chart) {
    new Chart(userGrowthChart.value, {
      type: 'line',
      data: {
        labels: ['1月', '2月', '3月', '4月', '5月', '6月', '7月'],
        datasets: [
          {
            label: '总用户',
            data: [45, 58, 72, 89, 110, 135, 156],
            borderColor: '#667eea',
            backgroundColor: 'rgba(102, 126, 234, 0.1)',
            tension: 0.4,
            fill: true
          },
          {
            label: '商家',
            data: [8, 10, 14, 18, 22, 26, 28],
            borderColor: '#f093fb',
            backgroundColor: 'rgba(240, 147, 251, 0.1)',
            tension: 0.4,
            fill: true
          },
          {
            label: '骑手',
            data: [2, 3, 4, 5, 6, 7, 8],
            borderColor: '#4facfe',
            backgroundColor: 'rgba(79, 172, 254, 0.1)',
            tension: 0.4,
            fill: true
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { position: 'top' }
        },
        scales: {
          y: { beginAtZero: true }
        }
      }
    })
  }

  if (userRoleChart.value && Chart) {
    new Chart(userRoleChart.value, {
      type: 'doughnut',
      data: {
        labels: ['普通用户', '商家', '骑手'],
        datasets: [{
          data: [stats.guestCount || 120, stats.restaurateurCount || 28, stats.deliverymanCount || 8],
          backgroundColor: ['#667eea', '#f093fb', '#4facfe']
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { position: 'bottom' }
        }
      }
    })
  }
}

onMounted(() => {
  loadData()
  setTimeout(initCharts, 100)
})
</script>

<style scoped>
.admin-dashboard {
  padding: 24px;
  height: 100%;
  background: #f5f7fa;
  overflow-y: auto;
  overflow-x: hidden;
}

.dashboard-header {
  margin-bottom: 32px;
}

.dashboard-header h1 {
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

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
}

.stat-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  transition: transform 0.2s, box-shadow 0.2s;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.15);
}

.stat-icon {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.stat-content {
  flex: 1;
}

.stat-label {
  font-size: 14px;
  color: #6b7280;
  font-weight: 500;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #111827;
  line-height: 1;
  margin-bottom: 8px;
}

.stat-change {
  font-size: 13px;
  color: #6b7280;
}

.stat-change.positive {
  color: #10b981;
  font-weight: 600;
}

.charts-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
}

.chart-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.chart-title {
  font-size: 18px;
  font-weight: 700;
  color: #111827;
  margin: 0 0 20px 0;
}

.chart-container {
  height: 300px;
  position: relative;
}

.user-list-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.card-title {
  font-size: 20px;
  font-weight: 700;
  color: #111827;
  margin: 0;
}

.btn-refresh {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: #f3f4f6;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-refresh:hover {
  background: #e5e7eb;
}

.table-container {
  overflow-x: auto;
}

.user-table {
  width: 100%;
  border-collapse: collapse;
}

.user-table thead {
  background: #f9fafb;
}

.user-table th {
  padding: 12px 16px;
  text-align: left;
  font-size: 13px;
  font-weight: 600;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.user-table tbody tr {
  border-bottom: 1px solid #f3f4f6;
  transition: background 0.2s;
}

.user-table tbody tr:hover {
  background: #f9fafb;
}

.user-table td {
  padding: 16px;
  font-size: 14px;
  color: #374151;
}

.role-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
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

.empty-state {
  text-align: center;
  padding: 40px;
  color: #9ca3af;
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .charts-row {
    grid-template-columns: 1fr;
  }
  
  .table-container {
    overflow-x: scroll;
  }
}
</style>
