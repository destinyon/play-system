<template>
  <div class="guest-home">
    <div class="content-wrapper">
      <section class="page-hero">
        <div>
          <h1>欢迎回来，{{ greetingName }}</h1>
          <p>探索附近的热门餐厅，随时下单外卖</p>
        </div>
        <button class="locate-btn" :disabled="mapState.loading" @click="refreshMap">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 2v4" stroke-linecap="round" />
            <path d="M12 18v4" stroke-linecap="round" />
            <path d="M4.93 4.93l2.83 2.83" stroke-linecap="round" />
            <path d="M16.24 16.24l2.83 2.83" stroke-linecap="round" />
            <path d="M2 12h4" stroke-linecap="round" />
            <path d="M18 12h4" stroke-linecap="round" />
            <path d="M4.93 19.07l2.83-2.83" stroke-linecap="round" />
            <path d="M16.24 7.76l2.83-2.83" stroke-linecap="round" />
            <circle cx="12" cy="12" r="3" />
          </svg>
          <span>{{ mapState.loading ? '定位中...' : '重新定位' }}</span>
        </button>
      </section>

      <div class="map-and-list">
        <div class="map-card">
          <div class="card-header">
            <h2>
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polygon points="1 6 1 22 8 18 16 22 23 18 23 2 16 6 8 2 1 6" />
                <line x1="8" y1="2" x2="8" y2="18" />
                <line x1="16" y1="6" x2="16" y2="22" />
              </svg>
              附近餐厅地图
            </h2>
            <button class="refresh-btn" :disabled="mapState.loading" @click="refreshMap">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="23 4 23 10 17 10" />
                <polyline points="1 20 1 14 7 14" />
                <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
              </svg>
              <span>{{ mapState.loading ? '刷新中...' : '刷新' }}</span>
            </button>
          </div>
          <div class="map-container">
            <div ref="mapContainer" class="map-canvas"></div>
            <div v-if="mapState.loading" class="map-overlay">
              <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <polygon points="1 6 1 22 8 18 16 22 23 18 23 2 16 6 8 2 1 6"></polygon>
                <line x1="8" y1="2" x2="8" y2="18"></line>
                <line x1="16" y1="6" x2="16" y2="22"></line>
              </svg>
              <p>地图加载中...</p>
              <small>正在获取附近餐厅信息</small>
            </div>
            <div v-else-if="mapState.error" class="map-overlay map-overlay--error">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <circle cx="12" cy="12" r="10" />
                <line x1="12" y1="8" x2="12" y2="12" />
                <circle cx="12" cy="16" r="1" />
              </svg>
              <p>{{ mapState.error }}</p>
              <button class="retry-btn" @click="initMap">重新尝试</button>
            </div>
          </div>
        </div>

        <div class="nearby-card">
          <div class="card-header">
            <h2>附近外卖推荐</h2>
            <span v-if="!restaurantState.loading && sortedRestaurants.length" class="count-badge">
              {{ sortedRestaurants.length }} 家餐厅
            </span>
          </div>
          <div v-if="restaurantState.loading" class="list-placeholder">
            <div class="skeleton" v-for="n in 4" :key="n"></div>
          </div>
          <div v-else-if="restaurantState.error" class="list-empty">
            <p>{{ restaurantState.error }}</p>
            <button class="retry-btn" @click="refreshMap">重新加载</button>
          </div>
          <div v-else-if="sortedRestaurants.length" class="restaurant-list">
            <button
              v-for="restaurant in sortedRestaurants"
              :key="restaurant.id"
              type="button"
              class="restaurant-item"
              :class="{ active: selectedRestaurantId === restaurant.id }"
              @click="focusRestaurant(restaurant)"
            >
              <div class="item-main">
                <h3>{{ restaurant.name }}</h3>
                <p>{{ restaurant.address || '地址待完善' }}</p>
              </div>
              <div class="item-extra">
                <span class="restaurant-distance">{{ formatDistance(restaurant.distance) }}</span>
              </div>
            </button>
          </div>
          <div v-else class="list-empty">
            <p>附近暂无餐厅，稍后再试试吧。</p>
          </div>
        </div>
      </div>

      <div class="quick-actions">
        <router-link to="/app/guest/orders" class="action-card">
          <div class="action-icon orders">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="9" cy="21" r="1"></circle>
              <circle cx="20" cy="21" r="1"></circle>
              <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"></path>
            </svg>
          </div>
          <h3>我的订单</h3>
          <p>查看订单历史</p>
        </router-link>

        <router-link to="/app/guest/cart" class="action-card">
          <div class="action-icon cart">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M6 6h15l-1.5 9h-12z"></path>
              <circle cx="9" cy="20" r="1"></circle>
              <circle cx="18" cy="20" r="1"></circle>
            </svg>
          </div>
          <h3>购物车</h3>
          <p>管理您的购物车</p>
        </router-link>

        <router-link to="/app/profile" class="action-card">
          <div class="action-icon profile">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
              <circle cx="12" cy="7" r="4"></circle>
            </svg>
          </div>
          <h3>个人信息</h3>
          <p>编辑个人资料</p>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { listRestaurants } from '@/api/restaurant'

