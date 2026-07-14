<template>
  <div class="admin-users-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">用户管理</h2>
        <p class="page-subtitle">管理系统所有用户账号</p>
      </div>
      <el-button type="primary" :icon="Plus" class="btn-primary-action" @click="showAddDialog = true">
        新增用户
      </el-button>
    </div>

    <div class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="账号">
          <el-input v-model="searchForm.userAccount" placeholder="请输入账号" clearable class="search-input" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="fetchUsers" class="search-btn">搜索</el-button>
          <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="stats-row">
      <div class="stat-item">
        <div class="stat-icon primary">
          <el-icon :size="18"><User /></el-icon>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ total }}</span>
          <span class="stat-label">总用户数</span>
        </div>
      </div>
    </div>

    <div class="table-card">
      <el-table :data="userList" v-loading="loading" stripe class="data-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="userAccount" label="账号" min-width="150" />
        <el-table-column prop="userRole" label="角色" width="140">
          <template #default="{ row }">
            <div class="role-tag">
              <el-tag v-if="row.userRole === 'admin'" effect="dark" type="danger" size="small" round>
                管理员
              </el-tag>
              <el-tag v-else type="info" size="small" round>
                普通用户
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.updateTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
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
          @size-change="fetchUsers"
          @current-change="fetchUsers"
          background
        />
      </div>
    </div>

    <el-dialog v-model="showAddDialog" title="新增用户" width="500px" class="form-dialog">
      <el-form :model="addForm" :rules="addRules" ref="addFormRef" label-width="100px">
        <el-form-item label="账号" prop="userAccount">
          <el-input v-model="addForm.userAccount" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="角色" prop="userRole">
          <el-select v-model="addForm.userRole" style="width: 100%">
            <el-option label="普通用户" value="user" />
            <el-option label="管理员" value="admin" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" :loading="addLoading" @click="handleAdd" class="btn-primary-action">
          确认新增
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showEditDialog" title="编辑用户" width="500px" class="form-dialog">
      <el-form :model="editForm" :rules="editRules" ref="editFormRef" label-width="100px">
        <el-form-item label="账号" prop="userAccount">
          <el-input v-model="editForm.userAccount" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="角色" prop="userRole">
          <el-select v-model="editForm.userRole" style="width: 100%">
            <el-option label="普通用户" value="user" />
            <el-option label="管理员" value="admin" />
          </el-select>
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh, Edit, Delete, User } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import {
  listUserVOByPage,
  addUser,
  updateUser,
  deleteUser
} from '@/api/user'

const loading = ref(false)
const userList = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const searchForm = reactive({
  userAccount: ''
})

const showAddDialog = ref(false)
const addFormRef = ref(null)
const addLoading = ref(false)
const addForm = reactive({
  userAccount: '',
  userRole: 'user'
})
const addRules = {
  userAccount: [{ required: true, message: '请输入账号', trigger: 'blur' }]
}

const showEditDialog = ref(false)
const editFormRef = ref(null)
const editLoading = ref(false)
const editForm = reactive({
  id: null,
  userAccount: '',
  userRole: 'user'
})
const editRules = {
  userAccount: [{ required: true, message: '请输入账号', trigger: 'blur' }]
}

const formatDate = (date) => {
  if (!date) return '-'
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
}

const fetchUsers = async () => {
  loading.value = true
  try {
    const res = await listUserVOByPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      userAccount: searchForm.userAccount || undefined
    })
    userList.value = res.records || []
    total.value = res.totalRow || 0
  } catch (error) {
    console.error('获取用户列表失败:', error)
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchForm.userAccount = ''
  pageNum.value = 1
  fetchUsers()
}

const handleAdd = async () => {
  if (!addFormRef.value) return
  try {
    await addFormRef.value.validate()
    addLoading.value = true
    await addUser(addForm)
    ElMessage.success('新增成功')
    showAddDialog.value = false
    addForm.userAccount = ''
    addForm.userRole = 'user'
    fetchUsers()
  } catch (error) {
    console.error('新增用户失败:', error)
  } finally {
    addLoading.value = false
  }
}

const handleEdit = (row) => {
  editForm.id = row.id
  editForm.userAccount = row.userAccount
  editForm.userRole = row.userRole
  showEditDialog.value = true
}

const handleUpdate = async () => {
  if (!editFormRef.value) return
  try {
    await editFormRef.value.validate()
    editLoading.value = true
    await updateUser({
      id: editForm.id,
      userAccount: editForm.userAccount,
      userRole: editForm.userRole
    })
    ElMessage.success('更新成功')
    showEditDialog.value = false
    fetchUsers()
  } catch (error) {
    console.error('更新用户失败:', error)
  } finally {
    editLoading.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除用户「${row.userAccount}」吗？`, '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    })
    await deleteUser({ id: row.id })
    ElMessage.success('删除成功')
    fetchUsers()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除用户失败:', error)
    }
  }
}

onMounted(() => {
  fetchUsers()
})
</script>

<style scoped>
.admin-users-page {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
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

.btn-primary-action {
  height: 40px;
  padding: 0 20px;
  border-radius: 10px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  border: none;
  font-weight: 500;
  transition: all 0.2s ease;
}

.btn-primary-action:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.35);
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
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  background: var(--bg-primary);
  border: 1px solid var(--border-primary);
  border-radius: 12px;
  min-width: 200px;
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

.role-tag {
  display: flex;
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

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .stats-row {
    flex-direction: column;
  }

  .stat-item {
    width: 100%;
  }
}
</style>
