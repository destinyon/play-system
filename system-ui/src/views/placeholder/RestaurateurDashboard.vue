<template>
  <div class="dashboard-page">
    <div class="dashboard-header">
      <h1>商家运营概览 (Restaurateur)</h1>
      <p class="subtitle">查看您的餐厅经营数据和订单统计</p>
    </div>

    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2">
            <line x1="12" y1="1" x2="12" y2="23"></line>
            <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"></path>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">总收入</div>
          <div class="stat-value">¥{{ stats.totalIncome.toLocaleString() }}</div>
          <div class="stat-change positive">+12.5%</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2">
            <path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"></path>
            <rect x="8" y="2" width="8" height="4" rx="1" ry="1"></rect>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">待处理订单</div>
          <div class="stat-value">{{ stats.pendingOrders }}</div>
          <div class="stat-change">实时更新</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2">
            <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"></polyline>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">今日订单</div>
          <div class="stat-value">{{ stats.todayOrders }}</div>
          <div class="stat-change positive">+{{ stats.todayOrdersGrowth }}%</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
            <circle cx="9" cy="7" r="4"></circle>
            <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
            <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">菜品数量</div>
          <div class="stat-value">{{ stats.dishCount }}</div>
          <div class="stat-change">库存管理</div>
        </div>
      </div>
    </div>

    <div class="charts-row">
      <div class="chart-card">
        <h2 class="chart-title">收入趋势</h2>
        <div class="chart-container">
          <canvas ref="incomeChart"></canvas>
        </div>
      </div>

      <div class="chart-card">
        <h2 class="chart-title">订单分布</h2>
        <div class="chart-container">
          <canvas ref="ordersChart"></canvas>
        </div>
      </div>
    </div>

    <div class="info-card">
      <h2 class="card-title">我的餐厅</h2>
      <div class="restaurant-info" v-if="restaurant">
        <img v-if="restaurant.photoUrl" :src="restaurant.photoUrl" alt="restaurant" class="restaurant-img" />
        <div class="restaurant-details">
          <h3>{{ restaurant.name }}</h3>
          <p><strong>地址：</strong>{{ restaurant.address }}</p>
          <p><strong>坐标：</strong>经度 {{ restaurant.lng?.toFixed(6) }}，纬度 {{ restaurant.lat?.toFixed(6) }}</p>
        </div>
      </div>
      <div v-else class="empty-state">
        <p>暂无餐厅信息</p>
        <router-link to="/app/restaurateur/restaurant" class="btn-link">前往设置</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { listRestaurants } from '@/api/restaurant'
import { getRestaurateurMetrics, getRestaurateurStats } from '@/api/order'
import { getPendingOrders } from '@/api/order'
import Chart from 'chart.js/auto'

const auth = useAuthStore()
// 从登录信息提取 restaurateurId，缺失时回退为 1（开发期）
const restaurateurId = computed(() => {
  const id = (auth.restaurant as any)?.id
  const n = typeof id === 'string' ? parseInt(id, 10) : id
  const num = typeof n === 'number' ? n : Number(n)
  return Number.isFinite(num) ? num : 1
})

const incomeChart = ref<HTMLCanvasElement | null>(null)
const ordersChart = ref<HTMLCanvasElement | null>(null)

const stats = reactive({
  totalIncome: 0,
  pendingOrders: 0,
  todayOrders: 0,
  todayOrdersGrowth: 0,
  dishCount: 28
})

const restaurant = ref<any>(null)
const metricsData = ref<any>(null)

