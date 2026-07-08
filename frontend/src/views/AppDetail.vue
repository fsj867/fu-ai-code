<template>
  <div class="app-detail-page" v-loading="loading">
    <el-page-header @back="goBack" content="应用详情" class="page-header" />
    
    <el-card v-if="app" class="detail-card">
      <div class="app-header">
        <div class="app-cover-large">
          <img v-if="app.cover" :src="app.cover" :alt="app.appName" />
          <div v-else class="cover-placeholder">
            <el-icon :size="64"><Picture /></el-icon>
          </div>
        </div>
        <div class="app-header-info">
          <h1 class="app-name">{{ app.appName }}</h1>
          <el-tag size="large" class="type-tag">{{ getTypeLabel(app.codeGenType) }}</el-tag>
          <div class="app-desc">
            <p v-if="app.initPrompt">{{ app.initPrompt }}</p>
            <p v-else class="no-desc">暂无描述</p>
          </div>
          <div class="app-actions">
            <el-button type="primary" size="large" :icon="ChatDotRound" @click="goToChat">
              开始对话
            </el-button>
            <el-button size="large" :icon="Download" @click="handleDownload">
              下载代码
            </el-button>
            <el-button size="large" :icon="UploadFilled" @click="handleDeploy">
              部署应用
            </el-button>
          </div>
        </div>
      </div>

      <el-divider />

      <div class="app-meta-grid">
        <div class="meta-item">
          <span class="meta-label">应用 ID</span>
          <span class="meta-value">{{ app.id }}</span>
        </div>
        <div class="meta-item">
          <span class="meta-label">创建时间</span>
          <span class="meta-value">{{ formatDate(app.createTime) }}</span>
        </div>
        <div class="meta-item">
          <span class="meta-label">更新时间</span>
          <span class="meta-value">{{ formatDate(app.updateTime) }}</span>
        </div>
        <div class="meta-item">
          <span class="meta-label">部署状态</span>
          <el-tag v-if="app.deployKey" type="success">已部署</el-tag>
          <el-tag v-else type="info">未部署</el-tag>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Picture, ChatDotRound, Download, UploadFilled } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { getAppDetail, downloadAppCode, deployApp } from '@/api/app'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const app = ref(null)

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
}

.page-header {
  margin-bottom: 20px;
}

.detail-card {
  border-radius: 12px;
}

.app-header {
  display: flex;
  gap: 32px;
}

.app-cover-large {
  width: 320px;
  height: 200px;
  border-radius: 12px;
  overflow: hidden;
  background-color: #f3f4f6;
  flex-shrink: 0;
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
  color: #9ca3af;
}

.app-header-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.app-name {
  font-size: 28px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 12px;
}

.type-tag {
  margin-bottom: 16px;
  width: fit-content;
}

.app-desc {
  flex: 1;
  margin-bottom: 20px;
}

.app-desc p {
  color: #6b7280;
  line-height: 1.6;
  margin: 0;
}

.no-desc {
  color: #9ca3af;
  font-style: italic;
}

.app-actions {
  display: flex;
  gap: 12px;
}

.app-meta-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.meta-label {
  font-size: 13px;
  color: #9ca3af;
}

.meta-value {
  font-size: 15px;
  color: #1f2937;
  font-weight: 500;
}
</style>
