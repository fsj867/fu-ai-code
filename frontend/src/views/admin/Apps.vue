<template>
  <div class="admin-apps-page">
    <div class="page-header">
      <h2 class="page-title">应用管理</h2>
    </div>

    <el-card class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="应用名称">
          <el-input v-model="searchForm.appName" placeholder="请输入应用名称" clearable />
        </el-form-item>
        <el-form-item label="生成类型">
          <el-select v-model="searchForm.codeGenType" placeholder="全部" clearable style="width: 150px">
            <el-option label="HTML 单页" value="html" />
            <el-option label="多文件项目" value="multi_file" />
            <el-option label="Vue 项目" value="vue_project" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="fetchApps">搜索</el-button>
          <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table :data="appList" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="appName" label="应用名称" min-width="150" />
        <el-table-column prop="codeGenType" label="生成类型" width="120">
          <template #default="{ row }">
            <el-tag size="small">{{ getTypeLabel(row.codeGenType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="priority" label="优先级" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.priority && row.priority > 0" type="warning" size="small">
              {{ row.priority }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="deployKey" label="部署状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.deployKey" type="success" size="small">已部署</el-tag>
            <el-tag v-else type="info" size="small">未部署</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" :icon="Edit" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button size="small" type="danger" :icon="Delete" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchApps"
          @current-change="fetchApps"
        />
      </div>
    </el-card>

    <el-dialog v-model="showEditDialog" title="编辑应用" width="600px">
      <el-form :model="editForm" :rules="editRules" ref="editFormRef" label-width="100px">
        <el-form-item label="应用名称" prop="appName">
          <el-input v-model="editForm.appName" placeholder="请输入应用名称" />
        </el-form-item>
        <el-form-item label="应用封面" prop="cover">
          <el-input v-model="editForm.cover" placeholder="请输入封面图片URL" />
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-input-number v-model="editForm.priority" :min="0" :max="999" />
          <span style="margin-left: 8px; color: #909399; font-size: 12px;">数值越大越靠前，0表示不推荐</span>
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Edit, Delete } from '@element-plus/icons-vue'
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
    await ElMessageBox.confirm(`确定要删除应用「${row.appName}」吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
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

.search-card {
  margin-bottom: 16px;
}

.table-card {
  border-radius: 8px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
