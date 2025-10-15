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

    <!-- Side panel (draggable + collapsible) -->
    <aside
      v-show="!isPanelCollapsed"
      ref="panelRef"
      class="gdmap__panel gdmap__panel--left"
      :style="panelStyle"
      @mousedown.self="startDragPanel"
    >
      <header class="panel__header" @mousedown.prevent="startDragPanel">
        <h2 class="font">地图工具</h2>
        <div class="panel__right">
          <small class="muted">当前位置：<span v-if="currentPlaceName">{{ currentPlaceName }}</span><span v-else>定位中…</span></small>
          <button class="btn-toggle" @click="togglePanel" title="折叠">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M19 12H5" stroke-linecap="round"/>
              <path d="M12 5l-7 7 7 7" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
        </div>
      </header>

      

      <section class="panel__section">
        <h3>标记点</h3>
        <p class="muted">单击地图添加标记，或清空除“当前位置”的标记。</p>
        <div class="actions">
          <button class="btn btn--danger" @click="clearMarkers" :disabled="markers.length <= 1">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 6h18"/><path d="M8 6v14a2 2 0 0 0 2 2h4a2 2 0 0 0 2-2V6"/><path d="M10 11v6M14 11v6"/></svg>
            清空标记
          </button>
        </div>
        <ul class="marker-list" v-if="markers.length">
          <li v-for="m in markers" :key="m.id" class="marker-item">
            <div class="marker-meta" @click="flyTo(m.lnglat)">
              <strong>{{ m.name }}</strong>
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
          <select v-model="originId" class="select">
            <option :value="''">未选择</option>
            <option v-for="m in markers" :key="m.id" :value="m.id">{{ m.name }}</option>
          </select>
        </div>
        <div class="route-row">
          <label>终点：</label>
          <select v-model="destinationId" class="select">
            <option :value="''">未选择</option>
            <option v-for="m in markers" :key="m.id" :value="m.id">{{ m.name }}</option>
          </select>
        </div>
        <div class="actions">
          <button class="btn btn--primary" @click="planWalkingRoute" :disabled="!originId || !destinationId">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18l6-6-6-6"/></svg>
            规划步行
          </button>
          <button class="btn btn--secondary" @click="clearRoute" :disabled="!routePlotted">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 4l16 16"/><path d="M20 4L4 20"/></svg>
            清除路线
          </button>
        </div>
        <div class="route-summary" v-if="routeSummary">
          <div class="summary-item"><span>距离</span><strong>{{ routeSummary.distanceText }}</strong></div>
          <div class="summary-item"><span>预计时间</span><strong>{{ routeSummary.durationText }}</strong></div>
        </div>
        <div class="route-steps">
          <p class="muted">提示：选择起点与终点后点击“规划步行”。详细走法可在卡片中点击“路线具体详情”。</p>
        </div>
      </section>

    </aside>

    <!-- 路线信息卡片（可拖动，包含起终点） -->
    <div v-if="routePlotted && routeSummary" ref="routeCardRef" class="route-info-card" :style="{ top: routeCardPos.top + 'px', left: routeCardPos.left + 'px' }">
      <div class="route-info-header" @mousedown.prevent="startDrag">
        <h3>🚶 步行路线</h3>
        <button class="btn-close-card" @click="clearRoute" title="清除路线">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18"/>
            <line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </button>
      </div>
      <div class="route-endpoints">
        <div class="ep origin"><span class="dot dot--origin"></span><strong>起点</strong>：{{ originName }}</div>
        <div class="ep dest"><span class="dot dot--dest"></span><strong>终点</strong>：{{ destinationName }}</div>
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
      <div class="route-info-actions">
        <button class="btn btn--secondary" @click="showRouteDetails = true" :disabled="!routeSteps.length">路线具体详情</button>
      </div>
    </div>

    <!-- 路线具体详情卡片 -->
    <div v-if="showRouteDetails && routeSteps.length" class="route-steps-panel">
      <div class="steps-header">
        <h3>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right:6px;">
            <path d="M3 12h18M3 6h18M3 18h18"/>
          </svg>
          路线具体详情
        </h3>
        <button class="btn-icon" @click="showRouteDetails = false" title="关闭">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18"/>
            <line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </button>
      </div>
      <ul class="steps-list">
        <li v-for="s in routeSteps" :key="s.idx" class="step-item">
          <span class="step-index">{{ s.idx }}</span>
          <span class="step-text">{{ s.text || '按路况前行' }}</span>
        </li>
      </ul>
    </div>

    <!-- Map container -->
    <div ref="mapContainer" class="gdmap__container"></div>
    <div v-if="error" class="gdmap__error">{{ error }}</div>

    <!-- 折叠后的浮动按钮 -->
    <button v-if="isPanelCollapsed" class="panel-fab" @click="togglePanel" title="展开工具面板">
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M5 12h14" stroke-linecap="round"/>
        <path d="M12 19l7-7-7-7" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
    </button>

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
import { onMounted, onBeforeUnmount, ref, computed, nextTick } from 'vue'

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
    strokeOpacity?: number;
    isOutline?: boolean;
    outlineColor?: string;
    strokeStyle?: string;
    lineJoin?: string;
    lineCap?: string;
    [key: string]: any;
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
const currentPlaceName = ref('')
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

