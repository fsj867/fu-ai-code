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
          <el-switch
            v-model="useEventStream"
            active-text="事件流模式"
            inactive-text="普通模式"
            :disabled="isGenerating"
            size="small"
          />
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
              <div v-if="msg.isEventStream && msg.steps && msg.steps.length > 0" class="step-progress step-progress-static">
                <div
                  v-for="(step, index) in msg.steps"
                  :key="index"
                  :class="['step-item', step.status]"
                >
                  <div class="step-indicator">
                    <el-icon v-if="step.status === 'done'" :size="16" color="#10b981"><CircleCheck /></el-icon>
                    <el-icon v-else-if="step.status === 'active'" :size="16" color="#4f46e5" class="is-loading"><Loading /></el-icon>
                    <span v-else class="step-number">{{ index + 1 }}</span>
                  </div>
                  <span class="step-name">{{ step.name }}</span>
                </div>
              </div>
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
              <div v-if="useEventStream && stepList.length > 0" class="step-progress">
                <div
                  v-for="(step, index) in stepList"
                  :key="index"
                  :class="['step-item', step.status]"
                >
                  <div class="step-indicator">
                    <el-icon v-if="step.status === 'done'" :size="16" color="#10b981"><CircleCheck /></el-icon>
                    <el-icon v-else-if="step.status === 'active'" :size="16" color="#4f46e5" class="is-loading"><Loading /></el-icon>
                    <span v-else class="step-number">{{ index + 1 }}</span>
                  </div>
                  <span class="step-name">{{ step.name }}</span>
                </div>
              </div>
              <div class="message-text generating-text">
                {{ currentOutput || (useEventStream ? '正在启动工作流...' : '正在生成代码...') }}
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
  Promotion,
  CircleCheck
} from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { getAppDetail, downloadAppCode, deployApp, genCodeEventStreamUrl } from '@/api/app'
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
const useEventStream = ref(false)
const stepList = ref([])
let abortController = null

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
  stepList.value = []

  scrollToBottom()

  const appId = route.params.appId

  if (useEventStream.value) {
    sendEventStreamMessage(appId, userMessage)
  } else {
    sendSimpleStreamMessage(appId, userMessage)
  }
}

const sendSimpleStreamMessage = async (appId, userMessage) => {
  abortController = new AbortController()
  try {
    const response = await fetch(
      `/api/app/chat/gen/code?appId=${appId}&message=${encodeURIComponent(userMessage)}`,
      { credentials: 'include', signal: abortController.signal }
    )
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }
    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''

    while (true) {
      const { value, done } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''
      for (const line of lines) {
        if (line.startsWith('data:')) {
          const dataStr = line.slice(5).trimStart()
          if (!dataStr.trim()) continue
          try {
            const data = JSON.parse(dataStr)
            if (data.data) {
              currentOutput.value += data.data
              scrollToBottom()
            } else if (data.error) {
              ElMessage.error(data.error)
            }
          } catch (e) {
            currentOutput.value += dataStr
            scrollToBottom()
          }
        }
      }
    }
  } catch (error) {
    if (error.name === 'AbortError') {
      return
    }
    console.error('流式请求失败:', error)
    if (currentOutput.value) {
      ElMessage.error('生成中断')
    } else {
      ElMessage.error('请求失败: ' + error.message)
    }
  } finally {
    finishGeneration()
  }
}

const sendEventStreamMessage = async (appId, userMessage) => {
  abortController = new AbortController()
  const url = genCodeEventStreamUrl(appId, userMessage)
  console.log('[事件流] 开始请求:', url)
  try {
    const response = await fetch(url, { credentials: 'include', signal: abortController.signal })
    console.log('[事件流] 响应状态:', response.status, response.statusText)
    console.log('[事件流] Content-Type:', response.headers.get('content-type'))
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }
    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    let eventCount = 0

    while (true) {
      const { value, done } = await reader.read()
      if (done) {
        console.log('[事件流] 流结束，共收到', eventCount, '个事件')
        break
      }
      const chunk = decoder.decode(value, { stream: true })
      buffer += chunk
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''
      for (const line of lines) {
        if (line.startsWith('data:')) {
          const dataStr = line.slice(5).trimStart()
          if (!dataStr.trim()) continue
          try {
            const wrapper = JSON.parse(dataStr)
            if (wrapper.data) {
              const workflowEvent = JSON.parse(wrapper.data)
              eventCount++
              console.log('[事件流] 收到事件', eventCount, ':', workflowEvent.eventType, workflowEvent.stepName || '')
              handleWorkflowEvent(workflowEvent)
            }
          } catch (e) {
            console.error('[事件流] 解析失败:', e, '原始数据:', dataStr)
          }
        }
      }
    }
  } catch (error) {
    if (error.name === 'AbortError') {
      console.log('[事件流] 请求被取消')
      return
    }
    console.error('[事件流] 请求失败:', error)
    if (!currentOutput.value) {
      ElMessage.error('请求失败: ' + error.message)
    }
  } finally {
    console.log('[事件流] finally 调用 finishGeneration')
    finishGeneration()
  }
}

const handleWorkflowEvent = (event) => {
  const { eventType, stepName, content } = event
  console.log('[handleWorkflowEvent] type:', eventType, 'step:', stepName, 'contentLen:', content?.length || 0)

  switch (eventType) {
    case 'step':
      if (stepName) {
        const existingIndex = stepList.value.findIndex(s => s.name === stepName)
        if (existingIndex >= 0) {
          stepList.value[existingIndex].status = 'done'
        }
        stepList.value.forEach((s, i) => {
          if (i < stepList.value.length - 1) {
            s.status = 'done'
          }
        })
        stepList.value.push({ name: stepName, status: 'active' })
        scrollToBottom()
      }
      break
    case 'token':
      if (content) {
        currentOutput.value += content
        scrollToBottom()
      }
      break
    case 'complete':
      stepList.value.forEach(s => {
        s.status = 'done'
      })
      if (useEventStream.value) {
        messages.value.push({
          role: 'ai',
          content: currentOutput.value || '生成完成',
          steps: [...stepList.value],
          isEventStream: true,
          time: new Date()
        })
        currentOutput.value = ''
        stepList.value = []
      }
      finishGeneration()
      break
    case 'error':
      ElMessage.error(content || '生成失败')
      finishGeneration()
      break
    default:
      break
  }
}

const finishGeneration = () => {
  if (abortController) {
    abortController.abort()
    abortController = null
  }
  isGenerating.value = false
  if (!useEventStream.value && currentOutput.value) {
    messages.value.push({
      role: 'ai',
      content: currentOutput.value,
      time: new Date()
    })
    currentOutput.value = ''
  }
  stepList.value = []
  scrollToBottom()
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

.chat-header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.step-progress {
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.step-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 16px;
  font-size: 12px;
  background-color: #f3f4f6;
  color: #6b7280;
}

.step-item.done {
  background-color: #ecfdf5;
  color: #059669;
}

.step-item.active {
  background-color: #eef2ff;
  color: #4f46e5;
}

.step-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
}

.step-number {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background-color: #d1d5db;
  color: #fff;
  font-size: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.step-name {
  white-space: nowrap;
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
