<template>
  <div class="app-detail-page" v-loading="loading">
    <div class="detail-hero" v-if="app">
      <div class="hero-bg"></div>
      <div class="hero-content">
        <div class="back-btn" @click="goBack">
          <el-icon :size="18"><ArrowLeft /></el-icon>
          <span>返回</span>
        </div>
        <div class="hero-main">
          <div class="app-cover-large">
            <img v-if="app.cover" :src="app.cover" :alt="app.appName" />
            <div v-else class="cover-placeholder">
              <el-icon :size="48"><Picture /></el-icon>
            </div>
          </div>
          <div class="app-header-info">
            <div class="app-title-row">
              <h1 class="app-name">{{ app.appName }}</h1>
              <el-tag class="type-tag">{{ getTypeLabel(app.codeGenType) }}</el-tag>
            </div>
            <div class="app-desc">
              <p v-if="app.initPrompt">{{ app.initPrompt }}</p>
              <p v-else class="no-desc">暂无描述</p>
            </div>
            <div class="app-actions">
              <el-button type="primary" :icon="ChatDotRound" class="btn-primary-large" @click="goToChat">
                开始对话
              </el-button>
              <el-button v-if="app.deployKey" :icon="View" class="btn-secondary-large" @click="handlePreview">
                预览应用
              </el-button>
              <el-button :icon="Download" class="btn-secondary-large" @click="handleDownload">
                下载代码
              </el-button>
              <el-button :icon="UploadFilled" class="btn-secondary-large" @click="handleDeploy">
                部署应用
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="detail-content" v-if="app">
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-icon primary">
            <el-icon :size="20"><Key /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-label">应用 ID</span>
            <span class="stat-value">{{ app.id }}</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon success">
            <el-icon :size="20"><CircleCheck /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-label">部署状态</span>
            <span class="stat-value">
              <el-tag v-if="app.deployKey" type="success" size="small">已部署</el-tag>
              <el-tag v-else type="info" size="small">未部署</el-tag>
            </span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon warning">
            <el-icon :size="20"><Calendar /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-label">创建时间</span>
            <span class="stat-value small">{{ formatDate(app.createTime) }}</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon info">
            <el-icon :size="20"><Clock /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-label">更新时间</span>
            <span class="stat-value small">{{ formatDate(app.updateTime) }}</span>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="showPreviewDialog" title="应用预览" width="90%" class="preview-dialog" top="5vh" destroy-on-close>
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
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Picture, ChatDotRound, Download, UploadFilled, ArrowLeft, Key, CircleCheck, Calendar, Clock, View, Lock, Open } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { getAppDetail, downloadAppCode, deployApp } from '@/api/app'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const app = ref(null)
const showPreviewDialog = ref(false)
const previewUrl = ref('')

const handlePreview = () => {
  if (!app.value?.deployKey) {
    ElMessage.warning('请先部署应用后再预览')
    return
  }
  previewUrl.value = `/api/static/${app.value.deployKey}/`
  showPreviewDialog.value = true
}

const openInNewTab = () => {
  if (previewUrl.value) {
    window.open(previewUrl.value, '_blank')
  }
}

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
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
}

const fetchAppDetail = async () => {
  const id = route.params.id
  if (!id) return
  loading.value = true
  try {
    app.value = await getAppDetail(id)
  } catch (error) {
    console.error('获取应用详情失败:', error)
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.back()
}

const goToChat = () => {
  router.push(`/chat/${app.value.id}`)
}

const handleDownload = () => {
  const url = downloadAppCode(app.value.id)
  window.open(url, '_blank')
}

const handleDeploy = async () => {
  try {
    const deployUrl = await deployApp({ appId: app.value.id })
    ElMessage.success('部署成功')
    fetchAppDetail()
    if (deployUrl) {
      window.open(deployUrl, '_blank')
    }
  } catch (error) {
    console.error('部署失败:', error)
  }
}

onMounted(() => {
  fetchAppDetail()
})
</script>

<style scoped>
.app-detail-page {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.detail-hero {
  position: relative;
  margin-bottom: 24px;
  border-radius: 16px;
  overflow: hidden;
}

.hero-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.1) 0%, rgba(139, 92, 246, 0.1) 100%);
  border-radius: 16px;
}

.hero-content {
  position: relative;
  padding: 24px;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: var(--bg-primary);
  border: 1px solid var(--border-primary);
  border-radius: 8px;
  font-size: 13px;
  color: var(--text-secondary);
  cursor: pointer;
  margin-bottom: 20px;
  transition: all 0.15s ease;
}

.back-btn:hover {
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

.hero-main {
  display: flex;
  gap: 28px;
  align-items: flex-start;
}

.app-cover-large {
  width: 340px;
  height: 220px;
  border-radius: 14px;
  overflow: hidden;
  background: var(--bg-primary);
  border: 1px solid var(--border-primary);
  flex-shrink: 0;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.app-cover-large img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-quaternary);
}

.app-header-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

.app-title-row {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.app-name {
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
  letter-spacing: -0.02em;
}

.type-tag {
  background: var(--primary-light);
  color: var(--primary);
  border: none;
  font-weight: 500;
  padding: 4px 12px;
}

.app-desc {
  min-height: 48px;
}

.app-desc p {
  color: var(--text-secondary);
  line-height: 1.7;
  margin: 0;
  font-size: 14px;
}

.no-desc {
  color: var(--text-quaternary);
  font-style: italic;
}

.app-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.btn-primary-large {
  height: 44px;
  padding: 0 24px;
  border-radius: 10px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  border: none;
  font-weight: 600;
  font-size: 14px;
  transition: all 0.2s ease;
}

.btn-primary-large:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(99, 102, 241, 0.35);
}

.btn-secondary-large {
  height: 44px;
  padding: 0 24px;
  border-radius: 10px;
  background: var(--bg-primary);
  border: 1px solid var(--border-primary);
  color: var(--text-secondary);
  font-weight: 500;
  font-size: 14px;
  transition: all 0.2s ease;
}

.btn-secondary-large:hover {
  background: var(--bg-tertiary);
  border-color: var(--text-placeholder);
  color: var(--text-primary);
}

.detail-content {
  flex: 1;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  padding: 20px;
  background: var(--bg-primary);
  border: 1px solid var(--border-primary);
  border-radius: 14px;
  display: flex;
  align-items: center;
  gap: 14px;
  transition: all 0.2s ease;
}

.stat-card:hover {
  border-color: var(--border-secondary);
  box-shadow: var(--shadow-sm);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon.primary {
  background: var(--primary-light);
  color: var(--primary);
}

.stat-icon.success {
  background: var(--success-light);
  color: var(--success);
}

.stat-icon.warning {
  background: var(--warning-light);
  color: var(--warning);
}

.stat-icon.info {
  background: #e0e7ff;
  color: #6366f1;
}

.stat-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.stat-label {
  font-size: 12px;
  color: var(--text-tertiary);
  font-weight: 500;
}

.stat-value {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.stat-value.small {
  font-size: 14px;
  font-weight: 500;
}

@media (max-width: 1024px) {
  .hero-main {
    flex-direction: column;
  }

  .app-cover-large {
    width: 100%;
    height: 200px;
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .stats-grid {
    grid-template-columns: 1fr;
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
