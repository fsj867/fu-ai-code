<template>
  <div class="apps-page">
    <div class="page-header-section">
      <div class="header-info">
        <h1 class="page-title">我的应用</h1>
        <p class="page-desc">管理你的 AI 生成项目，随时继续创作</p>
      </div>
      <el-button type="primary" :icon="Plus" class="new-app-btn" @click="showAddDialog = true">
        新建应用
      </el-button>
    </div>

    <div class="toolbar">
      <div class="search-box">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索应用名称..."
          :prefix-icon="Search"
          clearable
          @keyup.enter="fetchApps"
          @clear="fetchApps"
          class="search-input"
        />
      </div>
      <div class="filter-tabs">
        <el-button
          v-for="tab in typeTabs"
          :key="tab.value"
          :type="activeType === tab.value ? 'primary' : 'default'"
          size="default"
          @click="handleTypeChange(tab.value)"
          class="filter-tab"
        >
          {{ tab.label }}
        </el-button>
      </div>
    </div>

    <div v-loading="loading" class="apps-grid">
      <div
        v-for="app in appList"
        :key="app.id"
        class="app-card"
        @click="goToChat(app.id)"
      >
        <div class="app-cover">
          <img v-if="fixImageUrl(app.cover)" :src="fixImageUrl(app.cover)" :alt="app.appName" @error="app.cover = ''" />
          <div v-else class="cover-placeholder">
            <div class="cover-icon">
              <el-icon :size="36"><Document /></el-icon>
            </div>
            <span class="cover-type">{{ getTypeLabel(app.codeGenType) }}</span>
          </div>
          <div class="cover-overlay">
            <div class="overlay-actions">
              <el-button type="primary" size="small" @click.stop="goToChat(app.id)">
                开始对话
              </el-button>
              <el-button
                v-if="app.deployKey"
                size="small"
                @click.stop="handlePreview(app)"
                class="preview-btn"
              >
                预览
              </el-button>
            </div>
          </div>
        </div>
        <div class="app-body">
          <div class="app-header">
            <h3 class="app-name" :title="app.appName">{{ app.appName }}</h3>
            <span class="status-dot" :class="{ deployed: app.deployKey }"></span>
          </div>
          <div class="app-meta">
            <el-tag size="small" :type="getTypeTagType(app.codeGenType)" effect="light" class="type-tag">
              {{ getTypeLabel(app.codeGenType) }}
            </el-tag>
            <span v-if="app.deployKey" class="deploy-tag">
              <el-icon><CircleCheck /></el-icon>
              已部署
            </span>
          </div>
          <div class="app-footer">
            <span class="create-time">
              <el-icon :size="12"><Clock /></el-icon>
              {{ formatDate(app.createTime) }}
            </span>
            <div class="action-buttons" @click.stop>
              <el-tooltip v-if="app.deployKey" content="预览" placement="top">
                <el-button :icon="View" circle size="small" @click.stop="handlePreview(app)" />
              </el-tooltip>
              <el-tooltip content="部署" placement="top">
                <el-button :icon="UploadFilled" circle size="small" @click.stop="handleDeploy(app)" />
              </el-tooltip>
              <el-tooltip content="下载" placement="top">
                <el-button :icon="Download" circle size="small" @click.stop="handleDownload(app)" />
              </el-tooltip>
              <el-tooltip content="删除" placement="top">
                <el-button type="danger" :icon="Delete" circle size="small" @click.stop="handleDelete(app)" />
              </el-tooltip>
            </div>
          </div>
        </div>
      </div>

      <div v-if="!loading && appList.length === 0" class="empty-state">
        <div class="empty-icon">
          <el-icon :size="64"><FolderOpened /></el-icon>
        </div>
        <h3 class="empty-title">暂无应用</h3>
        <p class="empty-desc">点击右上角「新建应用」开始你的第一个 AI 项目</p>
        <el-button type="primary" :icon="Plus" @click="showAddDialog = true">
          创建第一个应用
        </el-button>
      </div>
    </div>

    <div v-if="total > 0" class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[9, 18, 36]"
        layout="total, prev, pager, next"
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

    <el-dialog v-model="showAddDialog" title="新建应用" width="560px" class="app-dialog">
      <el-form :model="addForm" :rules="addRules" ref="addFormRef" label-width="100px">
        <el-form-item label="应用名称" prop="appName">
          <el-input v-model="addForm.appName" placeholder="给你的应用起个名字" size="large" />
        </el-form-item>
        <el-form-item label="生成类型" prop="codeGenType">
          <div class="type-selector">
            <div
              v-for="type in typeOptions"
              :key="type.value"
              :class="['type-option', { active: addForm.codeGenType === type.value }]"
              @click="addForm.codeGenType = type.value"
            >
              <div class="type-icon" :style="{ background: type.color }">
                <el-icon :size="20">{{ type.icon }}</el-icon>
              </div>
              <div class="type-info">
                <span class="type-name">{{ type.label }}</span>
                <span class="type-desc">{{ type.desc }}</span>
              </div>
              <div v-if="addForm.codeGenType === type.value" class="type-check">
                <el-icon><CircleCheck /></el-icon>
              </div>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="初始需求" prop="initPrompt">
          <el-input
            v-model="addForm.initPrompt"
            type="textarea"
            :rows="4"
            placeholder="描述你想要生成的功能，越详细效果越好..."
            resize="none"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false" size="large">取消</el-button>
        <el-button type="primary" :loading="addLoading" @click="handleAdd" size="large">创建应用</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch, markRaw } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  Search,
  Clock,
  Delete,
  UploadFilled,
  Download,
  FolderOpened,
  Document,
  CircleCheck,
  EditPen,
  Picture,
  View,
  Lock,
  Open
} from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import {
  listMyApps,
  addApp,
  updateApp,
  deleteApp,
  deployApp,
  downloadAppCode
} from '@/api/app'

