<template>
  <div class="featured-page">
    <div class="page-header">
      <h2 class="page-title">精选应用</h2>
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
        @click="handleAppClick(app)"
      >
        <div class="app-cover">
          <img v-if="app.cover" :src="app.cover" :alt="app.appName" />
          <div v-else class="cover-placeholder">
            <el-icon :size="48"><Picture /></el-icon>
          </div>
          <div v-if="app.priority" class="featured-badge">
            <el-icon><Star /></el-icon>
            精选
          </div>
        </div>
        <div class="app-info">
          <h3 class="app-name">{{ app.appName }}</h3>
          <p class="app-type">{{ getTypeLabel(app.codeGenType) }}</p>
          <div class="app-meta">
            <span class="meta-item">
              <el-icon><Clock /></el-icon>
              {{ formatDate(app.createTime) }}
            </span>
          </div>
        </div>
      </el-card>

      <el-empty v-if="!loading && appList.length === 0" description="暂无精选应用" />
    </div>

    <div v-if="total > 0" class="pagination">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchApps"
        @current-change="fetchApps"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Picture, Clock, Star } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { listFeaturedApps } from '@/api/app'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const appList = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
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

const handleAppClick = (app) => {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录后使用')
    router.push('/login')
    return
  }
  router.push(`/chat/${app.id}`)
}

onMounted(() => {
  fetchApps()
})
</script>

<style scoped>
.featured-page {
  height: 100%;
}

.page-header {
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
  position: relative;
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

.featured-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: linear-gradient(135deg, #f59e0b, #ef4444);
  color: #fff;
  font-size: 12px;
  font-weight: 500;
  border-radius: 20px;
}

.app-info {
  margin-bottom: 12px;
}

.app-name {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 8px;
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

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
