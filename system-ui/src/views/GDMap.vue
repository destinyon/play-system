<template>
  <div class="gdmap">
    <!-- 地图工具栏 -->
    <div class="map-toolbar">
      <button 
        :class="['tool-btn', { 'tool-btn--active': mapTool === 'drag' }]" 
        @click="setMapTool('drag')"
        title="拖拽模式">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor">
          <path d="M5 9l-3 3 3 3M9 5l3-3 3 3M15 19l-3 3-3-3M19 9l3 3-3 3M2 12h20M12 2v20" stroke-width="2" stroke-linecap="round"/>
        </svg>
        <span>拖拽</span>
      </button>
      <button 
        :class="['tool-btn', { 'tool-btn--active': mapTool === 'marker' }]" 
        @click="setMapTool('marker')"
        title="标记模式">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor">
          <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" stroke-width="2"/>
          <circle cx="12" cy="10" r="3" stroke-width="2"/>
        </svg>
        <span>标记</span>
      </button>
    </div>

    <!-- Side panel -->
    <aside :class="['gdmap__panel', { 'gdmap__panel--collapsed': isPanelCollapsed }]">
      <header class="panel__header">
        <h2>地图工具</h2>
        <small class="muted">当前位置：<span v-if="currentLngLat">{{ currentLngLat[0].toFixed(6) }}, {{ currentLngLat[1].toFixed(6) }}</span><span v-else>定位中…</span></small>
      </header>

      <div v-if="noProxy" class="panel__warn">
        正在直连高德服务（未设置代理）。建议在生产环境配置 VITE_AMAP_PROXY_HOST 和安全密钥。
      </div>
      <div v-else-if="proxyDown" class="panel__warn panel__warn--danger">
        代理不可达，已自动直连高德 REST。请检查本机 9090 端口或 nginx 配置。
      </div>

      <section class="panel__section">
        <h3>标记点</h3>
        <p class="muted">单击地图添加标记，或使用下方按钮。</p>
        <div class="actions">
          <button class="btn" @click="addMarkerAtCenter">在中心添加</button>
          <button class="btn" @click="clearMarkers" :disabled="markers.length === 0">清空标记</button>
        </div>
        <ul class="marker-list" v-if="markers.length">
          <li v-for="m in markers" :key="m.id" class="marker-item">
            <div class="marker-meta" @click="flyTo(m.lnglat)">
              <strong>{{ m.name }}</strong>
              <small>{{ m.lnglat[0].toFixed(6) }}, {{ m.lnglat[1].toFixed(6) }}</small>
              <small v-if="m.address" class="marker-addr">{{ m.address }}</small>
            </div>
            <div class="marker-actions">
              <button class="btn btn--sm" @click="setAs('origin', m)">设为起点</button>
              <button class="btn btn--sm" @click="setAs('destination', m)">设为终点</button>
              <button class="btn btn--sm btn--danger" @click="removeMarker(m.id)">删除</button>
            </div>
          </li>
        </ul>
        <p v-else class="muted">暂无标记。</p>
      </section>

      <section class="panel__section">
        <h3>步行路线规划</h3>
        <div class="route-row">
          <label>起点：</label>
          <select v-model="originId">
            <option :value="''">未选择</option>
            <option v-for="m in markers" :key="m.id" :value="m.id">{{ m.name }}</option>
          </select>
        </div>
        <div class="route-row">
          <label>终点：</label>
          <select v-model="destinationId">
            <option :value="''">未选择</option>
            <option v-for="m in markers" :key="m.id" :value="m.id">{{ m.name }}</option>
          </select>
        </div>
        <div class="actions">
          <button class="btn" @click="planWalkingRoute" :disabled="!originId || !destinationId">规划步行</button>
          <button class="btn" @click="clearRoute" :disabled="!routePlotted">清除路线</button>
        </div>
        <div class="route-summary" v-if="routeSummary">
          <div class="summary-item"><span>距离</span><strong>{{ routeSummary.distanceText }}</strong></div>
          <div class="summary-item"><span>预计时间</span><strong>{{ routeSummary.durationText }}</strong></div>
        </div>
        <div class="route-steps">
          <template v-if="routeSteps.length">
            <ol class="steps">
              <li v-for="s in routeSteps" :key="s.idx" class="step-item">
                <span class="step-index">{{ s.idx }}</span>
                <span class="step-text">{{ s.text || '按路况前行' }}</span>
              </li>
            </ol>
          </template>
          <p v-else class="muted">提示：先选择起点与终点并点击“规划步行”。</p>
        </div>
      </section>

      <footer class="panel__footer">
        <span class="muted">提示：允许浏览器定位以自动居中。</span>
      </footer>
    </aside>

    <!-- 路线信息卡片 -->
    <div v-if="routePlotted && routeSummary" class="route-info-card">
      <div class="route-info-header">
        <h3>🚶 步行路线</h3>
        <button class="btn-close-card" @click="clearRoute" title="清除路线">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18"/>
            <line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </button>
      </div>
      <div class="route-info-body">
        <div class="route-info-item">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M3 12h18M3 6h18M3 18h18"/>
          </svg>
          <div>
            <span>距离</span>
            <strong>{{ routeSummary.distanceText }}</strong>
          </div>
        </div>
        <div class="route-info-item">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <polyline points="12 6 12 12 16 14"/>
          </svg>
          <div>
            <span>预计时间</span>
            <strong>{{ routeSummary.durationText }}</strong>
          </div>
        </div>
      </div>
    </div>

    <!-- Map container -->
    <div ref="mapContainer" class="gdmap__container"></div>
    <div v-if="error" class="gdmap__error">{{ error }}</div>

    <!-- Custom Add-Marker Modal -->
    <div v-if="showAddModal" class="modal__mask" @click.self="cancelAdd">
      <div class="modal">
        <h3>添加标记</h3>
        <p class="modal__coords">📍 经纬度：{{ pendingLngLat?.[0].toFixed(6) }}, {{ pendingLngLat?.[1].toFixed(6) }}</p>
        <div class="form-group">
          <label class="modal__label">名称</label>
          <input class="modal__input" v-model="modalName" placeholder="请输入名称" />
        </div>
        <div class="form-group">
          <label class="modal__label">地址/建筑</label>
          <input class="modal__input" v-model="modalAddress" :placeholder="modalAddress === '检索中…' ? '正在识别最近的建筑...' : '自动识别最近的建筑'" />
        </div>
        <div class="actions">
          <button class="btn btn--success" @click="confirmAdd">确定</button>
          <button class="btn btn--secondary" @click="cancelAdd">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref, computed } from 'vue'

