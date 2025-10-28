<template>
  <div class="merchant-restaurant">
    <div class="content-scroll">
      <div class="layout layout--top">
        <section class="card summary-card">
          <div class="summary-header">
            <div class="summary-title">
              <h1>{{ form.name || '我的餐馆' }}</h1>
              <p class="summary-address" v-if="form.address">{{ form.address }}</p>
              <p class="summary-address summary-address--empty" v-else>请完善餐馆地址信息</p>
            </div>
          </div>
          <div class="summary-stats">
            <div class="stat">
              <div class="stat-icon stat-icon--rating">
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M12 2.5l2.67 5.42 6 0.87-4.34 4.23 1.03 5.98L12 16.94l-5.36 2.83 1.03-5.98L3.33 8.79l6-0.87L12 2.5z" />
                </svg>
              </div>
              <div class="stat-info">
                <span class="label">综合评分</span>
                <div class="value">
                  <strong>{{ avgRatingDisplay }}</strong>
                  <span class="stars">
                    <i v-for="star in 5" :key="star" :class="['icon-star', { 'icon-star--filled': star <= Math.round(summary.avgRating) }]" />
                  </span>
                </div>
              </div>
            </div>
            <div class="stat">
              <div class="stat-icon stat-icon--review">
                <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M21 15a2 2 0 0 1-2 2H8l-4 4V5a2 2 0 0 1 2-2h13a2 2 0 0 1 2 2z" />
                </svg>
              </div>
              <div class="stat-info">
                <span class="label">评论数量</span>
                <div class="value">
                  <strong>{{ summary.reviewCount }}</strong>
                </div>
              </div>
            </div>
            <div class="stat">
              <div class="stat-icon stat-icon--like">
                <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
                </svg>
              </div>
              <div class="stat-info">
                <span class="label">点赞总数</span>
                <div class="value">
                  <strong>{{ summary.likeCount }}</strong>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section v-if="highlight" class="card highlight-card">
          <header>
            <div>
              <h3>{{ highlight.user?.nickname || highlight.user?.username || '匿名用户' }}</h3>
              <p class="muted">{{ formatDate(highlight.createdAt) }}</p>
            </div>
            <span class="pill">精选评论</span>
          </header>
          <p class="content">{{ highlight.content }}</p>
          <footer>
            <span class="stars">
              <i v-for="star in 5" :key="star" :class="['icon-star', { 'icon-star--filled': star <= highlight.rating }]" />
            </span>
            <span class="likes">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
              </svg>
              {{ highlight.likes }}
            </span>
          </footer>
        </section>
      </div>

      <div class="layout layout--main">
        <div class="col col--main">
        <div class="card form-card">
            <header class="card-header">
              <div>
                <h2>餐馆信息</h2>
                <p class="muted">完善餐馆基础资料与介绍，这些信息将展示在用户端。</p>
              </div>
            </header>

          <div class="form-grid">
            <div class="form-group">
              <label>餐馆名称 <span class="required">*</span></label>
              <input v-model.trim="form.name" placeholder="请输入餐馆名称" />
            </div>

            <div class="form-group">
              <label>餐馆地址 <span class="required">*</span></label>
              <div class="font-yellow">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="" stroke="currentColor" stroke-width="2">
                  <path d="M12 21s-6-4.35-6-10a6 6 0 1 1 12 0c0 5.65-6 10-6 10z" />
                  <circle cx="12" cy="11" r="2.5" />
                </svg>
                <span>{{ form.address || '尚未定位' }}</span>
              </div>
            </div>

            <div class="form-group form-group--row">
              <div>
                <label>经度 <span class="required">*</span></label>
                <div class="font-yellow">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M2 12h20" />
                    <path d="M12 2v20" />
                  </svg>
                  <span>{{ form.lng != null ? form.lng : '—' }}</span>
                </div>
              </div>
              <div>
                <label>纬度 <span class="required">*</span></label>
                <div class="font-yellow">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M2 12h20" />
                    <path d="M12 2v20" />
                  </svg>
                  <span>{{ form.lat != null ? form.lat : '—' }}</span>
                </div>
              </div>
            </div>

            <div class="form-group">
              <label>搜索地点</label>
              <div class="search-row">
                <input v-model.trim="searchQuery" placeholder="请输入地标或地址进行搜索" @keydown.enter.prevent="searchLocation" />
                <button class="btn btn--ghost" type="button" @click="searchLocation">搜索</button>
              </div>
            </div>

            <div class="form-group form-group--wide">
              <label>餐馆介绍</label>
              <textarea v-model.trim="form.description" rows="4" placeholder="可选：介绍菜品特色、服务亮点、就餐须知等信息" />
            </div>

            <div class="form-group form-group--wide">
              <label>餐馆图片</label>
              <div class="upload-row">
                <div class="preview" v-if="form.photoUrl">
                  <img :src="form.photoUrl" alt="餐馆图片" />
                </div>
                <div class="preview preview--placeholder" v-else>
                  <span>暂未上传图片</span>
                </div>
                <div class="upload-actions">
                  <input ref="fileInput" type="file" accept="image/*" hidden @change="onFileChange" />
                  <button class="btn btn--ghost" type="button" @click="triggerFile">选择图片</button>
                  <p class="muted">支持图片格式，推荐尺寸 800×600 以上，文件不超过 5MB。</p>
                </div>
              </div>
            </div>
          </div>

          <transition name="fade">
            <div v-if="message.show" :class="['alert', `alert--${message.type}`]">{{ message.text }}</div>
          </transition>

          <footer class="form-footer">
            <div class="footer-tip">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 9v4" />
                <circle cx="12" cy="17" r="1" />
                <path d="M21 16.5a9 9 0 1 0-7.5 7.45" />
              </svg>
              <span>确认定位后再保存信息，确保用户端展示准确。</span>
            </div>
            <button class="btn btn--primary form-save" :disabled="isSaving" @click="onSave">
              <span v-if="isSaving">保存中…</span>
              <span v-else>保存信息</span>
            </button>
          </footer>
        </div>

  <div class="card reviews-card">
          <header class="card-header">
            <div>
              <h2>用户评价</h2>
              <p class="muted">查看用户的真实反馈，点击卡片可以展开详情，支持点赞互动。</p>
            </div>
          </header>

          <div v-if="reviewState.loading" class="loading-block">正在加载评论…</div>
          <div v-else-if="!reviewState.list.length" class="empty-block">暂无评论，鼓励用户完成首条评价吧。</div>
          <ul v-else class="review-list">
            <li v-for="item in reviewState.list" :key="item.id" class="review-item">
              <div class="review-header" @click="toggleExpand(item.id)">
                <div class="review-meta">
                  <span
                    class="avatar"
                    :style="avatarStyle(item.user?.avatarUrl)"
                    role="button"
                    tabindex="0"
                    @click.stop="openUserProfile(item)"
                    @keydown.enter.stop.prevent="openUserProfile(item)"
                    @keydown.space.stop.prevent="openUserProfile(item)"
                  >
                    {{ avatarInitial(item.user?.nickname || item.user?.username) }}
                  </span>
                  <div class="review-meta__info">
                    <p
                      class="name"
                      role="button"
                      tabindex="0"
                      @click.stop="openUserProfile(item)"
                      @keydown.enter.stop.prevent="openUserProfile(item)"
                      @keydown.space.stop.prevent="openUserProfile(item)"
                    >
                      {{ item.user?.nickname || item.user?.username || '匿名用户' }}
                    </p>
                    <p class="time">{{ formatDate(item.createdAt) }}</p>
                  </div>
                </div>
                <div class="review-rating">
                  <span class="stars">
                    <i v-for="star in 5" :key="star" :class="['icon-star', { 'icon-star--filled': star <= item.rating }]" />
                  </span>
                  <span class="score">{{ item.rating.toFixed(1) }}</span>
                </div>
              </div>

              <p class="review-content">{{ item.content }}</p>

              <transition name="expand">
                <div v-if="expanded.has(item.id) && item.detail" class="review-detail">
                  <pre>{{ item.detail }}</pre>
                </div>
              </transition>

              <footer class="review-footer">
                <button class="btn-like" :class="{ liked: item.liked }" @click.stop="toggleLike(item)">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
                  </svg>
                  <span>{{ item.likes }}</span>
                </button>
                <button class="btn-more" type="button" @click="toggleExpand(item.id)">
                  {{ expanded.has(item.id) ? '收起详情' : '展开详情' }}
                </button>
              </footer>
            </li>
          </ul>

          <div v-if="reviewState.total > reviewState.size" class="paginator">
            <button class="btn btn--ghost" :disabled="reviewState.page === 1" @click="changePage(reviewState.page - 1)">上一页</button>
            <span class="page-info">第 {{ reviewState.page }} / {{ totalPages }} 页</span>
            <button class="btn btn--ghost" :disabled="reviewState.page >= totalPages" @click="changePage(reviewState.page + 1)">下一页</button>
          </div>
        </div>
        </div>

        <div class="col col--side">
          <div class="card map-card">
            <header class="card-header">
              <div>
                <h2>地图定位</h2>
                <p class="muted">点击地图可更新坐标，拖拽标记微调位置。</p>
              </div>
            </header>
            <div class="map-wrapper">
              <div ref="mapContainer" class="map-container"></div>
              <transition name="fade">
                <div v-if="pendingLocation.visible" class="map-confirm">
                  <div class="map-confirm__title">确定要定在此处？</div>
                  <div v-if="pendingLocation.placeName" class="map-confirm__place">{{ pendingLocation.placeName }}</div>
                  <div class="map-confirm__coords">
                    <span>经度：{{ pendingLocation.lng }}</span>
                    <span>纬度：{{ pendingLocation.lat }}</span>
                  </div>
                  <div v-if="pendingLocation.addressHint" class="map-confirm__address">{{ pendingLocation.addressHint }}</div>
                  <div class="map-confirm__actions">
                    <button class="btn btn--ghost" type="button" @click="cancelPendingLocation">取消</button>
                    <button class="btn btn--primary" type="button" @click="confirmPendingLocation">确定</button>
                  </div>
                </div>
              </transition>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  uploadRestaurantPhoto,
  getRestaurantProfile,
  saveRestaurantProfile,
  getRestaurantReviews,
  toggleRestaurantReviewLike,
  type RestaurantProfile,
  type RestaurantProfileResponse,
  type RestaurantReviewItem,
  type RestaurantSummaryInfo,
} from '@/api/restaurant'

