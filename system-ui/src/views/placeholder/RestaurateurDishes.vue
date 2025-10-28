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
import { buildDataRequestBlob } from '@/api/http'
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

<style scoped>
.card {
  padding: 10px 32px 100px 32px;
  background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%);
  border-radius: 20px;
  box-shadow: 0 8px 32px rgba(251, 191, 36, 0.15);
  min-height: 100vh;
  position: relative;
  /* allow vertical scrolling inside the card (like restaurant view) */
  overflow-y: auto;
  overflow-x: hidden;
  /* keep the card within the viewport and enable inner scrolling when content grows */
  max-height: calc(100vh - 40px);
}

.card::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -50%;
  width: 100%;
  height: 100%;
  background: radial-gradient(circle, rgba(251, 191, 36, 0.1) 0%, transparent 70%);
  animation: float 20s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translate(0, 0) rotate(0deg); }
  33% { transform: translate(30px, -30px) rotate(120deg); }
  66% { transform: translate(-20px, 20px) rotate(240deg); }
}

.page-header {
  animation: slideDown 0.6s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  z-index: 1;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.header-content {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 20px;
  box-shadow: 0 4px 20px rgba(251, 191, 36, 0.15);
  backdrop-filter: blur(10px);
  position: relative;
  overflow: hidden;
  height: 65px;
}

.header-content::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 4px;
  background: linear-gradient(90deg, #fbbf24, #f59e0b, #fbbf24);
  background-size: 200% 100%;
  animation: shimmer 3s linear infinite;
}

@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

.header-icon {
  font-size: 36px;
  animation: bounce 2s ease-in-out infinite;
  filter: drop-shadow(0 2px 4px rgba(251, 191, 36, 0.3));
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

.header-content h2 {
  margin: 0;
  font-size: 32px;
  font-weight: 800;
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 1px;
  text-shadow: 0 2px 4px rgba(245, 158, 11, 0.1);
}

.header-decoration {
  display: flex;
  margin-left: auto;
}

.header-decoration .deco {
  font-size: 24px;
  display: inline-block;
  animation: rotate 4s linear infinite;
  filter: drop-shadow(0 2px 4px rgba(251, 191, 36, 0.2));
}

.header-decoration .deco:nth-child(1) {
  animation-delay: 0.3s;
}

.header-decoration .deco:nth-child(2) {
  animation-delay: 0.3s;
}

.header-decoration .deco:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes rotate {
  0%, 100% { transform: rotate(0deg) scale(1); }
  25% { transform: rotate(10deg) scale(1.1); }
  50% { transform: rotate(0deg) scale(1.1); }
  75% { transform: rotate(-10deg) scale(1.1); }
}

.header-stats {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 2px;
  position: relative;
  height: 180px;
}

.stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 10px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(254, 243, 199, 0.8) 100%);
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(251, 191, 36, 0.12);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: default;
  border: 2px solid rgba(251, 191, 36, 0.2);
  position: relative;
  overflow: hidden;
}

.stat-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(251, 191, 36, 0.2), transparent);
  transition: left 0.5s;
}

.stat-item:hover::before {
  left: 100%;
}

.stat-item:hover {
  transform: translateY(-4px) scale(1.02);
  box-shadow: 0 8px 24px rgba(251, 191, 36, 0.25);
  border-color: rgba(251, 191, 36, 0.4);
}

.stat-icon {
  font-size: 20px;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.1));
}

.stat-label {
  font-size: 18px;
  font-weight: 600;
  color: #6b7280;
  text-transform: uppercase;
}

.stat-value {
  font-size: 24px;
  font-weight: 800;
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 20px;
  background: rgba(255, 255, 255, 0.8);
  padding: 5px;
  border-radius: 16px;
  backdrop-filter: blur(10px);
  box-shadow: 0 4px 16px rgba(251, 191, 36, 0.1);
}

.left-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

button, .button {
  padding: 12px 24px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(251, 191, 36, 0.3);
  position: relative;
  overflow: hidden;
}

button::before, .button::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.3);
  transform: translate(-50%, -50%);
  transition: width 0.6s, height 0.6s;
}

button:hover::before, .button:hover::before {
  width: 300px;
  height: 300px;
}

button:hover, .button:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 20px rgba(251, 191, 36, 0.4);
}