// Load the AMap JS SDK with proxy-based security configuration.
type AMapMap = {
  destroy(): void
  setZoomAndCenter(zoom: number, center: [number, number]): void
  setCenter(center: [number, number]): void
  add(overlays: unknown | unknown[]): void
  resize(): void
  on?: (event: string, handler: (e: any) => void) => void
}

type AMapSDK = {
  Map: new (container: string | HTMLElement, options?: Record<string, unknown>) => AMapMap
  Marker: new (opts: { position: [number, number]; offset?: unknown; title?: string; icon?: string; extData?: any }) => any
  plugin: (names: string[] | string, cb: () => void) => void
  Walking: new (opts: { map?: AMapMap; panel?: string | HTMLElement }) => {
    search: (
      origin: [number, number],
      destination: [number, number],
      cb?: (status: string, result: any) => void
    ) => void
    clear: () => void
  }
  Geocoder: new (opts?: { city?: string; batch?: boolean; [key: string]: any }) => {
    getAddress: (lnglat: [number, number], cb: (status: string, result: any) => void) => void
  }
  Polyline: new (opts: {
    path: Array<[number, number]>;
    showDir?: boolean;
    strokeColor?: string;
    strokeWeight?: number;
    lineJoin?: string;
    lineCap?: string;
  }) => unknown
  InfoWindow: new (opts: {
    content: string;
    offset?: any;
    anchor?: string;
  }) => any
}

declare global {
  interface Window {
    AMap?: AMapSDK
    _AMapSecurityConfig?: {
      securityJsCode?: string
      serviceHost?: string
    }
  }
}

const DEFAULT_AMAP_KEY = '218f3df7356ff0dcbc0635c0abf39893'
const DEFAULT_SECURITY_JS_CODE = '3b0d644aa49fcb13d08e0693933c6767'

const loadAmap = (): Promise<AMapSDK> => {
  return new Promise((resolve, reject) => {
    if (window.AMap) {
      resolve(window.AMap)
      return
    }

    const apiKey = import.meta.env.VITE_AMAP_KEY || DEFAULT_AMAP_KEY
    const securityCode = import.meta.env.VITE_AMAP_SECURITY_JSCODE || DEFAULT_SECURITY_JS_CODE

    // 设置安全密钥（必须在加载SDK之前）
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
        reject(new Error('高德地图脚本已加载，但未检测到 AMap 对象。'))
      }
    }

    script.onerror = () => {
      reject(new Error('高德地图脚本加载失败，请检查网络或代理配置。'))
    }

    document.head.appendChild(script)
  })
}

const mapContainer = ref<HTMLDivElement | null>(null)
const error = ref<string | null>(null)
let mapInstance: AMapMap | null = null
let AMapRef: AMapSDK | null = null
type WalkingInstance = { search: (o: [number, number], d: [number, number], cb?: (status: string, result: any) => void) => void; clear: () => void }
let walking: WalkingInstance | null = null

// 缓存用户位置，避免重复定位
const CACHED_LOCATION_KEY = 'amap_user_location'
const LOCATION_CACHE_DURATION = 30 * 60 * 1000 // 30分钟缓存

