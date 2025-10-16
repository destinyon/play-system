<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const tips = [
  {
    icon: '💡',
    title: '添加标记',
    description: '在地图上点击任意位置即可快速添加标记点'
  },
  {
    icon: '✏️',
    title: '编辑信息',
    description: '点击标记可编辑名称和地址，支持自动识别建筑物'
  },
  {
    icon: '🎯',
    title: '设置路线',
    description: '选择任意标记作为起点或终点，规划导航路线'
  },
  {
    icon: '📍',
    title: '定位查看',
    description: '点击列表中的标记可快速定位到地图上的位置'
  }
]

const features = [
  {
    title: '智能命名',
    description: '自动识别最近的建筑物和POI，智能填充标记名称',
    icon: '🏢'
  },
  {
    title: '地址识别',
    description: '反向地理编码，自动获取详细地址信息',
    icon: '📮'
  },
  {
    title: '批量管理',
    description: '支持批量删除、导出标记点数据',
    icon: '📊'
  },
  {
    title: '收藏保存',
    description: '常用地点可保存为收藏，快速访问',
    icon: '⭐'
  }
]

function goToMap() {
  router.push('/map')
}
</script>

<template>
  <div class="markers-page">
    <div class="markers-container">
      <!-- Hero Section -->
      <section class="hero-section">
        <div class="hero-icon">
          <svg width="72" height="72" viewBox="0 0 24 24" fill="none" stroke="url(#gradient-marker)" stroke-width="2">
            <defs>
              <linearGradient id="gradient-marker" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" style="stop-color:#667eea" />
                <stop offset="100%" style="stop-color:#764ba2" />
              </linearGradient>
            </defs>
            <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path>
            <circle cx="12" cy="10" r="3"></circle>
          </svg>
        </div>
        <h1 class="hero-title">标记管理</h1>
        <p class="hero-subtitle">轻松管理您的地图标记点，让导航更加便捷</p>
        
        <button class="cta-button" @click="goToMap">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polygon points="1 6 1 22 8 18 16 22 23 18 23 2 16 6 8 2 1 6"></polygon>
            <line x1="8" y1="2" x2="8" y2="18"></line>
            <line x1="16" y1="6" x2="16" y2="22"></line>
          </svg>
          前往地图添加标记
        </button>
      </section>

      <!-- Quick Tips -->
      <section class="tips-section">
        <h2 class="section-title">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"></circle>
            <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"></path>
            <line x1="12" y1="17" x2="12.01" y2="17"></line>
          </svg>
          使用技巧
        </h2>
        <div class="tips-grid">
          <div v-for="(tip, index) in tips" :key="index" class="tip-card">
            <div class="tip-icon">{{ tip.icon }}</div>
            <h3 class="tip-title">{{ tip.title }}</h3>
            <p class="tip-description">{{ tip.description }}</p>
          </div>
        </div>
      </section>

      <!-- Features -->
      <section class="features-section">
        <h2 class="section-title">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon>
          </svg>
          强大功能
        </h2>
        <div class="features-grid">
          <div v-for="(feature, index) in features" :key="index" class="feature-card">
            <div class="feature-icon">{{ feature.icon }}</div>
            <div class="feature-content">
              <h3 class="feature-title">{{ feature.title }}</h3>
              <p class="feature-description">{{ feature.description }}</p>
            </div>
          </div>
        </div>
      </section>

      <!-- How to Use -->
      <section class="howto-section">
        <h2 class="section-title">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="5" x2="12" y2="19"></line>
            <polyline points="19 12 12 19 5 12"></polyline>
          </svg>
          如何使用
        </h2>
        <div class="steps-container">
          <div class="step-card">
            <div class="step-number">1</div>
            <div class="step-content">
              <h3>进入地图</h3>
              <p>从左侧导航栏进入“首页”，即可访问地图与餐厅标记</p>
            </div>
          </div>
          <div class="step-arrow">↓</div>
          <div class="step-card">
            <div class="step-number">2</div>
            <div class="step-content">
              <h3>添加标记</h3>
              <p>在地图上点击任意位置，或点击"在中心添加"按钮</p>
            </div>
          </div>
          <div class="step-arrow">↓</div>
          <div class="step-card">
            <div class="step-number">3</div>
            <div class="step-content">
              <h3>编辑信息</h3>
              <p>系统会自动识别地址，您也可以自定义名称</p>
            </div>
          </div>
          <div class="step-arrow">↓</div>
          <div class="step-card">
            <div class="step-number">4</div>
            <div class="step-content">
              <h3>开始导航</h3>
              <p>设置起点终点，开始规划您的路线</p>
            </div>
          </div>
        </div>
      </section>

      <!-- Action Card -->
      <section class="action-section">
        <div class="action-card">
          <div class="action-content">
            <h2>准备好开始了吗？</h2>
            <p>立即前往地图页面，体验便捷的标记管理功能</p>
            <button class="action-button" @click="goToMap">
              立即开始
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="5" y1="12" x2="19" y2="12"></line>
                <polyline points="12 5 19 12 12 19"></polyline>
              </svg>
            </button>
          </div>
          <div class="action-illustration">
            <div class="illustration-circle pulse"></div>
            <div class="illustration-pin">📍</div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.markers-page {
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

.markers-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 24px;
}