type AMapSDK = any

declare global {
  interface Window {
    AMap?: AMapSDK
    _AMapSecurityConfig?: {
      securityJsCode?: string
    }
  }
}

const DEFAULT_CENTER: [number, number] = [121.490317, 31.241701]
const DEFAULT_AMAP_KEY = '218f3df7356ff0dcbc0635c0abf39893'
const DEFAULT_SECURITY_JS_CODE = '3b0d644aa49fcb13d08e0693933c6767'

const profile = ref<RestaurantProfile | null>(null)
const summary = ref<RestaurantSummaryInfo>({ reviewCount: 0, avgRating: 0, likeCount: 0 })
const highlight = ref<RestaurantReviewItem | null>(null)

const form = reactive({
  name: '',
  address: '',
  description: '',
  lng: null as number | null,
  lat: null as number | null,
  photoUrl: '' as string | null,
  photoFile: null as File | null,
})

const message = reactive({ show: false, text: '', type: 'success' as 'success' | 'error' })
const isSaving = ref(false)
const searchQuery = ref('')
const router = useRouter()

const reviewState = reactive({
  loading: false,
  list: [] as RestaurantReviewItem[],
  page: 1,
  size: 6,
  total: 0,
})

const expanded = reactive(new Set<number>())

const avgRatingDisplay = computed(() => summary.value.avgRating?.toFixed(1) ?? '0.0')
const totalPages = computed(() => (reviewState.total === 0 ? 1 : Math.ceil(reviewState.total / reviewState.size)))
const restaurantId = computed(() => profile.value?.id ?? null)