function getCachedLocation(): { lnglat: [number, number]; timestamp: number } | null {
  try {
    const cached = localStorage.getItem(CACHED_LOCATION_KEY)
    if (cached) {
      const data = JSON.parse(cached)
      if (Date.now() - data.timestamp < LOCATION_CACHE_DURATION) {
        return data
      }
    }
  } catch {}
  return null
}

function setCachedLocation(lnglat: [number, number]) {
  try {
    localStorage.setItem(CACHED_LOCATION_KEY, JSON.stringify({
      lnglat,
      timestamp: Date.now()
    }))
  } catch {}
}

type MarkerItem = { id: string; name: string; lnglat: [number, number]; overlay?: unknown; address?: string }
const markers = ref<MarkerItem[]>([])
const currentLngLat = ref<[number, number] | null>(null)
const originId = ref('')
const destinationId = ref('')
const routePlotted = ref(false)
const routeSummary = ref<{ distanceText: string; durationText: string } | null>(null)
const routePanelRef = ref<HTMLDivElement | null>(null)
const routeSteps = ref<Array<{ idx: number; text: string }>>([])
let routePolyline: unknown | null = null
const PROXY_HOST = import.meta.env.VITE_AMAP_PROXY_HOST as string | undefined
const noProxy = !PROXY_HOST
const proxyDown = ref(false)

// add-marker modal state
const showAddModal = ref(false)
const pendingLngLat = ref<[number, number] | null>(null)
const modalName = ref('')
const modalAddress = ref('')

// selected marker state for info panel
const selectedMarkerId = ref('')
const selectedMarker = computed(() => markers.value.find(m => m.id === selectedMarkerId.value))

// 面板折叠状态
const isPanelCollapsed = ref(false)

// 地图工具状态：'marker' 或 'drag'
const mapTool = ref<'marker' | 'drag'>('drag')

function selectMarker(m: MarkerItem) {
  selectedMarkerId.value = m.id
  flyTo(m.lnglat)
}

function togglePanel() {
  isPanelCollapsed.value = !isPanelCollapsed.value
}

function setMapTool(tool: 'marker' | 'drag') {
  mapTool.value = tool
  if (mapInstance) {
    const container = mapInstance as any
    if (tool === 'marker') {
      container.setDefaultCursor && container.setDefaultCursor('crosshair')
    } else {
      container.setDefaultCursor && container.setDefaultCursor('default')
    }
  }
}

onMounted(async () => {
  if (!mapContainer.value) {
    error.value = '未找到地图容器。'
    return
  }

  try {
    const AMap = await loadAmap()
    AMapRef = AMap
    mapInstance = new AMap.Map(mapContainer.value, {
      zoom: 12,
      center: [116.397428, 39.90923],
    })

    // 优先使用缓存位置，避免重复定位
    const cached = getCachedLocation()
    if (cached) {
      currentLngLat.value = cached.lnglat
      mapInstance!.setZoomAndCenter(15, cached.lnglat)
      try {
        const marker = new AMap.Marker({ 
          position: cached.lnglat, 
          title: '我的位置',
          icon: '//a.amap.com/jsapi_demos/static/demo-center/icons/poi-marker-default.png'
        })
        mapInstance!.add(marker)
      } catch (e) {}
    } else if ('geolocation' in navigator) {
      // 首次加载时获取位置并缓存
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          const { longitude, latitude } = pos.coords
          const lnglat: [number, number] = [longitude, latitude]
          currentLngLat.value = lnglat
          setCachedLocation(lnglat) // 缓存位置
          mapInstance!.setZoomAndCenter(15, lnglat)
          try {
            const marker = new AMap.Marker({ 
              position: lnglat, 
              title: '我的位置',
              icon: '//a.amap.com/jsapi_demos/static/demo-center/icons/poi-marker-default.png'
            })
            mapInstance!.add(marker)
          } catch (e) {}
        },
        (err) => {
          console.warn('定位失败：', err.message)
        },
        { enableHighAccuracy: true, timeout: 8000, maximumAge: 0 }
      )
    }

    // 视口变化时自适应
    const onResize = () => mapInstance!.resize()
    window.addEventListener('resize', onResize)
    // 将监听器挂到实例上以便卸载
    ;(mapInstance as unknown as { __onResize?: () => void }).__onResize = onResize

    // 点击地图：只在标记模式下弹出对话框
    mapInstance.on && mapInstance.on('click', async (e: any) => {
      if (!AMapRef || mapTool.value !== 'marker') return
      const lnglat: [number, number] = [e.lnglat.getLng?.() ?? e.lnglat.lng, e.lnglat.getLat?.() ?? e.lnglat.lat]
      pendingLngLat.value = lnglat
      modalName.value = `标记 ${markers.value.length + 1}`
      modalAddress.value = '检索中…'
      showAddModal.value = true
      try {
        const nameFromGeo = await reverseGeocode(lnglat)
        if (showAddModal.value) {
          modalAddress.value = nameFromGeo.address
          if (!modalName.value || modalName.value.startsWith('标记')) modalName.value = nameFromGeo.name
        }
      } catch {
        if (showAddModal.value) modalAddress.value = ''
      }
    })
  } catch (err) {
    error.value = (err as Error).message
  }
})

