<template>
  <div class="featured-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">精选应用</h2>
        <p class="page-subtitle">发现优质的 AI 生成应用，一键体验</p>
      </div>
      <div class="header-right">
        <div class="search-box">
          <el-icon class="search-icon"><Search /></el-icon>
          <input
            v-model="searchKeyword"
            placeholder="搜索应用名称"
            class="search-input"
            @keyup.enter="fetchApps"
          />
          <el-button v-if="searchKeyword" text :icon="Close" @click="clearSearch" />
        </div>
      </div>
    </div>

    <div class="featured-banner">
      <div class="banner-content">
        <div class="banner-icon">
          <el-icon :size="28"><Star /></el-icon>
        </div>
        <div class="banner-text">
          <h3>精选推荐</h3>
          <p>由管理员精心挑选的高质量应用模板</p>
        </div>
      </div>
      <div class="banner-stats">
        <div class="stat-item">
          <span class="stat-num">{{ total }}</span>
          <span class="stat-label">个精选应用</span>
        </div>
      </div>
    </div>

    <div v-loading="loading" class="apps-grid">
      <div
        v-for="app in appList"
        :key="app.id"
        class="app-card"
        @click="handleAppClick(app)"
      >
        <div class="app-cover">
          <img v-if="app.cover" :src="app.cover" :alt="app.appName" loading="lazy" />
          <div v-else class="cover-placeholder">
            <el-icon :size="48"><Picture /></el-icon>
          </div>
          <div class="card-overlay">
            <div class="overlay-actions">
              <el-button v-if="app.deployKey" :icon="View" class="overlay-btn preview-btn" @click.stop="handlePreview(app)">
                预览
              </el-button>
              <el-button type="primary" :icon="VideoPlay" class="overlay-btn" @click.stop="handleAppClick(app)">
                立即使用
              </el-button>
            </div>
          </div>
          <div v-if="app.priority" class="featured-badge">
            <el-icon :size="12"><Star /></el-icon>
            精选
          </div>
        </div>
        <div class="app-info">
          <div class="app-title-row">
            <h3 class="app-name">{{ app.appName }}</h3>
          </div>
          <div class="app-meta-row">
            <span class="app-type-tag">{{ getTypeLabel(app.codeGenType) }}</span>
            <span class="app-date">
              <el-icon :size="12"><Clock /></el-icon>
              {{ formatDate(app.createTime) }}
            </span>
          </div>
        </div>
      </div>

      <div v-if="!loading && appList.length === 0" class="empty-state">
        <el-empty description="暂无精选应用" :image-size="120">
          <template #image>
            <div class="empty-icon">
              <el-icon :size="64"><Star /></el-icon>
            </div>
          </template>
        </el-empty>
      </div>
    </div>

    <div v-if="total > 0" class="pagination-wrap">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="prev, pager, next"
        @size-change="fetchApps"
        @current-change="fetchApps"
        background
      />
    </div>

    <el-dialog v-model="showPreviewDialog" :title="previewAppName" width="90%" class="preview-dialog" top="5vh" destroy-on-close>
      <div class="preview-container">
        <div class="preview-toolbar">
          <div class="preview-url" :title="previewUrl">
            <el-icon :size="14"><Lock /></el-icon>
            <span>{{ previewUrl }}</span>
          </div>
          <el-button size="small" :icon="Open" @click="openInNewTab">
            新窗口打开
          </el-button>
        </div>
        <div class="preview-frame-wrapper">
          <iframe
            v-if="showPreviewDialog"
            :src="previewUrl"
            class="preview-iframe"
            frameborder="0"
            sandbox="allow-same-origin allow-scripts allow-forms allow-popups"
          ></iframe>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Picture, Clock, Star, VideoPlay, Close, View, Lock, Open } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { listFeaturedApps } from '@/api/app'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const appList = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(12)
const searchKeyword = ref('')

const getTypeLabel = (type) => {
  const map = {
    html: 'HTML 单页',
    multi_file: '多文件项目',
    vue_project: 'Vue 项目'
  }
  return map[type] || type
}

const formatDate = (date) => {
  if (!date) return '-'
  return dayjs(date).format('YYYY-MM-DD')
}