// 可拖动的路线信息卡片
const routeCardRef = ref<HTMLDivElement | null>(null)
const routeCardPos = ref({ top: 20, left: 20 })
let dragging = false
let dragStart = { x: 0, y: 0, left: 0, top: 0 }
// 路线详情显示开关
const showRouteDetails = ref(false)

// 侧边面板可拖拽
const panelRef = ref<HTMLDivElement | null>(null)
const panelPos = ref({ left: 20, top: 20 })
let panelDragging = false
let panelStart = { x: 0, y: 0, left: 20, top: 20 }
// 保存面板尺寸，折叠/展开时保持尺寸不变
const panelSize = ref<{ w: number; h: number } | null>(null)

const panelStyle = computed(() => {
  const style: Record<string, string> = {
    left: panelPos.value.left + 'px',
    top: panelPos.value.top + 'px',
  }
  if (panelSize.value?.w) style.width = panelSize.value.w + 'px'
  return style
})

// 地图工具状态：'marker' 或 'drag'
const mapTool = ref<'marker' | 'drag'>('drag')

function selectMarker(m: MarkerItem) {
  selectedMarkerId.value = m.id
  flyTo(m.lnglat)
}

function togglePanel() {
  if (!isPanelCollapsed.value) {
    // 即将折叠，记录当前尺寸
    const el = panelRef.value
    if (el) {
      const rect = el.getBoundingClientRect()
      panelSize.value = { w: Math.round(rect.width), h: Math.round(rect.height) }
    }
    isPanelCollapsed.value = true
  } else {
    // 展开，保持之前的尺寸
    isPanelCollapsed.value = false
    // 等待渲染后再根据容器边界微调位置
    nextTick(() => {
      const container = mapContainer.value
      const el = panelRef.value
      if (!container || !el) return
      const cw = container.clientWidth
      const ch = container.clientHeight
      const rect = el.getBoundingClientRect()
      const w = Math.round(panelSize.value?.w || rect.width)
      const h = Math.round(rect.height)
      let left = panelPos.value.left
      let top = panelPos.value.top
      left = Math.max(8, Math.min(cw - w - 8, left))
      top = Math.max(8, Math.min(ch - h - 8, top))
      panelPos.value = { left, top }
    })
  }
}