onBeforeUnmount(() => {
  if (mapInstance) {
    // 卸载窗口监听
    const handler = (mapInstance as unknown as { __onResize?: () => void }).__onResize
    if (handler) window.removeEventListener('resize', handler)
    mapInstance.destroy()
    mapInstance = null
  }
})

// Helpers
function addMarker(lnglat: [number, number], name?: string, address?: string) {
  if (!mapInstance || !AMapRef) return
  const id = `${lnglat[0]}_${lnglat[1]}_${Date.now()}`
  const displayName = name || `标记 ${markers.value.length + 1}`
  let overlay: any
  try {
    overlay = new AMapRef.Marker({ 
      position: lnglat, 
      title: displayName,
      extData: { id, name: displayName, address }
    })
    
    // 添加点击事件显示信息窗体
    overlay.on('click', () => {
      const infoContent = `
        <div style="padding: 12px; min-width: 200px;">
          <h3 style="margin: 0 0 8px; font-size: 16px; color: #1f2937; font-weight: 600;">${displayName}</h3>
          ${address ? `<p style="margin: 0 0 6px; font-size: 13px; color: #6b7280; line-height: 1.5;">📍 ${address}</p>` : ''}
          <p style="margin: 0; font-size: 12px; color: #9ca3af; font-family: monospace;">${lnglat[0].toFixed(6)}, ${lnglat[1].toFixed(6)}</p>
        </div>
      `
      const infoWindow = new AMapRef!.InfoWindow({
        content: infoContent,
        offset: new (AMapRef as any).Pixel(0, -30),
        anchor: 'bottom-center'
      })
      infoWindow.open(mapInstance, lnglat)
    })
    
    mapInstance.add(overlay)
  } catch {}
  markers.value.push({ id, name: displayName, lnglat, overlay, address })
}

function addMarkerAtCenter() {
  if (!mapInstance) return
  // 无类型 Map 的 getCenter 尝试
  // @ts-ignore
  const c = mapInstance.getCenter?.()
  const lng = c?.getLng?.() ?? c?.lng ?? 116.397428
  const lat = c?.getLat?.() ?? c?.lat ?? 39.90923
  addMarker([lng, lat])
}

function clearMarkers() {
  if (!mapInstance) return
  // 简化处理：不一一从地图移除，依赖 AMap 自动清理；如需严格移除，请遍历并调用 mapInstance.remove
  markers.value = []
}

function removeMarker(id: string) {
  const idx = markers.value.findIndex((m) => m.id === id)
  if (idx >= 0) {
    // TODO: 如需显示从地图移除，保存并调用 map.remove
    markers.value.splice(idx, 1)
  }
}

function flyTo(lnglat: [number, number]) {
  mapInstance?.setZoomAndCenter(16, lnglat)
}

function setAs(type: 'origin' | 'destination', m: MarkerItem) {
  if (type === 'origin') originId.value = m.id
  else destinationId.value = m.id
}

function planRoute() {
  if (!AMapRef || !mapInstance) return
  const origin = markers.value.find((m) => m.id === originId.value)
  const dest = markers.value.find((m) => m.id === destinationId.value)
  if (!origin || !dest) return

  // 兼容旧方法名：转到步行规划
  planWalkingRoute()
}

async function planWalkingRoute() {
  if (!AMapRef || !mapInstance) return
  const origin = markers.value.find((m) => m.id === originId.value)
  const dest = markers.value.find((m) => m.id === destinationId.value)
  if (!origin || !dest) return

  // 清理旧路线
  if (routePolyline && mapInstance) {
    try { (mapInstance as any).remove && (mapInstance as any).remove(routePolyline) } catch {}
    routePolyline = null
  }
  routeSteps.value = []

  try {
    // 使用Web服务Key进行REST API调用
    const webServiceKey = import.meta.env.VITE_AMAP_WEB_SERVICE_KEY || import.meta.env.VITE_AMAP_KEY || DEFAULT_AMAP_KEY
    const data = await amapFetch('/v3/direction/walking', {
      origin: `${origin.lnglat[0]},${origin.lnglat[1]}`,
      destination: `${dest.lnglat[0]},${dest.lnglat[1]}`,
      output: 'json',
      extensions: 'all',
    }, webServiceKey)
    const path0 = data?.route?.paths?.[0]
    if (!path0) throw new Error('无有效路径')
    const d = Number(path0.distance || 0)
    const t = Number(path0.duration || 0)
    routeSummary.value = { distanceText: formatDistance(d), durationText: formatDuration(t) }
    const path: Array<[number, number]> = []
    const steps = path0.steps || []
    steps.forEach((s: any, i: number) => {
      routeSteps.value.push({ idx: i + 1, text: s.instruction || '' })
      if (typeof s.polyline === 'string') {
        s.polyline.split(';').forEach((pair: string) => {
          const [lngS, latS] = pair.split(',')
          const lng = Number(lngS); const lat = Number(latS)
          if (!isNaN(lng) && !isNaN(lat)) path.push([lng, lat])
        })
      }
    })
    if (path.length > 1 && AMapRef) {
      routePolyline = new (AMapRef as AMapSDK).Polyline({
        path,
        showDir: true,
        strokeColor: '#2563eb',
        strokeWeight: 5,
        lineJoin: 'round',
        lineCap: 'round',
      })
      try { (mapInstance as any).add(routePolyline) } catch {}
      // 自动适配路线并折叠左侧面板
      try { 
        (mapInstance as any).setFitView && (mapInstance as any).setFitView([routePolyline], false, [100, 60, 60, 60])
      } catch {}
      isPanelCollapsed.value = true
    }
    routePlotted.value = true
  } catch (e) {
    routePlotted.value = false
    routeSummary.value = null
    error.value = '步行路线规划失败（REST）。请检查网络或代理设置。'
  }
}