const fetchApps = async () => {
  loading.value = true
  try {
    const res = await listFeaturedApps({
      appName: searchKeyword.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    appList.value = res.list || []
    total.value = res.total || 0
  } catch (error) {
    console.error('获取精选应用失败:', error)
  } finally {
    loading.value = false
  }
}

const clearSearch = () => {
  searchKeyword.value = ''
  pageNum.value = 1
  fetchApps()
}

const handleAppClick = (app) => {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录后使用')
    router.push('/login')
    return
  }
  router.push(`/chat/${app.id}`)
}

const showPreviewDialog = ref(false)
const previewUrl = ref('')
const previewAppName = ref('')

const handlePreview = (app) => {
  if (!app.deployKey) {
    ElMessage.warning('该应用暂未部署，无法预览')
    return
  }
  previewAppName.value = app.appName
  previewUrl.value = `/api/static/${app.deployKey}/`
  showPreviewDialog.value = true
}

const openInNewTab = () => {
  if (previewUrl.value) {
    window.open(previewUrl.value, '_blank')
  }
}

onMounted(() => {
  fetchApps()
})
</script>

<style scoped>
.featured-page {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 16px;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.page-title {
  font-size: 26px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
  letter-spacing: -0.02em;
}

.page-subtitle {
  font-size: 14px;
  color: var(--text-tertiary);
  margin: 0;
}

.search-box {
  display: flex;
  align-items: center;
  width: 320px;
  height: 40px;
  padding: 0 14px;
  background: var(--bg-primary);
  border: 1px solid var(--border-primary);
  border-radius: 10px;
  transition: all 0.2s ease;
}

.search-box:focus-within {
  border-color: var(--primary);
  box-shadow: 0 0 0 4px var(--primary-light);
}

.search-icon {
  color: var(--text-placeholder);
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 14px;
  color: var(--text-primary);
  padding: 0 10px;
}

.search-input::placeholder {
  color: var(--text-placeholder);
}

.featured-banner {
  padding: 24px 28px;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.1) 0%, rgba(139, 92, 246, 0.1) 100%);
  border-radius: 14px;
  border: 1px solid rgba(99, 102, 241, 0.15);
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.banner-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.banner-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 16px rgba(99, 102, 241, 0.3);
}

.banner-text h3 {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 4px;
}

.banner-text p {
  font-size: 13px;
  color: var(--text-tertiary);
  margin: 0;
}

.banner-stats {
  display: flex;
  gap: 32px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-num {
  font-size: 28px;
  font-weight: 700;
  color: var(--primary);
  line-height: 1;
}

.stat-label {
  font-size: 12px;
  color: var(--text-tertiary);
}

.apps-grid {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  align-content: start;
}

.app-card {
  background: var(--bg-primary);
  border: 1px solid var(--border-primary);
  border-radius: 14px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.25s ease;
}

.app-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
  border-color: transparent;
}

.app-cover {
  position: relative;
  width: 100%;
  height: 180px;
  overflow: hidden;
  background: var(--bg-secondary);
}

.app-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s ease;
}

.app-card:hover .app-cover img {
  transform: scale(1.05);
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-quaternary);
}

.card-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.25s ease;
  backdrop-filter: blur(2px);
}

.app-card:hover .card-overlay {
  opacity: 1;
}

.overlay-btn {
  background: #fff;
  color: var(--text-primary);
  border: none;
  font-weight: 600;
  padding: 0 20px;
  height: 40px;
  border-radius: 20px;
  transition: all 0.2s ease;
}

.overlay-btn:hover {
  transform: scale(1.05);
  background: #fff;
  color: var(--primary);
}

.overlay-actions {
  display: flex;
  gap: 10px;
}

.preview-btn {
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(10px);
}

.preview-btn:hover {
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
  border-color: rgba(255, 255, 255, 0.6);
}

.featured-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: linear-gradient(135deg, #f59e0b, #ef4444);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  border-radius: 20px;
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.4);
}

.app-info {
  padding: 16px 18px 18px;
}

.app-title-row {
  margin-bottom: 8px;
}

.app-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.app-type-tag {
  font-size: 12px;
  color: var(--primary);
  background: var(--primary-light);
  padding: 3px 10px;
  border-radius: 20px;
  font-weight: 500;
}

.app-date {
  font-size: 12px;
  color: var(--text-quaternary);
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.empty-state {
  grid-column: 1 / -1;
  padding: 60px 0;
}

.empty-icon {
  color: var(--text-quaternary);
  opacity: 0.5;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 28px;
  padding-top: 8px;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .search-box {
    width: 100%;
  }

  .featured-banner {
    flex-direction: column;
    gap: 16px;
    text-align: center;
  }

  .banner-stats {
    gap: 24px;
  }
}

.preview-dialog :deep(.el-dialog) {
  border-radius: 16px;
  overflow: hidden;
  max-height: 90vh;
}

.preview-dialog :deep(.el-dialog__header) {
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-secondary);
  margin-right: 0;
}

.preview-dialog :deep(.el-dialog__body) {
  padding: 0;
  max-height: calc(90vh - 60px);
  overflow: hidden;
}

.preview-container {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.preview-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border-secondary);
}

.preview-url {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-tertiary);
  background: var(--bg-primary);
  padding: 6px 12px;
  border-radius: 8px;
  border: 1px solid var(--border-primary);
  max-width: 60%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-frame-wrapper {
  flex: 1;
  background: #fff;
  min-height: 60vh;
}

.preview-iframe {
  width: 100%;
  height: 60vh;
  border: none;
}
</style>
