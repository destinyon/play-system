<template>
  <div class="rest-panel">
    <div class="grid">
      <!-- 左列：基础信息 + 上传 -->
      <div class="form">
        <div class="item">
          <label>餐馆名称</label>
          <input :class="{ invalid: nameInvalid }" v-model.trim="model.name" placeholder="请输入餐馆名称（必填）" />
          <small v-if="nameInvalid" class="error">请填写餐馆名称</small>
        </div>
        <div class="item">
          <label>搜索地点</label>
          <div class="search-row">
            <input v-model="query" @keydown.enter.prevent="onSearch" placeholder="请输入地址/地标进行搜索" />
            <button class="btn" type="button" @click="onSearch">搜索</button>
          </div>
          <small class="hint">支持模糊搜索，选择后会在地图上标注</small>
        </div>
        <div class="item">
          <label>详细地址</label>
          <input :class="{ invalid: addressInvalid }" v-model="model.address" placeholder="选择后自动填入，可手动微调（必填）" />
          <small v-if="addressInvalid" class="error">请填写详细地址</small>
          <!-- 图片控件移动到右侧预览下方，避免遮挡表单按钮 -->
        </div>
      </div>

      <!-- 右列：大尺寸预览与提示 -->
      <div class="preview-col">
        <div class="preview" v-if="model.photoUrl">
          <img :src="model.photoUrl" alt="预览" />
        </div>
        <div class="no-preview" v-else>暂无图片，先上传或粘贴 URL 试试</div>
        <p class="coord" v-if="model.lng && model.lat">坐标：{{ model.lng.toFixed(6) }}, {{ model.lat.toFixed(6) }}</p>

        <!-- 将上传按钮与 URL 放在预览下面 -->
        <div class="item">
          <div class="upload">
            <input ref="fileInput" type="file" accept="image/*" @change="onFileChange" />
            <label style="font-size: 15px">餐馆图片:</label>
            <button class="btn" type="button" @click="triggerFile">选择图片</button>
          </div>
        </div>
        <div class="item">
          <label>图片 URL（可选）</label>
          <input v-model.trim="model.photoUrl" placeholder="粘贴已上传的图片地址" />
        </div>
      </div>

    </div>

    <!-- 地图容器：渲染到父级页面提供的插槽（左侧介绍卡片底部）。
         若目标暂不可用，Teleport 会在当前组件位置渲染，目标出现后自动移动。 -->
    <teleport :to="mapTargetElement" :disabled="teleportDisabled">
      <div class="map map--intro" ref="mapEl"></div>
    </teleport>

    <!-- 自定义 Toast -->
    <transition name="toast">
      <div v-if="toast.show" :class="['toast', toast.type]">{{ toast.message }}</div>
    </transition>

    <!-- 自定义 Modal -->
    <div v-if="modal.show" class="modal-mask" @click.self="closeModal()">
      <div class="modal">
        <div class="modal__header">{{ modal.title }}</div>
        <div class="modal__body">{{ modal.message }}</div>
        <div class="modal__actions">
          <button class="btn btn--secondary" v-if="modal.cancelText" @click="closeModal(false)">{{ modal.cancelText }}</button>
          <button class="btn btn--primary" @click="closeModal(true)">{{ modal.okText || '确定' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch, computed, nextTick, onBeforeUnmount } from 'vue'

type AMapSDK = any
const emit = defineEmits(['update:modelValue'])
const props = defineProps<{ modelValue: { name?: string; address?: string; lng?: number; lat?: number; photoUrl?: string; photoFile?: File }, mapTarget?: string }>()

const model = ref({ ...(props.modelValue || {}) } as any)

// sync prop -> local only when different
watch(() => props.modelValue, (v) => {
  try {
    const incoming = v || {}
    if (JSON.stringify(incoming) !== JSON.stringify(model.value)) {
      model.value = { ...incoming }
    }
  } catch {
    model.value = { ...(v || {}) }
  }
})

// emit only when changed
watch(model, (v) => {
  try {
    const current = props.modelValue || {}
    if (JSON.stringify(current) !== JSON.stringify(v)) {
      emit('update:modelValue', v)
    }
  } catch {
    emit('update:modelValue', v)
  }
}, { deep: true })

// simple validations
const nameInvalid = computed(() => !model.value.name || !String(model.value.name).trim())
const addressInvalid = computed(() => !model.value.address || !String(model.value.address).trim())

// map related
const mapEl = ref<HTMLDivElement | null>(null)
const mapTargetElement = ref<HTMLElement | null>(null)
const teleportDisabled = ref(true)
let teleportObserver: MutationObserver | null = null
let AMapRef: AMapSDK | null = null
let map: any = null
let marker: any = null
let geocoder: any = null
const query = ref('')

const DEFAULT_AMAP_KEY = '218f3df7356ff0dcbc0635c0abf39893'
const DEFAULT_SECURITY_JS_CODE = '3b0d644aa49fcb13d08e0693933c6767'

function loadAmap(): Promise<AMapSDK> {
  return new Promise((resolve, reject) => {
    if ((window as any).AMap) return resolve((window as any).AMap)
    ;(window as any)._AMapSecurityConfig = { securityJsCode: DEFAULT_SECURITY_JS_CODE }
    const script = document.createElement('script')
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${DEFAULT_AMAP_KEY}`
    script.async = true
    script.onload = () => resolve((window as any).AMap)
    script.onerror = () => reject(new Error('AMap load error'))
    document.head.appendChild(script)
  })
}

function onSearch() {
  if (!AMapRef || !map || !query.value) return
  AMapRef.plugin(['AMap.PlaceSearch'], () => {
    const placeSearch = new (AMapRef as any).PlaceSearch({ city: '全国' })
    placeSearch.search(query.value, (status: string, result: any) => {
      const poi = result?.poiList?.pois?.[0]
      if (!poi) { showToast('未找到相关地点，请尝试更精确的关键词', 'error'); return }
      const lng = Number(poi.location?.lng)
      const lat = Number(poi.location?.lat)
      if (isNaN(lng) || isNaN(lat)) return
      query.value = poi.name || query.value
      setPoint(lng, lat, poi.name, poi.address)
    })
  })
}

function setPoint(lng: number, lat: number, name?: string, address?: string) {
  model.value.lng = lng
  model.value.lat = lat
  if (name && !model.value.name) model.value.name = name
  if (address) model.value.address = address
  if (map) {
    try {
      map?.panTo([lng, lat])
      const currentZoom = map.getZoom?.() || 12
      if (currentZoom < 16) map?.setZoom(17)
    } catch {
      map.setZoomAndCenter(17, [lng, lat])
    }
    if (marker) { try { marker.setMap(null) } catch {} }
    marker = new (AMapRef as any).Marker({ position: [lng, lat], title: model.value.name || '餐馆', anchor: 'bottom-center' })
    map.add(marker)
    const text = new (AMapRef as any).Text({
      text: (query.value || model.value.name || '选中位置'),
      position: [lng, lat],
      style: { 'background': 'rgba(255,255,255,.9)', 'padding': '4px 6px', 'border-radius': '8px', 'border': '1px solid #93c5fd', 'font-weight': '800', 'color': '#0c4a6e' }
    })
    map.add(text)
  }
  if (!address && geocoder) {
    geocoder.getAddress([lng, lat], (status: string, result: any) => {
      const ad = result?.regeocode?.formattedAddress
      const pois = result?.regeocode?.pois
      if (Array.isArray(pois) && pois.length > 0 && pois[0]?.name) {
        query.value = pois[0].name
      }
      if (ad) {
        model.value.address = ad
        showToast('已根据坐标自动填入地址', 'success')
      }
    })
  }
}

function destroyMap() {
  if (map) {
    try { map.destroy() } catch (err) { console.warn('销毁地图失败', err) }
  }
  map = null
  marker = null
  geocoder = null
}

async function ensureMapContainerReady() {
  await nextTick()
  let tries = 0
  while (mapEl.value && !mapEl.value.isConnected && tries < 20) {
    await new Promise(resolve => setTimeout(resolve, 40))
    tries++
  }
  if (!mapEl.value || !mapEl.value.isConnected) {
    throw new Error('Map container div not exist')
  }
}

function updateTeleportState() {
  if (typeof window === 'undefined') {
    mapTargetElement.value = null
    teleportDisabled.value = true
    return
  }
  if (!props.mapTarget) {
    mapTargetElement.value = null
    teleportDisabled.value = true
    return
  }
  try {
    const el = document.querySelector(props.mapTarget) as HTMLElement | null
    mapTargetElement.value = el
    teleportDisabled.value = !el
  } catch {
    mapTargetElement.value = null
    teleportDisabled.value = true
  }
}

async function initMap() {
  try {
    AMapRef = await loadAmap()
    await ensureMapContainerReady()
    map = new (AMapRef as any).Map(mapEl.value!, { zoom: 12, center: [121.473701,31.230416] })
    AMapRef.plugin('AMap.Geocoder', () => { geocoder = new (AMapRef as any).Geocoder({}) })
    map.on('click', (e: any) => {
      const lng = e.lnglat.getLng?.() ?? e.lnglat.lng
      const lat = e.lnglat.getLat?.() ?? e.lnglat.lat
      setPoint(lng, lat)
    })
    if (model.value.lng && model.value.lat) setPoint(model.value.lng, model.value.lat)
  } catch (err) {
    console.warn(err)
  }
}

onMounted(async () => {
  updateTeleportState()
  if (!teleportObserver) {
    teleportObserver = new MutationObserver(() => updateTeleportState())
    teleportObserver.observe(document.body, { childList: true, subtree: true })
  }
  await initMap()
})

watch(() => props.mapTarget, () => updateTeleportState())

watch(() => mapTargetElement.value, async () => {
  if (map) {
    try {
      await ensureMapContainerReady()
      map.resize?.()
    } catch (err) {
      console.warn(err)
    }
  }
})

onBeforeUnmount(() => {
  teleportObserver?.disconnect()
  teleportObserver = null
  destroyMap()
})

// file input / preview (deferred upload)
const fileInput = ref<HTMLInputElement | null>(null)
function triggerFile() { fileInput.value?.click() }
async function onFileChange(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  let processed: File = file
  try { if (file.size > 5 * 1024 * 1024) processed = await compressImage(file, 1600, 0.85) } catch {}
  try {
    const reader = new FileReader()
    reader.onload = (ev) => {
      const preview = ev.target?.result as string
      if (preview) {
        model.value.photoUrl = preview
        ;(model.value as any).photoFile = processed
      }
    }
    reader.readAsDataURL(processed)
  } catch (err) {
    showToast('读取图片预览失败', 'error')
  }
}

async function compressImage(file: File, maxWidth = 1600, quality = 0.85): Promise<File> {
  const blob = await file.arrayBuffer()
  const img = await createImageBitmap(new Blob([blob]))
  const scale = Math.min(1, maxWidth / img.width)
  const w = Math.round(img.width * scale)
  const h = Math.round(img.height * scale)
  const canvas = document.createElement('canvas')
  canvas.width = w; canvas.height = h
  const ctx = canvas.getContext('2d')!
  ctx.drawImage(img, 0, 0, w, h)
  const dataUrl = canvas.toDataURL('image/jpeg', quality)
  const b = await (await fetch(dataUrl)).blob()
  return new File([b], file.name.replace(/\.[^.]+$/, '.jpg'), { type: 'image/jpeg' })
}

// Toast & modal
const toast = ref<{ show: boolean; message: string; type: 'info'|'success'|'error' }>({ show: false, message: '', type: 'info' })
let toastTimer: any
function showToast(message: string, type: 'info'|'success'|'error' = 'info') { toast.value = { show: true, message, type }; clearTimeout(toastTimer); toastTimer = setTimeout(() => toast.value.show = false, 1800) }
const modal = ref<{ show: boolean; title: string; message: string; okText?: string; cancelText?: string; resolver?: (ok: boolean)=>void }>({ show: false, title: '', message: '' })
function openModal(title: string, message: string, okText = '确定', cancelText?: string) {
  return new Promise<boolean>(resolve => { modal.value = { show: true, title, message, okText, cancelText, resolver: resolve } })
}
function closeModal(ok = true) { if (modal.value.resolver) modal.value.resolver(ok); modal.value.show = false }
</script>

<style scoped>
.rest-panel { background: linear-gradient(135deg,#fff 0%,#f0f9ff 100%); border:2px solid #c7d2fe; border-radius:16px; padding:16px; box-shadow:0 6px 24px rgba(59,130,246,.15); position: relative; }
.grid { display:grid; grid-template-columns: 1.2fr 0.8fr; gap:16px; }
.form { display:flex; flex-direction:column; gap:12px; }
.item { display:flex; flex-direction:column; gap:6px; }
label { font-weight:800; font-size:13px; color:#334155; }
input { border:2px solid #e2e8f0; border-radius:10px; padding:10px 12px; background:#f8fafc; font-weight:600; }
.search-row { display:flex; gap:8px; }
.btn { background:linear-gradient(135deg,#3b82f6 0%, #06b6d4 100%); color:#fff; border:none; border-radius:10px; padding:10px 14px; font-weight:800; cursor:pointer; }
.hint { color:#64748b; font-size:12px; font-weight:600; }
.preview-col{ display:flex; flex-direction:column; gap:10px; align-items:center; justify-content:flex-start; }
.map { width:100%; height:100%; border-radius:12px; border:2px solid #93c5fd; overflow:hidden; }
.map--intro{ height:450px; }
.map--inline{ height:260px; }
.coord { margin:4px 0 0; color:#0c4a6e; font-size:13px; font-weight:700; }
.tip { color:#475569; font-size:12px; font-weight:600; }
.upload { display:flex; align-items:center; gap:8px; }
.upload input[type="file"] { display:none; }
.preview { width:100%; height:220px; display:grid; place-items:center; background:#fff; border-radius:12px; border:2px solid #e5e7eb; overflow:hidden; }
.preview img { width:100%; height:100%; object-fit:cover; }
.no-preview { width:100%; height:220px; display:grid; place-items:center; background:repeating-linear-gradient(45deg,#f8fafc 0 10px,#f1f5f9 10px 20px); color:#64748b; border:2px dashed #cbd5e1; border-radius:12px; font-weight:700; }

/* 内联地图容器（当父级未提供挂载点时显示） */
.inline-map-wrap{ margin-top:12px; }
.inline-map__title{ font-size:12px; font-weight:900; color:#0c4a6e; margin-bottom:6px; }

/* Toast */
.toast{ position:fixed; left:50%; transform:translateX(-50%); bottom:24px; background:#111827; color:#fff; padding:10px 14px; border-radius:10px; box-shadow:0 10px 30px rgba(0,0,0,.25); z-index:60; font-weight:800; letter-spacing:.2px; }
.toast.success{ background:#16a34a; }
.toast.error{ background:#dc2626; }
.toast-enter-active,.toast-leave-active{ transition:all .2s ease; }
.toast-enter-from,.toast-leave-to{ opacity:0; transform:translate(-50%, 20px); }

/* Modal */
.modal-mask{ position:fixed; inset:0; background:rgba(15,23,42,.45); display:grid; place-items:center; z-index:70; }
.modal{ width:min(420px, 92vw); background:#fff; border:2px solid #c7d2fe; border-radius:16px; box-shadow:0 20px 60px rgba(59,130,246,.35); overflow:hidden; }
.modal__header{ padding:12px 16px; font-weight:900; color:#0c4a6e; background:linear-gradient(135deg,#eff6ff,#e0f2fe); border-bottom:2px solid #bfdbfe; }
.modal__body{ padding:16px; color:#0f172a; font-weight:700; }
.modal__actions{ display:flex; gap:10px; padding:12px 16px 16px; justify-content:flex-end; }

@media (max-width: 900px){
  .grid { grid-template-columns: 1fr; }
}
</style>