function clearRoute() {
  if (walking) {
    try { walking.clear() } catch {}
  }
  routePlotted.value = false
  routeSummary.value = null
  routeSteps.value = []
  if (routePolyline && mapInstance) {
    try { (mapInstance as any).remove && (mapInstance as any).remove(routePolyline) } catch {}
    routePolyline = null
  }
}

// Reverse geocode: get nearest building/POI
function reverseGeocode(lnglat: [number, number]): Promise<{ name: string; address: string }> {
  return new Promise((resolve) => {
    if (!AMapRef) {
      console.error('AMap 未初始化')
      resolve({ name: '未命名位置', address: '' })
      return
    }
    
    console.log('开始反向地理编码:', lnglat)
    
    // 使用 AMap JS API 的 Geocoder 插件
    AMapRef.plugin('AMap.Geocoder', () => {
      try {
        const geocoder = new AMapRef!.Geocoder({
          radius: 1000,
          extensions: 'all'
        })
        
        geocoder.getAddress(lnglat, (status: string, result: any) => {
          console.log('Geocoder 返回结果:', status, result)
          
          if (status !== 'complete' || !result || !result.regeocode) {
            console.warn('地理编码未完成:', status, result)
            resolve({ name: '未命名位置', address: '' })
            return
          }
          
          const regeocode = result.regeocode
          const fmt = regeocode.formattedAddress || ''
          let name = ''
          
          // 第一优先级：最近的 POI
          if (regeocode.pois && Array.isArray(regeocode.pois) && regeocode.pois.length > 0) {
            const pois = regeocode.pois.filter((p: any) => p.name && p.name.trim())
            if (pois.length > 0) {
              // 按距离排序
              pois.sort((a: any, b: any) => {
                const distA = parseFloat(a.distance) || 999999
                const distB = parseFloat(b.distance) || 999999
                return distA - distB
              })
              name = pois[0].name
              console.log('找到最近POI:', name, '距离:', pois[0].distance)
            }
          }
          
          // 第二优先级：建筑物名称
          if (!name && regeocode.addressComponent) {
            const addr = regeocode.addressComponent
            if (addr.building && typeof addr.building === 'string' && addr.building !== '[]') {
              name = addr.building
              console.log('使用建筑物名称:', name)
            } else if (addr.township && typeof addr.township === 'string') {
              name = addr.township
              console.log('使用街道名称:', name)
            } else if (addr.neighborhood && typeof addr.neighborhood === 'string') {
              name = addr.neighborhood
              console.log('使用社区名称:', name)
            }
          }
          
          // 第三优先级：使用地址
          if (!name && fmt) {
            name = fmt
            console.log('使用格式化地址:', name)
          }
          
          // 如果还是没有
          if (!name) {
            name = '未命名位置'
          }
          
          console.log('最终结果:', { name, address: fmt })
          resolve({ name, address: fmt })
        })
      } catch (err) {
        console.error('反向地理编码异常:', err)
        resolve({ name: '未命名位置', address: '' })
      }
    })
  })
}

// 调用 AMap REST 服务（支持带代理）
async function amapFetch(path: string, params: Record<string, string>, customKey?: string) {
  const apiKey = customKey || import.meta.env.VITE_AMAP_KEY || DEFAULT_AMAP_KEY
  const qs = new URLSearchParams({ key: apiKey, ...params }).toString()

  // Try proxy first if configured
  if (PROXY_HOST) {
    const proxyBase = `${PROXY_HOST}/_AMapService`
    const url = `${proxyBase}${path}?${qs}`
    try {
      console.log('Using AMap service host:', proxyBase)
      const res = await fetch(url)
      if (res.ok) {
        const data = await res.json()
        if (!data.status || `${data.status}` === '1') return data
        // AMap业务失败也直接返回给调用方，由上层展示
        return data
      } else {
        proxyDown.value = true
      }
    } catch (e) {
      proxyDown.value = true
    }
  }

  // Fallback to direct REST
  const directBase = 'https://restapi.amap.com'
  const url2 = `${directBase}${path}?${qs}`
  console.log('Using AMap service host:', directBase)
  const res2 = await fetch(url2)
  if (!res2.ok) throw new Error(`HTTP ${res2.status}`)
  const data2 = await res2.json()
  if (data2.status && `${data2.status}` !== '1') throw new Error(data2.info || 'AMap error')
  return data2
}

