import { defineStore } from 'pinia'
import { login as loginApi, getLoginUser, logout as logoutApi, register as registerApi } from '@/api/user'

export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: null,
    token: null
  }),
  getters: {
    isLogin: (state) => !!state.userInfo,
    isAdmin: (state) => state.userInfo?.userRole === 'admin'
  },
  actions: {
    async login(userAccount, userPassword) {
      const res = await loginApi({ userAccount, userPassword })
      this.userInfo = res
      return res
    },
    async register(userAccount, userPassword, checkPassword) {
      return await registerApi({ userAccount, userPassword, checkPassword })
    },
    async fetchUserInfo() {
      try {
        const res = await getLoginUser()
        this.userInfo = res
        return res
      } catch (error) {
        this.userInfo = null
        throw error
      }
    },
    async logout() {
      try {
        await logoutApi()
      } finally {
        this.userInfo = null
      }
    }
  },
  persist: {
    key: 'user-store',
    storage: localStorage
  }
})