const loadStats = async () => {
  try {
    // 获取基础统计数据
    const statsRes = await getRestaurateurStats(restaurateurId.value)
    if (statsRes.status === 200 && statsRes.data) {
      stats.totalIncome = statsRes.data.totalIncome || 0
      stats.pendingOrders = statsRes.data.pendingOrders || 0
      stats.todayOrders = statsRes.data.todayOrders || 0
      stats.todayOrdersGrowth = statsRes.data.todayOrdersGrowth || 0
      stats.dishCount = statsRes.data.dishCount || 0
    }

    // 获取过去7天的数据用于图表
    const to = new Date().toISOString().split('T')[0]
    const from = new Date(Date.now() - 6 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
    const metricsRes = await getRestaurateurMetrics(from, to, 'day')
    
    if (metricsRes.status === 200 && metricsRes.data) {
      metricsData.value = metricsRes.data
    }
  } catch (error) {
    console.error('Failed to load stats:', error)
  }
}

const loadRestaurant = async () => {
  try {
    const res = await listRestaurants()
    if (res.status === 200 && res.data && res.data.length > 0) {
      restaurant.value = res.data[0]
    }
  } catch (error) {
    console.error('Failed to load restaurant:', error)
  }
}

const initCharts = () => {
  if (incomeChart.value && metricsData.value) {
    const timeSeries = metricsData.value.timeSeries || []
    const labels = timeSeries.map((item: any) => {
      const date = new Date(item.date)
      return `${date.getMonth() + 1}/${date.getDate()}`
    })
    const incomeData = timeSeries.map((item: any) => item.income || 0)

    new Chart(incomeChart.value, {
      type: 'line',
      data: {
        labels,
        datasets: [{
          label: '每日收入 (¥)',
          data: incomeData,
          borderColor: '#fbbf24',
          backgroundColor: 'rgba(251, 191, 36, 0.15)',
          tension: 0.4,
          fill: true,
          pointBackgroundColor: '#f59e0b',
          pointBorderColor: '#fff',
          pointBorderWidth: 2
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false }
        },
        scales: {
          y: { beginAtZero: true }
        }
      }
    })
  }

  if (ordersChart.value) {
    new Chart(ordersChart.value, {
      type: 'doughnut',
      data: {
        labels: ['已完成', '配送中', '待处理', '已取消'],
        datasets: [{
          data: [145, 28, stats.pendingOrders || 5, 12],
          backgroundColor: ['#fbbf24', '#fb923c', '#fde047', '#fca5a5'],
          borderWidth: 2,
          borderColor: '#fff'
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
  loadStats().then(() => {
    setTimeout(initCharts, 100)
  })
  loadRestaurant()
})
</script>

<style scoped>
.dashboard-page {
  padding: 24px;
  height: 100%;
  background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 50%, #fde68a 100%);
  overflow-y: auto;
  overflow-x: hidden;
}

.dashboard-header {
  margin-bottom: 32px;
}

.dashboard-header h1 {
  font-size: 32px;
  font-weight: 700;
  color: #78350f;
  margin: 0 0 8px 0;
}

.subtitle {
  font-size: 16px;
  color: #92400e;
  margin: 0;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
}

.stat-card {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 20px;
  box-shadow: 0 4px 6px rgba(251, 191, 36, 0.15);
  border: 1px solid rgba(251, 191, 36, 0.2);
  transition: transform 0.2s, box-shadow 0.2s;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 16px rgba(251, 191, 36, 0.25);
  border-color: rgba(251, 191, 36, 0.4);
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
  color: #92400e;
  font-weight: 500;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #78350f;
  line-height: 1;
  margin-bottom: 8px;
}

.stat-change {
  font-size: 13px;
  color: #92400e;
}

.stat-change.positive {
  color: #f59e0b;
  font-weight: 600;
}

.charts-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
}

.chart-card {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 6px rgba(251, 191, 36, 0.15);
  border: 1px solid rgba(251, 191, 36, 0.2);
}

.chart-title {
  font-size: 18px;
  font-weight: 700;
  color: #78350f;
  margin: 0 0 20px 0;
}

.chart-container {
  height: 300px;
  position: relative;
}

.info-card {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 6px rgba(251, 191, 36, 0.15);
  border: 1px solid rgba(251, 191, 36, 0.2);
}

.card-title {
  font-size: 20px;
  font-weight: 700;
  color: #78350f;
  margin: 0 0 20px 0;
}

.restaurant-info {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.restaurant-img {
  width: 160px;
  height: 160px;
  border-radius: 12px;
  object-fit: cover;
  flex-shrink: 0;
}

.restaurant-details h3 {
  font-size: 24px;
  font-weight: 700;
  color: #78350f;
  margin: 0 0 16px 0;
}

.restaurant-details p {
  font-size: 15px;
  color: #92400e;
  margin: 8px 0;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #9ca3af;
}

.btn-link {
  display: inline-block;
  margin-top: 16px;
  padding: 10px 20px;
  background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%);
  color: white;
  text-decoration: none;
  border-radius: 8px;
  font-weight: 600;
  transition: transform 0.2s, box-shadow 0.2s;
  box-shadow: 0 2px 4px rgba(251, 191, 36, 0.3);
}

.btn-link:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(251, 191, 36, 0.4);
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .charts-row {
    grid-template-columns: 1fr;
  }
  
  .restaurant-info {
    flex-direction: column;
  }
}
</style>