// Modal actions
function confirmAdd() {
  if (!pendingLngLat.value) return cancelAdd()
  addMarker(pendingLngLat.value, modalName.value.trim() || undefined, modalAddress.value.trim() || undefined)
  showAddModal.value = false
  pendingLngLat.value = null
  modalName.value = ''
  modalAddress.value = ''
}
function cancelAdd() {
  showAddModal.value = false
  pendingLngLat.value = null
}

// Format helpers
function formatDistance(meters: number) {
  if (!meters || isNaN(meters)) return '-'
  if (meters < 1000) return `${Math.round(meters)} m`
  return `${(meters / 1000).toFixed(2)} km`
}
function formatDuration(seconds: number) {
  if (!seconds || isNaN(seconds)) return '-'
  const m = Math.round(seconds / 60)
  if (m < 60) return `${m} 分钟`
  const h = Math.floor(m / 60)
  const mm = m % 60
  return `${h} 小时 ${mm} 分`
}
</script>

<style scoped>
.gdmap {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  background: #f5f7fa;
}

/* 地图工具栏 */
.map-toolbar {
  position: absolute;
  top: 20px;
  right: 20px;
  z-index: 20;
  display: flex;
  gap: 8px;
  background: linear-gradient(135deg, #ffffff 0%, #f0f9ff 100%);
  backdrop-filter: blur(20px);
  padding: 8px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.15), 0 0 0 1px rgba(147, 197, 253, 0.3);
  border: 2px solid rgba(147, 197, 253, 0.3);
}

.tool-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 10px 16px;
  border: 2px solid #bae6fd;
  border-radius: 10px;
  background: #ffffff;
  color: #0284c7;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.tool-btn:hover {
  border-color: #3b82f6;
  background: linear-gradient(135deg, #f0f9ff 0%, #dbeafe 100%);
  color: #0369a1;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.2);
}