type RestaurantSummary = {
  id: number | string
  name: string
  address: string
  lng: number
  lat: number
  photoUrl?: string
  distance?: number | null
}

declare global {
  interface Window {
    AMap?: any
    _AMapSecurityConfig?: {
      securityJsCode?: string
    }
  }
}

const DEFAULT_CENTER: [number, number] = [121.4737, 31.2304]
const DEFAULT_AMAP_KEY = '218f3df7356ff0dcbc0635c0abf39893'
const DEFAULT_SECURITY_JS_CODE = '3b0d644aa49fcb13d08e0693933c6767'
const RESTAURANT_ICON = 'https://webapi.amap.com/theme/v1.3/markers/n/mark_r.png'
const USER_ICON = 'https://webapi.amap.com/theme/v1.3/markers/n/mark_b.png'

const auth = useAuthStore()
const greetingName = computed(() => auth.nickname || auth.username || '用户')

const mapContainer = ref<HTMLDivElement | null>(null)
const mapState = reactive({ loading: false, error: '' })
const restaurantState = reactive({ loading: false, error: '' })
const restaurants = ref<RestaurantSummary[]>([])
const sortedRestaurants = computed(() => {
  return [...restaurants.value].sort((a, b) => {
    const da = typeof a.distance === 'number' ? a.distance : Number.POSITIVE_INFINITY
    const db = typeof b.distance === 'number' ? b.distance : Number.POSITIVE_INFINITY
    return da - db
  })
})
const selectedRestaurantId = ref<RestaurantSummary['id'] | null>(null)
const userLocation = ref<[number, number] | null>(null)

let AMapRef: any = null
let mapInstance: any = null
let amapPromise: Promise<any> | null = null
const mapMarkers: any[] = []
let userMarker: any = null
let infoWindow: any = null

