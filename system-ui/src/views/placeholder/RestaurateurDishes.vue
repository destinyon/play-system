<template>
  <div class="menu-page">
    <header class="menu-header">
      <div>
        <h2>菜品管理</h2>
        <p class="subtitle">维护菜品信息，支持分类查询与分页浏览。</p>
      </div>
      <button class="btn primary" @click="openCreate">新增菜品</button>
    </header>

    <section class="filters">
      <input
        v-model.trim="keyword"
        type="text"
        placeholder="输入关键词（名称 / 类别 / 描述）回车搜索"
        @keyup.enter="handleSearch"
      />
      <select v-model="categoryFilter" @change="handleSearch" :disabled="loading">
        <option value="">全部类别</option>
        <option v-for="option in categoryOptions" :key="option" :value="option">{{ option }}</option>
      </select>
      <select v-model="statusFilter" @change="handleSearch" :disabled="loading">
        <option value="">全部状态</option>
        <option value="ON_SHELF">上架</option>
        <option value="OFF_SHELF">下架</option>
      </select>
      <div class="filter-actions">
        <button class="btn" :disabled="loading" @click="handleSearch">查询</button>
        <button class="btn ghost" :disabled="loading" @click="resetFilters">重置</button>
      </div>
    </section>

    <section v-if="loading" class="loading-state">
      <span class="spinner"></span>
      正在加载菜品...
    </section>

    <section v-else>
      <div class="card-container">
        <div v-if="items.length === 0" class="empty-state">
          <img src="../assets/logo.svg" alt="empty" />
          <div>暂无菜品，点击“新增菜品”开始创建</div>
        </div>

        <div v-else class="card-grid">
          <div v-for="item in items" :key="item.id" :class="['menu-card', statusClass(item.status)]">
            <div class="card-image" @click="openDetail(item)">
              <img :src="item.imageUrl || placeholder" alt="菜品图片" @error="onImageError" />
            </div>
            <div class="card-body">
              <h3 class="card-title">{{ item.name }}</h3>
              <div class="category">{{ item.category || '未分类' }}</div>
              <p class="price">{{ formatPrice(item.price) }}</p>
              <div class="detail-tag" @click.stop="openDetail(item)">详情</div>
            </div>
            <div class="card-actions">
              <div>
                <button class="btn small" @click="openEdit(item)">编辑</button>
                <button class="btn danger small" @click="confirmDelete(item)">删除</button>
              </div>
              <div class="status-label">{{ item.status || 'UNKNOWN' }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="pagination" v-if="total > pageSize">
        <button class="btn" :disabled="page === 1" @click="changePage(page - 1)">上一页</button>
        <span>第 {{ page }} 页 / 共 {{ totalPages }} 页</span>
        <button class="btn" :disabled="page >= totalPages" @click="changePage(page + 1)">下一页</button>
      </div>
    </section>

    <!-- Detail Modal -->
    <div v-if="showDetail" class="modal-backdrop" @click.self="closeDetail">
      <div class="modal large">
        <header>
          <h3>菜品详情</h3>
          <button class="close" @click="closeDetail">×</button>
        </header>
        <div class="modal-content">
          <img :src="detailItem.imageUrl || placeholder" alt="详情图片" @error="onImageError" />
          <div class="info-grid">
            <label>名称</label>
            <div>{{ detailItem.name }}</div>
            <label>价格</label>
            <div>{{ formatPrice(detailItem.price) }}</div>
            <label>类别</label>
            <div>{{ detailItem.category || '未分类' }}</div>
            <label>状态</label>
            <div>{{ detailItem.status }}</div>
            <label>介绍</label>
            <div>{{ detailItem.description || '—' }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Form Modal -->
    <div v-if="showForm" class="modal-backdrop" @click.self="closeForm">
      <div class="modal">
        <header>
          <h3>{{ formMode === 'create' ? '新增菜品' : '编辑菜品' }}</h3>
          <button class="close" @click="closeForm">×</button>
        </header>
        <div class="form-body">
          <label>
            名称
            <input v-model.trim="form.name" />
          </label>
          <label>
            价格（元）
            <input v-model.number="form.price" type="number" />
          </label>
          <label>
            类别
            <input v-model.trim="form.category" />
          </label>
          <label>
            图片 URL
            <input v-model.trim="form.imageUrl" />
          </label>
          <label>
            状态
            <select v-model="form.status">
              <option value="ON_SHELF">上架</option>
              <option value="OFF_SHELF">下架</option>
            </select>
          </label>
          <label>
            介绍
            <textarea v-model.trim="form.description" rows="4"></textarea>
          </label>
          <div class="form-actions">
            <button class="btn ghost" @click="closeForm">取消</button>
            <button class="btn primary" @click="submitForm">保存</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { fetchMenuPage, fetchMenuCategories, createMenuItem, updateMenuItem, deleteMenuItem } from '@/api/menu'

// 使用内联 SVG 作为占位图，避免静态文件缺失导致的显示问题
const placeholder = 'data:image/svg+xml;utf8,\
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 150">\
  <defs>\
    <linearGradient id="g" x1="0" x2="1" y1="0" y2="1">\
      <stop offset="0%" stop-color="%23fff7ed"/>\
      <stop offset="100%" stop-color="%23fde68a"/>\
    </linearGradient>\
  </defs>\
  <rect width="200" height="150" fill="url(%23g)"/>\
  <g fill="%23a16207" opacity="0.6">\
    <circle cx="60" cy="90" r="26"/>\
    <rect x="90" y="70" width="64" height="24" rx="6"/>\
  </g>\
  <text x="100" y="128" font-size="12" fill="%2392550d" text-anchor="middle" font-family="Segoe UI, Arial">No Image</text>\
</svg>'

// 从登录信息获取商家ID，缺失时回退为 1（开发期）
const auth = useAuthStore()
const restaurateurId = computed(() => {
  const id = auth.restaurant?.id
  const n = typeof id === 'string' ? parseInt(id, 10) : id
  const num = typeof n === 'number' ? n : Number(n)
  return Number.isFinite(num) ? num : 1
})

const items = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(8)
const total = ref(0)
const keyword = ref('')
const categoryFilter = ref('')
const statusFilter = ref('')
const categoryOptions = ref([])

const showDetail = ref(false)
const detailItem = ref({})
const showForm = ref(false)
const formMode = ref('create')
const form = ref({ name: '', price: 0, category: '', imageUrl: '', status: 'ON_SHELF', description: '' })
// 保存编辑前的原始数据，用于避免未修改图片时被清空覆盖
const editingOriginal = ref(null)

function statusClass(status) {
  if (status === 'ON_SHELF') return 'status-online'
  if (status === 'OFF_SHELF') return 'status-offline'
  return 'status-default'
}

function formatPrice(value) {
  if (value == null) return '¥0.00'
  return `¥${(+value).toFixed(2)}`
}

async function loadData() {
  loading.value = true
  try {
    const params = {
      restaurateurId: restaurateurId.value,
      page: page.value,
      size: pageSize.value,
      keyword: keyword.value,
      category: categoryFilter.value,
      status: statusFilter.value
    }
    const res = await fetchMenuPage(params)
    if (res && res.status === 200 && res.data) {
      items.value = res.data.records || []
      total.value = res.data.total || 0
    } else {
      items.value = []
      total.value = 0
    }
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    const res = await fetchMenuCategories(restaurateurId.value)
    if (res && res.status === 200) {
      categoryOptions.value = res.data || []
    } else {
      categoryOptions.value = []
    }
  } catch (e) {
    categoryOptions.value = []
  }
}

function handleSearch() {
  page.value = 1
  loadData()
}

function resetFilters() {
  keyword.value = ''
  categoryFilter.value = ''
  statusFilter.value = ''
  handleSearch()
}

function changePage(p) {
  page.value = p
  loadData()
}

function openDetail(item) {
  detailItem.value = { ...item }
  showDetail.value = true
}
function closeDetail() {
  showDetail.value = false
}

function openCreate() {
  formMode.value = 'create'
  form.value = { name: '', price: 0, category: '', imageUrl: '', status: 'ON_SHELF', description: '' }
  editingOriginal.value = null
  showForm.value = true
}

function openEdit(item) {
  formMode.value = 'edit'
  form.value = { ...item }
  editingOriginal.value = { ...item }
  showForm.value = true
}

function closeForm() {
  showForm.value = false
}

async function submitForm() {
  try {
    if (formMode.value === 'create') {
      const payload = { ...form.value }
      if (!payload.imageUrl) delete payload.imageUrl
      const res = await createMenuItem(restaurateurId.value, payload)
      if (!(res && res.status === 200)) throw new Error(res?.message || '创建失败')
    } else {
      const payload = { ...form.value }
      if (editingOriginal.value) {
        const orig = editingOriginal.value
        if ((!payload.imageUrl || payload.imageUrl.trim() === '') && orig.imageUrl) {
          delete payload.imageUrl
        }
      }
      const res = await updateMenuItem(restaurateurId.value, form.value.id, payload)
      if (!(res && res.status === 200)) throw new Error(res?.message || '更新失败')
    }
    closeForm()
    loadData()
  } catch (e) {
    console.error(e)
    alert('保存失败，请查看控制台')
  }
}

function confirmDelete(item) {
  if (!confirm('确定要删除此菜品吗？（软删除）')) return
  deleteMenuItem(restaurateurId.value, item.id)
    .then((res) => {
      if (!(res && res.status === 200)) throw new Error(res?.message || '删除失败')
      return loadData()
    })
    .catch((e) => { console.error(e); alert('删除失败') })
}

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

onMounted(() => {
  loadCategories()
  loadData()
})

function onImageError(ev) {
  const img = ev?.target
  if (img && img.src !== placeholder) {
    img.src = placeholder
  }
}
</script>

<style scoped>
.menu-page {
  padding: 24px;
  min-height: 100%;
  display: flex;
  flex-direction: column;
  gap: 24px;
  background: radial-gradient(1400px 720px at 0% -20%, rgba(250, 204, 21, 0.45) 0%, transparent 60%),
    linear-gradient(160deg, rgba(254, 243, 199, 0.88) 0%, rgba(250, 204, 21, 0.16) 50%, rgba(253, 230, 138, 0.4) 100%);
}

.menu-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 22px;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(253, 224, 71, 0.45), rgba(252, 211, 77, 0.25));
  border: 1px solid rgba(234, 179, 8, 0.28);
  box-shadow: 0 12px 28px rgba(250, 204, 21, 0.18);
}

