<template>
  <div class="restaurateur-home">
    <div class="content-wrapper">
        <!-- 餐厅运营概览 - 引用新的 RestaurateurDashboard -->
        <RestaurateurDashboard />
    </div>
  </div>
</template>

<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import RestaurateurDashboard from './placeholder/RestaurateurDashboard.vue'

const auth = useAuthStore()
</script>

<style scoped>
.restaurateur-home {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: radial-gradient(1200px 600px at -10% 0%, rgba(253, 230, 138, 0.45) 0%, transparent 60%),
              linear-gradient(135deg, rgba(249, 115, 22, 0.08) 0%, rgba(251, 191, 36, 0.2) 100%);
}

.content-wrapper {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}
</style>