const ensureAmap = (): Promise<any> => {
  if (window.AMap) {
    return Promise.resolve(window.AMap)
  }
  if (amapPromise) {
    return amapPromise
  }

  const apiKey = import.meta.env.VITE_AMAP_KEY || DEFAULT_AMAP_KEY
  const securityCode = import.meta.env.VITE_AMAP_SECURITY_JSCODE || DEFAULT_SECURITY_JS_CODE

  amapPromise = new Promise((resolve, reject) => {
    window._AMapSecurityConfig = {
      securityJsCode: securityCode
    }

    const script = document.createElement('script')
    script.type = 'text/javascript'
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${apiKey}`
    script.async = true
    script.onload = () => {
      if (window.AMap) {
        resolve(window.AMap)
      } else {
        reject(new Error('高德地图加载失败'))
      }
    }
    script.onerror = () => reject(new Error('高德地图脚本加载失败'))

    document.head.appendChild(script)
  }).catch((error) => {
    amapPromise = null
    throw error
  })

  return amapPromise
}

const calcDistance = (origin: [number, number], target: [number, number]) => {
  const toRad = (value: number) => (value * Math.PI) / 180
  const [lng1, lat1] = origin
  const [lng2, lat2] = target
  const R = 6371
  const dLat = toRad(lat2 - lat1)
  const dLng = toRad(lng2 - lng1)
  const a = Math.sin(dLat / 2) ** 2 + Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLng / 2) ** 2
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
  return Math.round(R * c * 100) / 100
}

const formatDistance = (distance?: number | null) => {
  if (distance == null || Number.isNaN(distance)) return '距离未知'
  if (distance < 1) return `${Math.round(distance * 1000)} m`
  return `${distance.toFixed(1)} km`
}

const cleanupMap = (destroyInstance = false) => {
  mapMarkers.forEach((marker) => {
    if (marker?.setMap) marker.setMap(null)
    if (mapInstance?.remove) mapInstance.remove(marker)
  })
  mapMarkers.length = 0

  if (userMarker) {
    if (userMarker.setMap) userMarker.setMap(null)
    if (mapInstance?.remove) mapInstance.remove(userMarker)
    userMarker = null
  }

  if (infoWindow) {
    try { infoWindow.close() } catch (e) { /* noop */ }
    infoWindow = null
  }

  if (destroyInstance && mapInstance) {
    if (typeof mapInstance.destroy === 'function') {
      mapInstance.destroy()
    }
    mapInstance = null
  }
}

const fitMapToContent = () => {
  if (!mapInstance || typeof mapInstance.setFitView !== 'function') return
  const overlays = [...mapMarkers]
  if (userMarker) overlays.push(userMarker)
  if (overlays.length) {
    mapInstance.setFitView(overlays)
  }
}

const renderUserMarker = () => {
  if (!mapInstance || !AMapRef || !userLocation.value) return
  if (userMarker) {
    if (userMarker.setMap) userMarker.setMap(null)
    if (mapInstance?.remove) mapInstance.remove(userMarker)
    userMarker = null
  }
  userMarker = new AMapRef.Marker({
    position: userLocation.value,
    title: '我的位置',
    icon: USER_ICON
  })
  mapInstance.add(userMarker)
  fitMapToContent()
}

const applyDistances = (origin: [number, number]) => {
  restaurants.value = restaurants.value.map((item) => ({
    ...item,
    distance: calcDistance(origin, [item.lng, item.lat])
  }))
}

const getBrowserLocation = (): Promise<[number, number] | null> => {
  if (!navigator.geolocation) {
    return Promise.resolve(null)
  }
  return new Promise((resolve) => {
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        resolve([pos.coords.longitude, pos.coords.latitude])
      },
      () => resolve(null),
      { enableHighAccuracy: true, timeout: 8000 }
    )
  })
}

const updateUserLocation = async () => {
  const coords = await getBrowserLocation()
  if (!coords) return
  userLocation.value = coords
  renderUserMarker()
  applyDistances(coords)
  if (mapInstance?.setZoomAndCenter) {
    mapInstance.setZoomAndCenter(14, coords)
  }
}

const renderRestaurantMarkers = () => {
  if (!mapInstance || !AMapRef) return
  mapMarkers.forEach((marker) => {
    if (marker?.setMap) marker.setMap(null)
    if (mapInstance?.remove) mapInstance.remove(marker)
  })
  mapMarkers.length = 0

  restaurants.value.forEach((restaurant) => {
    const marker = new AMapRef.Marker({
      position: [restaurant.lng, restaurant.lat],
      title: restaurant.name,
      icon: RESTAURANT_ICON
    })
    marker.on?.('click', () => showRestaurantInfo(restaurant))
    mapInstance.add(marker)
    mapMarkers.push(marker)
  })

  fitMapToContent()
}

const showRestaurantInfo = (restaurant: RestaurantSummary) => {
  selectedRestaurantId.value = restaurant.id
  if (mapInstance?.setZoomAndCenter) {
    mapInstance.setZoomAndCenter(15, [restaurant.lng, restaurant.lat])
  }
  if (!AMapRef || !mapInstance) return
  if (!infoWindow) {
    try {
      infoWindow = new AMapRef.InfoWindow({
        offset: AMapRef.Pixel ? new AMapRef.Pixel(0, -24) : undefined,
        closeWhenClickMap: true
      })
    } catch (e) {
      infoWindow = new AMapRef.InfoWindow({ closeWhenClickMap: true })
    }
  }
  const content = `
    <div class="map-info-window">
      <h4>${restaurant.name}</h4>
      <p>${restaurant.address || '地址暂未提供'}</p>
      ${typeof restaurant.distance === 'number' ? `<span>约 ${formatDistance(restaurant.distance)}</span>` : ''}
    </div>
  `
  infoWindow.setContent(content)
  infoWindow.open(mapInstance, [restaurant.lng, restaurant.lat])
}

const loadRestaurantsData = async () => {
  restaurantState.loading = true
  restaurantState.error = ''
  try {
    const res = await listRestaurants()
    if (res.status === 200 && Array.isArray(res.data)) {
      const origin = userLocation.value
      const data = res.data
        .filter((item) => typeof item.lng === 'number' && typeof item.lat === 'number')
        .map((item) => ({
          id: item.id,
          name: item.name,
          address: item.address,
          lng: item.lng,
          lat: item.lat,
          photoUrl: item.photoUrl,
          distance: origin ? calcDistance(origin, [item.lng, item.lat]) : null
        }))
      restaurants.value = data
      if (!data.some((item) => item.id === selectedRestaurantId.value)) {
        selectedRestaurantId.value = data[0]?.id ?? null
      }
      renderRestaurantMarkers()
      if (selectedRestaurantId.value) {
        const match = data.find((item) => item.id === selectedRestaurantId.value)
        if (match) showRestaurantInfo(match)
      }
    } else {
      restaurantState.error = res.message || '未能获取附近餐厅'
    }
  } catch (error) {
    restaurantState.error = error instanceof Error ? error.message : '获取餐厅列表失败'
  } finally {
    restaurantState.loading = false
  }
}

const focusRestaurant = (restaurant: RestaurantSummary) => {
  showRestaurantInfo(restaurant)
}

const initMap = async () => {
  if (mapInstance || mapState.loading) return
  mapState.loading = true
  mapState.error = ''
  try {
    await nextTick()
    const AMap = await ensureAmap()
    AMapRef = AMap
    if (!mapContainer.value) {
      throw new Error('地图容器未准备就绪')
    }
    mapInstance = new AMap.Map(mapContainer.value, {
      viewMode: '3D',
      zoom: 12,
      center: DEFAULT_CENTER,
      resizeEnable: true
    })
    await updateUserLocation()
    await loadRestaurantsData()
  } catch (error) {
    mapState.error = error instanceof Error ? error.message : '地图初始化失败'
    cleanupMap(true)
  } finally {
    mapState.loading = false
  }
}

const refreshMap = async () => {
  if (mapState.loading) return
  mapState.loading = true
  mapState.error = ''
  try {
    await updateUserLocation()
    await loadRestaurantsData()
  } catch (error) {
    mapState.error = error instanceof Error ? error.message : '刷新地图失败'
  } finally {
    mapState.loading = false
  }
}

onMounted(() => {
  initMap()
})

onBeforeUnmount(() => {
  cleanupMap(true)
})
</script>

<style scoped>
.guest-home {
  height: 100%;
  overflow-y: auto;
  background: linear-gradient(135deg, #667eea15 0%, #764ba215 100%);
}

.content-wrapper {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.page-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}

.page-hero h1 {
  margin: 0 0 8px;
  font-size: 28px;
  font-weight: 700;
  color: #111827;
}

.page-hero p {
  margin: 0;
  font-size: 16px;
  color: #6b7280;
}

.locate-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border-radius: 999px;
  border: none;
  background: linear-gradient(135deg, #3b82f6 0%, #6366f1 100%);
  color: #fff;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.locate-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(99, 102, 241, 0.35);
}

.locate-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
  box-shadow: none;
}

.map-and-list {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(0, 1fr);
  gap: 24px;
  margin-bottom: 32px;
}

.map-card,
.nearby-card {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #e5e7eb;
}

.card-header h2 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #111827;
}

.refresh-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.refresh-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.refresh-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  box-shadow: none;
}

.map-container {
  position: relative;
  height: 480px;
  background: #f9fafb;
}

.map-canvas {
  width: 100%;
  height: 100%;
}

.map-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 12px;
  background: rgba(255, 255, 255, 0.92);
  color: #6b7280;
  text-align: center;
}

.map-overlay p {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #374151;
}

.map-overlay small {
  font-size: 14px;
  color: #9ca3af;
}

.map-overlay--error {
  color: #ef4444;
}

.retry-btn {
  padding: 8px 16px;
  border-radius: 8px;
  border: none;
  background: linear-gradient(135deg, #f97316 0%, #f59e0b 100%);
  color: #fff;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.retry-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(249, 115, 22, 0.35);
}

.count-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(102, 126, 234, 0.12);
  color: #4f46e5;
  font-weight: 600;
  font-size: 13px;
}

.list-placeholder {
  padding: 24px;
  display: grid;
  gap: 12px;
}

.skeleton {
  height: 72px;
  border-radius: 12px;
  background: linear-gradient(90deg, #f3f4f6 0%, #e5e7eb 50%, #f3f4f6 100%);
  background-size: 200% 100%;
  animation: shimmer 1.6s infinite;
}

@keyframes shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

.restaurant-list {
  display: flex;
  flex-direction: column;
  padding: 16px 24px 24px;
  gap: 12px;
}

.restaurant-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  width: 100%;
  border: none;
  border-radius: 12px;
  padding: 16px;
  background: #f9fafb;
  text-align: left;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s, background 0.2s;
}

.restaurant-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.15);
}

.restaurant-item.active {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.15) 0%, rgba(118, 75, 162, 0.2) 100%);
  box-shadow: 0 8px 18px rgba(118, 75, 162, 0.2);
}

.item-main h3 {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.item-main p {
  margin: 0;
  font-size: 14px;
  color: #6b7280;
}

.item-extra {
  flex-shrink: 0;
}

.restaurant-distance {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(16, 185, 129, 0.12);
  color: #059669;
  font-weight: 600;
  font-size: 13px;
}

.map-info-window {
  min-width: 200px;
  color: #1f2937;
  font-family: 'Segoe UI', system-ui, -apple-system, BlinkMacSystemFont, sans-serif;
}

.map-info-window h4 {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 700;
}

.map-info-window p {
  margin: 0 0 4px;
  font-size: 13px;
  color: #4b5563;
}

.map-info-window span {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.12);
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 600;
}

.list-empty {
  padding: 32px 24px;
  text-align: center;
  color: #9ca3af;
}

.list-empty p {
  margin: 0 0 12px;
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
}

.action-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  text-decoration: none;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transition: transform 0.2s, box-shadow 0.2s;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.action-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.action-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}

.action-icon.orders {
  background: linear-gradient(135deg, #667eea15 0%, #764ba215 100%);
  color: #667eea;
}

.action-icon.cart {
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.15) 0%, rgba(249, 115, 22, 0.2) 100%);
  color: #f59e0b;
}

.action-icon.profile {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.15) 0%, rgba(5, 150, 105, 0.2) 100%);
  color: #10b981;
}

.action-card h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 600;
  color: #111827;
}

.action-card p {
  margin: 0;
  font-size: 14px;
  color: #6b7280;
}

@media (max-width: 1024px) {
  .map-and-list {
    grid-template-columns: 1fr;
  }

  .page-hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .locate-btn {
    align-self: stretch;
    justify-content: center;
  }
}

@media (max-width: 768px) {
  .map-container {
    height: 360px;
  }

  .restaurant-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .item-extra {
    align-self: flex-end;
  }
}
</style>
