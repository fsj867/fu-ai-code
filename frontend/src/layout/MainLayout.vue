<template>
  <el-container class="main-layout">
    <el-aside :width="sidebarWidth" class="sidebar">
      <div class="sidebar-inner">
        <div class="logo-section">
          <div class="logo-icon">
            <el-icon :size="22"><MagicStick /></el-icon>
          </div>
          <div class="logo-text">
            <span class="logo-title">Fu AI Code</span>
            <span class="logo-subtitle">智能代码生成</span>
          </div>
        </div>

        <div class="nav-section">
          <div class="nav-label">工作台</div>
          <el-menu
            :default-active="activeMenu"
            class="sidebar-menu"
            :collapse="false"
            router
          >
            <el-menu-item index="/apps">
              <el-icon><FolderOpened /></el-icon>
              <span>我的应用</span>
            </el-menu-item>
            <el-menu-item index="/featured">
              <el-icon><Star /></el-icon>
              <span>精选应用</span>
            </el-menu-item>
          </el-menu>
        </div>

        <template v-if="userStore.isAdmin">
          <div class="nav-section">
            <div class="nav-label">管理后台</div>
            <el-menu
              :default-active="activeMenu"
              class="sidebar-menu"
              :collapse="false"
              router
            >
              <el-menu-item index="/admin/users">
                <el-icon><User /></el-icon>
                <span>用户管理</span>
              </el-menu-item>
              <el-menu-item index="/admin/apps">
                <el-icon><Grid /></el-icon>
                <span>应用管理</span>
              </el-menu-item>
            </el-menu>
          </div>
        </template>

        <div class="sidebar-footer">
          <div class="user-card" @click="handleUserClick">
            <el-avatar :size="36" :icon="UserFilled" class="user-avatar" />
            <div class="user-info">
              <span class="user-name">{{ userStore.userInfo?.userAccount || '用户' }}</span>
              <span class="user-role">{{ userStore.isAdmin ? '管理员' : '普通用户' }}</span>
            </div>
            <el-icon class="user-arrow"><ArrowDown /></el-icon>
          </div>
        </div>
      </div>
    </el-aside>

    <el-container class="main-container">
      <el-header class="header">
        <div class="header-left">
          <el-breadcrumb separator="/" class="breadcrumb">
            <el-breadcrumb-item :to="{ path: '/apps' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentPageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <div class="header-actions">
            <el-tooltip content="帮助中心" placement="bottom">
              <el-button class="icon-btn" :icon="InfoFilled" circle />
            </el-tooltip>
            <el-tooltip content="消息通知" placement="bottom">
              <el-badge :value="0" :hidden="true">
                <el-button class="icon-btn" :icon="Bell" circle />
              </el-badge>
            </el-tooltip>
          </div>
          <el-dropdown @command="handleCommand" trigger="click">
            <div class="header-user">
              <el-avatar :size="32" :icon="UserFilled" />
            </div>
            <template #dropdown>
              <el-dropdown-menu class="user-dropdown">
                <div class="dropdown-header">
                  <el-avatar :size="40" :icon="UserFilled" />
                  <div class="dropdown-user-info">
                    <span class="dropdown-username">{{ userStore.userInfo?.userAccount || '用户' }}</span>
                    <span class="dropdown-role">{{ userStore.isAdmin ? '管理员' : '普通用户' }}</span>
                  </div>
                </div>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>
                  <span>个人中心</span>
                </el-dropdown-item>
                <el-dropdown-item command="settings">
                  <el-icon><Setting /></el-icon>
                  <span>设置</span>
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon>
                  <span>退出登录</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="page-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  MagicStick,
  FolderOpened,
  Star,
  Setting,
  User,
  Grid,
  UserFilled,
  ArrowDown,
  SwitchButton,
  InfoFilled,
  Bell
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const sidebarWidth = '260px'

const activeMenu = computed(() => route.path)

const currentPageTitle = computed(() => route.meta.title || '')

const handleUserClick = () => {
}

const handleCommand = async (command) => {
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await userStore.logout()
      ElMessage.success('退出成功')
      router.push('/login')
    } catch (error) {
      if (error !== 'cancel') {
        console.error(error)
      }
    }
  } else if (command === 'profile') {
    ElMessage.info('个人中心功能开发中')
  } else if (command === 'settings') {
    ElMessage.info('设置功能开发中')
  }
}
</script>