.menu-header h2 {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
  color: #78350f;
}

.subtitle {
  margin: 4px 0 0;
  color: #a16207;
  font-size: 14px;
}

.filters {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  align-items: center;
}

.filters input,
.filters select {
  padding: 10px 12px;
  border: 1px solid rgba(217, 119, 6, 0.3);
  border-radius: 8px;
  background: rgba(255, 251, 235, 0.9);
  font-size: 14px;
  transition: border-color 0.2s ease;
}

.filters input:focus,
.filters select:focus {
  outline: none;
  border-color: #f59e0b;
  box-shadow: 0 0 0 3px rgba(245, 158, 11, 0.25);
}

.filter-actions {
  display: flex;
  gap: 8px;
}

.card-container {
  max-height: calc(100vh - 280px);
  overflow-y: auto;
  padding-right: 4px;
  scrollbar-width: thin;
  scrollbar-color: rgba(234, 179, 8, 0.6) rgba(254, 243, 199, 0.35);
}

.card-container::-webkit-scrollbar {
  width: 8px;
}

.card-container::-webkit-scrollbar-track {
  background: rgba(254, 243, 199, 0.6);
  border-radius: 999px;
}

.card-container::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, rgba(250, 204, 21, 0.9), rgba(234, 179, 8, 0.85));
  border-radius: 999px;
}