const mapContainer = ref<HTMLDivElement | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)

const API_FILE_BASE =
  import.meta.env.VITE_FILE_BASE ||
  (import.meta.env.DEV
    ? 'http://localhost:8080'
    : typeof window !== 'undefined'
      ? window.location.origin
      : '')

const pendingLocation = reactive({
  visible: false,
  lng: '',
  lat: '',
  addressHint: '',
  placeName: '',
  prevLng: null as number | null,
  prevLat: null as number | null,
})

let AMapRef: AMapSDK | null = null
let map: any = null
let marker: any = null
let geocoder: any = null
let previewMarker: any = null

function showMessage(text: string, type: 'success' | 'error' = 'success') {
  message.text = text
  message.type = type
  message.show = true
  setTimeout(() => (message.show = false), 2600)
}

function normalizeImageUrl(url?: string | null) {
  if (!url) return null
  if (/^(data:|https?:)/i.test(url)) return url
  const path = url.startsWith('/') ? url : `/${url}`
  if (!API_FILE_BASE) return path
  return `${API_FILE_BASE}${path}`
}

function avatarInitial(name?: string | null) {
  if (!name) return 'U'
  return name.slice(0, 1).toUpperCase()
}

function avatarStyle(url?: string | null) {
  const normalized = normalizeImageUrl(url)
  return normalized ? { backgroundImage: `url(${normalized})` } : {}
}

