import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { title: '注册', requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layout/MainLayout.vue'),
    redirect: '/apps',
    children: [
      {
        path: 'apps',
        name: 'Apps',
        component: () => import('@/views/Apps.vue'),
        meta: { title: '我的应用', requiresAuth: true }
      },
      {
        path: 'featured',
        name: 'Featured',
        component: () => import('@/views/Featured.vue'),
        meta: { title: '精选应用', requiresAuth: false }
      },
      {
        path: 'app/:id',
        name: 'AppDetail',
        component: () => import('@/views/AppDetail.vue'),
        meta: { title: '应用详情', requiresAuth: true }
      },
      {
        path: 'chat/:appId',
        name: 'Chat',
        component: () => import('@/views/Chat.vue'),
        meta: { title: 'AI 代码生成', requiresAuth: true }
      },
      {
        path: 'admin/users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/Users.vue'),
        meta: { title: '用户管理', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'admin/apps',
        name: 'AdminApps',
        component: () => import('@/views/admin/Apps.vue'),
        meta: { title: '应用管理', requiresAuth: true, requiresAdmin: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  
  if (to.meta.requiresAuth && !userStore.isLogin) {
    next('/login')
    return
  }
  
  if (to.meta.requiresAdmin && !userStore.isAdmin) {
    next('/apps')
    return
  }
  
  next()
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - Fu AI Code` : 'Fu AI Code'
})

export default router
