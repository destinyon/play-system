<script setup lang="ts">
// Placeholder page
</script>

<template>
  <div class="placeholder-page">
    <div class="placeholder-content">
      <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="#9ca3af" stroke-width="1.5">
        <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path>
        <circle cx="12" cy="10" r="3"></circle>
      </svg>
      <h2>欢迎使用高德导航系统</h2>
      <p>请从侧边栏选择功能</p>
    </div>
  </div>
</template>

<style scoped>
.placeholder-page {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8eef5 100%);
}

.placeholder-content {
  text-align: center;
  color: #6b7280;
}

.placeholder-content svg {
  margin: 0 auto 24px;
}

.placeholder-content h2 {
  margin: 0 0 12px;
  font-size: 24px;
  color: #374151;
  font-weight: 600;
}

.placeholder-content p {
  margin: 0;
  font-size: 16px;
  color: #9ca3af;
}
</style>
