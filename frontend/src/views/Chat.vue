<template>
  <div class="chat-page">
    <div class="chat-sidebar">
      <div class="sidebar-header">
        <el-button type="primary" :icon="Plus" class="new-chat-btn" @click="goToApps">
          新建应用
        </el-button>
      </div>
      <div class="app-info-section" v-if="app">
        <div class="app-info-header">
          <el-avatar :size="40" :icon="Grid" />
          <div class="app-info-text">
            <h4 class="app-name">{{ app.appName }}</h4>
            <span class="app-type">{{ getTypeLabel(app.codeGenType) }}</span>
          </div>
        </div>
      </div>
      <div class="sidebar-actions">
        <el-button :icon="Download" @click="handleDownload" style="width: 100%">
          下载代码
        </el-button>
        <el-button :icon="UploadFilled" @click="handleDeploy" style="width: 100%">
          部署应用
        </el-button>
        <el-button type="danger" :icon="Delete" @click="clearHistory" style="width: 100%">
          清空对话
        </el-button>
      </div>
    </div>

    <div class="chat-main">
      <div class="chat-header">
        <div class="chat-title">
          <el-icon :size="20" color="#4f46e5"><MagicStick /></el-icon>
          <span>AI 代码生成助手</span>
        </div>
        <div class="chat-header-actions">
          <span class="hint-text">
            描述你想要的功能，AI 将为你生成代码
          </span>
        </div>
      </div>

      <div class="chat-messages" ref="messagesContainer">
        <div class="message-item ai-message" v-if="messages.length === 0">
          <div class="message-avatar">
            <el-icon :size="20"><MagicStick /></el-icon>
          </div>
          <div class="message-content">
            <div class="message-bubble welcome-bubble">
              <h3>👋 你好！我是 AI 代码生成助手</h3>
              <p>我可以帮你生成各种类型的代码，包括：</p>
              <ul>
                <li>HTML 单页应用</li>
                <li>多文件项目</li>
                <li>Vue 项目</li>
              </ul>
              <p>请描述你想要实现的功能，我将为你生成代码！</p>
            </div>
          </div>
        </div>

        <div
          v-for="(msg, index) in messages"
          :key="index"
          :class="['message-item', msg.role === 'user' ? 'user-message' : 'ai-message']"
        >
          <div class="message-avatar">
            <el-icon v-if="msg.role === 'user'" :size="20"><User /></el-icon>
            <el-icon v-else :size="20"><MagicStick /></el-icon>
          </div>
          <div class="message-content">
            <div class="message-bubble">
              <div v-if="msg.content" class="message-text" v-html="formatMessage(msg.content)"></div>
              <div v-else class="message-text">
                <el-icon class="is-loading"><Loading /></el-icon>
                正在思考...
              </div>
            </div>
            <div class="message-time">{{ formatTime(msg.time) }}</div>
          </div>
        </div>

        <div v-if="isGenerating" class="message-item ai-message">
          <div class="message-avatar">
            <el-icon :size="20"><MagicStick /></el-icon>
          </div>
          <div class="message-content">
            <div class="message-bubble">
              <div class="message-text generating-text">
                {{ currentOutput || '正在生成代码...' }}
                <span class="cursor">|</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="chat-input-area">
        <div class="input-wrapper">
          <el-input
            v-model="inputMessage"
            type="textarea"
            :rows="3"
            placeholder="请描述你想要生成的代码功能..."
            :disabled="isGenerating"
            @keydown.enter.ctrl="sendMessage"
          />
          <div class="input-actions">
            <span class="tip-text">按 Ctrl + Enter 发送</span>
            <el-button
              type="primary"
              :icon="Promotion"
              :loading="isGenerating"
              @click="sendMessage"
            >
              {{ isGenerating ? '生成中...' : '发送' }}
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  Grid,
  Download,
  UploadFilled,
  Delete,
  MagicStick,
  User,
  Loading,
  Promotion
} from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { getAppDetail, downloadAppCode, deployApp } from '@/api/app'
import { listByAppId, deleteByAppId } from '@/api/chatHistory'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const app = ref(null)
const messages = ref([])
const inputMessage = ref('')
const isGenerating = ref(false)
const currentOutput = ref('')
const messagesContainer = ref(null)

const getTypeLabel = (type) => {
  const map = {
    html: 'HTML 单页',
    multi_file: '多文件项目',
    vue_project: 'Vue 项目'
  }
  return map[type] || type
}

const formatTime = (time) => {
  if (!time) return ''
  return dayjs(time).format('HH:mm')
}

const formatMessage = (content) => {
  if (!content) return ''
  return content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\n/g, '<br>')
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

const goToApps = () => {
  router.push('/apps')
}

const fetchAppDetail = async () => {
  const appId = route.params.appId
  if (!appId) return
  try {
    app.value = await getAppDetail(appId)
  } catch (error) {
    console.error('获取应用详情失败:', error)
  }
}

const fetchHistory = async () => {
  const appId = route.params.appId
  if (!appId) return
  try {
    const historyList = await listByAppId(appId, { pageSize: 50 })
    if (historyList && historyList.length > 0) {
      messages.value = historyList
        .slice()
        .reverse()
        .map((item) => ({
          role: item.messageType === 'user' ? 'user' : 'ai',
          content: item.content,
          time: item.createTime
        }))
    }
  } catch (error) {
    console.error('获取对话历史失败:', error)
  }
}