/* Hero Section */
.hero-section {
  text-align: center;
  margin-bottom: 80px;
  animation: fadeInUp 0.6s ease;
}

.hero-icon {
  margin-bottom: 24px;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

.hero-title {
  font-size: 48px;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 16px;
}

.hero-subtitle {
  font-size: 20px;
  color: #6b7280;
  margin-bottom: 32px;
}

.cta-button {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 16px 32px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 18px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.cta-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.4);
}

/* Section Title */
.section-title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  font-size: 32px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 40px;
  text-align: center;
}

.section-title svg {
  color: #667eea;
}

/* Tips Section */
.tips-section {
  margin-bottom: 80px;
  animation: fadeInUp 0.6s ease 0.2s both;
}

.tips-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 24px;
}

.tip-card {
  background: white;
  border-radius: 16px;
  padding: 32px;
  text-align: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.tip-card:hover {
  transform: translateY(-8px);
  border-color: #667eea;
  box-shadow: 0 12px 28px rgba(102, 126, 234, 0.2);
}

.tip-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.tip-title {
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 12px;
}

.tip-description {
  color: #6b7280;
  line-height: 1.6;
  font-size: 15px;
}

/* Features Section */
.features-section {
  margin-bottom: 80px;
  animation: fadeInUp 0.6s ease 0.3s both;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
}

.feature-card {
  background: white;
  border-radius: 16px;
  padding: 28px;
  display: flex;
  align-items: flex-start;
  gap: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
}

.feature-card:hover {
  transform: translateX(8px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.12);
}

.feature-icon {
  font-size: 40px;
  flex-shrink: 0;
}

.feature-content {
  flex: 1;
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

/* How to Use Section */
.howto-section {
  margin-bottom: 80px;
  animation: fadeInUp 0.6s ease 0.4s both;
}

.steps-container {
  max-width: 600px;
  margin: 0 auto;
}

.step-card {
  background: white;
  border-radius: 16px;
  padding: 28px;
  display: flex;
  align-items: center;
  gap: 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  margin-bottom: 16px;
  transition: all 0.3s ease;
}

.step-card:hover {
  transform: translateX(8px);
  box-shadow: 0 8px 20px rgba(102, 126, 234, 0.2);
}

.step-number {
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 700;
  flex-shrink: 0;
}

.step-content h3 {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 8px;
}

.step-content p {
  color: #6b7280;
  line-height: 1.5;
  font-size: 14px;
  margin: 0;
}

.step-arrow {
  text-align: center;
  font-size: 32px;
  color: #d1d5db;
  margin: 8px 0;
  animation: bounce 2s ease-in-out infinite;
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(8px); }
}

/* Action Section */
.action-section {
  animation: fadeInUp 0.6s ease 0.5s both;
}

.action-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 24px;
  padding: 60px;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 40px;
  align-items: center;
  box-shadow: 0 12px 40px rgba(102, 126, 234, 0.3);
  position: relative;
  overflow: hidden;
}

.action-content {
  color: white;
}

.action-content h2 {
  font-size: 36px;
  font-weight: 700;
  margin-bottom: 16px;
}

.action-content p {
  font-size: 18px;
  opacity: 0.9;
  margin-bottom: 32px;
  line-height: 1.6;
}

.action-button {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 14px 28px;
  background: white;
  color: #667eea;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.action-button:hover {
  transform: translateX(4px);
  box-shadow: 0 4px 12px rgba(255, 255, 255, 0.3);
}

.action-illustration {
  position: relative;
  width: 120px;
  height: 120px;
}

.illustration-circle {
  position: absolute;
  width: 120px;
  height: 120px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  top: 0;
  left: 0;
}

.illustration-circle.pulse {
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.2);
    opacity: 0.6;
  }
}

.illustration-pin {
  position: absolute;
  font-size: 64px;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation: float 3s ease-in-out infinite;
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
  .markers-container {
    padding: 24px 16px;
  }

  .hero-title {
    font-size: 36px;
  }

  .section-title {
    font-size: 28px;
  }

  .tips-grid,
  .features-grid {
    grid-template-columns: 1fr;
  }

  .action-card {
    grid-template-columns: 1fr;
    padding: 40px 24px;
    text-align: center;
  }

  .action-content h2 {
    font-size: 28px;
  }

  .action-illustration {
    margin: 0 auto;
  }
}
</style>