.tool-btn--active {
  border-color: #3b82f6;
  background: linear-gradient(135deg, #3b82f6 0%, #06b6d4 100%);
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

.tool-btn--active:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(59, 130, 246, 0.5);
}

.tool-btn svg {
  flex-shrink: 0;
}

.tool-btn span {
  letter-spacing: -0.2px;
}

.gdmap__container {
  width: 100%;
  height: 100%;
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
}

.gdmap__error {
  position: absolute;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  background: #fee2e2;
  border: 1px solid #fca5a5;
  border-radius: 8px;
  padding: 12px 20px;
  color: #991b1b;
  font-size: 14px;
  line-height: 1.5;
  max-width: 500px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  z-index: 30;
}

.gdmap__error strong {
  display: block;
  margin-bottom: 4px;
  font-size: 16px;
}

/* Panels */
.gdmap__panel {
  position: absolute;
  background: linear-gradient(135deg, #ffffff 0%, #f0f9ff 100%);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(59, 130, 246, 0.15), 0 0 0 1px rgba(59, 130, 246, 0.1);
  padding: 20px;
  overflow-y: auto;
  overflow-x: hidden;
  z-index: 10;
  max-height: calc(100vh - 140px);
  border: 2px solid rgba(147, 197, 253, 0.3);
}

.gdmap__panel--left {
  left: 20px;
  top: 20px;
  width: 360px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.gdmap__panel--collapsed {
  width: 56px;
  gap: 12px;
}

.gdmap__panel--collapsed .panel__content {
  opacity: 0;
  transform: scale(0.95);
  pointer-events: none;
}

.panel__content {
  transition: all 0.3s ease;
  opacity: 1;
  transform: scale(1);
}

.btn-toggle {
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  color: #3b82f6;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 36px;
  min-height: 36px;
}

.btn-toggle:hover {
  background: #dbeafe;
  color: #0369a1;
  transform: scale(1.1);
}

/* 路线信息卡片 */
.route-info-card {
  position: absolute;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 15;
  background: linear-gradient(135deg, #ffffff 0%, #f0f9ff 100%);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  padding: 20px;
  min-width: 320px;
  box-shadow: 0 8px 32px rgba(59, 130, 246, 0.2), 0 0 0 1px rgba(147, 197, 253, 0.3);
  border: 2px solid rgba(147, 197, 253, 0.5);
  animation: slideDown 0.4s ease;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateX(-50%) translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateX(-50%) translateY(0);
  }
}

.route-info-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.route-info-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #0369a1;
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-close-card {
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 6px;
  border-radius: 6px;
  color: #64748b;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-close-card:hover {
  background: #fee2e2;
  color: #dc2626;
}

.route-info-body {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.route-info-item {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  border: 2px solid #93c5fd;
  border-radius: 12px;
  padding: 14px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.route-info-item svg {
  color: #3b82f6;
  flex-shrink: 0;
}

.route-info-item div {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.route-info-item span {
  font-size: 12px;
  font-weight: 700;
  color: #0c4a6e;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.route-info-item strong {
  font-size: 20px;
  font-weight: 700;
  color: #0369a1;
  letter-spacing: -0.5px;
}

.gdmap__panel--right {
  right: 20px;
  top: 20px;
  width: 340px;
  animation: slideInRight 0.3s ease;
}

@keyframes slideInRight {
  from {
    opacity: 0;
    transform: translateX(20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.panel__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 2px solid #f3f4f6;
}

.panel__title {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #667eea;
}

.panel__title h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  background: linear-gradient(135deg, #3b82f6 0%, #06b6d4 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: -0.3px;
}

.panel__title svg {
  flex-shrink: 0;
}

.btn-close {
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  color: #6b7280;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-close:hover {
  background: #f3f4f6;
  color: #111827;
}

.muted {
  color: #111827;
  font-size: 14px;
  line-height: 1.6;
  font-weight: 600;
}

.panel__warn {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  border: 2px solid #f59e0b;
  border-radius: 10px;
  padding: 12px 16px;
  font-size: 14px;
  font-weight: 700;
  color: #92400e;
  line-height: 1.5;
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.2);
}

.panel__warn--danger {
  background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%);
  border-color: #ef4444;
  color: #7f1d1d;
  box-shadow: 0 2px 8px rgba(239, 68, 68, 0.2);
}

.panel__section {
  border-top: 1px solid #f3f4f6;
  padding-top: 16px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 16px;
  font-size: 17px;
  font-weight: 700;
  color: #0ea5e9;
  letter-spacing: -0.2px;
}

.section-title,
.panel__section h3,
.panel__section h4 {
  color: #0ea5e9 !important;
  font-weight: 700 !important;
}

.section-title svg {
  color: #3b82f6;
  flex-shrink: 0;
}

.empty-state {
  text-align: center;
  padding: 32px 24px;
  color: #1f2937;
  font-size: 15px;
  font-weight: 600;
  background: #f9fafb;
  border-radius: 12px;
  border: 2px dashed #d1d5db;
}

/* Actions */
.actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin: 16px 0;
}

.actions .btn {
  flex: 1;
  min-width: 120px;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: none;
  padding: 10px 18px;
  border-radius: 10px;
  cursor: pointer;
  font-size: 15px;
  font-weight: 600;
  transition: all 0.2s;
  white-space: nowrap;
  letter-spacing: -0.2px;
}

.btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.btn--primary {
  background: linear-gradient(135deg, #3b82f6 0%, #06b6d4 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

.btn--primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(59, 130, 246, 0.5);
}

.btn--success {
  background: linear-gradient(135deg, #10b981 0%, #14b8a6 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.4);
}

.btn--success:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(16, 185, 129, 0.5);
}

.btn--secondary {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  color: #0284c7;
  font-weight: 700;
  border: 2px solid #bae6fd;
}

.btn--secondary:hover:not(:disabled) {
  background: linear-gradient(135deg, #e0f2fe 0%, #bae6fd 100%);
  border-color: #7dd3fc;
  transform: translateY(-1px);
}

.btn--danger {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(239, 68, 68, 0.25);
}

.btn--danger:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(239, 68, 68, 0.4);
}

.btn-icon {
  background: transparent;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 6px;
  cursor: pointer;
  color: #6b7280;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-icon:hover {
  background: #f3f4f6;
  border-color: #d1d5db;
  color: #111827;
}

/* Marker List */
.marker-list {
  list-style: none;
  padding: 0;
  margin: 12px 0 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 300px;
  overflow-y: auto;
}

.marker-item {
  background: linear-gradient(135deg, #ffffff 0%, #f0f9ff 100%);
  border: 2px solid #bae6fd;
  border-radius: 12px;
  padding: 14px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  transition: all 0.2s;
  box-shadow: 0 2px 4px rgba(59, 130, 246, 0.08);
}

.marker-item:hover {
  border-color: #3b82f6;
  background: linear-gradient(135deg, #f0f9ff 0%, #dbeafe 100%);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.2);
  transform: translateX(4px);
}

.marker-item.selected {
  border-color: #3b82f6;
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.3);
}

.marker-meta {
  flex: 1;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.marker-meta strong {
  font-size: 16px;
  font-weight: 700;
  color: #0369a1;
  letter-spacing: -0.2px;
  display: block;
  margin-bottom: 4px;
}

.marker-meta .coord {
  font-size: 13px;
  color: #0c4a6e;
  font-family: 'SF Mono', 'Consolas', monospace;
  font-weight: 600;
}

.marker-meta .marker-addr {
  font-size: 14px;
  color: #0284c7;
  line-height: 1.4;
  font-weight: 500;
}

.marker-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

/* Route Planning */
.route-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 10px 0;
}

.route-row label {
  width: 50px;
  color: #111827;
  font-size: 14px;
  font-weight: 700;
}

.route-row .select,
.select {
  flex: 1;
  padding: 10px 14px;
  border-radius: 10px;
  border: 2px solid #bae6fd;
  font-size: 14px;
  font-weight: 600;
  color: #0c4a6e;
  background: linear-gradient(135deg, #ffffff 0%, #f0f9ff 100%);
  cursor: pointer;
  transition: all 0.2s;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%233b82f6' d='M6 9L1 4h10z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
  padding-right: 36px;
}

.route-row .select:hover,
.select:hover {
  border-color: #3b82f6;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.2);
  transform: translateY(-1px);
}

.route-row .select:focus,
.select:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.15);
}

.route-summary {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin: 16px 0;
}

.summary-card {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  border: 2px solid #93c5fd;
  border-radius: 10px;
  padding: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.summary-card svg {
  color: #3b82f6;
  flex-shrink: 0;
}

.summary-card div {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.summary-card span {
  color: #0c4a6e;
  font-size: 13px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.summary-card strong {
  font-size: 18px;
  color: #0369a1;
  font-weight: 700;
  letter-spacing: -0.3px;
}

/* Route Steps Panel */
.route-steps-panel {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  width: 90%;
  max-width: 600px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.15);
  z-index: 10;
  max-height: 300px;
  display: flex;
  flex-direction: column;
  animation: slideInUp 0.3s ease;
}

@keyframes slideInUp {
  from {
    opacity: 0;
    transform: translateX(-50%) translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateX(-50%) translateY(0);
  }
}

.steps-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  border-bottom: 2px solid #f3f4f6;
}

.steps-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  display: flex;
  align-items: center;
  gap: 8px;
}

.steps-header h3 svg {
  color: #667eea;
}

.steps-list {
  list-style: none;
  padding: 0;
  margin: 0;
  overflow-y: auto;
  max-height: 240px;
}

.step-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid #f3f4f6;
  transition: background 0.2s;
}

.step-item:hover {
  background: #f9fafb;
}

.step-item:last-child {
  border-bottom: none;
}

.step-index {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
}

.step-text {
  flex: 1;
  font-size: 14px;
  color: #111827;
  line-height: 1.5;
  font-weight: 500;
}

/* Info Panel */
.info-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.info-field label {
  font-size: 12px;
  font-weight: 700;
  color: #374151;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.info-value {
  margin: 0;
  padding: 10px 12px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  color: #111827;
  line-height: 1.5;
}

.info-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 8px;
  padding-top: 16px;
  border-top: 2px solid #f3f4f6;
}

.info-actions .btn {
  width: 100%;
  justify-content: center;
}

/* Modal */
.modal__mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 50;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.modal {
  width: 90%;
  max-width: 480px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.3);
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  animation: scaleIn 0.3s ease;
}

@keyframes scaleIn {
  from {
    opacity: 0;
    transform: scale(0.9);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.modal h3 {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 700;
  color: #111827;
  letter-spacing: -0.3px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.modal h3::before {
  content: '📍';
  font-size: 24px;
}

.modal__coords {
  margin: 0 0 20px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #eef2ff 0%, #e0e7ff 100%);
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  color: #4338ca;
  border: 2px solid #c7d2fe;
  font-family: 'SF Mono', 'Consolas', monospace;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.modal__label {
  font-size: 14px;
  font-weight: 700;
  color: #111827;
  letter-spacing: -0.2px;
  margin-bottom: 2px;
}

.modal__input {
  border: 2px solid #e5e7eb;
  border-radius: 10px;
  padding: 12px 16px;
  font-size: 15px;
  font-weight: 500;
  color: #111827;
  background: #f9fafb;
  transition: all 0.2s;
}

.modal__input::placeholder {
  color: #9ca3af;
  font-weight: 400;
}

.modal__input:focus {
  outline: none;
  border-color: #667eea;
  background: #ffffff;
  box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1);
  transform: translateY(-1px);
}

.modal__input:disabled {
  background: #f3f4f6;
  color: #6b7280;
  cursor: not-allowed;
}

/* Responsive */
@media (max-width: 768px) {
  .gdmap__panel--left {
    width: 280px;
    left: 10px;
    top: 10px;
  }
  
  .gdmap__panel--right {
    right: 10px;
    top: 10px;
    width: 280px;
  }
  
  .route-steps-panel {
    width: 95%;
  }
}

/* 提示文字美化 */
.panel__section > p {
  color: #111827;
  font-size: 14px;
  line-height: 1.6;
  font-weight: 600;
  margin: 8px 0;
}

.panel__section > p:first-of-type {
  margin-top: 0;
}
</style>
