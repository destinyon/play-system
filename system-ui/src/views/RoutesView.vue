<script setup lang="ts">
import { ref } from 'vue'

const routeHistory = ref([
  {
    id: 1,
    from: '山东大学',
    to: '泉城广场',
    distance: '3.2 km',
    duration: '45 分钟',
    type: 'walking',
    date: '2025-10-13 14:30'
  },
  {
    id: 2,
    from: '趵突泉',
    to: '大明湖',
    distance: '2.8 km',
    duration: '38 分钟',
    type: 'walking',
    date: '2025-10-12 10:15'
  }
])

const routeTypes = [
  { value: 'walking', label: '步行', icon: '🚶', color: '#10b981' },
  { value: 'driving', label: '驾车', icon: '🚗', color: '#3b82f6' },
  { value: 'transit', label: '公交', icon: '🚌', color: '#f59e0b' },
  { value: 'riding', label: '骑行', icon: '🚴', color: '#8b5cf6' }
]

const features = [
  {
    icon: '🎯',
    title: '精准路线',
    description: '基于高德地图实时路况，提供最优出行方案'
  },
  {
    icon: '⏱️',
    title: '时间预估',
    description: '准确预估到达时间，合理规划行程'
  },
  {
    icon: '📍',
    title: '多点规划',
    description: '支持添加途经点，灵活定制路线'
  },
  {
    icon: '🔄',
    title: '历史记录',
    description: '保存常用路线，快速调用历史规划'
  }
]

function getTypeInfo(type: string) {
  return routeTypes.find(t => t.value === type) || routeTypes[0]
}

const getColor = (type: string) => getTypeInfo(type)!.color
const getIcon = (type: string) => getTypeInfo(type)!.icon
</script>

<template>
  <div class="routes-page">
    <div class="routes-container">
      <!-- Header Section -->
      <section class="header-section">
        <div class="header-content">
          <div class="header-icon">
            <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="url(#gradient-route)" stroke-width="2">
              <defs>
                <linearGradient id="gradient-route" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" style="stop-color:#667eea" />
                  <stop offset="100%" style="stop-color:#764ba2" />
                </linearGradient>
              </defs>
              <circle cx="6" cy="19" r="3"></circle>
              <path d="M9 19h8.5a3.5 3.5 0 0 0 0-7h-11a3.5 3.5 0 0 1 0-7H15"></path>
            </svg>
          </div>
          <h1 class="header-title">路线规划</h1>
          <p class="header-subtitle">智能规划您的出行路线，支持多种出行方式</p>
        </div>
      </section>

      <!-- Quick Start Section -->
      <section class="quick-start-section">
        <h2 class="section-title">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="9 11 12 14 22 4"></polyline>
            <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"></path>
          </svg>
          快速开始
        </h2>
        <div class="quick-start-card">
          <div class="quick-start-content">
            <p class="quick-start-text">前往 <strong>地图导航</strong> 页面，添加起点和终点标记，即可开始规划路线</p>
            <router-link to="/map" class="start-button">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polygon points="1 6 1 22 8 18 16 22 23 18 23 2 16 6 8 2 1 6"></polygon>
                <line x1="8" y1="2" x2="8" y2="18"></line>
                <line x1="16" y1="6" x2="16" y2="22"></line>
              </svg>
              开始规划路线
            </router-link>
          </div>
          <div class="quick-start-illustration">
            <div class="illustration-step">
              <div class="step-number">1</div>
              <div class="step-content">
                <div class="step-icon">📍</div>
                <p>添加标记点</p>
              </div>
            </div>
            <div class="illustration-arrow">→</div>
            <div class="illustration-step">
              <div class="step-number">2</div>
              <div class="step-content">
                <div class="step-icon">🎯</div>
                <p>设置起终点</p>
              </div>
            </div>
            <div class="illustration-arrow">→</div>
            <div class="illustration-step">
              <div class="step-number">3</div>
              <div class="step-content">
                <div class="step-icon">🚀</div>
                <p>开始导航</p>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Route Types Section -->
      <section class="route-types-section">
        <h2 class="section-title">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="3" width="7" height="7"></rect>
            <rect x="14" y="3" width="7" height="7"></rect>
            <rect x="14" y="14" width="7" height="7"></rect>
            <rect x="3" y="14" width="7" height="7"></rect>
          </svg>
          出行方式
        </h2>
        <div class="route-types-grid">
          <div v-for="type in routeTypes" :key="type.value" class="route-type-card" :style="{ borderColor: type.color }">
            <div class="route-type-icon" :style="{ background: `${type.color}15` }">
              {{ type.icon }}
            </div>
            <h3 class="route-type-name">{{ type.label }}</h3>
            <div class="route-type-badge" :style="{ background: type.color }">
              {{ type.value === 'walking' ? '当前支持' : '即将推出' }}
            </div>
          </div>
        </div>
      </section>

      <!-- Features Section -->
      <section class="features-section">
        <h2 class="section-title">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon>
          </svg>
          功能特性
        </h2>
        <div class="features-grid">
          <div v-for="(feature, index) in features" :key="index" class="feature-card">
            <div class="feature-icon">{{ feature.icon }}</div>
            <h3 class="feature-title">{{ feature.title }}</h3>
            <p class="feature-description">{{ feature.description }}</p>
          </div>
        </div>
      </section>

      <!-- History Section -->
      <section class="history-section">
        <h2 class="section-title">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"></circle>
            <polyline points="12 6 12 12 16 14"></polyline>
          </svg>
          历史记录
        </h2>
        <div class="history-list">
          <div v-for="route in routeHistory" :key="route.id" class="history-card">
            <div class="history-header">
              <div class="history-type" :style="{ background: getColor(route.type) }">
                {{ getIcon(route.type) }}
              </div>
              <span class="history-date">{{ route.date }}</span>
            </div>
            <div class="history-route">
              <div class="route-point start">
                <div class="point-icon">🟢</div>
                <span class="point-name">{{ route.from }}</span>
              </div>
              <div class="route-line">
                <div class="line-dots"></div>
              </div>
              <div class="route-point end">
                <div class="point-icon">🔴</div>
                <span class="point-name">{{ route.to }}</span>
              </div>
            </div>
            <div class="history-info">
              <div class="info-item">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
                </svg>
                {{ route.distance }}
              </div>
              <div class="info-item">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"></circle>
                  <polyline points="12 6 12 12 16 14"></polyline>
                </svg>
                {{ route.duration }}
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.routes-page {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  width: 100%;
  height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8eef5 100%);
}

