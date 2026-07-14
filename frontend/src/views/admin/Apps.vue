<template>
  <div class="admin-apps-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">应用管理</h2>
        <p class="page-subtitle">管理所有用户创建的应用</p>
      </div>
    </div>

    <div class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="应用名称">
          <el-input v-model="searchForm.appName" placeholder="请输入应用名称" clearable class="search-input" />
        </el-form-item>
        <el-form-item label="生成类型">
          <el-select v-model="searchForm.codeGenType" placeholder="全部" clearable style="width: 150px">
            <el-option label="HTML 单页" value="html" />
            <el-option label="多文件项目" value="multi_file" />
            <el-option label="Vue 项目" value="vue_project" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="fetchApps" class="search-btn">搜索</el-button>
          <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="stats-row">
      <div class="stat-item">
        <div class="stat-icon primary">
          <el-icon :size="18"><Grid /></el-icon>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ total }}</span>
          <span class="stat-label">总应用数</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon success">
          <el-icon :size="18"><CircleCheck /></el-icon>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ deployedCount }}</span>
          <span class="stat-label">已部署</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon warning">
          <el-icon :size="18"><Star /></el-icon>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ featuredCount }}</span>
          <span class="stat-label">精选推荐</span>
        </div>
      </div>
    </div>

    <div class="table-card">
      <el-table :data="appList" v-loading="loading" stripe class="data-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="appName" label="应用名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="codeGenType" label="生成类型" width="130">
          <template #default="{ row }">
            <el-tag size="small" round :type="getTypeColor(row.codeGenType)">
              {{ getTypeLabel(row.codeGenType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="priority" label="优先级" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.priority && row.priority > 0" type="warning" size="small" round>
              {{ row.priority }}
            </el-tag>
            <span v-else class="muted-text">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="deployKey" label="部署状态" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.deployKey" type="success" size="small" round>已部署</el-tag>
            <el-tag v-else type="info" size="small" round>未部署</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" :icon="Edit" text @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button size="small" type="danger" :icon="Delete" text @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchApps"
          @current-change="fetchApps"
          background
        />
      </div>
    </div>

    <el-dialog v-model="showEditDialog" title="编辑应用" width="600px" class="form-dialog">
      <el-form :model="editForm" :rules="editRules" ref="editFormRef" label-width="110px">
        <el-form-item label="应用名称" prop="appName">
          <el-input v-model="editForm.appName" placeholder="请输入应用名称" />
        </el-form-item>
        <el-form-item label="应用封面" prop="cover">
          <el-input v-model="editForm.cover" placeholder="请输入封面图片URL" />
          <div v-if="editForm.cover" class="cover-preview">
            <img :src="editForm.cover" alt="封面预览" />
          </div>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-input-number v-model="editForm.priority" :min="0" :max="999" />
          <span class="hint-text">数值越大越靠前，0表示不推荐</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="handleUpdate" class="btn-primary-action">
          保存修改
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Edit, Delete, Grid, CircleCheck, Star } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import {
  adminListApps,
  adminUpdateApp,
  adminDeleteApp
} from '@/api/app'

const loading = ref(false)
const appList = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const searchForm = reactive({
  appName: '',
  codeGenType: ''
})

const deployedCount = computed(() => {
  return appList.value.filter(a => a.deployKey).length
})

const featuredCount = computed(() => {
  return appList.value.filter(a => a.priority && a.priority > 0).length
})

const showEditDialog = ref(false)
const editFormRef = ref(null)
const editLoading = ref(false)
const editForm = reactive({
  id: null,
  appName: '',
  cover: '',
  priority: 0
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

const getTypeColor = (type) => {
  const map = {
    html: 'danger',
    multi_file: 'warning',
    vue_project: 'primary'
  }
  return map[type] || 'info'
}

const formatDate = (date) => {
  if (!date) return '-'
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
}

const fetchApps = async () => {
  loading.value = true
  try {
    const res = await adminListApps({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      appName: searchForm.appName || undefined,
      codeGenType: searchForm.codeGenType || undefined
    })
    appList.value = res.list || []
    total.value = res.total || 0
  } catch (error) {
    console.error('获取应用列表失败:', error)
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchForm.appName = ''
  searchForm.codeGenType = ''
  pageNum.value = 1
  fetchApps()
}

const handleEdit = (row) => {
  editForm.id = row.id
  editForm.appName = row.appName
  editForm.cover = row.cover
  editForm.priority = row.priority || 0
  showEditDialog.value = true
}

const handleUpdate = async () => {
  if (!editFormRef.value) return
  try {
    await editFormRef.value.validate()
    editLoading.value = true
    await adminUpdateApp({
      id: editForm.id,
      appName: editForm.appName,
      cover: editForm.cover,
      priority: editForm.priority
    })
    ElMessage.success('更新成功')
    showEditDialog.value = false
    fetchApps()
  } catch (error) {
    console.error('更新应用失败:', error)
  } finally {
    editLoading.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除应用「${row.appName}」吗？`, '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    })
    await adminDeleteApp({ id: row.id })
    ElMessage.success('删除成功')
    fetchApps()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除应用失败:', error)
    }
  }
}

onMounted(() => {
  fetchApps()
})
</script>

<style scoped>
.admin-apps-page {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.page-header {
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
  letter-spacing: -0.02em;
}

.page-subtitle {
  font-size: 13px;
  color: var(--text-tertiary);
  margin: 0;
}

.search-card {
  background: var(--bg-primary);
  border: 1px solid var(--border-primary);
  border-radius: 12px;
  padding: 16px 20px;
  margin-bottom: 16px;
}

.search-card :deep(.el-form-item) {
  margin-bottom: 0;
}

.search-input {
  width: 240px;
}

.search-btn {
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  border: none;
}

.stats-row {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  background: var(--bg-primary);
  border: 1px solid var(--border-primary);
  border-radius: 12px;
  min-width: 160px;
  flex: 1;
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
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

.stat-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
}

.stat-label {
  font-size: 12px;
  color: var(--text-tertiary);
}

.table-card {
  flex: 1;
  background: var(--bg-primary);
  border: 1px solid var(--border-primary);
  border-radius: 12px;
  padding: 4px 4px 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.data-table {
  flex: 1;
}

.data-table :deep(.el-table__header th) {
  background: var(--bg-secondary);
  color: var(--text-secondary);
  font-weight: 600;
  font-size: 13px;
}

.data-table :deep(.el-table__row) {
  font-size: 14px;
}

.data-table :deep(.el-table--striped .el-table__body tr.el-table__row--striped td) {
  background: var(--bg-secondary);
}

.data-table :deep(.el-table td),
.data-table :deep(.el-table th.is-leaf) {
  border-bottom: 1px solid var(--border-secondary);
}

.muted-text {
  color: var(--text-quaternary);
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  padding: 16px 20px;
  border-top: 1px solid var(--border-secondary);
}

.form-dialog :deep(.el-dialog__header) {
  padding: 24px 24px 16px;
  border-bottom: 1px solid var(--border-secondary);
  margin-right: 0;
}

.form-dialog :deep(.el-dialog__title) {
  font-size: 18px;
  font-weight: 600;
}

.form-dialog :deep(.el-dialog__body) {
  padding: 24px;
}

.form-dialog :deep(.el-dialog__footer) {
  padding: 16px 24px 24px;
  border-top: 1px solid var(--border-secondary);
}

.btn-primary-action {
  height: 36px;
  padding: 0 20px;
  border-radius: 8px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  border: none;
  font-weight: 500;
  transition: all 0.2s ease;
}

.btn-primary-action:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.35);
}

.cover-preview {
  margin-top: 10px;
  width: 200px;
  height: 120px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid var(--border-primary);
}

.cover-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hint-text {
  margin-left: 8px;
  color: var(--text-tertiary);
  font-size: 12px;
}

@media (max-width: 768px) {
  .stats-row {
    flex-direction: column;
  }

  .stat-item {
    width: 100%;
  }
}
</style>
