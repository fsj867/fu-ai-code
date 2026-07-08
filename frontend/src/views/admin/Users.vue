<template>
  <div class="admin-users-page">
    <div class="page-header">
      <h2 class="page-title">用户管理</h2>
      <el-button type="primary" :icon="Plus" @click="showAddDialog = true">
        新增用户
      </el-button>
    </div>

    <el-card class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="账号">
          <el-input v-model="searchForm.userAccount" placeholder="请输入账号" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="fetchUsers">搜索</el-button>
          <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table :data="userList" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="userAccount" label="账号" min-width="150" />
        <el-table-column prop="userRole" label="角色" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.userRole === 'admin'" type="danger">管理员</el-tag>
            <el-tag v-else type="info">普通用户</el-tag>
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
          @size-change="fetchUsers"
          @current-change="fetchUsers"
        />
      </div>
    </el-card>

    <el-dialog v-model="showAddDialog" title="新增用户" width="500px">
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
        <el-button type="primary" :loading="addLoading" @click="handleAdd">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showEditDialog" title="编辑用户" width="500px">
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
        <el-button type="primary" :loading="editLoading" @click="handleUpdate">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh, Edit, Delete } from '@element-plus/icons-vue'
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
    await ElMessageBox.confirm(`确定要删除用户「${row.userAccount}」吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
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
