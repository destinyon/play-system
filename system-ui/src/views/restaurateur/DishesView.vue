<template>
  <div class="card">
    <div class="page-header">
      <div class="header-content">
        <div class="header-icon">🍴</div>
        <h2>菜品管理</h2>
        <div class="header-decoration">
          <span class="deco">✨</span>
          <span class="deco">🍜</span>
          <span class="deco">🍰</span>
        </div>
      </div>
      <div class="header-stats" >
        <div class="stat-item">
          <span class="stat-icon">📊</span>
          <span class="stat-label">总数</span>
          <span class="stat-value">{{ total }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-icon">✅</span>
          <span class="stat-label">上架</span>
          <span class="stat-value">{{ items.filter(i => i.status === 'ON_SHELF').length }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-icon">📦</span>
          <span class="stat-label">下架</span>
          <span class="stat-value">{{ items.filter(i => i.status === 'OFF_SHELF').length }}</span>
        </div>
      </div>
    </div>

    <div class="toolbar header-bar">
      <div class="left-actions">
        <button class="button primary" @click="openCreate">新增菜品</button>
        <button class="button danger ml8" @click="toggleDeleteMode">{{ deleteMode ? '取消删除' : '删除模式' }}</button>
      </div>
      <div class="search-sort">
        <input class="input" style="max-width:260px" v-model.trim="keywordEditing" placeholder="搜索：菜名/描述（模糊）" />
        <div class="cat-radios">
          <label class="radio" v-for="c in categoryOptions" :key="c">
            <input type="radio" name="cat" :value="c" v-model="categoryEditing" /> {{ c || '全部' }}
          </label>
        </div>
        <button class="button" @click="applySearch">查询</button>
        <div class="sorters" title="按价格排序">
          <button class="icon-btn" :class="{active: sortOrder==='ASC'}" @click="setSort('ASC')">▲</button>
          <button class="icon-btn" :class="{active: sortOrder==='DESC'}" @click="setSort('DESC')">▼</button>
        </div>
      </div>
    </div>

    <!-- 新增菜品弹窗 -->
    <div v-if="creating" class="modal" @click.self="closeCreate">
      <div class="dialog">
        <h3>新增菜品</h3>
        <div class="create-layout">
          <div class="left">
            <div class="imgbox">
              <img v-if="createForm.previewUrl" :src="createForm.previewUrl" alt="预览" />
              <div v-else class="placeholder">图片预览</div>
            </div>
            <label class="upload-btn">
              选择图片
              <input type="file" accept="image/*" @change="onCreateFile" hidden />
            </label>
            <p class="tip">建议横图，大小 &lt; 5MB</p>
          </div>
          <div class="right">
            <div class="field"><label>菜名 <span class="required">*</span></label><input class="input" v-model.trim="createForm.name" maxlength="50" /></div>
            <div class="field two">
              <div>
                <label>价格 <span class="required">*</span></label><input class="input" v-model.trim="createForm.priceStr" placeholder="如 12.50" type="number" step="0.01" min="0.01" max="99999.99" />
              </div>
              <div>
                <label>分类</label>
                <select class="select" v-model="createForm.category">
                  <option value="">可不选</option>
                  <option v-for="c in typeOptions" :key="c" :value="c">{{ c }}</option>
                </select>
              </div>
            </div>
            <div class="field">
              <label>描述</label>
              <textarea class="textarea" v-model.trim="createForm.description" maxlength="200" rows="4" placeholder="最多200字"></textarea>
              <div class="count">{{ createForm.description.length }}/200</div>
            </div>
            <div class="field">
              <label>状态</label>
              <select class="select" v-model="createForm.status">
                <option value="ON_SHELF">上架</option>
                <option value="OFF_SHELF">下架</option>
              </select>
            </div>
            <div class="tips">
              <span v-if="createForm.name && !nameRegex.test(createForm.name)" class="tip error">菜名需 1-50 个字</span>
              <span v-if="createForm.priceStr && !priceRegex.test(createForm.priceStr)" class="tip error">价格需为正数（0.01-99999.99）</span>
            </div>
          </div>
        </div>
        <div class="actions">
          <button class="button primary" :disabled="!canCreate" @click="submitCreate">保存</button>
          <button class="button ghost" @click="closeCreate">取消</button>
        </div>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="loading-state">
      <span class="spinner"></span>
      正在加载菜品...
    </div>

    <!-- 卡片网格 -->
    <div v-else class="cards-grid">
      <div v-if="items.length === 0" class="empty-state">
        <div class="empty-icon">🍽️</div>
        <p>暂无菜品，点击"新增菜品"开始创建</p>
      </div>
      
      <div v-for="d in items" :key="d.id" class="card-item">
        <div class="product-card clickable" :class="{'off-shelf': d.status === 'OFF_SHELF'}" @click="onCardClick(d)">
          <div class="thumb">
            <img :src="resolveImageSrc(d.imageUrl)" :alt="d.name" @error="onImageError" />
          </div>
          <button v-if="deleteMode" class="delete-x" title="删除" @click.stop="askRemove(d)">×</button>
          <div class="body">
            <div class="title">
              <span class="name">{{ d.name }}</span>
              <small class="price">¥{{ format(d.price) }}</small>
            </div>
            <div class="meta ellipsis">{{ d.category || '未分类' }}</div>
            <div class="status-badge" :class="statusClass(d.status)">{{ statusText(d.status) }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 分页控件 -->
    <div class="pager" v-if="totalPages > 1">
      <button class="button page-btn" :disabled="page === 1" @click="toPage(1)" title="首页">
        <span>⏮</span>
      </button>
      <button class="button page-btn" :disabled="page === 1" @click="toPage(page - 1)" title="上一页">
        <span>◀</span>
      </button>
      
      <div class="page-numbers">
        <button 
          v-for="p in visiblePages" 
          :key="p"
          class="page-number"
          :class="{ active: p === page, ellipsis: p === -1 }"
          @click="p !== -1 && toPage(p)"
          :disabled="p === -1"
        >
          {{ p === -1 ? '...' : p }}
        </button>
      </div>

      <button class="button page-btn" :disabled="page >= totalPages" @click="toPage(page + 1)" title="下一页">
        <span>▶</span>
      </button>
      <button class="button page-btn" :disabled="page >= totalPages" @click="toPage(totalPages)" title="末页">
        <span>⏭</span>
      </button>
      
      <div class="page-info">
        <span>共 {{ total }} 条</span>
        <span class="separator">|</span>
        <span>{{ totalPages }} 页</span>
      </div>
    </div>

    <!-- 编辑弹窗 -->
    <div v-if="editing" class="modal" @click.self="closeEdit">
      <div class="dialog">
        <h3>编辑菜品</h3>
        <div class="form-grid">
          <div class="img-field">
            <label>当前图片</label>
            <div class="imgbox large">
              <img :src="resolveImageSrc(editForm.previewUrl || editForm.imageUrl)" :alt="editForm.name" @error="onImageError" />
            </div>
            <label class="upload-btn mt8">
              选择新图片
              <input type="file" accept="image/*" @change="onEditFile" hidden />
            </label>
          </div>
          <div class="fields">
            <div class="field"><label>菜名 <span class="required">*</span></label><input class="input" v-model.trim="editForm.name" maxlength="50" /></div>
            <div class="field two">
              <div><label>价格 <span class="required">*</span></label><input class="input" v-model.trim="editForm.priceStr" type="number" step="0.01" min="0.01" max="99999.99" /></div>
              <div>
                <label>分类</label>
                <select class="select" v-model="editForm.category">
                  <option value="">请选择</option>
                  <option v-for="c in typeOptions" :key="c" :value="c">{{ c }}</option>
                </select>
              </div>
            </div>
            <div class="field"><label>描述</label><textarea class="textarea" v-model.trim="editForm.description" maxlength="200" rows="4"></textarea></div>
            <div class="field">
              <label>状态</label>
              <select class="select" v-model="editForm.status">
                <option value="ON_SHELF">上架</option>
                <option value="OFF_SHELF">下架</option>
              </select>
            </div>
          </div>
        </div>
        <div class="tips">
          <span v-if="editForm.priceStr && !priceRegex.test(editForm.priceStr)" class="tip error">价格需为正数（0.01-99999.99）</span>
          <span v-if="editForm.description && editForm.description.length>200" class="tip error">描述最多 200 字</span>
        </div>
        <div class="actions">
          <button class="button" @click="saveEdit">保存</button>
          <button class="button ghost" @click="closeEdit">取消</button>
        </div>
      </div>
    </div>

    <!-- 删除确认弹窗 -->
    <div v-if="confirming" class="modal" @click.self="cancelRemove">
      <div class="modal-content">
        <h3>删除确认</h3>
        <p>确认删除 <strong>{{ confirmingItem?.name }}</strong> ？</p>
        <div class="form-actions">
          <button class="danger" @click="remove">删除</button>
          <button @click="cancelRemove">取消</button>
        </div>
      </div>
    </div>

    <!-- 删除模式提示 -->
    <div v-if="deleteMode" class="delete-hint">删除模式：点击卡片右上角 × 删除菜品</div>

    <!-- 自定义提示弹窗 -->
    <div v-if="alertMessage" class="alert-modal" @click="closeAlert">
      <div class="alert-box" @click.stop>
        <div class="alert-icon" :class="alertType">
          <span v-if="alertType === 'success'">✓</span>
          <span v-else-if="alertType === 'error'">✕</span>
          <span v-else>ℹ</span>
        </div>
        <div class="alert-content">
          <h4 class="alert-title">{{ alertTitle }}</h4>
          <p class="alert-text">{{ alertMessage }}</p>
        </div>
        <button class="alert-close" @click="closeAlert">确定</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { buildDataRequestBlob, buildAuthHeaders } from '@/api/http'
import { fetchMenuPage, createMenuItem, updateMenuItem, deleteMenuItem } from '@/api/menu'

const auth = useAuthStore()
const restaurateurId = computed(() => {
  const id = auth.restaurant?.id
  const n = typeof id === 'string' ? parseInt(id, 10) : id
  const num = typeof n === 'number' ? n : Number(n)
  return Number.isFinite(num) ? num : 1
})

const assetBaseEnv = (import.meta.env.VITE_ASSET_BASE_URL ?? '')
const assetBase = assetBaseEnv ? assetBaseEnv.replace(/\/$/, '') : ''
const defaultImageUrl = '/api/restaurateur/menu/default-image'

function resolveImageSrc(url) {
  if (!url) {
    return assetBase ? `${assetBase}${defaultImageUrl}` : defaultImageUrl
  }
  const value = typeof url === 'string' ? url : String(url)
  if (value.startsWith('blob:')) {
    return value
  }
  if (/^https?:\/\//i.test(value)) {
    return value
  }
  const normalized = value.startsWith('/') ? value : `/${value}`
  return assetBase ? `${assetBase}${normalized}` : normalized
}

function extractImageUrl(data) {
  if (!data) return ''
  if (typeof data === 'string') return data
  if (typeof data === 'object') {
    return data.url || data.path || data.relativePath || ''
  }
  return ''
}

// Alert system
const alertMessage = ref('')
const alertTitle = ref('')
const alertType = ref('info') // 'success' | 'error' | 'info'

function showAlert(message, title = '提示', type = 'info') {
  alertMessage.value = message
  alertTitle.value = title
  alertType.value = type
}

function closeAlert() {
  alertMessage.value = ''
  alertTitle.value = ''
  alertType.value = 'info'
}

// Data
const items = ref([])
const page = ref(1)
const size = ref(12)
const totalPages = ref(1)
const total = ref(0)
const loading = ref(false)

// Computed visible pages for pagination
const visiblePages = computed(() => {
  const current = page.value
  const total = totalPages.value
  const delta = 2
  const range = []
  const rangeWithDots = []
  let l

  for (let i = 1; i <= total; i++) {
    if (i === 1 || i === total || (i >= current - delta && i <= current + delta)) {
      range.push(i)
    }
  }

  range.forEach(i => {
    if (l) {
      if (i - l === 2) {
        rangeWithDots.push(l + 1)
      } else if (i - l !== 1) {
        rangeWithDots.push(-1)
      }
    }
    rangeWithDots.push(i)
    l = i
  })

  return rangeWithDots
})

// Search/filter (editing = temp state, applied = what was submitted)
const typeOptions = ['凉菜', '热菜', '甜品', '饮品', '主食']
const categoryOptions = computed(() => ['', ...typeOptions]) // '' means '全部'
const keywordEditing = ref('')
const categoryEditing = ref('') // empty means '全部'
const keywordApplied = ref('')
const categoryApplied = ref('')

// Sort
const sortOrder = ref('') // '' | 'ASC' | 'DESC'

// Delete mode
const deleteMode = ref(false)

// Validation regex
const nameRegex = /^.{1,50}$/
const priceRegex = /^([1-9]\d{0,4}|0)(\.\d{1,2})?$/

// Create modal
const creating = ref(false)
const createForm = ref({
  name: '',
  category: '',
  price: null,
  priceStr: '',
  description: '',
  status: 'ON_SHELF',
  file: null,
  previewUrl: ''
})

const canCreate = computed(() => {
  const f = createForm.value
  return f.name && nameRegex.test(f.name) && 
         f.priceStr && priceRegex.test(f.priceStr)
})

// Edit modal
const editing = ref(false)
const editForm = ref({
  id: null,
  name: '',
  category: '',
  price: null,
  priceStr: '',
  description: '',
  status: 'ON_SHELF',
  file: null,
  previewUrl: '',
  imageUrl: ''
})

const canEdit = computed(() => {
  const f = editForm.value
  return f.name && nameRegex.test(f.name) &&
         f.priceStr && priceRegex.test(f.priceStr)
})

// Confirming delete
const confirming = ref(false)
const confirmingItem = ref(null)

// ===== Fetch page =====
async function fetchPage() {
  loading.value = true
  try {
    const params = {
      restaurateurId: restaurateurId.value,
      keyword: keywordApplied.value || undefined,
      category: categoryApplied.value || undefined,
      sortBy: sortOrder.value ? 'price' : undefined,
      sortOrder: sortOrder.value || undefined,
      page: page.value,
      size: size.value
    }
    const res = await fetchMenuPage(params)
    if (res && res.status === 200 && res.data) {
      items.value = res.data.records || []
      total.value = res.data.total || 0
      totalPages.value = Math.ceil(total.value / size.value)
    } else {
      items.value = []
      totalPages.value = 1
      total.value = 0
    }
  } catch (error) {
    console.error('获取菜品失败:', error)
    items.value = []
    totalPages.value = 1
    total.value = 0
  } finally {
    loading.value = false
  }
}

// ===== Create =====
function openCreate() {
  createForm.value = {
    name: '',
    category: '',
    price: null,
    priceStr: '',
    description: '',
    status: 'ON_SHELF',
    file: null,
    previewUrl: ''
  }
  creating.value = true
}

function closeCreate() {
  if (createForm.value.previewUrl) {
    URL.revokeObjectURL(createForm.value.previewUrl)
  }
  creating.value = false
}

function onCreateFile(e) {
  const file = e.target.files?.[0]
  if (!file) return

  // Validate
  const maxSize = 5 * 1024 * 1024
  if (file.size > maxSize) {
    showAlert('图片文件不能超过5MB', '文件过大', 'error')
    return
  }
  const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp']
  if (!allowedTypes.includes(file.type)) {
    showAlert('仅支持 JPG、PNG、GIF、WEBP 格式的图片', '格式不支持', 'error')
    return
  }

  // Revoke old
  if (createForm.value.previewUrl) {
    URL.revokeObjectURL(createForm.value.previewUrl)
  }

  createForm.value.file = file
  createForm.value.previewUrl = URL.createObjectURL(file)
}

async function submitCreate() {
  // Validate
  if (!createForm.value.name || createForm.value.name.trim() === '') {
    showAlert('菜品名称不能为空', '验证失败', 'error')
    return
  }
  if (createForm.value.name.length > 50) {
    showAlert('菜品名称不能超过50个字符', '验证失败', 'error')
    return
  }
  const price = parseFloat(createForm.value.priceStr)
  if (!price || price <= 0) {
    showAlert('价格必须大于0', '验证失败', 'error')
    return
  }
  if (price > 99999.99) {
    showAlert('价格不能超过99999.99', '验证失败', 'error')
    return
  }

  try {
    // First create the item without image to get the ID
    const payload = {
      name: createForm.value.name,
      category: createForm.value.category || null,
      price: price,
      description: createForm.value.description || null,
      status: createForm.value.status,
      imageUrl: null
    }

    const res = await createMenuItem(restaurateurId.value, payload)
    if (!(res && res.status === 200)) {
      throw new Error(res?.message || '创建失败')
    }

    const itemId = res.data // Get the newly created item ID

    // Then upload image if provided
    if (createForm.value.file && itemId) {
      const fd = new FormData()
      fd.append('request', buildDataRequestBlob({
        restaurateurId: restaurateurId.value,
        itemId,
        dishName: createForm.value.name
      }))
      fd.append('file', createForm.value.file)
      
      const uploadRes = await fetch('/api/restaurateur/menu/upload-image', {
        method: 'POST',
        headers: buildAuthHeaders(),
        body: fd
      })
      
      if (uploadRes.ok) {
        const uploadData = await uploadRes.json()
        if (uploadData.status === 200) {
          const uploadedUrl = extractImageUrl(uploadData.data)
          if (uploadedUrl) {
            const updatePayload = {
              name: createForm.value.name,
              category: createForm.value.category || null,
              price: price,
              description: createForm.value.description || null,
              status: createForm.value.status,
              imageUrl: uploadedUrl
            }
            await updateMenuItem(restaurateurId.value, itemId, updatePayload)
          }
        }
      }
    }

    showAlert('添加成功！', '成功', 'success')
    closeCreate()
    await fetchPage()
  } catch (error) {
    console.error('创建错误:', error)
    showAlert('操作失败: ' + error.message, '创建失败', 'error')
  }
}

// ===== Search =====
function applySearch() {
  keywordApplied.value = keywordEditing.value
  categoryApplied.value = categoryEditing.value
  page.value = 1
  fetchPage()
}

// ===== Sort =====
function setSort(order) {
  if (sortOrder.value === order) {
    sortOrder.value = ''
  } else {
    sortOrder.value = order
  }
  page.value = 1
  fetchPage()
}

// ===== Pagination =====
function toPage(p) {
  if (p < 1 || p > totalPages.value) return
  page.value = p
  fetchPage()
}

// ===== Delete mode =====
function toggleDeleteMode() {
  deleteMode.value = !deleteMode.value
}

// ===== Card click (when not in delete mode, open edit) =====
function onCardClick(item) {
  if (deleteMode.value) return
  openEdit(item)
}

// ===== Edit =====
function openEdit(item) {
  editForm.value = {
    id: item.id,
    name: item.name,
    category: item.category || '',
    price: item.price,
    priceStr: String(item.price),
    description: item.description || '',
    status: item.status,
    file: null,
    previewUrl: '',
    imageUrl: item.imageUrl || ''
  }
  editing.value = true
}

function closeEdit() {
  if (editForm.value.previewUrl) {
    URL.revokeObjectURL(editForm.value.previewUrl)
  }
  editing.value = false
}

function onEditFile(e) {
  const file = e.target.files?.[0]
  if (!file) return

  const maxSize = 5 * 1024 * 1024
  if (file.size > maxSize) {
    showAlert('图片文件不能超过5MB', '文件过大', 'error')
    return
  }
  const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp']
  if (!allowedTypes.includes(file.type)) {
    showAlert('仅支持 JPG、PNG、GIF、WEBP 格式的图片', '格式不支持', 'error')
    return
  }

  if (editForm.value.previewUrl) {
    URL.revokeObjectURL(editForm.value.previewUrl)
  }

  editForm.value.file = file
  editForm.value.previewUrl = URL.createObjectURL(file)
}

async function saveEdit() {
  if (!editForm.value.name || editForm.value.name.trim() === '') {
    showAlert('菜品名称不能为空', '验证失败', 'error')
    return
  }
  if (editForm.value.name.length > 50) {
    showAlert('菜品名称不能超过50个字符', '验证失败', 'error')
    return
  }
  const price = parseFloat(editForm.value.priceStr)
  if (!price || price <= 0) {
    showAlert('价格必须大于0', '验证失败', 'error')
    return
  }
  if (price > 99999.99) {
    showAlert('价格不能超过99999.99', '验证失败', 'error')
    return
  }

  try {
    let imageUrl = editForm.value.imageUrl
    if (editForm.value.file) {
      const fd = new FormData()
      fd.append('request', buildDataRequestBlob({
        restaurateurId: restaurateurId.value,
        itemId: editForm.value.id,
        dishName: editForm.value.name
      }))
      fd.append('file', editForm.value.file)
      const uploadRes = await fetch('/api/restaurateur/menu/upload-image', {
        method: 'POST',
        headers: buildAuthHeaders(),
        body: fd
      })
      if (!uploadRes.ok) throw new Error('上传失败')
      const uploadData = await uploadRes.json()
      if (uploadData.status !== 200) throw new Error(uploadData.message || '上传失败')
      const uploadedUrl = extractImageUrl(uploadData.data)
      if (!uploadedUrl) throw new Error('上传失败')
      imageUrl = uploadedUrl
    }

    const payload = {
      name: editForm.value.name,
      category: editForm.value.category || null,
      price: price,
      description: editForm.value.description || null,
      status: editForm.value.status,
      imageUrl: imageUrl || null
    }

    const res = await updateMenuItem(restaurateurId.value, editForm.value.id, payload)
    if (!(res && res.status === 200)) {
      throw new Error(res?.message || '更新失败')
    }

    showAlert('更新成功！', '成功', 'success')
    closeEdit()
    await fetchPage()
  } catch (error) {
    console.error('更新错误:', error)
    showAlert('操作失败: ' + error.message, '更新失败', 'error')
  }
}

// ===== Delete =====
function askRemove(item) {
  confirmingItem.value = item
  confirming.value = true
}

async function remove() {
  if (!confirmingItem.value) return
  try {
    const res = await deleteMenuItem(restaurateurId.value, confirmingItem.value.id)
    if (!(res && res.status === 200)) {
      throw new Error(res?.message || '删除失败')
    }
    showAlert('删除成功！', '成功', 'success')
    confirming.value = false
    confirmingItem.value = null
    await fetchPage()
  } catch (error) {
    console.error('删除错误:', error)
    showAlert('删除失败: ' + error.message, '删除失败', 'error')
  }
}

function cancelRemove() {
  confirming.value = false
  confirmingItem.value = null
}

// ===== Helpers =====
function format(val) {
  if (val == null) return '0.00'
  return (+val).toFixed(2)
}

function statusClass(status) {
  return status === 'OFF_SHELF' ? 'off-shelf' : ''
}

function statusText(status) {
  return status === 'ON_SHELF' ? '上架' : '下架'
}

function onImageError(ev) {
  const img = ev?.target
  const fallback = resolveImageSrc('')
  if (img && img.src !== fallback) {
    img.src = fallback
  }
}

onMounted(() => {
  fetchPage()
})
</script>
<style scoped src="../styles/RestaurateurDishes.css"></style>
