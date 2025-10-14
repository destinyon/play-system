<script setup lang="ts">
import { ref } from 'vue'

const features = [
  {
    icon: '🗺️',
    title: '智能地图导航',
    description: '基于高德地图API，提供精准的地理位置服务和实时路况信息'
  },
  {
    icon: '📍',
    title: '标记点管理',
    description: '轻松添加、编辑和管理地图标记点，支持自定义名称和地址信息'
  },
  {
    icon: '🚶',
    title: '步行路线规划',
    description: '智能规划最优步行路线，提供详细的导航步骤和距离时间预估'
  },
  {
    icon: '⚡',
    title: '位置缓存',
    description: '首次定位后智能缓存位置信息，提升加载速度和用户体验'
  },
  {
    icon: '🎨',
    title: '现代化界面',
    description: '采用Vue 3 + TypeScript构建，界面简洁美观，交互流畅自然'
  },
  {
    icon: '🔒',
    title: '安全可靠',
    description: '通过Nginx代理保护API密钥，采用CORS安全策略保障数据安全'
  }
]

const techStack = [
  { name: 'Vue 3', color: '#42b883' },
  { name: 'TypeScript', color: '#3178c6' },
  { name: 'Vite', color: '#646cff' },
  { name: '高德地图 API', color: '#00cafc' },
  { name: 'Nginx', color: '#009639' }
]
</script>

<template>
  <div class="about-page">
    <div class="about-container">
      <!-- Hero Section -->
      <section class="hero-section">
        <div class="hero-icon">
          <svg width="80" height="80" viewBox="0 0 24 24" fill="none" stroke="url(#gradient)" stroke-width="1.5">
            <defs>
              <linearGradient id="gradient" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" style="stop-color:#667eea" />
                <stop offset="100%" style="stop-color:#764ba2" />
              </linearGradient>
            </defs>
            <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path>
            <circle cx="12" cy="10" r="3"></circle>
          </svg>
        </div>
        <h1 class="hero-title">高德导航系统</h1>
        <p class="hero-subtitle">智能地图导航解决方案</p>
        <div class="hero-description">
          <p>这是一个基于高德地图API开发的现代化Web导航应用，提供地图浏览、标记点管理、路线规划等核心功能。</p>
          <p>采用最新的前端技术栈构建，注重用户体验和界面美观性，致力于为用户提供流畅便捷的导航服务。</p>
        </div>
      </section>

      <!-- Features Section -->
      <section class="features-section">
        <h2 class="section-title">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon>
          </svg>
          核心功能
        </h2>
        <div class="features-grid">
          <div v-for="(feature, index) in features" :key="index" class="feature-card">
            <div class="feature-icon">{{ feature.icon }}</div>
            <h3 class="feature-title">{{ feature.title }}</h3>
            <p class="feature-description">{{ feature.description }}</p>
          </div>
        </div>
      </section>

      <!-- Tech Stack Section -->
      <section class="tech-section">
        <h2 class="section-title">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="16 18 22 12 16 6"></polyline>
            <polyline points="8 6 2 12 8 18"></polyline>
          </svg>
          技术栈
        </h2>
        <div class="tech-stack">
          <div v-for="tech in techStack" :key="tech.name" class="tech-badge" :style="{ borderColor: tech.color }">
            <span class="tech-dot" :style="{ background: tech.color }"></span>
            {{ tech.name }}
          </div>
        </div>
      </section>

      <!-- Footer -->
      <footer class="about-footer">
        <p>© 2025 高德导航系统 · 版本 1.0.0</p>
        <p class="footer-hint">Built with ❤️ using Vue 3 & AMap API</p>
      </footer>
    </div>
  </div>
</template>

<style scoped>
.about-page {
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

.about-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 60px 24px;
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
  font-weight: 500;
}

.hero-description {
  max-width: 700px;
  margin: 0 auto;
  color: #4b5563;
  line-height: 1.8;
  font-size: 16px;
}

.hero-description p {
  margin-bottom: 12px;
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
  margin-bottom: 48px;
  text-align: center;
}

.section-title svg {
  color: #667eea;
}

/* Features Section */
.features-section {
  margin-bottom: 80px;
  animation: fadeInUp 0.6s ease 0.2s both;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 24px;
}

.feature-card {
  background: white;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.feature-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 28px rgba(102, 126, 234, 0.2);
  border-color: #667eea;
}

.feature-icon {
  font-size: 48px;
  margin-bottom: 20px;
  display: inline-block;
  animation: bounce 2s ease-in-out infinite;
}

@keyframes bounce {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

.feature-title {
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 12px;
}

.feature-description {
  color: #6b7280;
  line-height: 1.6;
  font-size: 15px;
}

/* Tech Stack Section */
.tech-section {
  margin-bottom: 60px;
  animation: fadeInUp 0.6s ease 0.4s both;
}

.tech-stack {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 16px;
}

.tech-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: white;
  border: 2px solid;
  border-radius: 50px;
  font-weight: 600;
  font-size: 15px;
  color: #374151;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.tech-badge:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.15);
}

.tech-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
}

/* Footer */
.about-footer {
  text-align: center;
  padding-top: 40px;
  border-top: 2px solid #e5e7eb;
  color: #6b7280;
  animation: fadeInUp 0.6s ease 0.6s both;
}

.about-footer p {
  margin-bottom: 8px;
  font-size: 14px;
}

.footer-hint {
  font-size: 13px;
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
  .about-container {
    padding: 40px 16px;
  }
  
  .hero-title {
    font-size: 36px;
  }
  
  .hero-subtitle {
    font-size: 18px;
  }
  
  .section-title {
    font-size: 28px;
  }
  
  .features-grid {
    grid-template-columns: 1fr;
  }
}
</style>