const router = useRouter()
const loading = ref(false)
const appList = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(9)
const searchKeyword = ref('')
const activeType = ref('all')

const typeTabs = [
  { label: '全部', value: 'all' },
  { label: 'HTML 单页', value: 'html' },
  { label: '多文件', value: 'multi_file' },
  { label: 'Vue 项目', value: 'vue_project' }
]

const typeOptions = [
  { value: 'html', label: 'HTML 单页', desc: '简单快速，适合单页面需求', icon: markRaw(Document), color: 'linear-gradient(135deg, #f59e0b, #f97316)' },
  { value: 'multi_file', label: '多文件项目', desc: '多文件结构，适合复杂需求', icon: markRaw(FolderOpened), color: 'linear-gradient(135deg, #3b82f6, #6366f1)' },
  { value: 'vue_project', label: 'Vue 项目', desc: '完整工程，企业级架构', icon: markRaw(EditPen), color: 'linear-gradient(135deg, #10b981, #14b8a6)' }
]

const showAddDialog = ref(false)
const addFormRef = ref(null)
const addLoading = ref(false)
const addForm = reactive({
  appName: '',
  codeGenType: 'html',
  initPrompt: ''
})
const addRules = {
  appName: [{ required: true, message: '请输入应用名称', trigger: 'blur' }],
  codeGenType: [{ required: true, message: '请选择生成类型', trigger: 'change' }]
}

const getTypeLabel = (type) => {
  const map = {
    html: 'HTML 单页',
    multi_file: '多文件项目',
    vue_project: 'Vue 项目'
  }
  return map[type] || type
}

const getTypeTagType = (type) => {
  const map = {
    html: 'warning',
    multi_file: '',
    vue_project: 'success'
  }
  return map[type] || ''
}

const formatDate = (date) => {
  if (!date) return '-'
  return dayjs(date).format('YYYY-MM-DD')
}

const fixImageUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  if (url.startsWith('//')) return 'https:' + url
  if (url.includes('myqcloud.com') || url.includes('cos.')) {
    return 'https://' + url.replace(/^\/+/, '')
  }
  return url
}