function startDragPanel(ev: MouseEvent) {
  panelDragging = true
  panelStart = { x: ev.clientX, y: ev.clientY, left: panelPos.value.left, top: panelPos.value.top }
  window.addEventListener('mousemove', onDragPanel)
  window.addEventListener('mouseup', stopDragPanel)
}
function onDragPanel(ev: MouseEvent) {
  if (!panelDragging) return
  const container = mapContainer.value
  const panel = panelRef.value
  if (!container || !panel) return
  const dx = ev.clientX - panelStart.x
  const dy = ev.clientY - panelStart.y
  const rect = panel.getBoundingClientRect()
  const cw = container.clientWidth
  const ch = container.clientHeight
  const w = rect.width
  const h = rect.height
  let left = panelStart.left + dx
  let top = panelStart.top + dy
  left = Math.max(8, Math.min(cw - w - 8, left))
  top = Math.max(8, Math.min(ch - h - 8, top))
  panelPos.value = { left, top }
}
function stopDragPanel() {
  panelDragging = false
  window.removeEventListener('mousemove', onDragPanel)
  window.removeEventListener('mouseup', stopDragPanel)
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
      let marker: any = null
      try {
        marker = new AMap.Marker({ 
          position: cached.lnglat, 
          title: '我的位置',
          icon: markerIcon('#10b981') as any
        })
        mapInstance!.add(marker)
      } catch (e) {}
      try {
        const geo = await reverseGeocode(cached.lnglat)
        currentPlaceName.value = geo.name
      } catch {}
      // 添加到列表并设为默认起点
      const id = `${cached.lnglat[0]}_${cached.lnglat[1]}_${Date.now()}`
  markers.value.push({ id, name: '我的位置', lnglat: cached.lnglat, overlay: marker, address: currentPlaceName.value })
      originId.value = id
    } else if ('geolocation' in navigator) {
      // 首次加载时获取位置并缓存
      navigator.geolocation.getCurrentPosition(
        async (pos) => {
          const { longitude, latitude } = pos.coords
          const lnglat: [number, number] = [longitude, latitude]
          currentLngLat.value = lnglat
          setCachedLocation(lnglat) // 缓存位置
          mapInstance!.setZoomAndCenter(15, lnglat)
          let marker: any = null
          try {
            marker = new AMap.Marker({ 
              position: lnglat, 
              title: '我的位置',
              icon: markerIcon('#10b981') as any
            })
            mapInstance!.add(marker)
          } catch (e) {}
          try {
            const geo = await reverseGeocode(lnglat)
            currentPlaceName.value = geo.name
          } catch {}
          const id = `${lnglat[0]}_${lnglat[1]}_${Date.now()}`
          markers.value.push({ id, name: '我的位置', lnglat, overlay: marker, address: currentPlaceName.value })
          originId.value = id
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
function markerIcon(color: string) {
  const svg = `<svg xmlns='http://www.w3.org/2000/svg' width='34' height='34' viewBox='0 0 24 24' fill='${color}' stroke='${color}' stroke-width='1.2'><path d='M21 10c0 7-9 13-9 13S3 17 3 10a9 9 0 1118 0z'/><circle cx='12' cy='10' r='3' fill='white' stroke='white' stroke-width='1.2'/></svg>`
  return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`
}
function addMarker(lnglat: [number, number], name?: string, address?: string) {
  if (!mapInstance || !AMapRef) return
  const id = `${lnglat[0]}_${lnglat[1]}_${Date.now()}`
  const displayName = name || `标记 ${markers.value.length + 1}`
  let overlay: any
  try {
    // 根据是否为起点/终点/普通标记选择颜色
    const role = getRoleForCoords(lnglat)
    const iconColor = role === 'origin' ? '#10b981' : role === 'destination' ? '#ef4444' : '#3b82f6'
  const svg = markerIcon(iconColor)
    overlay = new AMapRef.Marker({ 
      position: lnglat, 
      title: displayName,
      icon: svg as any,
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

function clearMarkers() {
  if (!mapInstance) return
  // 仅保留“当前位置”（认为是 markers 中第一个，且名称含“我的位置”或等于 currentPlaceName）
  const keep = markers.value.find(m => m.name === '我的位置' || m.name === currentPlaceName.value)
  // 从地图上移除其余覆盖物
  markers.value.forEach(m => {
    if (keep && m.id === keep.id) return
    try { (mapInstance as any).remove && (mapInstance as any).remove(m.overlay) } catch {}
  })
  markers.value = keep ? [keep] : []
  // 同时重置起点为当前位置
  if (keep) originId.value = keep.id
}

function removeMarker(id: string) {
  const idx = markers.value.findIndex((m) => m.id === id)
  if (idx >= 0) {
    const m = markers.value[idx]
    if (m) {
      try { (mapInstance as any)?.remove?.(m.overlay) } catch {}
      markers.value.splice(idx, 1)
    }
  }
}

function flyTo(lnglat: [number, number]) {
  mapInstance?.setZoomAndCenter(16, lnglat)
}

function setAs(type: 'origin' | 'destination', m: MarkerItem) {
  if (type === 'origin') originId.value = m.id
  else destinationId.value = m.id
  // 更新对应标记颜色
  recolorMarkers()
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
        strokeWeight: 6,
        strokeOpacity: 0.95,
        isOutline: true as any,
        outlineColor: '#93c5fd' as any,
        strokeStyle: 'solid',
        lineJoin: 'round',
        lineCap: 'round',
      })
      try { (mapInstance as any).add(routePolyline) } catch {}
      // 在地图上标注起点/终点名称
      try {
        addTextLabel(origin.lnglat, origin.name, 'origin')
        addTextLabel(dest.lnglat, dest.name, 'destination')
      } catch {}
      // 自动适配路线并折叠左侧面板
      try { 
        (mapInstance as any).setFitView && (mapInstance as any).setFitView([routePolyline], false, [100, 60, 60, 60])
      } catch {}
      isPanelCollapsed.value = true
      await nextTick()
      positionRouteCard()
    }
    routePlotted.value = true
    showRouteDetails.value = false
  } catch (e) {
    // 静默失败，不展示代理错误到前端
    console.warn('步行路线规划失败：', e)
    routePlotted.value = false
    routeSummary.value = null
    error.value = null
  }
}

function clearRoute() {
  if (walking) {
    try { walking.clear() } catch {}
  }
  routePlotted.value = false
  routeSummary.value = null
  routeSteps.value = []
  showRouteDetails.value = false
  if (routePolyline && mapInstance) {
    try { (mapInstance as any).remove && (mapInstance as any).remove(routePolyline) } catch {}
    routePolyline = null
  }
  // 移除文本标签
  cleanupTextLabels()
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

function positionRouteCard() {
  const card = routeCardRef.value
  const container = mapContainer.value
  if (!card || !container) return
  const cw = container.clientWidth
  const ch = container.clientHeight
  const rect = card.getBoundingClientRect()
  const w = rect.width
  const h = rect.height
  routeCardPos.value = { left: Math.max(8, Math.min(cw - w - 8, Math.floor((cw - w) / 2))), top: 20 }
}

function startDrag(ev: MouseEvent) {
  dragging = true
  dragStart = { x: ev.clientX, y: ev.clientY, left: routeCardPos.value.left, top: routeCardPos.value.top }
  window.addEventListener('mousemove', onDrag)
  window.addEventListener('mouseup', stopDrag)
}

function onDrag(ev: MouseEvent) {
  if (!dragging) return
  const container = mapContainer.value
  const card = routeCardRef.value
  if (!container || !card) return
  const dx = ev.clientX - dragStart.x
  const dy = ev.clientY - dragStart.y
  const rect = card.getBoundingClientRect()
  const cw = container.clientWidth
  const ch = container.clientHeight
  const w = rect.width
  const h = rect.height
  let left = dragStart.left + dx
  let top = dragStart.top + dy
  left = Math.max(8, Math.min(cw - w - 8, left))
  top = Math.max(8, Math.min(ch - h - 8, top))
  routeCardPos.value = { left, top }
}

function stopDrag() {
  dragging = false
  window.removeEventListener('mousemove', onDrag)
  window.removeEventListener('mouseup', stopDrag)
}

// 计算当前起终点名称（用于信息卡显示）
const originName = computed(() => markers.value.find(m => m.id === originId.value)?.name || '—')
const destinationName = computed(() => markers.value.find(m => m.id === destinationId.value)?.name || '—')

// 工具：基于坐标判断角色（起点/终点/普通）
function getRoleForCoords(lnglat: [number, number]) {
  const o = markers.value.find(m => m.id === originId.value)
  const d = markers.value.find(m => m.id === destinationId.value)
  const eq = (a?: [number, number], b?: [number, number]) => !!a && !!b && Math.abs(a[0]-b[0])<1e-8 && Math.abs(a[1]-b[1])<1e-8
  if (eq(o?.lnglat, lnglat)) return 'origin'
  if (eq(d?.lnglat, lnglat)) return 'destination'
  return 'normal'
}

// 根据角色为标记着色
function recolorMarkers() {
  if (!AMapRef) return
  markers.value.forEach(m => {
    const role = m.id === originId.value ? 'origin' : m.id === destinationId.value ? 'destination' : 'normal'
    const color = role === 'origin' ? '#10b981' : role === 'destination' ? '#ef4444' : '#3b82f6'
    try {
      const svg = markerIcon(color)
      ;(m.overlay as any)?.setIcon?.(svg)
    } catch {}
  })
}

// 地图文本标签
let textLabels: any[] = []
function addTextLabel(lnglat: [number, number], text: string, role: 'origin' | 'destination') {
  if (!AMapRef || !mapInstance) return
  const color = role === 'origin' ? '#10b981' : '#ef4444'
  const label = new (AMapRef as any).Text({
    text,
    position: lnglat,
    style: {
      'background-color': '#ffffff',
      'border': `2px solid ${color}`,
      'border-radius': '8px',
      'padding': '4px 8px',
      'color': '#111827',
      'font-weight': '700'
    },
    offset: new (AMapRef as any).Pixel(0, -30)
  })
  try { (mapInstance as any).add(label); textLabels.push(label) } catch {}
}
function cleanupTextLabels() {
  if (!mapInstance || !textLabels.length) return
  try { (mapInstance as any).remove(textLabels) } catch {}
  textLabels = []
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
  max-height: calc(100vh - 60px);
  border: 2px solid rgba(147, 197, 253, 0.3);
}

.gdmap__panel--left {
  /* left/top 由内联 style 控制用于拖拽定位 */
  width: auto;
  min-width: 360px;
  max-width: 560px;
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
  left: 20px;
  transform: none;
  z-index: 15;
  background: linear-gradient(135deg, #ffffff 0%, #f0f9ff 100%);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  padding: 20px;
  min-width: 320px;
  box-shadow: 0 8px 32px rgba(59, 130, 246, 0.2), 0 0 0 1px rgba(147, 197, 253, 0.3);
  border: 2px solid rgba(147, 197, 253, 0.5);
  animation: slideDown 0.4s ease;
  user-select: none;
  cursor: grab;
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

.route-info-actions {
  margin-top: 14px;
}

.route-endpoints {
  display: grid;
  grid-template-columns: 1fr;
  gap: 6px;
  margin-bottom: 10px;
}
.route-endpoints .ep { display: flex; align-items: center; gap: 8px; font-weight: 600; color: #0c4a6e; }
.route-endpoints .dot { width: 10px; height: 10px; border-radius: 50%; display: inline-block; }
.route-endpoints .dot--origin { background: #10b981; }
.route-endpoints .dot--dest { background: #ef4444; }

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
  align-items: center;
  margin-bottom: 6px;
  padding-bottom: 6px;
  border-bottom: 2px solid #440bbe;
}

.panel__header h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #e90e28;
  display: flex;
  align-items: center;
  gap: 8px;
}

.panel__right {
  display: flex;
  align-items: center;
  gap: 50px;
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
  background: linear-gradient(135deg, #3b82f6 0%, #1fc1dd 100%);
  -webkit-background-clip: text;
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

/* 折叠后的浮动按钮 */
.panel-fab {
  position: absolute;
  left: 20px;
  top: 20px;
  z-index: 12;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  border: 2px solid #93c5fd;
  background: linear-gradient(135deg, #ffffff 0%, #f0f9ff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #2563eb;
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.25);
  cursor: pointer;
  transition: transform .2s, box-shadow .2s;
}
.panel-fab:hover { transform: scale(1.05); box-shadow: 0 8px 28px rgba(59,130,246,.35); }

/* Marker List */
.marker-list {
  list-style: none;
  padding: 0;
  margin: 12px 0 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: clamp(240px, 42vh, 460px);
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
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.marker-meta .coord {
  font-size: 13px;
  color: #0c4a6e;
  font-family: 'SF Mono', 'Consolas', monospace;
  font-weight: 600;
}

.marker-meta .marker-addr {
  font-size: 12px;
  color: #0c4a6e;
  line-height: 1.5;
  font-weight: 500;
  display: -webkit-box;
  line-clamp: 2;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
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
  width: 92%;
  max-width: 680px;
  background: linear-gradient(135deg, #ffffff 0%, #f0f9ff 100%);
  border: 2px solid rgba(147, 197, 253, 0.5);
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(59, 130, 246, 0.15), 0 0 0 1px rgba(59, 130, 246, 0.1);
  z-index: 12;
  max-height: 360px;
  display: flex;
  flex-direction: column;
  animation: slideInUp 0.3s ease;
  overflow: hidden;
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
  border-bottom: 2px solid #e5effe;
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
  max-height: 280px;
}

.step-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid #e5effe;
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
  background: linear-gradient(135deg, #3b82f6 0%, #06b6d4 100%);
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
  color: #0c4a6e;
  line-height: 1.5;
  font-weight: 500;
}

/* Small button variant used in lists */
.btn--sm {
  padding: 6px 10px;
  font-size: 13px;
  border-radius: 8px;
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
    width: 300px;
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
