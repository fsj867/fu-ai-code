<template>
  <div class="login-page">
    <div class="login-bg">
      <div class="bg-grid"></div>
      <div class="bg-blob blob-1"></div>
      <div class="bg-blob blob-2"></div>
      <div class="bg-blob blob-3"></div>
    </div>

    <div class="login-content">
      <div class="hero-section">
        <div class="hero-brand">
          <div class="hero-logo">
            <el-icon :size="28"><MagicStick /></el-icon>
          </div>
          <span class="hero-name">Fu AI Code</span>
        </div>
        <h1 class="hero-title">
          用 AI 加速你的
          <span class="gradient-text">代码创作</span>
        </h1>
        <p class="hero-desc">
          智能代码生成平台，一句话生成完整前端项目。
          支持 HTML、多文件项目、Vue 工程，让创意快速落地。
        </p>
        <div class="hero-features">
          <div class="feature-item">
            <div class="feature-icon">
              <el-icon><Lightning /></el-icon>
            </div>
            <div class="feature-text">
              <span class="feature-title">极速生成</span>
              <span class="feature-desc">秒级生成完整项目</span>
            </div>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <el-icon><Cpu /></el-icon>
            </div>
            <div class="feature-text">
              <span class="feature-title">智能理解</span>
              <span class="feature-desc">精准理解你的需求</span>
            </div>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <el-icon><SetUp /></el-icon>
            </div>
            <div class="feature-text">
              <span class="feature-title">一键部署</span>
              <span class="feature-desc">生成即可上线发布</span>
            </div>
          </div>
        </div>
      </div>

      <div class="form-section">
        <div class="form-card">
          <div class="form-header">
            <h2 class="form-title">欢迎回来</h2>
            <p class="form-subtitle">登录你的账号，继续创作</p>
          </div>

          <el-form
            ref="loginFormRef"
            :model="loginForm"
            :rules="loginRules"
            class="login-form"
            @keyup.enter="handleLogin"
          >
            <el-form-item prop="userAccount">
              <el-input
                v-model="loginForm.userAccount"
                placeholder="请输入账号"
                size="large"
                :prefix-icon="User"
                class="form-input"
              />
            </el-form-item>
            <el-form-item prop="userPassword">
              <el-input
                v-model="loginForm.userPassword"
                type="password"
                placeholder="请输入密码"
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
                class="login-btn"
                :loading="loading"
                @click="handleLogin"
              >
                {{ loading ? '登录中...' : '登 录' }}
              </el-button>
            </el-form-item>
          </el-form>

          <div class="form-footer">
            <span class="footer-text">还没有账号？</span>
            <router-link to="/register" class="footer-link">立即注册</router-link>
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
import { User, Lock, MagicStick, Lightning, Cpu, SetUp } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const loginFormRef = ref(null)
const loading = ref(false)

const loginForm = reactive({
  userAccount: '',
  userPassword: ''
})

const loginRules = {
  userAccount: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 4, max: 20, message: '账号长度在 4 到 20 个字符', trigger: 'blur' }
  ],
  userPassword: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 20, message: '密码长度在 8 到 20 个字符', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!loginFormRef.value) return
  try {
    await loginFormRef.value.validate()
    loading.value = true
    await userStore.login(loginForm.userAccount, loginForm.userPassword)
    ElMessage.success('登录成功')
    router.push('/apps')
  } catch (error) {
    console.error('登录失败:', error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  background: linear-gradient(180deg, #f8fafc 0%, #eef2ff 100%);
}

.login-bg {
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
  left: -100px;
  animation: float 8s ease-in-out infinite;
}

.blob-2 {
  width: 300px;
  height: 300px;
  background: linear-gradient(135deg, #ec4899 0%, #f472b6 100%);
  bottom: -50px;
  right: 10%;
  animation: float 10s ease-in-out infinite reverse;
}

.blob-3 {
  width: 250px;
  height: 250px;
  background: linear-gradient(135deg, #06b6d4 0%, #22d3ee 100%);
  top: 40%;
  right: -50px;
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

.login-content {
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
  font-size: 48px;
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

.hero-features {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 8px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.8);
  transition: all 0.2s ease;
}

.feature-item:hover {
  background: rgba(255, 255, 255, 0.8);
  transform: translateX(4px);
}

.feature-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

.feature-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.feature-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
}

.feature-desc {
  font-size: 13px;
  color: #64748b;
}

.form-section {
  width: 420px;
  flex-shrink: 0;
}

.form-card {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  padding: 40px;
  box-shadow: 0 20px 60px -15px rgba(0, 0, 0, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.9);
}

.form-header {
  text-align: center;
  margin-bottom: 32px;
}

.form-title {
  font-size: 28px;
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

.login-form {
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

.login-btn {
  width: 100%;
  height: 46px;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  border: none;
  transition: all 0.2s ease;
  margin-top: 8px;
}

.login-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(99, 102, 241, 0.35);
}

.login-btn:active {
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
  .login-content {
    flex-direction: column;
    gap: 40px;
    padding: 40px 20px;
  }

  .hero-section {
    text-align: center;
    align-items: center;
  }

  .hero-title {
    font-size: 36px;
  }

  .hero-features {
    width: 100%;
    max-width: 400px;
  }

  .form-section {
    width: 100%;
    max-width: 420px;
  }
}
</style>
