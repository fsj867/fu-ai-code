<template>
  <div class="apps-page">
    <div class="page-header">
      <h2 class="page-title">我的应用</h2>
      <el-button type="primary" :icon="Plus" @click="showAddDialog = true">
        新建应用
      </el-button>
    </div>

    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索应用名称"
        :prefix-icon="Search"
        clearable
        @keyup.enter="fetchApps"
        @clear="fetchApps"
        style="width: 300px"
      />
    </div>

    <div v-loading="loading" class="apps-grid">
      <el-card
        v-for="app in appList"
        :key="app.id"
        class="app-card"
        shadow="hover"
        @click="goToChat(app.id)"
      >
        <div class="app-cover">
          <img v-if="fixImageUrl(app.cover)" :src="fixImageUrl(app.cover)" :alt="app.appName" @error="app.cover = ''" />
          <div v-else class="cover-placeholder">
            <el-icon :size="48"><Picture /></el-icon>
          </div>
        </div>
        <div class="app-info">
          <div class="app-name-row">
            <h3 class="app-name">{{ app.appName }}</h3>
            <el-tag v-if="app.deployKey" type="success" size="small">已部署</el-tag>
            <el-tag v-else type="info" size="small">未部署</el-tag>
          </div>
          <p class="app-type">{{ getTypeLabel(app.codeGenType) }}</p>
          <div class="app-meta">
            <span class="meta-item">
              <el-icon><Clock /></el-icon>
              {{ formatDate(app.createTime) }}
            </span>
          </div>
        </div>
        <div class="app-actions" @click.stop>
          <el-button size="small" :icon="ChatDotRound" @click="goToChat(app.id)">
            对话
          </el-button>
          <el-button size="small" type="success" :icon="UploadFilled" @click="handleDeploy(app)">
            部署
          </el-button>
          <el-button size="small" :icon="Download" @click="handleDownload(app)">
            下载
          </el-button>
          <el-button
            size="small"
            type="danger"
            :icon="Delete"
            @click="handleDelete(app)"
          >
            删除
          </el-button>
        </div>
      </el-card>

      <el-empty v-if="!loading && appList.length === 0" description="暂无应用，点击右上角新建" />
    </div>

    <div v-if="total > 0" class="pagination">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
      />
    </div>

    <el-dialog v-model="showAddDialog" title="新建应用" width="500px">
      <el-form :model="addForm" :rules="addRules" ref="addFormRef" label-width="100px">
        <el-form-item label="应用名称" prop="appName">
          <el-input v-model="addForm.appName" placeholder="请输入应用名称" />
        </el-form-item>
        <el-form-item label="生成类型" prop="codeGenType">
          <el-select v-model="addForm.codeGenType" placeholder="请选择生成类型" style="width: 100%">
            <el-option label="HTML 单页" value="html" />
            <el-option label="多文件项目" value="multi_file" />
            <el-option label="Vue 项目" value="vue_project" />
          </el-select>
        </el-form-item>
        <el-form-item label="初始化 Prompt" prop="initPrompt">
          <el-input
            v-model="addForm.initPrompt"
            type="textarea"
            :rows="4"
            placeholder="请输入应用的初始化提示词"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" :loading="addLoading" @click="handleAdd">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showEditDialog" title="编辑应用" width="500px">
      <el-form :model="editForm" :rules="editRules" ref="editFormRef" label-width="100px">
        <el-form-item label="应用名称" prop="appName">
          <el-input v-model="editForm.appName" placeholder="请输入应用名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="handleUpdate">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  Search,
  Picture,
  Clock,
  Edit,
  Delete,
  ChatDotRound,
  UploadFilled,
  Download
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
const pageSize = ref(10)
const searchKeyword = ref('')

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

const showEditDialog = ref(false)
const editFormRef = ref(null)
const editLoading = ref(false)
const editForm = reactive({
  id: null,
  appName: ''
})
const editRules = {
  appName: [{ required: true, message: '请输入应用名称', trigger: 'blur' }]
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
  return dayjs(date).format('YYYY-MM-DD')
}

const fetchApps = async () => {
  loading.value = true
  try {
    const res = await listMyApps({
      appName: searchKeyword.value,
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

watch([pageNum, pageSize], () => {
  fetchApps()
})

const fixImageUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  if (url.startsWith('//')) return 'https:' + url
  if (url.includes('myqcloud.com') || url.includes('cos.')) {
    return 'https://' + url.replace(/^\/+/, '')
  }
  return url
}

const goToChat = (appId) => {
  router.push(`/chat/${appId}`)
}

const handleEdit = (app) => {
  editForm.id = app.id
  editForm.appName = app.appName
  showEditDialog.value = true
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

const handleUpdate = async () => {
  if (!editFormRef.value) return
  try {
    await editFormRef.value.validate()
    editLoading.value = true
    await updateApp({ id: editForm.id, appName: editForm.appName })
    ElMessage.success('更新成功')
    showEditDialog.value = false
    fetchApps()
  } catch (error) {
    console.error('更新应用失败:', error)
  } finally {
    editLoading.value = false
  }
}

const handleDelete = async (app) => {
  try {
    await ElMessageBox.confirm(`确定要删除应用「${app.appName}」吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
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
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.search-bar {
  margin-bottom: 20px;
}

.apps-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.app-card {
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  overflow: hidden;
}

.app-card:hover {
  transform: translateY(-4px);
}

.app-cover {
  width: 100%;
  height: 160px;
  overflow: hidden;
  border-radius: 8px 8px 0 0;
  margin: -20px -20px 16px;
  background-color: #f3f4f6;
}

.app-cover img {
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

.app-info {
  margin-bottom: 12px;
}

.app-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.app-name {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-type {
  font-size: 13px;
  color: #6b7280;
  margin: 0 0 8px;
}

.app-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #9ca3af;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.app-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding-top: 12px;
  border-top: 1px solid #f3f4f6;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