const sendMessage = async () => {
  if (!inputMessage.value.trim() || isGenerating.value) return

  const userMessage = inputMessage.value.trim()
  inputMessage.value = ''

  messages.value.push({
    role: 'user',
    content: userMessage,
    time: new Date()
  })

  isGenerating.value = true
  currentOutput.value = ''

  scrollToBottom()

  const appId = route.params.appId
  const eventSource = new EventSource(
    `/api/app/chat/gen/code?appId=${appId}&message=${encodeURIComponent(userMessage)}`,
    { withCredentials: true }
  )

  eventSource.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data)
      if (data.data) {
        currentOutput.value += data.data
        scrollToBottom()
      }
    } catch (e) {
      currentOutput.value += event.data
      scrollToBottom()
    }
  }

  eventSource.onerror = () => {
    eventSource.close()
    isGenerating.value = false
    if (currentOutput.value) {
      messages.value.push({
        role: 'ai',
        content: currentOutput.value,
        time: new Date()
      })
      currentOutput.value = ''
    } else {
      ElMessage.error('生成失败，请重试')
    }
    scrollToBottom()
  }

  eventSource.addEventListener('close', () => {
    eventSource.close()
    isGenerating.value = false
    if (currentOutput.value) {
      messages.value.push({
        role: 'ai',
        content: currentOutput.value,
        time: new Date()
      })
      currentOutput.value = ''
    }
    scrollToBottom()
  })
}

const clearHistory = async () => {
  try {
    await ElMessageBox.confirm('确定要清空所有对话历史吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const appId = route.params.appId
    await deleteByAppId(appId)
    messages.value = []
    ElMessage.success('已清空对话历史')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('清空历史失败:', error)
    }
  }
}

const handleDownload = () => {
  const appId = route.params.appId
  const url = downloadAppCode(appId)
  window.open(url, '_blank')
}

const handleDeploy = async () => {
  try {
    const appId = route.params.appId
    const deployUrl = await deployApp({ appId })
    ElMessage.success('部署成功')
    fetchAppDetail()
    if (deployUrl) {
      window.open(deployUrl, '_blank')
    }
  } catch (error) {
    console.error('部署失败:', error)
  }
}

watch(
  () => route.params.appId,
  () => {
    fetchAppDetail()
    fetchHistory()
  }
)

onMounted(() => {
  fetchAppDetail()
  fetchHistory()
})
</script>

<style scoped>
.chat-page {
  display: flex;
  height: calc(100vh - 112px);
  background-color: #f9fafb;
  border-radius: 12px;
  overflow: hidden;
}

.chat-sidebar {
  width: 260px;
  background-color: #fff;
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid #e5e7eb;
}

.new-chat-btn {
  width: 100%;
}

.app-info-section {
  padding: 16px;
  border-bottom: 1px solid #e5e7eb;
}

.app-info-header {
  display: flex;
  gap: 12px;
  align-items: center;
}

.app-info-text {
  flex: 1;
  min-width: 0;
}

.app-name {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-type {
  font-size: 12px;
  color: #6b7280;
}

.sidebar-actions {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-header {
  height: 60px;
  padding: 0 24px;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #fff;
}

.chat-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.hint-text {
  font-size: 13px;
  color: #9ca3af;
}

.chat-messages {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.message-item {
  display: flex;
  gap: 12px;
  max-width: 85%;
}

.user-message {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ai-message .message-avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.user-message .message-avatar {
  background-color: #4f46e5;
  color: #fff;
}

.message-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.user-message .message-content {
  align-items: flex-end;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.6;
}

.ai-message .message-bubble {
  background-color: #fff;
  border: 1px solid #e5e7eb;
  border-top-left-radius: 4px;
}

.user-message .message-bubble {
  background-color: #4f46e5;
  color: #fff;
  border-top-right-radius: 4px;
}

.welcome-bubble {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border: none;
}

.welcome-bubble h3 {
  margin: 0 0 12px;
  font-size: 18px;
}

.welcome-bubble p {
  margin: 8px 0;
}

.welcome-bubble ul {
  margin: 8px 0;
  padding-left: 20px;
}

.welcome-bubble li {
  margin: 4px 0;
}

.message-text {
  font-size: 14px;
  white-space: pre-wrap;
  word-break: break-word;
}

.generating-text {
  min-height: 24px;
}

.cursor {
  display: inline-block;
  width: 2px;
  height: 16px;
  background-color: #4f46e5;
  margin-left: 2px;
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 50% {
    opacity: 1;
  }
  51%, 100% {
    opacity: 0;
  }
}

.message-time {
  font-size: 11px;
  color: #9ca3af;
}

.chat-input-area {
  padding: 16px 24px 24px;
  background-color: #fff;
  border-top: 1px solid #e5e7eb;
}

.input-wrapper {
  max-width: 900px;
  margin: 0 auto;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.tip-text {
  font-size: 12px;
  color: #9ca3af;
}
</style>
