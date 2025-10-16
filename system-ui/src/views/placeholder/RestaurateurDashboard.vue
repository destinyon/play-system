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
import { ref, reactive, onMounted } from 'vue'
import { getRestaurateurStats } from '@/api/user'
import { listRestaurants } from '@/api/restaurant'
import Chart from 'chart.js/auto'

const incomeChart = ref<HTMLCanvasElement | null>(null)
const ordersChart = ref<HTMLCanvasElement | null>(null)

const stats = reactive({
  totalIncome: 0,
  pendingOrders: 0,
  todayOrders: 0,
  todayOrdersGrowth: 0,
  dishCount: 0
})

const restaurant = ref<any>(null)

const loadStats = async () => {
  try {
    const res = await getRestaurateurStats()
    if (res.data) {
      Object.assign(stats, res.data)
    } else {
      stats.totalIncome = 18500
      stats.pendingOrders = 5
      stats.todayOrders = 23
      stats.todayOrdersGrowth = 15
      stats.dishCount = 28
    }
  } catch (error) {
    console.error('Failed to load stats:', error)
    stats.totalIncome = 18500
    stats.pendingOrders = 5
    stats.todayOrders = 23
    stats.todayOrdersGrowth = 15
    stats.dishCount = 28
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
  if (incomeChart.value) {
    new Chart(incomeChart.value, {
      type: 'line',
      data: {
        labels: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
        datasets: [{
          label: '每日收入 (¥)',
          data: [2100, 2400, 2800, 2200, 3100, 3500, 2900],
          borderColor: '#667eea',
          backgroundColor: 'rgba(102, 126, 234, 0.1)',
          tension: 0.4,
          fill: true
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
          data: [145, 28, 5, 12],
          backgroundColor: ['#10b981', '#3b82f6', '#f59e0b', '#ef4444']
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
  loadStats()
  loadRestaurant()
  setTimeout(initCharts, 100)
})
</script>

<style scoped>
.dashboard-page {
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

.info-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.card-title {
  font-size: 20px;
  font-weight: 700;
  color: #111827;
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
  color: #111827;
  margin: 0 0 16px 0;
}

.restaurant-details p {
  font-size: 15px;
  color: #6b7280;
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  text-decoration: none;
  border-radius: 8px;
  font-weight: 600;
  transition: transform 0.2s;
}

.btn-link:hover {
  transform: translateY(-2px);
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