.card-grid {
  display: grid;
  gap: 18px;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
}

.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  gap: 12px;
  color: #a16207;
  background: rgba(254, 243, 199, 0.8);
  border-radius: 18px;
  font-size: 15px;
}

.spinner {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 3px solid rgba(250, 204, 21, 0.18);
  border-top-color: #facc15;
  animation: spin 0.8s linear infinite;
}

.menu-card {
  position: relative;
  border-radius: 18px;
  overflow: hidden;
  min-height: 320px;
  background: #fffbea;
  border: 1px solid rgba(217, 119, 6, 0.12);
  box-shadow: 0 20px 36px rgba(146, 64, 14, 0.12);
  display: flex;
  flex-direction: column;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.menu-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 26px 46px rgba(146, 64, 14, 0.18);
}

.card-image {
  width: 100%;
  background: rgba(255, 247, 237, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  aspect-ratio: 4 / 3;
  cursor: pointer;
}

.card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.card-body {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
}

.card-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #92400e;
}

.price {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #ca8a04;
}

.category {
  margin: 0;
  font-size: 13px;
  color: #a16207;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.detail-tag {
  margin-top: 10px;
  align-self: flex-start;
  font-size: 12px;
  color: #854d0e;
  background: rgba(253, 224, 71, 0.42);
  padding: 4px 8px;
  border-radius: 999px;
  cursor: pointer;
  transition: background 0.2s ease;
}

