<template>
  <div class="dashboard-page">
    <div class="dashboard-header">
      <h1>骑手工作台 (Deliveryman)</h1>
      <p class="subtitle">查看您的配送数据和收入统计</p>
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
          <div class="stat-change positive">+18.2%</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2">
            <circle cx="12" cy="12" r="10"></circle>
            <polyline points="12 6 12 12 16 14"></polyline>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">待配送订单</div>
          <div class="stat-value">{{ stats.pendingDeliveries }}</div>
          <div class="stat-change">实时更新</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2">
            <path d="M12 2L2 7l10 5 10-5-10-5z"></path>
            <path d="M2 17l10 5 10-5"></path>
            <path d="M2 12l10 5 10-5"></path>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">今日配送</div>
          <div class="stat-value">{{ stats.todayDeliveries }}</div>
          <div class="stat-change positive">+{{ stats.todayGrowth }}%</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2">
            <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"></polyline>
            <polyline points="17 6 23 6 23 12"></polyline>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">完成率</div>
          <div class="stat-value">{{ stats.completionRate }}%</div>
          <div class="stat-change positive">优秀</div>
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
        <h2 class="chart-title">配送统计</h2>
        <div class="chart-container">
          <canvas ref="deliveryChart"></canvas>
        </div>
      </div>
    </div>

    <div class="info-card">
      <h2 class="card-title">最近配送</h2>
      <div class="delivery-list">
        <div class="delivery-item" v-for="item in recentDeliveries" :key="item.id">
          <div class="delivery-status" :class="item.status">
            <svg v-if="item.status === 'completed'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="20 6 9 17 4 12"></polyline>
            </svg>
            <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"></circle>
              <polyline points="12 6 12 12 16 14"></polyline>
            </svg>
          </div>
          <div class="delivery-info">
            <h4>{{ item.restaurant }}</h4>
            <p>{{ item.address }}</p>
          </div>
          <div class="delivery-fee">¥{{ item.fee }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getDeliverymanStats } from '@/api/user'
let Chart: any = null

const incomeChart = ref<HTMLCanvasElement | null>(null)
const deliveryChart = ref<HTMLCanvasElement | null>(null)

const stats = reactive({
  totalIncome: 0,
  pendingDeliveries: 0,
  todayDeliveries: 0,
  todayGrowth: 0,
  completionRate: 0
})

const recentDeliveries = ref([
  { id: 1, restaurant: '张三餐厅', address: '北京市朝阳区xxx路123号', fee: 12, status: 'completed' },
  { id: 2, restaurant: '李四小吃', address: '北京市海淀区xxx街456号', fee: 15, status: 'completed' },
  { id: 3, restaurant: '王五烧烤', address: '北京市丰台区xxx巷789号', fee: 18, status: 'delivering' },
  { id: 4, restaurant: '赵六火锅', address: '北京市西城区xxx道321号', fee: 20, status: 'completed' }
])

const loadStats = async () => {
  try {
    const res = await getDeliverymanStats()
    if (res.data) {
      Object.assign(stats, res.data)
    } else {
      stats.totalIncome = 6500
      stats.pendingDeliveries = 2
      stats.todayDeliveries = 15
      stats.todayGrowth = 20
      stats.completionRate = 98
    }
  } catch (error) {
    console.error('Failed to load stats:', error)
    stats.totalIncome = 6500
    stats.pendingDeliveries = 2
    stats.todayDeliveries = 15
    stats.todayGrowth = 20
    stats.completionRate = 98
  }
}

const initCharts = async () => {
  try {
    Chart = (await import('chart.js/auto')).default
  } catch (e) {
    console.warn('Chart.js not available')
    return
  }

  if (incomeChart.value && Chart) {
    new Chart(incomeChart.value, {
      type: 'bar',
      data: {
        labels: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
        datasets: [{
          label: '每日收入 (¥)',
          data: [850, 920, 1100, 880, 1150, 1280, 1320],
          backgroundColor: 'rgba(102, 126, 234, 0.8)',
          borderRadius: 8
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

  if (deliveryChart.value && Chart) {
    new Chart(deliveryChart.value, {
      type: 'line',
      data: {
        labels: ['8:00', '10:00', '12:00', '14:00', '16:00', '18:00', '20:00', '22:00'],
        datasets: [{
          label: '配送单量',
          data: [3, 5, 12, 8, 6, 15, 18, 10],
          borderColor: '#f093fb',
          backgroundColor: 'rgba(240, 147, 251, 0.1)',
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
}

onMounted(() => {
  loadStats()
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

.delivery-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.delivery-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #f9fafb;
  border-radius: 12px;
  transition: background 0.2s;
}

.delivery-item:hover {
  background: #f3f4f6;
}

.delivery-status {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.delivery-status.completed {
  background: #d1fae5;
  color: #10b981;
}

.delivery-status.delivering {
  background: #dbeafe;
  color: #3b82f6;
}

.delivery-info {
  flex: 1;
}

.delivery-info h4 {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 4px 0;
}

.delivery-info p {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.delivery-fee {
  font-size: 16px;
  font-weight: 700;
  color: #111827;
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .charts-row {
    grid-template-columns: 1fr;
  }
}
</style>
