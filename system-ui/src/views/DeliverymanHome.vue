<template>
  <div class="deliveryman-home">

    <div class="content-wrapper">
  <!-- 骑手工作台 - 引用新的 DeliverymanDashboard -->
  <DeliverymanDashboard />
    </div>
  </div>
</template>

<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import DeliverymanDashboard from './placeholder/DeliverymanDashboard.vue'

const auth = useAuthStore()
</script>

<style scoped>
.deliveryman-home {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: radial-gradient(1100px 600px at 110% 10%, rgba(6, 182, 212, 0.35) 0%, transparent 60%),
              linear-gradient(135deg, rgba(13, 148, 136, 0.15) 0%, rgba(59, 130, 246, 0.12) 100%);
}

.content-wrapper {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}
</style>