const fetchApps = async () => {
  loading.value = true
  try {
    const res = await listMyApps({
      appName: searchKeyword.value,
      codeGenType: activeType.value === 'all' ? undefined : activeType.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    appList.value = res.list || []
    total.value = Number(res.total) || 0
  } catch (error) {
    console.error('获取应用列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleTypeChange = (type) => {
  activeType.value = type
  pageNum.value = 1
  fetchApps()
}

watch([pageNum, pageSize], () => {
  fetchApps()
})

const goToChat = (appId) => {
  router.push(`/chat/${appId}`)
}

const handleAdd = async () => {
  if (!addFormRef.value) return
  try {
    await addFormRef.value.validate()
    addLoading.value = true
    await addApp(addForm)
    ElMessage.success('创建成功')
    showAddDialog.value = false
    addForm.appName = ''
    addForm.codeGenType = 'html'
    addForm.initPrompt = ''
    fetchApps()
  } catch (error) {
    console.error('创建应用失败:', error)
  } finally {
    addLoading.value = false
  }
}

const handleDelete = async (app) => {
  try {
    await ElMessageBox.confirm(`确定要删除应用「${app.appName}」吗？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    })
    await deleteApp({ id: app.id })
    ElMessage.success('删除成功')
    fetchApps()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除应用失败:', error)
    }
  }
}

const showPreviewDialog = ref(false)
const previewUrl = ref('')
const previewAppName = ref('')

const handlePreview = (app) => {
  if (!app.deployKey) {
    ElMessage.warning('请先部署应用后再预览')
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

const handleDeploy = async (app) => {
  try {
    const deployUrl = await deployApp({ appId: app.id })
    ElMessage.success('部署成功')
    fetchApps()
    if (deployUrl) {
      window.open(deployUrl, '_blank')
    }
  } catch (error) {
    console.error('部署失败:', error)
  }
}

const handleDownload = (app) => {
  const url = downloadAppCode(app.id)
  window.open(url, '_blank')
}

onMounted(() => {
  fetchApps()
})
</script>

<style scoped>
.apps-page {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.page-header-section {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 24px;
}

.header-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
  letter-spacing: -0.02em;
}

.page-desc {
  font-size: 14px;
  color: var(--text-tertiary);
  margin: 0;
}

.new-app-btn {
  height: 40px;
  padding: 0 20px;
  border-radius: 10px;
  font-weight: 500;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  border: none;
  transition: all 0.2s ease;
}

.new-app-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(99, 102, 241, 0.35);
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  gap: 16px;
}

.search-box {
  flex-shrink: 0;
}

.search-input {
  width: 320px;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px var(--border-primary);
  transition: all 0.2s ease;
}

.search-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--text-placeholder);
}

.search-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.25);
}

.filter-tabs {
  display: flex;
  gap: 4px;
  padding: 4px;
  background: var(--bg-tertiary);
  border-radius: 10px;
}

.filter-tab {
  border: none !important;
  background: transparent !important;
  font-weight: 500;
}

.filter-tab:not(.el-button--primary) {
  color: var(--text-tertiary);
}

.filter-tab.el-button--primary {
  background: var(--bg-primary) !important;
  color: var(--primary) !important;
  box-shadow: var(--shadow-sm);
}

.apps-grid {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  align-content: start;
}

.app-card {
  background: var(--bg-primary);
  border-radius: 16px;
  border: 1px solid var(--border-primary);
  overflow: hidden;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
}

.app-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
  border-color: transparent;
}

.app-cover {
  position: relative;
  width: 100%;
  height: 160px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  overflow: hidden;
}

.app-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.app-card:hover .app-cover img {
  transform: scale(1.05);
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: rgba(255, 255, 255, 0.9);
}

.cover-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  justify-content: center;
}

.cover-type {
  font-size: 13px;
  font-weight: 500;
}

.cover-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0.2) 0%, rgba(0, 0, 0, 0.5) 100%);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding-bottom: 20px;
  opacity: 0;
  transition: opacity 0.25s ease;
}

.app-card:hover .cover-overlay {
  opacity: 1;
}

.overlay-actions {
  transform: translateY(10px);
  transition: transform 0.25s ease;
}

.app-card:hover .overlay-actions {
  transform: translateY(0);
}

.app-body {
  padding: 16px 18px 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.app-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--text-placeholder);
  flex-shrink: 0;
}

.status-dot.deployed {
  background: var(--success);
  box-shadow: 0 0 8px rgba(16, 185, 129, 0.5);
}

.app-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.type-tag {
  --el-tag-padding-horizontal: 8px;
  --el-tag-height: 22px;
  font-size: 11px;
}

.deploy-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: var(--success);
  font-weight: 500;
}

.app-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid var(--border-secondary);
}

.create-time {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-quaternary);
}

.action-buttons {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.app-card:hover .action-buttons {
  opacity: 1;
}

.action-buttons :deep(.el-button) {
  width: 28px;
  height: 28px;
  padding: 0;
}

.action-buttons :deep(.el-button .el-icon) {
  font-size: 14px;
}

.empty-state {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
}

.empty-icon {
  width: 96px;
  height: 96px;
  border-radius: 24px;
  background: var(--primary-light);
  color: var(--primary);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}

.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 8px;
}

.empty-desc {
  font-size: 14px;
  color: var(--text-tertiary);
  margin: 0 0 24px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 32px;
  padding-top: 16px;
  border-top: 1px solid var(--border-secondary);
}

.app-dialog :deep(.el-dialog) {
  border-radius: 16px;
}

.app-dialog :deep(.el-dialog__header) {
  padding: 24px 24px 20px;
  border-bottom: 1px solid var(--border-secondary);
}

.app-dialog :deep(.el-dialog__title) {
  font-size: 18px;
  font-weight: 600;
}

.app-dialog :deep(.el-dialog__body) {
  padding: 24px;
}

.app-dialog :deep(.el-dialog__footer) {
  padding: 16px 24px 24px;
  border-top: 1px solid var(--border-secondary);
}

.type-selector {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
}

.type-option {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border: 2px solid var(--border-primary);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
}

.type-option:hover {
  border-color: var(--text-placeholder);
  background: var(--bg-secondary);
}

.type-option.active {
  border-color: var(--primary);
  background: var(--primary-light);
}

.type-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.type-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
}

.type-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.type-desc {
  font-size: 12px;
  color: var(--text-tertiary);
}

.type-check {
  color: var(--primary);
  font-size: 20px;
}

.preview-btn {
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(10px);
}

.preview-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  color: #fff;
  border-color: rgba(255, 255, 255, 0.6);
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
