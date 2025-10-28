<template>
  <div class="order-management">
    <div class="page-header">
      <h1>订单管理</h1>
      <p class="subtitle">处理顾客订单，查看订单详情</p>
    </div>

    <div class="filter-bar">
      <div class="filter-tabs">
        <button
          v-for="tab in statusTabs"
          :key="tab.value"
          :class="['tab-btn', { active: currentStatus === tab.value }]"
          @click="switchStatus(tab.value)"
        >
          {{ tab.label }}
          <span v-if="tab.count !== undefined && tab.count > 0" class="badge">{{ tab.count }}</span>
        </button>
      </div>
      <div class="search-box">
        <input
          v-model="keyword"
          @keyup.enter="loadOrders"
          placeholder="搜索订单号"
          class="search-input"
        />
        <button class="search-btn" @click="loadOrders">搜索</button>
      </div>
    </div>

    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>

    <div v-else-if="orders.length === 0" class="empty-state">
      <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"></path>
        <rect x="8" y="2" width="8" height="4" rx="1" ry="1"></rect>
      </svg>
      <p>暂无订单</p>
    </div>

    <div v-else>
      <!-- 分组视图（全部） -->
      <template v-if="currentStatus === 'ALL'">
        <div v-for="group in groupedStatuses" :key="group.value" class="group-section">
          <div class="group-header">
            <h3>{{ group.label }}</h3>
            <div class="group-actions">
              <span class="group-count">共 {{ countsMap[group.value] || 0 }} 单</span>
              <button class="search-btn small" @click="switchStatus(group.value)">查看更多</button>
            </div>
          </div>
          <ul class="orders-list">
            <li
              v-for="order in (groupedMap[group.value] || [])"
              :key="order.id"
              class="order-row"
              @click="viewDetail(order.id)"
            >
              <div class="row-left">
                <img
                  v-if="thumbOf(order)"
                  :src="thumbOf(order)"
                  class="row-thumb"
                  alt="dish"
                  @error="onThumbError"
                />
                <span class="order-no">{{ order.orderNo }}</span>
                <span class="muted">{{ formatTime(order.createdAt) }}</span>
              </div>
              <div class="row-right">
                <span :class="['mini-status', order.status.toLowerCase()]">{{ getStatusLabel(order.status) }}</span>
                <span class="amount">¥{{ order.totalAmount.toFixed(2) }}</span>
              </div>
            </li>
          </ul>
        </div>
      </template>

      <!-- 单状态视图（分页） -->
      <div v-else class="orders-grid">
        <div
          v-for="order in orders"
          :key="order.id"
          :class="['order-card', `status-${order.status.toLowerCase()}`]"
          @click="viewDetail(order.id)"
        >
          <div class="order-header">
            <span class="order-no">#{{ order.orderNo }}</span>
            <span :class="['status-badge', order.status.toLowerCase()]">{{ getStatusLabel(order.status) }}</span>
          </div>
          <div class="order-info">
            <div class="info-row">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                <circle cx="12" cy="7" r="4"></circle>
              </svg>
              <span>{{ order.customerName || '顾客' }}</span>
            </div>
            <div class="info-row">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path>
                <circle cx="12" cy="10" r="3"></circle>
              </svg>
              <span class="address">{{ order.deliveryAddress }}</span>
            </div>
            <div class="info-row">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"></circle>
                <polyline points="12 6 12 12 16 14"></polyline>
              </svg>
              <span>{{ formatTime(order.createdAt) }}</span>
            </div>
          </div>
          <div class="order-footer">
            <span class="amount">¥{{ order.totalAmount.toFixed(2) }}</span>
            <button
              v-if="canOperate(order.status)"
              class="action-btn"
              @click.stop="handleAction(order)"
              :disabled="processing"
            >
              {{ getActionLabel(order.status) }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="currentStatus !== 'ALL' && total > pageSize" class="pagination">
      <button :disabled="page === 1" @click="changePage(page - 1)">上一页</button>
      <span>第 {{ page }} / {{ Math.ceil(total / pageSize) }} 页</span>
      <button :disabled="page >= Math.ceil(total / pageSize)" @click="changePage(page + 1)">下一页</button>
    </div>

    <!-- 订单详情弹窗 -->
    <div v-if="selectedOrder" class="modal-overlay" @click.self="closeDetail">
      <div class="modal-content">
        <div class="modal-header">
          <h2>订单详情</h2>
          <button class="close-btn" @click="closeDetail">×</button>
        </div>
        <div class="modal-body">
          <div class="detail-section">
            <h3>订单信息</h3>
            <div class="detail-row"><label>订单号:</label><span>#{{ selectedOrder.orderNo }}</span></div>
            <div class="detail-row">
              <label>状态:</label>
              <span :class="['status-badge', selectedOrder.status.toLowerCase()]">{{ getStatusLabel(selectedOrder.status) }}</span>
            </div>
            <div class="detail-row"><label>客户:</label><span>{{ selectedOrder.customerName || '顾客' }}</span></div>
            <div class="detail-row" v-if="selectedOrder.customerPhone"><label>电话:</label><span>{{ selectedOrder.customerPhone }}</span></div>
            <div class="detail-row"><label>地址:</label><span>{{ selectedOrder.deliveryAddress }}</span></div>
            <div class="detail-row" v-if="selectedOrder.remark"><label>备注:</label><span class="remark">{{ selectedOrder.remark }}</span></div>
            <div class="detail-row"><label>下单时间:</label><span>{{ formatFullTime(selectedOrder.createdAt) }}</span></div>
          </div>
          <div class="detail-section">
            <h3>菜品明细</h3>
            <div class="items-list">
              <div v-for="item in selectedOrder.items" :key="item.id" class="item-row">
                <div class="item-info">
                  <img v-if="item.dishImage" :src="item.dishImage" :alt="item.dishName" class="item-image" />
                  <span class="item-name">{{ item.dishName }}</span>
                </div>
                <span class="item-price">¥{{ item.unitPrice.toFixed(2) }} × {{ item.quantity }}</span>
                <span class="item-total">¥{{ (item.unitPrice * item.quantity).toFixed(2) }}</span>
              </div>
            </div>
            <div class="total-row">
              <span>总计:</span>
              <span class="total-amount">¥{{ selectedOrder.totalAmount.toFixed(2) }}</span>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-secondary" @click="closeDetail">关闭</button>
          <button class="btn-secondary" @click="goToProfile" v-if="selectedOrder">用户资料</button>
          <button class="btn-secondary" @click="goToChat" v-if="selectedOrder">联系顾客</button>
          <button
            v-if="canOperate(selectedOrder.status)"
            class="btn-primary"
            @click="handleDetailAction"
            :disabled="processing"
          >
            {{ getActionLabel(selectedOrder.status) }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getOrderList, getOrderDetail, acceptOrder, startCooking, markOrderReady } from '@/api/order'

const auth = useAuthStore()
const restaurateurId = computed(() => {
  const id = auth.restaurant?.id as any
  const n = typeof id === 'string' ? parseInt(id, 10) : id
  const num = typeof n === 'number' ? n : Number(n)
  return Number.isFinite(num) ? num : 1
})
const router = useRouter()

const currentStatus = ref<string>('ALL')
const keyword = ref('')
const page = ref(1)
const pageSize = ref(12)
const total = ref(0)
const orders = ref<any[]>([])
const loading = ref(false)
const processing = ref(false)
const selectedOrder = ref<any | null>(null)
const thumbPlaceholder = 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40"><defs><linearGradient id="g" x1="0" x2="1" y1="0" y2="1"><stop offset="0%" stop-color="%23fff7ed"/><stop offset="100%" stop-color="%23fde68a"/></linearGradient></defs><rect width="40" height="40" rx="8" fill="url(%23g)"/><circle cx="16" cy="22" r="8" fill="%23a16207" opacity=".4"/><rect x="22" y="16" width="12" height="8" rx="2" fill="%2392550d" opacity=".4"/></svg>'

const statusTabs = ref([
  { label: '全部', value: 'ALL', count: 0 },
  { label: '待接单', value: 'PENDING', count: 0 },
  { label: '处理中', value: 'PROCESSING', count: 0 },
  { label: '已出餐', value: 'READY', count: 0 },
  { label: '已完成', value: 'COMPLETED', count: 0 }
])
const groupedStatuses = [
  { label: '待接单', value: 'PENDING' },
  { label: '处理中', value: 'PROCESSING' },
  { label: '已出餐', value: 'READY' },
  { label: '已完成', value: 'COMPLETED' },
]
const ordersByStatus = ref<Record<string, any[]>>({})
const countsByStatus = ref<Record<string, number>>({})
const groupedMap = computed(() => ordersByStatus.value || {})
const countsMap = computed(() => countsByStatus.value || {})

const getStatusLabel = (status: string) => {
  const map: Record<string, string> = {
    PENDING: '待接单',
    PROCESSING: '处理中',
    READY: '已出餐',
    COMPLETED: '已完成',
    CANCELLED: '已取消'
  }
  return map[status] || status
}

const canOperate = (status: string) => {
  return ['PENDING', 'PROCESSING'].includes(status)
}

const getActionLabel = (status: string) => {
  const map: Record<string, string> = {
    PENDING: '接单',
    PROCESSING: '出餐完成'
  }
  return map[status] || '操作'
}

const formatTime = (dateStr: string) => {
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  
  return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

const formatFullTime = (dateStr: string) => {
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', { 
    year: 'numeric',
    month: '2-digit', 
    day: '2-digit', 
    hour: '2-digit', 
    minute: '2-digit',
    second: '2-digit'
  })
}

