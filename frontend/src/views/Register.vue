<template>
  <div class="register-container">
    <div class="register-card">
      <div class="register-header">
        <h1 class="title">注册账号</h1>
        <p class="subtitle">加入 Fu AI Code，开启 AI 编程之旅</p>
      </div>
      <el-form
        ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
        class="register-form"
      >
        <el-form-item prop="userAccount">
          <el-input
            v-model="registerForm.userAccount"
            placeholder="请输入账号"
            size="large"
            :prefix-icon="User"
          />
        </el-form-item>
        <el-form-item prop="userPassword">
          <el-input
            v-model="registerForm.userPassword"
            type="password"
            placeholder="请输入密码"
            size="large"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item prop="checkPassword">
          <el-input
            v-model="registerForm.checkPassword"
            type="password"
            placeholder="请再次输入密码"
            size="large"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="register-btn"
            :loading="loading"
            @click="handleRegister"
          >
            注册
          </el-button>
        </el-form-item>
        <div class="register-footer">
          <span>已有账号？</span>
          <router-link to="/login" class="login-link">立即登录</router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const registerFormRef = ref(null)
const loading = ref(false)

const registerForm = reactive({
  userAccount: '',
  userPassword: '',
  checkPassword: ''
})

const validateCheckPassword = (rule, value, callback) => {
  if (value !== registerForm.userPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const registerRules = {
  userAccount: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 4, max: 20, message: '账号长度在 4 到 20 个字符', trigger: 'blur' }
  ],
  userPassword: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 20, message: '密码长度在 8 到 20 个字符', trigger: 'blur' }
  ],
  checkPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateCheckPassword, trigger: 'blur' }
  ]
}

const handleRegister = async () => {
  if (!registerFormRef.value) return
  try {
    await registerFormRef.value.validate()
    loading.value = true
    await userStore.register(
      registerForm.userAccount,
      registerForm.userPassword,
      registerForm.checkPassword
    )
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (error) {
    console.error('注册失败:', error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.register-card {
  width: 420px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.register-header {
  text-align: center;
  margin-bottom: 32px;
}

.title {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 8px;
}

.subtitle {
  font-size: 14px;
  color: #909399;
}

.register-form {
  margin-top: 24px;
}

.register-btn {
  width: 100%;
}

.register-footer {
  text-align: center;
  font-size: 14px;
  color: #909399;
}

.login-link {
  color: #409eff;
  margin-left: 4px;
}
</style>