.routes-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 24px;
}

/* Header Section */
.header-section {
  text-align: center;
  margin-bottom: 60px;
  animation: fadeInUp 0.6s ease;
}

.header-content {
  position: relative;
}

.header-icon {
  margin-bottom: 20px;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

.header-title {
  font-size: 42px;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 12px;
}

.header-subtitle {
  font-size: 18px;
  color: #6b7280;
  font-weight: 500;
}

/* Section Title */
.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 28px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 32px;
}

.section-title svg {
  color: #667eea;
}

/* Quick Start Section */
.quick-start-section {
  margin-bottom: 60px;
  animation: fadeInUp 0.6s ease 0.1s both;
}

.quick-start-card {
  background: white;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 40px;
  align-items: center;
}

.quick-start-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.quick-start-text {
  font-size: 16px;
  color: #4b5563;
  line-height: 1.8;
}

.quick-start-text strong {
  color: #667eea;
  font-weight: 600;
}

.start-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 14px 28px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  text-decoration: none;
  width: fit-content;
}

.start-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(102, 126, 234, 0.4);
}

.quick-start-illustration {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.illustration-step {
  flex: 1;
  text-align: center;
}

.step-number {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  margin: 0 auto 12px;
}

.step-content {
  background: #f9fafb;
  padding: 20px;
  border-radius: 12px;
  border: 2px solid #e5e7eb;
}

.step-icon {
  font-size: 32px;
  margin-bottom: 8px;
}

.step-content p {
  font-size: 14px;
  color: #6b7280;
  font-weight: 500;
  margin: 0;
}

.illustration-arrow {
  font-size: 24px;
  color: #d1d5db;
  font-weight: 600;
}

/* Route Types Section */
.route-types-section {
  margin-bottom: 60px;
  animation: fadeInUp 0.6s ease 0.2s both;
}

.route-types-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 20px;
}

.route-type-card {
  background: white;
  border: 3px solid;
  border-radius: 16px;
  padding: 32px;
  text-align: center;
  transition: all 0.3s ease;
  position: relative;
}

.route-type-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.12);
}

.route-type-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  margin: 0 auto 20px;
}

.route-type-name {
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 12px;
}

.route-type-badge {
  display: inline-block;
  padding: 6px 16px;
  border-radius: 20px;
  color: white;
  font-size: 12px;
  font-weight: 600;
}

/* Features Section */
.features-section {
  margin-bottom: 60px;
  animation: fadeInUp 0.6s ease 0.3s both;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 24px;
}

.feature-card {
  background: white;
  border-radius: 16px;
  padding: 28px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.feature-card:hover {
  border-color: #667eea;
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(102, 126, 234, 0.15);
}

.feature-icon {
  font-size: 40px;
  margin-bottom: 16px;
}

.feature-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 8px;
}

.feature-description {
  color: #6b7280;
  line-height: 1.6;
  font-size: 14px;
}

/* History Section */
.history-section {
  animation: fadeInUp 0.6s ease 0.4s both;
}

.history-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 20px;
}

.history-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
}

.history-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.12);
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.history-type {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.history-date {
  font-size: 13px;
  color: #9ca3af;
}

.history-route {
  margin-bottom: 20px;
}

.route-point {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.point-icon {
  font-size: 16px;
}

.point-name {
  font-size: 15px;
  font-weight: 600;
  color: #374151;
}

.route-line {
  margin-left: 8px;
  padding-left: 20px;
  border-left: 2px dashed #d1d5db;
  height: 24px;
}

.history-info {
  display: flex;
  gap: 20px;
  padding-top: 16px;
  border-top: 1px solid #e5e7eb;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #6b7280;
}

.info-item svg {
  color: #9ca3af;
}

/* Animations */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Responsive */
@media (max-width: 768px) {
  .routes-container {
    padding: 24px 16px;
  }

  .header-title {
    font-size: 32px;
  }

  .section-title {
    font-size: 24px;
  }

  .quick-start-card {
    grid-template-columns: 1fr;
    gap: 32px;
  }

  .quick-start-illustration {
    flex-direction: column;
  }

  .illustration-arrow {
    transform: rotate(90deg);
  }

  .route-types-grid,
  .features-grid,
  .history-list {
    grid-template-columns: 1fr;
  }
}
</style>