button:active, .button:active {
  transform: translateY(-1px);
}

button:disabled, .button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

.primary {
  background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%) !important;
  box-shadow: 0 4px 12px rgba(251, 191, 36, 0.4) !important;
}

.primary:hover:not(:disabled) {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%) !important;
  box-shadow: 0 8px 24px rgba(251, 191, 36, 0.5) !important;
}

.danger {
  background: linear-gradient(135deg, #f87171 0%, #ef4444 100%) !important;
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.4) !important;
}

.danger:hover:not(:disabled) {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%) !important;
  box-shadow: 0 8px 24px rgba(239, 68, 68, 0.5) !important;
}

.ghost {
  background: #f3f4f6 !important;
  color: #6b7280 !important;
}

.ghost:hover {
  background: #e5e7eb !important;
}

.ml8 {
  margin-left: 8px;
}

.mt8 {
  margin-top: 8px;
}

.search-sort {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.input, .select, .textarea {
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
  font-family: inherit;
}

.input:focus, .select:focus, .textarea:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.sorters {
  display: flex;
  gap: 4px;
}

.icon-btn {
  padding: 6px 12px !important;
  background: #f3f4f6 !important;
  color: #6b7280 !important;
  min-width: 36px;
}

.icon-btn.active {
  background: #3b82f6 !important;
  color: white !important;
}

.header-bar {
  padding-bottom: 16px;
  border-bottom: 1px solid #e5e7eb;
}

.cat-radios {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.cat-radios label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #374151;
  cursor: pointer;
}

.cat-radios input[type="radio"] {
  cursor: pointer;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  gap: 20px;
  color: #6b7280;
  font-size: 16px;
  animation: fadeIn 0.5s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.spinner {
  width: 48px;
  height: 48px;
  border: 4px solid rgba(251, 191, 36, 0.2);
  border-top-color: #fbbf24;
  border-right-color: #f59e0b;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  box-shadow: 0 0 20px rgba(251, 191, 36, 0.3);
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 30px;
  margin-bottom: 10px;
  min-height: 200px;
}

@media (max-width: 1400px) {
  .cards-grid {
    grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
    gap: 20px;
  }
}

@media (max-width: 1200px) {
  .cards-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 18px;
  }
}

@media (max-width: 900px) {
  .cards-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }
}

@media (max-width: 600px) {
  .cards-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }
}

.empty-state {
  grid-column: 1 / -1;
  text-align: center;
  padding: 80px 20px;
  color: #9ca3af;
  animation: fadeInScale 0.6s ease-out;
}

@keyframes fadeInScale {
  from {
    opacity: 0;
    transform: scale(0.9);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 20px;
  animation: swing 2s ease-in-out infinite;
  display: inline-block;
  filter: drop-shadow(0 4px 8px rgba(156, 163, 175, 0.3));
}

@keyframes swing {
  0%, 100% { transform: rotate(0deg); }
  25% { transform: rotate(10deg); }
  75% { transform: rotate(-10deg); }
}

.empty-state p {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
  color: #6b7280;
}

.product-card {
  position: relative;
  background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%);
  border-radius: 20px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 16px rgba(251, 191, 36, 0.15);
  border: 2px solid rgba(251, 191, 36, 0.2);
  animation: cardFadeIn 0.5s ease-out backwards;
}