const fetchList = async (status?: string, pageNo = 1, size = pageSize.value) => {
  const res = await getOrderList(restaurateurId.value, status, keyword.value, pageNo, size)
  if (res.status === 200 && res.data) {
    return res.data
  }
  return { total: 0, records: [] }
}

const loadOrders = async () => {
  loading.value = true
  try {
    if (currentStatus.value === 'ALL') {
      // 首屏每个状态取一页
      const results = await Promise.all([
        fetchList('PENDING', 1, 8),
        fetchList('PROCESSING', 1, 8),
        fetchList('READY', 1, 8),
        fetchList('COMPLETED', 1, 8),
      ])
      const keys = ['PENDING', 'PROCESSING', 'READY', 'COMPLETED']
      keys.forEach((k, i) => {
        const r = (results && results[i]) ? results[i] : { total: 0, records: [] }
        ordersByStatus.value[k] = r.records || []
        countsByStatus.value[k] = r.total || 0
      })
      // 更新 tab 计数
      statusTabs.value.forEach(tab => {
        if (tab.value !== 'ALL') tab.count = countsByStatus.value[tab.value] || 0
      })
    } else {
      const data = await fetchList(currentStatus.value, page.value, pageSize.value)
      orders.value = data.records || []
      total.value = data.total || 0
      const tab = statusTabs.value.find(t => t.value === currentStatus.value)
      if (tab) tab.count = total.value
    }
  } catch (error) {
    console.error('加载订单失败:', error)
    orders.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const viewDetail = async (orderId: number) => {
  try {
    const res = await getOrderDetail(orderId, restaurateurId.value)
    if (res.status === 200 && res.data) {
      selectedOrder.value = res.data
    }
  } catch (error) {
    console.error('加载订单详情失败:', error)
  }
}

const closeDetail = () => {
  selectedOrder.value = null
}

// 取订单首个菜品图片（兼容不同字段），用于分组列表缩略图
const thumbOf = (order: any): string | '' => {
  const img = order?.firstItemImage || order?.coverImage || order?.items?.[0]?.dishImage
  return img || ''
}

function onThumbError(ev: Event) {
  const img = ev.target as HTMLImageElement | null
  if (img && img.src !== thumbPlaceholder) {
    img.src = thumbPlaceholder
  }
}

const handleAction = async (order: any) => {
  processing.value = true
  try {
    let res
    if (order.status === 'PENDING') {
      res = await acceptOrder(order.id, restaurateurId.value)
      if (res.status === 200) {
        // 接单后自动开始制作
        await startCooking(order.id, restaurateurId.value)
      }
    } else if (order.status === 'PROCESSING') {
      res = await markOrderReady(order.id, restaurateurId.value)
    }
    
    if (res && res.status === 200) {
      if (currentStatus.value === 'ALL') {
        await loadOrders()
      } else {
        await loadOrders()
      }
    }
  } catch (error) {
    console.error('操作失败:', error)
  } finally {
    processing.value = false
  }
}

const handleDetailAction = async () => {
  if (!selectedOrder.value) return
  
  processing.value = true
  try {
    let res
    if (selectedOrder.value.status === 'PENDING') {
      res = await acceptOrder(selectedOrder.value.id, restaurateurId.value)
      if (res.status === 200) {
        await startCooking(selectedOrder.value.id, restaurateurId.value)
      }
    } else if (selectedOrder.value.status === 'PROCESSING') {
      res = await markOrderReady(selectedOrder.value.id, restaurateurId.value)
    }
    
    if (res && res.status === 200) {
      await loadOrders()
      closeDetail()
    }
  } catch (error) {
    console.error('操作失败:', error)
  } finally {
    processing.value = false
  }
}

const switchStatus = (status: string) => {
  currentStatus.value = status
  page.value = 1
  loadOrders()
}

const changePage = (newPage: number) => {
  page.value = newPage
  loadOrders()
}

onMounted(() => {
  loadOrders()
})

const goToChat = () => {
  if (!selectedOrder.value) return
  router.push({
    name: 'restaurateur-chat',
    query: {
      orderId: selectedOrder.value.id,
      orderNo: selectedOrder.value.orderNo,
      customer: selectedOrder.value.customerName || '顾客'
    }
  })
}

const goToProfile = () => {
  if (!selectedOrder.value) return
  const identifier = selectedOrder.value.customerPhone || selectedOrder.value.customerName || selectedOrder.value.orderNo
  router.push({ name: 'user-public-profile', params: { identifier } })
}
</script>
<style scoped src="../styles/RestaurateurOrders.css"></style>