function formatDate(value?: string | null) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value.replace('T', ' ')
  }
  return date.toLocaleString()
}

async function loadAmap(): Promise<AMapSDK> {
  if (window.AMap) return window.AMap
  window._AMapSecurityConfig = { securityJsCode: DEFAULT_SECURITY_JS_CODE }
  return new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${DEFAULT_AMAP_KEY}`
    script.async = true
    script.onload = () => {
      if (window.AMap) resolve(window.AMap)
      else reject(new Error('AMap load error'))
    }
    script.onerror = () => reject(new Error('AMap load error'))
    document.head.appendChild(script)
  })
}

function clearMarker() {
  if (marker && map) {
    map.remove(marker)
    marker = null
  }
}

function hidePreviewMarker() {
  if (previewMarker && map) {
    map.remove(previewMarker)
    previewMarker = null
  }
}

function showPreviewMarker(lng: number, lat: number) {
  if (!map || !AMapRef) return
  if (!previewMarker) {
    previewMarker = new AMapRef.Marker({
      position: [lng, lat],
      anchor: 'bottom-center',
      icon: new AMapRef.Icon({
        size: new AMapRef.Size(30, 42),
        imageSize: new AMapRef.Size(30, 42),
        image: 'https://a.amap.com/jsapi_demos/static/demo-center/icons/poi-marker-default.png',
      }),
    })
    map.add(previewMarker)
  } else {
    previewMarker.setPosition([lng, lat])
  }
  map.setZoomAndCenter(16, [lng, lat])
}

function setMarkerPosition(lng: number, lat: number) {
  if (!map || !AMapRef) return
  hidePreviewMarker()
  if (!marker) {
    marker = new AMapRef.Marker({
      position: [lng, lat],
      draggable: true,
      anchor: 'bottom-center',
    })
    marker.on('dragend', (e: any) => {
      const newLng = e.lnglat?.lng ?? e.lnglat?.getLng?.()
      const newLat = e.lnglat?.lat ?? e.lnglat?.getLat?.()
      if (typeof newLng === 'number' && typeof newLat === 'number') {
        if (form.lng != null && form.lat != null) {
          marker.setPosition([form.lng, form.lat])
        }
        promptLocation(newLng, newLat)
      }
    })
    map.add(marker)
  }
  marker.setPosition([lng, lat])
  map.setZoomAndCenter(16, [lng, lat])
}

function reverseGeocode(lng: number | null, lat: number | null) {
  if (!geocoder || lng == null || lat == null) return
  geocoder.getAddress([lng, lat], (status: string, result: any) => {
    if (status === 'complete' && result?.regeocode?.formattedAddress) {
      form.address = result.regeocode.formattedAddress
    }
  })
}

function promptLocation(lng: number, lat: number, addressHint = '', placeName = '') {
  if (!map) return
  pendingLocation.prevLng = form.lng
  pendingLocation.prevLat = form.lat
  pendingLocation.lng = Number(lng).toFixed(6)
  pendingLocation.lat = Number(lat).toFixed(6)
  pendingLocation.addressHint = addressHint
  pendingLocation.placeName = placeName
  pendingLocation.visible = true
  showPreviewMarker(lng, lat)
}

function applyLocation(lng: number, lat: number, addressHint = '') {
  const normalizedLng = Number(lng.toFixed(6))
  const normalizedLat = Number(lat.toFixed(6))
  form.lng = normalizedLng
  form.lat = normalizedLat
  setMarkerPosition(normalizedLng, normalizedLat)
  if (addressHint) {
    form.address = addressHint
  } else {
    reverseGeocode(normalizedLng, normalizedLat)
  }
}

function confirmPendingLocation() {
  if (!pendingLocation.visible) return
  const lng = Number(pendingLocation.lng)
  const lat = Number(pendingLocation.lat)
  pendingLocation.visible = false
  hidePreviewMarker()
  applyLocation(lng, lat, pendingLocation.addressHint)
  showMessage('位置已更新', 'success')
}

function cancelPendingLocation() {
  if (!pendingLocation.visible) return
  pendingLocation.visible = false
  hidePreviewMarker()
  if (pendingLocation.prevLng != null && pendingLocation.prevLat != null) {
    setMarkerPosition(pendingLocation.prevLng, pendingLocation.prevLat)
  } else {
    clearMarker()
  }
}

async function ensureMap() {
  if (map) return
  await nextTick()
  if (!mapContainer.value) return
  AMapRef = await loadAmap()
  map = new AMapRef.Map(mapContainer.value, {
    zoom: 14,
    center: form.lng != null && form.lat != null ? [form.lng, form.lat] : DEFAULT_CENTER,
    viewMode: '2D',
  })
  AMapRef.plugin('AMap.Geocoder', () => {
    geocoder = new AMapRef.Geocoder({ extensions: 'all' })
  })
  map.on('click', (e: any) => {
    const lng = e.lnglat?.lng ?? e.lnglat?.getLng?.()
    const lat = e.lnglat?.lat ?? e.lnglat?.getLat?.()
    if (typeof lng === 'number' && typeof lat === 'number') {
      promptLocation(lng, lat)
    }
  })
  if (form.lng != null && form.lat != null) {
    setMarkerPosition(form.lng, form.lat)
  }
}

async function searchLocation() {
  if (!searchQuery.value.trim()) {
    showMessage('请输入搜索关键词', 'error')
    return
  }
  await ensureMap()
  if (!AMapRef) return
  AMapRef.plugin(['AMap.PlaceSearch'], () => {
    const placeSearch = new AMapRef.PlaceSearch({ city: '上海' })
    placeSearch.search(searchQuery.value, (status: string, result: any) => {
      const poi = result?.poiList?.pois?.[0]
      if (!poi?.location) {
        showMessage('未找到匹配的位置，请尝试更精确的关键词', 'error')
        return
      }
      const lng = Number(poi.location.lng)
      const lat = Number(poi.location.lat)
      if (Number.isFinite(lng) && Number.isFinite(lat)) {
        promptLocation(lng, lat, poi.address || poi.name || '', poi.name || '')
      }
    })
  })
}

async function loadProfile() {
  const res = await getRestaurantProfile()
  if (res.status === 200 && res.data) {
    const data = res.data as RestaurantProfileResponse
    const normalizedPhoto = normalizeImageUrl(data.restaurant.photoUrl)
    profile.value = { ...data.restaurant, photoUrl: normalizedPhoto }
    summary.value = data.summary
    highlight.value = data.highlight
      ? {
          ...data.highlight,
          user: data.highlight.user
            ? { ...data.highlight.user, avatarUrl: normalizeImageUrl(data.highlight.user.avatarUrl) ?? undefined }
            : undefined,
        }
      : null

    form.name = data.restaurant.name || ''
    form.address = data.restaurant.address || ''
    form.description = data.restaurant.description || ''
    form.lng = data.restaurant.lng ?? null
    form.lat = data.restaurant.lat ?? null
    form.photoUrl = normalizedPhoto

    await ensureMap()
    if (form.lng != null && form.lat != null) {
      setMarkerPosition(form.lng, form.lat)
    }
    await loadReviews(1)
  } else {
    showMessage(res.message || '获取餐馆信息失败', 'error')
  }
}

async function loadReviews(page = reviewState.page) {
  if (!restaurantId.value) {
    reviewState.list = []
    reviewState.total = 0
    return
  }
  reviewState.loading = true
  try {
    const res = await getRestaurantReviews(restaurantId.value, page, reviewState.size)
    if (res.status === 200 && res.data) {
      reviewState.page = res.data.page
      reviewState.size = res.data.size
      reviewState.total = res.data.total
      reviewState.list =
        res.data.items?.map(item => ({
          ...item,
          createdAt: item.createdAt ?? '',
          user: item.user
            ? { ...item.user, avatarUrl: normalizeImageUrl(item.user.avatarUrl) ?? undefined }
            : undefined,
        })) ?? []
      expanded.clear()
    }
  } finally {
    reviewState.loading = false
  }
}

function changePage(page: number) {
  if (page < 1 || page > totalPages.value) return
  loadReviews(page)
}

function toggleExpand(id: number) {
  if (expanded.has(id)) expanded.delete(id)
  else expanded.add(id)
}

async function toggleLike(item: RestaurantReviewItem) {
  const res = await toggleRestaurantReviewLike(item.id)
  if (res.status === 200 && res.data) {
    item.likes = res.data.likes ?? item.likes
    item.liked = res.data.liked ?? !item.liked
  }
}

function openUserProfile(item: RestaurantReviewItem) {
  const raw = item.user?.username || (item.user?.id != null ? String(item.user.id) : '')
  const identifier = raw?.trim?.()
  if (!identifier) return
  router.push({ name: 'user-public-profile', params: { identifier }, query: item.user?.id ? { userId: String(item.user.id) } : undefined })
}

function triggerFile() {
  fileInput.value?.click()
}

async function onFileChange(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  if (file.size > 5 * 1024 * 1024) {
    showMessage('图片过大，请选择 5MB 以内的文件', 'error')
    return
  }
  form.photoFile = file
  const reader = new FileReader()
  reader.onload = ev => {
    const preview = ev.target?.result as string
    if (preview) form.photoUrl = preview
  }
  reader.readAsDataURL(file)
}

async function onSave() {
  if (!form.name || !form.address || form.lng == null || form.lat == null) {
    showMessage('请填写完整的餐馆名称、地址并在地图上定位', 'error')
    return
  }
  isSaving.value = true
  try {
    let photoUrl = form.photoUrl
    if (form.photoFile) {
      const res = await uploadRestaurantPhoto(form.photoFile)
      if (res.status === 200 && res.data?.url) {
        photoUrl = normalizeImageUrl(res.data.url)
        form.photoUrl = photoUrl
      }
    }
    const payload: Partial<RestaurantProfile> = {
      name: form.name,
      address: form.address,
      description: form.description,
      lng: form.lng,
      lat: form.lat,
      photoUrl,
      id: profile.value?.id ?? undefined,
    }
    const res = await saveRestaurantProfile(payload)
    if (res.status === 200 && res.data) {
      await loadProfile()
      showMessage('保存成功', 'success')
      form.photoFile = null
    } else {
      showMessage(res.message || '保存失败', 'error')
    }
  } finally {
    isSaving.value = false
  }
}

onMounted(async () => {
  await ensureMap()
  loadProfile()
})

watch(
  () => [form.lng, form.lat] as const,
  ([lng, lat]) => {
    if (lng != null && lat != null) {
      setMarkerPosition(lng, lat)
    } else {
      clearMarker()
    }
  },
)
</script>

<style scoped>
.merchant-restaurant {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: linear-gradient(135deg, #fff7ed 0%, #fef3c7 40%, #f8fafc 100%);
}

.content-scroll {
  flex: 1;
  padding: 24px 28px 32px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 28px;
  scrollbar-color: #f97316 rgba(251, 191, 36, 0.12);
}

.content-scroll::-webkit-scrollbar {
  width: 8px;
}

.content-scroll::-webkit-scrollbar-track {
  background: rgba(251, 191, 36, 0.12);
  border-radius: 999px;
}

.content-scroll::-webkit-scrollbar-thumb {
  background: linear-gradient(135deg, #f97316, #f59e0b);
  border-radius: 999px;
}

.layout {
  display: grid;
  gap: 24px;
}

.layout--top {
  grid-template-columns: minmax(0, 2fr) minmax(0, 1.1fr);
  align-items: stretch;
}

.layout--main {
  grid-template-columns: minmax(0, 2.25fr) minmax(0, 1fr);
  align-items: flex-start;
}

.col {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.card {
  background: rgba(255, 255, 255, 0.96);
  border-radius: 18px;
  padding: 24px;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
  border: 1px solid rgba(226, 232, 240, 0.65);
}

.summary-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(254, 243, 199, 0.9) 95%);
  border: 1px solid rgba(249, 115, 22, 0.24);
  box-shadow: 0 14px 36px rgba(249, 115, 22, 0.18);
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.summary-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.summary-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.summary-header h1 {
  margin: 0 0 6px 0;
  font-size: 30px;
  font-weight: 800;
  color: #7c2d12;
}

.summary-address {
  margin: 0;
  font-size: 15px;
  color: #f97316;
  font-weight: 700;
}

.summary-address--empty {
  color: #fb923c;
}

.summary-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 18px;
}

.stat {
  background: linear-gradient(145deg, rgba(254, 243, 199, 0.92), rgba(253, 186, 116, 0.9));
  padding: 16px 18px;
  border-radius: 16px;
  box-shadow: inset 0 0 0 1px rgba(249, 115, 22, 0.25);
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  background: rgba(251, 191, 36, 0.35);
  color: #c2410c;
  box-shadow: inset 0 0 0 1px rgba(234, 88, 12, 0.25);
}

.stat .label {
  font-size: 13px;
  color: #fb923c;
  font-weight: 700;
}

.stat .value {
  margin-top: 8px;
  font-size: 26px;
  font-weight: 800;
  color: #7c2d12;
  display: flex;
  align-items: center;
  gap: 12px;
}

.highlight-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: linear-gradient(135deg, rgba(253, 230, 138, 0.96), rgba(251, 191, 36, 0.9));
  border: 1px solid rgba(249, 115, 22, 0.32);
  box-shadow: 0 18px 40px rgba(234, 88, 12, 0.22);
}

.highlight-card header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.highlight-card h3 {
  margin: 0 0 4px 0;
  font-size: 17px;
  font-weight: 800;
  color: #7c2d12;
}

.highlight-card .content {
  margin: 0;
  color: #9a3412;
  line-height: 1.6;
}

.highlight-card footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #7c2d12;
  font-weight: 700;
}

.highlight-card .likes {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.pill {
  background: #f97316;
  color: #fff;
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 600;
}

.muted {
  color: #fb923c;
  font-size: 13px;
  font-weight: 700;
}

.stars {
  display: inline-flex;
  gap: 4px;
}

.icon-star {
  width: 16px;
  height: 16px;
  display: inline-block;
  background: rgba(251, 191, 36, 0.25);
  clip-path: polygon(50% 0%, 62% 35%, 100% 35%, 69% 57%, 82% 91%, 50% 70%, 18% 91%, 31% 57%, 0% 35%, 38% 35%);
}

.icon-star--filled {
  background: #f97316;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.card-header h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 800;
  color: #7c2d12;
}

.btn {
  border: none;
  border-radius: 12px;
  padding: 10px 18px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.btn--primary {
  background: linear-gradient(135deg, #f97316 0%, #f59e0b 100%);
  color: #fff;
  box-shadow: 0 10px 25px rgba(249, 115, 22, 0.35);
}

.btn--primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  box-shadow: none;
}

.btn--ghost {
  background: rgba(249, 115, 22, 0.12);
  color: #f97316;
}

.btn--primary:not(:disabled):hover,
.btn--ghost:hover {
  transform: translateY(-1px);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group--row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.form-group--wide {
  grid-column: 1 / -1;
}

label {
  font-size: 14px;
  font-weight: 700;
  color: #7c2d12;
}

.required {
  color: #ef4444;
}

input,
textarea {
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.6);
  padding: 10px 12px;
  font-size: 14px;
  font-weight: 600;
  color: #7c2d12;
  background: rgba(255, 255, 255, 0.9);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

input:focus,
textarea:focus {
  outline: none;
  border-color: #f97316;
  box-shadow: 0 0 0 3px rgba(249, 115, 22, 0.18);
}

.search-row {
  display: flex;
  gap: 12px;
}

.upload-row {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 18px;
  align-items: center;
}

.preview {
  width: 100%;
  aspect-ratio: 4 / 3;
  border-radius: 14px;
  overflow: hidden;
  background: #fff7ed;
  border: 1px dashed rgba(249, 115, 22, 0.4);
  display: grid;
  place-items: center;
}

.preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview--placeholder span {
  color: rgba(249, 115, 22, 0.8);
  font-weight: 600;
}

.upload-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.alert {
  margin-top: 18px;
  padding: 12px 16px;
  border-radius: 12px;
  font-weight: 600;
}

.alert--success {
  background: rgba(16, 185, 129, 0.12);
  color: #047857;
}

.alert--error {
  background: rgba(248, 113, 113, 0.12);
  color: #b91c1c;
}

.reviews-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.review-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.review-item {
  border: 1px solid rgba(249, 115, 22, 0.3);
  border-radius: 18px;
  padding: 20px;
  transition: box-shadow 0.2s ease, transform 0.2s ease;
  background: linear-gradient(150deg, rgba(180, 83, 9, 0.95), rgba(234, 88, 12, 0.92));
  box-shadow: 0 20px 46px rgba(120, 53, 15, 0.45);
  color: #fff7ed;
}

.review-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 26px 52px rgba(124, 45, 18, 0.52);
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
}

.review-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: rgba(254, 215, 170, 0.25);
  color: #fde68a;
  display: grid;
  place-items: center;
  font-weight: 800;
  font-size: 18px;
  background-size: cover;
  background-position: center;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.avatar:focus-visible,
.avatar:focus {
  outline: 2px solid rgba(253, 186, 116, 0.8);
  outline-offset: 4px;
}

.avatar:hover {
  transform: translateY(-2px) scale(1.03);
}

.review-meta__info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.review-meta__info .name {
  margin: 0;
  color: #fffbeb;
  font-weight: 800;
  cursor: pointer;
}

.review-meta__info .name:focus-visible,
.review-meta__info .name:focus {
  outline: none;
  text-decoration: underline;
}

.review-meta__info .name:hover {
  text-decoration: underline;
}

.review-meta__info .time {
  margin: 0;
  color: rgba(254, 215, 170, 0.9);
  font-weight: 600;
}

.review-rating {
  display: flex;
  align-items: center;
  gap: 8px;
}

.review-rating .score {
  font-weight: 700;
  color: #fbbf24;
}

.review-content {
  margin: 12px 0;
  color: #fffbeb;
  line-height: 1.65;
}

.review-detail {
  padding: 14px;
  background: rgba(253, 186, 116, 0.25);
  border-radius: 14px;
  color: #fff7ed;
}

.review-detail pre {
  margin: 0;
  white-space: pre-wrap;
  line-height: 1.5;
}

.review-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
}

.btn-like {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: none;
  background: rgba(237, 11, 56, 0.12);
  color: #f91629;
  padding: 8px 12px;
  border-radius: 999px;
  font-weight: 700;
  cursor: pointer;
}

.btn-like.liked {
  background: #f97316;
  color: #fff;
}

.btn-more {
  border: none;
  background: none;
  color: #f97316;
  font-weight: 700;
  cursor: pointer;
}

.loading-block,
.empty-block {
  padding: 40px 0;
  text-align: center;
  color: #fb923c;
  font-weight: 700;
}

.paginator {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
}

.page-info {
  color: #7c2d12;
  font-weight: 700;
}

.map-card {
  padding: 0;
  overflow: hidden;
  position: sticky;
  top: 24px;
}

.map-card .card-header {
  padding: 24px 24px 0 24px;
}

.map-wrapper {
  position: relative;
}

.map-confirm {
  position: absolute;
  bottom: 22px;
  right: 22px;
  width: 260px;
  background: linear-gradient(155deg, rgba(251, 191, 36, 0.96), rgba(249, 115, 22, 0.94));
  color: #7c2d12;
  border-radius: 16px;
  box-shadow: 0 20px 36px rgba(124, 45, 18, 0.35);
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  border: 1px solid rgba(249, 115, 22, 0.4);
}

.map-confirm__title {
  font-weight: 800;
  font-size: 16px;
  color: #7c2d12;
}

.map-confirm__place {
  font-weight: 700;
  color: #92400e;
}

.map-confirm__coords,
.map-confirm__address {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: #7c2d12;
  font-weight: 600;
}

.map-confirm__actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 6px;
}

.map-container {
  width: 100%;
  height: 520px;
  border-radius: 0 0 18px 18px;
  overflow: hidden;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.expand-enter-active,
.expand-leave-active {
  transition: max-height 0.25s ease, opacity 0.25s ease, transform 0.25s ease;
}

.font-yellow {
  color: #f97316;
  font-weight: bolder;
}

.expand-enter-from,
.expand-leave-to {
  max-height: 0;
  opacity: 0;
  transform: translateY(-6px);
}

@media (max-width: 1280px) {
  .layout--top,
  .layout--main {
    grid-template-columns: 1fr;
  }

  .highlight-card {
    order: -1;
  }

  .map-card {
    position: static;
  }

  .upload-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .content-scroll {
    padding: 18px 18px 24px;
  }

  .card,
  .summary-card,
  .highlight-card {
    padding: 20px;
  }

  .summary-header h1 {
    font-size: 26px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .map-container {
    height: 320px;
  }
}
</style>