@keyframes cardFadeIn {
  from {
    opacity: 0;
    transform: translateY(20px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.card-item:nth-child(1) .product-card { animation-delay: 0.05s; }
.card-item:nth-child(2) .product-card { animation-delay: 0.1s; }
.card-item:nth-child(3) .product-card { animation-delay: 0.15s; }
.card-item:nth-child(4) .product-card { animation-delay: 0.2s; }
.card-item:nth-child(5) .product-card { animation-delay: 0.25s; }
.card-item:nth-child(6) .product-card { animation-delay: 0.3s; }
.card-item:nth-child(7) .product-card { animation-delay: 0.35s; }
.card-item:nth-child(8) .product-card { animation-delay: 0.4s; }
.card-item:nth-child(9) .product-card { animation-delay: 0.45s; }
.card-item:nth-child(10) .product-card { animation-delay: 0.5s; }
.card-item:nth-child(11) .product-card { animation-delay: 0.55s; }
.card-item:nth-child(12) .product-card { animation-delay: 0.6s; }

.product-card.off-shelf {
  background: linear-gradient(135deg, #fef2f2 0%, #fee2e2 100%);
  border-color: rgba(239, 68, 68, 0.2);
}

.product-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(251, 191, 36, 0.1) 0%, transparent 100%);
  opacity: 0;
  transition: opacity 0.5s;
  z-index: 1;
  pointer-events: none;
}

.product-card:hover::before {
  opacity: 1;
}

.product-card:hover {
  transform: translateY(-12px) scale(1.03);
  box-shadow: 0 20px 40px rgba(251, 191, 36, 0.3);
  border-color: rgba(251, 191, 36, 0.6);
}

.product-card.off-shelf:hover {
  box-shadow: 0 20px 40px rgba(239, 68, 68, 0.3);
  border-color: rgba(239, 68, 68, 0.6);
}

.product-card .thumb {
  width: 100%;
  height: 0;
  padding-bottom: 56.25%; /* 16:9 比例 */
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
}

.product-card .thumb img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.8s cubic-bezier(0.4, 0, 0.2, 1);
}

.product-card:hover .thumb img {
  transform: scale(1.15) rotate(3deg);
}

.delete-x {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  cursor: pointer;
  z-index: 10;
  font-size: 22px;
  line-height: 1;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  border: 3px solid white;
  padding: 0;
  box-shadow: 0 4px 12px rgba(220, 38, 38, 0.4);
  animation: deleteXPulse 2s ease-in-out infinite;
}

@keyframes deleteXPulse {
  0%, 100% { 
    box-shadow: 0 4px 12px rgba(220, 38, 38, 0.4);
    transform: scale(1);
  }
  50% { 
    box-shadow: 0 4px 20px rgba(220, 38, 38, 0.6);
    transform: scale(1.05);
  }
}

.delete-x:hover {
  background: linear-gradient(135deg, #dc2626 0%, #b91c1c 100%);
  transform: scale(1.3) rotate(90deg);
  box-shadow: 0 8px 24px rgba(220, 38, 38, 0.6);
  animation: none;
}

.product-card .body {
  padding: 16px;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(10px);
}

.product-card .title {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 8px;
  gap: 12px;
}

.product-card .name {
  font-size: 17px;
  font-weight: 700;
  color: #92400e;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
  text-shadow: 0 1px 2px rgba(146, 64, 14, 0.1);
}

.product-card .price {
  font-size: 20px;
  font-weight: 800;
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-left: 8px;
  white-space: nowrap;
}

.product-card .meta {
  font-size: 12px;
  color: #6b7280;
}

.ellipsis {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.product-card .status-badge {
  display: inline-block;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  background: #d1fae5;
  color: #065f46;
}

.product-card .status-badge.off-shelf {
  background: #fee2e2;
  color: #991b1b;
}

.pager {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  margin-top: 32px;
  padding: 20px 0;
  flex-wrap: wrap;
}

.page-btn {
  min-width: 44px !important;
  padding: 10px 16px !important;
  background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%) !important;
  font-size: 16px !important;
}

.page-btn span {
  display: inline-block;
  transition: transform 0.3s;
}

.page-btn:hover:not(:disabled) span {
  transform: scale(1.2);
}

.page-numbers {
  display: flex;
  gap: 8px;
  align-items: center;
}

.page-number {
  min-width: 44px;
  height: 44px;
  padding: 8px 12px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  border: 2px solid rgba(251, 191, 36, 0.3);
  background: rgba(255, 255, 255, 0.9);
  color: #f59e0b;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 8px rgba(251, 191, 36, 0.1);
}

.page-number:hover:not(:disabled):not(.ellipsis) {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  border-color: rgba(251, 191, 36, 0.6);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(251, 191, 36, 0.2);
}

.page-number.active {
  background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%);
  color: white;
  border-color: #f59e0b;
  box-shadow: 0 4px 16px rgba(251, 191, 36, 0.4);
  transform: scale(1.1);
  font-weight: 700;
}

.page-number.ellipsis {
  border: none;
  background: transparent;
  cursor: default;
  box-shadow: none;
  color: #9ca3af;
}

.page-info {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-left: 8px;
  padding: 10px 20px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  color: #6b7280;
  border: 2px solid rgba(251, 191, 36, 0.2);
  backdrop-filter: blur(10px);
}

.page-info .separator {
  color: rgba(251, 191, 36, 0.5);
  font-weight: 400;
}

.pager span {
  font-size: 14px;
  color: #6b7280;
}

.modal {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
  animation: modalFadeIn 0.3s ease-out;
}

@keyframes modalFadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.modal-content, .dialog {
  background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%);
  border-radius: 24px;
  padding: 32px;
  width: 90%;
  max-width: 750px;
  max-height: 90vh;
  overflow: auto;
  box-shadow: 0 24px 64px rgba(251, 191, 36, 0.3);
  border: 2px solid rgba(251, 191, 36, 0.3);
  animation: modalSlideUp 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes modalSlideUp {
  from {
    opacity: 0;
    transform: translateY(40px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.dialog h3 {
  margin: 0 0 24px;
  font-size: 24px;
  font-weight: 700;
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  text-align: center;
}

.dialog p {
  margin: 0 0 20px;
  font-size: 14px;
  color: #6b7280;
}

.create-layout {
  display: grid;
  grid-template-columns: 200px 1fr;
  gap: 24px;
  margin-bottom: 20px;
}

.create-layout .left {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.create-layout .right {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-grid {
  display: grid;
  grid-template-columns: 200px 1fr;
  gap: 24px;
  margin-bottom: 20px;
}

.form-grid .img-field {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.form-grid .fields {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field.two {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.field.two > div {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field label {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
}

.required {
  color: #ef4444;
}

.field .count {
  text-align: right;
  font-size: 12px;
  color: #9ca3af;
  margin-top: -4px;
}

.textarea {
  resize: vertical;
  min-height: 80px;
}

.imgbox {
  width: 200px;
  height: 150px;
  border-radius: 16px;
  overflow: hidden;
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  border: 3px dashed rgba(251, 191, 36, 0.4);
  transition: all 0.3s ease;
  box-shadow: inset 0 2px 8px rgba(251, 191, 36, 0.1);
}

.imgbox:hover {
  border-color: rgba(251, 191, 36, 0.8);
  transform: scale(1.05);
}

.imgbox.large {
  height: 200px;
  border: none;
}

.imgbox img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.imgbox .placeholder {
  color: #9ca3af;
  font-size: 14px;
}

.upload-btn {
  display: inline-block;
  padding: 10px 20px;
  background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%);
  color: white;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  text-align: center;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 12px rgba(251, 191, 36, 0.3);
}

.upload-btn:hover {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(251, 191, 36, 0.4);
}

.upload-btn input[type="file"] {
  display: none;
}

.tip {
  font-size: 12px;
  color: #6b7280;
  margin: 0;
}

.tips {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.tip.error {
  color: #ef4444;
}

.actions, .form-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 20px;
}

.delete-hint {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  border-left: 5px solid #f59e0b;
  padding: 16px 20px;
  margin-top: 20px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  color: #92400e;
  box-shadow: 0 4px 16px rgba(251, 191, 36, 0.2);
  animation: hintSlideIn 0.5s ease-out;
  display: flex;
  align-items: center;
  gap: 12px;
}

.delete-hint::before {
  content: '⚠️';
  font-size: 20px;
  animation: hintBounce 2s ease-in-out infinite;
}

@keyframes hintSlideIn {
  from {
    opacity: 0;
    transform: translateX(-20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes hintBounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-4px); }
}

@media (max-width: 1200px) {
  .cards-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 18px;
  }
  
  .header-stats {
    flex-wrap: wrap;
  }
  
  .page-numbers {
    flex-wrap: wrap;
  }
}

@media (max-width: 768px) {
  .cards-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .create-layout, .form-grid {
    grid-template-columns: 1fr;
  }
  
  .header-content {
    flex-wrap: wrap;
    justify-content: center;
    text-align: center;
  }
  
  .header-decoration {
    margin-left: 0;
  }
  
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  
  .search-sort {
    flex-direction: column;
    align-items: stretch;
  }
  
  .pager {
    flex-direction: column;
    gap: 16px;
  }
  
  .page-info {
    margin-left: 0;
  }
}

@media (max-width: 480px) {
  .cards-grid {
    grid-template-columns: 1fr;
  }
  
  .header-content h2 {
    font-size: 24px;
  }
  
  .stat-item {
    padding: 16px;
  }
  
  .stat-icon {
    font-size: 28px;
  }
  
  .stat-value {
    font-size: 24px;
  }
}

/* 自定义滚动条 - 优化版本 */
* {
  scrollbar-width: thin;
  scrollbar-color: #fbbf24 rgba(251, 191, 36, 0.1);
}

*::-webkit-scrollbar {
  width: 12px;
  height: 12px;
}

*::-webkit-scrollbar-track {
  background: linear-gradient(135deg, rgba(254, 243, 199, 0.3) 0%, rgba(253, 230, 138, 0.3) 100%);
  border-radius: 10px;
  margin: 4px;
}

*::-webkit-scrollbar-thumb {
  background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%);
  border-radius: 10px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  transition: all 0.3s ease;
  box-shadow: inset 0 0 6px rgba(0, 0, 0, 0.1);
}

*::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  border-color: rgba(255, 255, 255, 0.5);
  box-shadow: inset 0 0 8px rgba(0, 0, 0, 0.2);
}

*::-webkit-scrollbar-thumb:active {
  background: linear-gradient(135deg, #d97706 0%, #b45309 100%);
}

*::-webkit-scrollbar-corner {
  background: transparent;
}

/* Card-scoped scrollbar styling to match restaurant scrollbar */
.card::-webkit-scrollbar {
  width: 12px;
}
.card::-webkit-scrollbar-track {
  background: rgba(255,255,255,0.6);
  border-radius: 12px;
  margin: 6px 0;
}
.card::-webkit-scrollbar-thumb {
  background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%);
  border-radius: 12px;
  border: 3px solid rgba(255,255,255,0.6);
  box-shadow: inset 0 0 6px rgba(0,0,0,0.08);
}
.card::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
}
.card {
  /* Firefox */
  scrollbar-width: thin;
  scrollbar-color: #f59e0b rgba(0,0,0,0.06);
}

/* 美化原生alert (通过覆盖样式) */
@supports (-webkit-appearance: none) {
  dialog {
    border-radius: 24px !important;
    border: 2px solid rgba(251, 191, 36, 0.3) !important;
    background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%) !important;
    box-shadow: 0 24px 64px rgba(251, 191, 36, 0.3) !important;
    padding: 32px !important;
  }
  
  dialog::backdrop {
    background: rgba(0, 0, 0, 0.6) !important;
    backdrop-filter: blur(8px) !important;
  }
}

/* 自定义Alert弹窗 */
.alert-modal {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(12px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  animation: modalFadeIn 0.3s ease-out;
}

.alert-box {
  background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%);
  border-radius: 24px;
  padding: 40px;
  min-width: 400px;
  max-width: 500px;
  box-shadow: 0 24px 64px rgba(251, 191, 36, 0.4);
  border: 2px solid rgba(251, 191, 36, 0.4);
  animation: alertPopIn 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
}

@keyframes alertPopIn {
  0% {
    opacity: 0;
    transform: scale(0.8) translateY(-20px);
  }
  100% {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.alert-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  font-weight: bold;
  animation: iconBounce 0.6s ease-out;
}

@keyframes iconBounce {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

.alert-icon.success {
  background: linear-gradient(135deg, #d1fae5 0%, #a7f3d0 100%);
  color: #065f46;
  box-shadow: 0 8px 24px rgba(16, 185, 129, 0.3);
}

.alert-icon.error {
  background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%);
  color: #991b1b;
  box-shadow: 0 8px 24px rgba(239, 68, 68, 0.3);
}

.alert-icon.info {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  color: #1e40af;
  box-shadow: 0 8px 24px rgba(59, 130, 246, 0.3);
}

.alert-content {
  text-align: center;
}

.alert-title {
  margin: 0 0 12px;
  font-size: 24px;
  font-weight: 700;
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.alert-text {
  margin: 0;
  font-size: 16px;
  color: #6b7280;
  line-height: 1.6;
}

.alert-close {
  padding: 14px 48px !important;
  background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%) !important;
  color: white !important;
  border-radius: 16px !important;
  font-size: 16px !important;
  font-weight: 700 !important;
  box-shadow: 0 6px 20px rgba(251, 191, 36, 0.4) !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
}

.alert-close:hover {
  transform: translateY(-3px) !important;
  box-shadow: 0 10px 28px rgba(251, 191, 36, 0.5) !important;
}
</style>