.detail-tag:hover {
  background: rgba(250, 204, 21, 0.6);
}

.card-actions {
  display: flex;
  justify-content: space-between;
  padding: 12px 16px 16px;
}

.btn {
  padding: 8px 16px;
  border-radius: 999px;
  border: none;
  cursor: pointer;
  font-size: 14px;
  transition: transform 0.2s ease, box-shadow 0.2s ease, background-color 0.2s ease;
  background: rgba(253, 224, 71, 0.32);
  color: #78350f;
}

.btn.primary {
  background: linear-gradient(135deg, #fbbf24, #f59e0b);
  color: #fff;
  box-shadow: 0 12px 24px rgba(234, 179, 8, 0.38);
}

.btn.primary:hover {
  box-shadow: 0 16px 30px rgba(234, 179, 8, 0.46);
}

.btn.danger {
  background: rgba(250, 112, 112, 0.22);
  color: #b91c1c;
}

.btn.ghost {
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(234, 179, 8, 0.45);
}

.btn.small {
  padding: 6px 12px;
  font-size: 13px;
}

.btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
  box-shadow: none;
}

.menu-card.status-online {
  border-color: rgba(22, 163, 74, 0.35);
  background: linear-gradient(180deg, rgba(34, 197, 94, 0.12), #fefce8);
}

.menu-card.status-offline {
  border-color: rgba(250, 204, 21, 0.55);
  background: linear-gradient(180deg, rgba(250, 204, 21, 0.2), #fffdea);
}

.menu-card.status-default {
  border-color: rgba(191, 219, 254, 0.4);
  background: linear-gradient(180deg, rgba(254, 240, 138, 0.18), #fffbea);
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  color: #a16207;
  margin-top: auto;
}

.empty-state {
  padding: 64px 24px;
  border-radius: 18px;
  background: rgba(254, 243, 199, 0.85);
  text-align: center;
  color: #a16207;
  display: flex;
  flex-direction: column;
  gap: 16px;
  align-items: center;
}

.empty-state img {
  width: 48px;
  opacity: 0.75;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: grid;
  place-items: center;
  z-index: 1000;
  padding: 24px;
}

.modal {
  background: #fff;
  border-radius: 18px;
  width: min(600px, 100%);
  max-height: 90vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 30px 60px rgba(202, 138, 4, 0.42);
}

.modal.large {
  width: min(720px, 100%);
}

.modal header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 24px;
  border-bottom: 1px solid rgba(234, 179, 8, 0.3);
  background: linear-gradient(135deg, rgba(254, 249, 195, 0.9), rgba(253, 224, 71, 0.35));
}

.modal header h3 {
  margin: 0;
  font-size: 20px;
  color: #854d0e;
}

.modal .close {
  border: none;
  background: transparent;
  font-size: 20px;
  cursor: pointer;
  line-height: 1;
  color: #ca8a04;
}

.modal-content {
  padding: 24px;
  display: grid;
  gap: 24px;
}

.modal-content img {
  width: 100%;
  border-radius: 12px;
  object-fit: cover;
  max-height: 320px;
}

.info-grid {
  display: grid;
  grid-template-columns: 120px 1fr;
  gap: 12px;
  font-size: 14px;
  color: #78350f;
}

.info-grid label {
  color: #a16207;
}

.form-body {
  padding: 24px;
  display: grid;
  gap: 18px;
}

.form-body label {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 14px;
  color: #78350f;
}

.form-body input,
.form-body select,
.form-body textarea {
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid rgba(217, 119, 6, 0.35);
  font-size: 14px;
  transition: border-color 0.2s ease;
  background: rgba(255, 251, 235, 0.92);
}

.form-body input:focus,
.form-body select:focus,
.form-body textarea:focus {
  outline: none;
  border-color: #f59e0b;
  box-shadow: 0 0 0 3px rgba(245, 158, 11, 0.25);
}

.form-body .textarea textarea {
  resize: vertical;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 8px;
}

@media (max-width: 768px) {
  .menu-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  .filters {
    grid-template-columns: 1fr;
  }
  .card-actions {
    flex-direction: column;
    gap: 8px;
  }
  .info-grid {
    grid-template-columns: 1fr;
  }
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
/* 仅在小屏显示“查询/重置”按钮，大屏通过输入回车即可 */
@media (min-width: 769px) {
  .filter-actions { display: none; }
}
</style>
