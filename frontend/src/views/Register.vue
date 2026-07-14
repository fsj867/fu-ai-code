<template>
  <div class="register-page">
    <div class="register-bg">
      <div class="bg-grid"></div>
      <div class="bg-blob blob-1"></div>
      <div class="bg-blob blob-2"></div>
      <div class="bg-blob blob-3"></div>
    </div>

    <div class="register-content">
      <div class="hero-section">
        <div class="hero-brand">
          <div class="hero-logo">
            <el-icon :size="28"><MagicStick /></el-icon>
          </div>
          <span class="hero-name">Fu AI Code</span>
        </div>
        <h1 class="hero-title">
          开启你的
          <span class="gradient-text">AI 编程之旅</span>
        </h1>
        <p class="hero-desc">
          立即注册，体验智能代码生成的魔力。
          让 AI 成为你的编程助手，释放无限创造力。
        </p>
        <div class="hero-stats">
          <div class="stat-item">
            <span class="stat-number">1000+</span>
            <span class="stat-label">项目已生成</span>
          </div>
          <div class="stat-item">
            <span class="stat-number">500+</span>
            <span class="stat-label">活跃用户</span>
          </div>
          <div class="stat-item">
            <span class="stat-number">99%</span>
            <span class="stat-label">满意度</span>
          </div>
        </div>
      </div>

      <div class="form-section">
        <div class="form-card">
          <div class="form-header">
            <h2 class="form-title">创建账号</h2>
            <p class="form-subtitle">加入我们，开启智能编程新时代</p>
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
                class="form-input"
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
                class="form-input"
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
                class="form-input"
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
                {{ loading ? '注册中...' : '立即注册' }}
              </el-button>
            </el-form-item>
          </el-form>

          <div class="form-footer">
            <span class="footer-text">已有账号？</span>
            <router-link to="/login" class="footer-link">立即登录</router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, MagicStick } from '@element-plus/icons-vue'
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
.register-page {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  background: linear-gradient(180deg, #f8fafc 0%, #eef2ff 100%);
}

.register-bg {
  position: absolute;
  inset: 0;
  overflow: hidden;
}

.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(79, 70, 229, 0.08) 1px, transparent 1px),
    linear-gradient(90deg, rgba(79, 70, 229, 0.08) 1px, transparent 1px);
  background-size: 40px 40px;
  mask-image: radial-gradient(ellipse at center, black 40%, transparent 80%);
  -webkit-mask-image: radial-gradient(ellipse at center, black 40%, transparent 80%);
}

.bg-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.4;
}

.blob-1 {
  width: 400px;
  height: 400px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  top: -100px;
  right: -100px;
  animation: float 8s ease-in-out infinite;
}

.blob-2 {
  width: 300px;
  height: 300px;
  background: linear-gradient(135deg, #ec4899 0%, #f472b6 100%);
  bottom: -50px;
  left: 10%;
  animation: float 10s ease-in-out infinite reverse;
}

.blob-3 {
  width: 250px;
  height: 250px;
  background: linear-gradient(135deg, #06b6d4 0%, #22d3ee 100%);
  top: 40%;
  left: -50px;
  animation: float 12s ease-in-out infinite;
}

@keyframes float {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  33% {
    transform: translate(30px, -30px) scale(1.05);
  }
  66% {
    transform: translate(-20px, 20px) scale(0.95);
  }
}

.register-content {
  position: relative;
  z-index: 10;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  gap: 80px;
  max-width: 1200px;
  margin: 0 auto;
}

.hero-section {
  flex: 1;
  max-width: 500px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.hero-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hero-logo {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 24px rgba(99, 102, 241, 0.3);
}

.hero-name {
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
  letter-spacing: -0.01em;
}

.hero-title {
  font-size: 44px;
  font-weight: 700;
  line-height: 1.2;
  color: #0f172a;
  letter-spacing: -0.02em;
  margin: 0;
}

.gradient-text {
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 50%, #ec4899 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-desc {
  font-size: 17px;
  line-height: 1.7;
  color: #64748b;
  margin: 0;
}

.hero-stats {
  display: flex;
  gap: 32px;
  margin-top: 8px;
  padding-top: 24px;
  border-top: 1px solid rgba(148, 163, 184, 0.2);
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-number {
  font-size: 28px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.02em;
}

.stat-label {
  font-size: 13px;
  color: #64748b;
}

.form-section {
  width: 440px;
  flex-shrink: 0;
}

.form-card {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  padding: 36px 40px;
  box-shadow: 0 20px 60px -15px rgba(0, 0, 0, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.9);
}

.form-header {
  text-align: center;
  margin-bottom: 28px;
}

.form-title {
  font-size: 26px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 8px;
  letter-spacing: -0.02em;
}

.form-subtitle {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

.register-form {
  margin-top: 0;
}

.form-input :deep(.el-input__wrapper) {
  border-radius: 10px;
  padding: 4px 12px;
  box-shadow: 0 0 0 1px #e2e8f0;
  transition: all 0.2s ease;
}

.form-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #cbd5e1;
}

.form-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.3);
}

.register-btn {
  width: 100%;
  height: 46px;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  border: none;
  transition: all 0.2s ease;
  margin-top: 4px;
}

.register-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(99, 102, 241, 0.35);
}

.register-btn:active {
  transform: translateY(0);
}

.form-footer {
  text-align: center;
  font-size: 14px;
  color: #64748b;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #e2e8f0;
}

.footer-link {
  color: #6366f1;
  margin-left: 6px;
  font-weight: 500;
  transition: color 0.2s ease;
}

.footer-link:hover {
  color: #4f46e5;
}

@media (max-width: 960px) {
  .register-content {
    flex-direction: column-reverse;
    gap: 40px;
    padding: 40px 20px;
  }

  .hero-section {
    text-align: center;
    align-items: center;
  }

  .hero-title {
    font-size: 32px;
  }

  .hero-stats {
    justify-content: center;
  }

  .form-section {
    width: 100%;
    max-width: 440px;
  }
}
</style>