<style scoped>
.main-layout {
  height: 100vh;
  background: var(--bg-secondary);
}

.sidebar {
  background: var(--bg-primary);
  border-right: 1px solid var(--border-primary);
  overflow: hidden;
  position: relative;
  z-index: 100;
}

.sidebar-inner {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 0;
}

.logo-section {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 20px;
  border-bottom: 1px solid var(--border-secondary);
}

.logo-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--primary) 0%, #7c3aed 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
}

.logo-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.logo-title {
  font-size: 17px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.01em;
  line-height: 1.2;
}

.logo-subtitle {
  font-size: 11px;
  color: var(--text-quaternary);
  margin-top: 2px;
}

.nav-section {
  padding: 16px 12px 8px;
}

.nav-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-quaternary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  padding: 0 8px 8px;
}

.sidebar-menu {
  border-right: none;
  background: transparent;
}

.sidebar-menu :deep(.el-menu-item) {
  height: 42px;
  line-height: 42px;
  border-radius: 8px;
  margin-bottom: 2px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  transition: var(--transition-fast);
}

.sidebar-menu :deep(.el-menu-item .el-icon) {
  font-size: 18px;
  color: var(--text-tertiary);
  transition: var(--transition-fast);
}

.sidebar-menu :deep(.el-menu-item:hover) {
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

.sidebar-menu :deep(.el-menu-item:hover .el-icon) {
  color: var(--primary);
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: var(--primary-light);
  color: var(--primary);
}

.sidebar-menu :deep(.el-menu-item.is-active .el-icon) {
  color: var(--primary);
}

.sidebar-footer {
  margin-top: auto;
  padding: 12px;
  border-top: 1px solid var(--border-secondary);
}

.user-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-radius: 10px;
  cursor: pointer;
  transition: var(--transition-fast);
}

.user-card:hover {
  background: var(--bg-tertiary);
}

.user-avatar {
  flex-shrink: 0;
  background: linear-gradient(135deg, var(--primary) 0%, #7c3aed 100%);
}

.user-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.user-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-role {
  font-size: 11px;
  color: var(--text-quaternary);
  margin-top: 1px;
}

.user-arrow {
  font-size: 14px;
  color: var(--text-quaternary);
}

.main-container {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.header {
  background: var(--bg-primary);
  border-bottom: 1px solid var(--border-primary);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  height: 64px;
  flex-shrink: 0;
}

.header-left {
  flex: 1;
  min-width: 0;
}

.breadcrumb {
  font-size: 14px;
}

.breadcrumb :deep(.el-breadcrumb__inner) {
  color: var(--text-tertiary);
}

.breadcrumb :deep(.el-breadcrumb__inner.is-link:hover) {
  color: var(--primary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-right: 8px;
  padding-right: 12px;
  border-right: 1px solid var(--border-secondary);
}

.icon-btn {
  background: transparent !important;
  border: none !important;
  color: var(--text-tertiary);
}

.icon-btn:hover {
  background: var(--bg-tertiary) !important;
  color: var(--primary);
}

.header-user {
  cursor: pointer;
  padding: 4px;
  border-radius: 8px;
  transition: var(--transition-fast);
}

.header-user:hover {
  background: var(--bg-tertiary);
}

.user-dropdown {
  min-width: 200px;
  padding: 8px;
}

.dropdown-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  margin-bottom: 4px;
  border-bottom: 1px solid var(--border-secondary);
}

.dropdown-user-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.dropdown-username {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.dropdown-role {
  font-size: 12px;
  color: var(--text-quaternary);
  margin-top: 2px;
}

.user-dropdown :deep(.el-dropdown-menu__item) {
  border-radius: 6px;
  font-size: 14px;
  padding: 8px 12px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-dropdown :deep(.el-dropdown-menu__item .el-icon) {
  font-size: 16px;
  color: var(--text-tertiary);
}

.main-content {
  background: var(--bg-secondary);
  padding: 24px 28px;
  overflow-y: auto;
  flex: 1;
}

.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.page-fade-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
