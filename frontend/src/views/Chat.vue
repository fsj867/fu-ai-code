<template>
  <div class="chat-page">
    <div class="chat-sidebar">
      <div class="sidebar-header">
        <el-button type="primary" :icon="Plus" class="new-chat-btn" @click="goToApps">
          新建应用
        </el-button>
      </div>

      <div class="app-info-card" v-if="app">
        <div class="app-icon">
          <el-icon :size="24"><MagicStick /></el-icon>
        </div>
        <div class="app-info-text">
          <h4 class="app-name">{{ app.appName }}</h4>
          <span class="app-type">{{ getTypeLabel(app.codeGenType) }}</span>
        </div>
        <div v-if="app.deployKey" class="deploy-badge">
          <el-icon :size="12"><CircleCheck /></el-icon>
          已部署
        </div>
      </div>

      <div class="sidebar-actions">
        <div class="action-group">
          <span class="action-label">应用操作</span>
          <el-button v-if="app?.deployKey" :icon="View" class="action-btn" @click="handlePreview">
            预览应用
          </el-button>
          <el-button :icon="Download" class="action-btn" @click="handleDownload">
            下载代码
          </el-button>
          <el-button :icon="UploadFilled" class="action-btn" @click="handleDeploy">
            部署应用
          </el-button>
        </div>
        <div class="action-group danger-group">
          <span class="action-label">危险操作</span>
          <el-button type="danger" :icon="Delete" class="action-btn" @click="clearHistory">
            清空对话
          </el-button>
        </div>
      </div>
    </div>

    <div class="chat-main">
      <div class="chat-header">
        <div class="chat-title-section">
          <div class="chat-icon">
            <el-icon :size="18"><MagicStick /></el-icon>
          </div>
          <div class="chat-title-text">
            <span class="chat-title">AI 代码生成助手</span>
            <span class="chat-subtitle">{{ app?.appName || '选择一个应用开始' }}</span>
          </div>
        </div>
        <div class="chat-header-actions">
          <div class="mode-switch">
            <el-switch
              v-model="useEventStream"
              active-text="工作流模式"
              inactive-text="普通模式"
              :disabled="isGenerating"
              size="small"
            />
          </div>
        </div>
      </div>

      <div class="chat-messages" ref="messagesContainer">
        <div v-if="messages.length === 0" class="welcome-section">
          <div class="welcome-hero">
            <div class="welcome-icon">
              <el-icon :size="36"><MagicStick /></el-icon>
            </div>
            <h2 class="welcome-title">开始你的 AI 编程之旅</h2>
            <p class="welcome-desc">描述你想要的功能，我将为你生成完整的代码</p>
          </div>
          <div class="quick-prompts">
            <span class="quick-prompts-label">快速开始</span>
            <div class="prompt-grid">
              <div class="prompt-card" v-for="(prompt, index) in quickPrompts" :key="index" @click="usePrompt(prompt)">
                <div class="prompt-icon">{{ prompt.icon }}</div>
                <div class="prompt-info">
                  <span class="prompt-title">{{ prompt.title }}</span>
                  <span class="prompt-desc">{{ prompt.desc }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div
          v-for="(msg, index) in messages"
          :key="index"
          :class="['message-item', msg.role === 'user' ? 'user-message' : 'ai-message']"
        >
          <div class="message-avatar" :class="msg.role">
            <el-icon v-if="msg.role === 'user'" :size="18"><User /></el-icon>
            <el-icon v-else :size="18"><MagicStick /></el-icon>
          </div>
          <div class="message-content">
            <div class="message-meta">
              <span class="message-role">{{ msg.role === 'user' ? '你' : 'AI 助手' }}</span>
              <span class="message-time">{{ formatTime(msg.time) }}</span>
            </div>
            <div class="message-bubble">
              <div v-if="msg.isEventStream && msg.steps && msg.steps.length > 0" class="step-progress step-progress-static">
                <div
                  v-for="(step, stepIndex) in msg.steps"
                  :key="stepIndex"
                  :class="['step-item', step.status]"
                >
                  <div class="step-indicator">
                    <el-icon v-if="step.status === 'done'" :size="14" color="#10b981"><CircleCheck /></el-icon>
                    <el-icon v-else-if="step.status === 'active'" :size="14" color="#4f46e5" class="is-loading"><Loading /></el-icon>
                    <span v-else class="step-number">{{ stepIndex + 1 }}</span>
                  </div>
                  <span class="step-name">{{ step.name }}</span>
                </div>
              </div>
              <div v-if="msg.content" class="message-text" v-html="formatMessage(msg.content)"></div>
              <div v-else class="message-text thinking-text">
                <el-icon class="is-loading"><Loading /></el-icon>
                正在思考...
              </div>
            </div>
          </div>
        </div>

        <div v-if="isGenerating" class="message-item ai-message">
          <div class="message-avatar ai">
            <el-icon :size="18"><MagicStick /></el-icon>
          </div>
          <div class="message-content">
            <div class="message-meta">
              <span class="message-role">AI 助手</span>
              <span class="message-time">正在生成...</span>
            </div>
            <div class="message-bubble">
              <div v-if="useEventStream && stepList.length > 0" class="step-progress">
                <div
                  v-for="(step, index) in stepList"
                  :key="index"
                  :class="['step-item', step.status]"
                >
                  <div class="step-indicator">
                    <el-icon v-if="step.status === 'done'" :size="14" color="#10b981"><CircleCheck /></el-icon>
                    <el-icon v-else-if="step.status === 'active'" :size="14" color="#4f46e5" class="is-loading"><Loading /></el-icon>
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
        <div class="input-container">
          <div class="input-wrapper">
            <el-input
              v-model="inputMessage"
              type="textarea"
              :rows="2"
              :autosize="{ minRows: 2, maxRows: 6 }"
              placeholder="描述你想要生成的代码功能... (Ctrl + Enter 发送)"
              :disabled="isGenerating"
              @keydown.enter.ctrl="sendMessage"
              resize="none"
              class="chat-input"
            />
          </div>
          <div class="input-footer">
            <div class="input-hints">
              <span class="hint-item">
                <el-icon :size="12"><InfoFilled /></el-icon>
                描述越详细，生成效果越好
              </span>
            </div>
            <el-button
              type="primary"
              :icon="Promotion"
              :loading="isGenerating"
              @click="sendMessage"
              class="send-btn"
            >
              {{ isGenerating ? '生成中' : '发送' }}
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="showPreviewDialog" title="应用预览" width="85%" class="preview-dialog" top="5vh" destroy-on-close>
      <div class="preview-container">
        <div class="preview-toolbar">
          <div class="preview-url" :title="previewUrl">
            <el-icon :size="14"><Lock /></el-icon>
            <span>{{ previewUrl }}</span>
          </div>
          <el-button size="small" :icon="Open" @click="openInNewTab">
            新窗口打开
          </el-button>
        </div>
        <div class="preview-frame-wrapper">
          <iframe
            v-if="showPreviewDialog"
            :src="previewUrl"
            class="preview-iframe"
            frameborder="0"
            sandbox="allow-same-origin allow-scripts allow-forms allow-popups"
          ></iframe>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  MagicStick,
  User,
  Loading,
  Promotion,
  CircleCheck,
  Download,
  UploadFilled,
  Delete,
  InfoFilled,
  View,
  Lock,
  Open
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

const showPreviewDialog = ref(false)
const previewUrl = ref('')

const handlePreview = () => {
  if (!app.value?.deployKey) {
    ElMessage.warning('请先部署应用后再预览')
    return
  }
  previewUrl.value = `/api/static/${app.value.deployKey}/`
  showPreviewDialog.value = true
}

const openInNewTab = () => {
  if (previewUrl.value) {
    window.open(previewUrl.value, '_blank')
  }
}

const quickPrompts = [
  { icon: '🌐', title: '企业官网', desc: '生成一个现代化企业展示网站', text: '生成一个现代化的企业官网，包含首页、产品中心、关于我们、新闻资讯、联系我们等页面，风格要大气有科技感' },
  { icon: '🛒', title: '电商后台', desc: '生成电商管理后台系统', text: '生成一个电商后台管理系统，包含商品管理、订单管理、用户管理、数据统计等功能模块' },
  { icon: '📱', title: '移动端H5', desc: '生成移动端适配页面', text: '生成一个移动端H5活动页面，包含活动介绍、参与方式、奖品展示、立即参与按钮等，适配手机屏幕' },
  { icon: '📊', title: '数据大屏', desc: '生成数据可视化大屏', text: '生成一个数据可视化大屏页面，展示销售数据、用户增长、热门排行等，要有酷炫的图表效果' }
]

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

const usePrompt = (prompt) => {
  inputMessage.value = prompt.text
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
    await ElMessageBox.confirm('确定要清空所有对话历史吗？', '清空确认', {
      confirmButtonText: '确定清空',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
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
  height: 100%;
  background: var(--bg-secondary);
  border-radius: 16px;
  overflow: hidden;
}

.chat-sidebar {
  width: 280px;
  background: var(--bg-primary);
  border-right: 1px solid var(--border-primary);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid var(--border-secondary);
}

.new-chat-btn {
  width: 100%;
  height: 40px;
  border-radius: 10px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  border: none;
  font-weight: 500;
  transition: all 0.2s ease;
}

.new-chat-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
}

.app-info-card {
  margin: 16px;
  padding: 16px;
  background: var(--bg-secondary);
  border-radius: 12px;
  border: 1px solid var(--border-secondary);
}

.app-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}

.app-info-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 10px;
}

.app-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-type {
  font-size: 12px;
  color: var(--text-tertiary);
}

.deploy-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: var(--success-light);
  color: var(--success);
  font-size: 11px;
  font-weight: 500;
  border-radius: 20px;
}

.sidebar-actions {
  padding: 8px 16px 16px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.action-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.action-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-quaternary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  padding: 0 4px;
}

.action-btn {
  width: 100%;
  justify-content: flex-start;
  padding-left: 12px;
  height: 38px;
  border-radius: 8px;
  border: 1px solid var(--border-primary);
  transition: all 0.15s ease;
}

.action-btn:hover {
  background: var(--bg-secondary);
  border-color: var(--text-placeholder);
}

.danger-group .action-btn {
  color: var(--danger);
  border-color: var(--danger-light);
}

.danger-group .action-btn:hover {
  background: var(--danger-light);
  border-color: var(--danger);
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: var(--bg-primary);
}

.chat-header {
  height: 60px;
  padding: 0 24px;
  border-bottom: 1px solid var(--border-secondary);
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
  background: var(--bg-primary);
}

.chat-title-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chat-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chat-title-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.chat-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.2;
}

.chat-subtitle {
  font-size: 12px;
  color: var(--text-tertiary);
}

.mode-switch {
  font-size: 12px;
  color: var(--text-tertiary);
}

.chat-messages {
  flex: 1;
  padding: 32px 24px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.welcome-section {
  max-width: 720px;
  margin: auto;
  width: 100%;
}

.welcome-hero {
  text-align: center;
  margin-bottom: 40px;
}

.welcome-icon {
  width: 64px;
  height: 64px;
  border-radius: 18px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
  box-shadow: 0 8px 24px rgba(99, 102, 241, 0.3);
}

.welcome-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 8px;
  letter-spacing: -0.02em;
}

.welcome-desc {
  font-size: 15px;
  color: var(--text-tertiary);
  margin: 0;
}

.quick-prompts {
  width: 100%;
}

.quick-prompts-label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-quaternary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 12px;
  text-align: center;
}

.prompt-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.prompt-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: var(--bg-secondary);
  border: 1px solid var(--border-primary);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.prompt-card:hover {
  background: var(--primary-light);
  border-color: var(--primary);
  transform: translateY(-2px);
  box-shadow: var(--shadow-sm);
}

.prompt-icon {
  font-size: 24px;
  flex-shrink: 0;
}

.prompt-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.prompt-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.prompt-desc {
  font-size: 12px;
  color: var(--text-tertiary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-item {
  display: flex;
  gap: 14px;
  max-width: 100%;
}

.user-message {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.message-avatar.ai {
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: #fff;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.25);
}

.message-avatar.user {
  background: var(--bg-tertiary);
  color: var(--text-secondary);
}

.message-content {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-width: calc(100% - 50px);
}

.user-message .message-content {
  align-items: flex-end;
}

.message-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 4px;
}

.message-role {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-tertiary);
}

.message-time {
  font-size: 11px;
  color: var(--text-quaternary);
}

.message-bubble {
  padding: 14px 18px;
  border-radius: 14px;
  line-height: 1.7;
  word-break: break-word;
}

.ai-message .message-bubble {
  background: var(--bg-secondary);
  border: 1px solid var(--border-primary);
  border-top-left-radius: 4px;
}

.user-message .message-bubble {
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: #fff;
  border-top-right-radius: 4px;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.25);
}

.step-progress {
  margin-bottom: 14px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border-secondary);
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.step-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  background: var(--bg-tertiary);
  color: var(--text-tertiary);
  font-weight: 500;
}

.step-item.done {
  background: var(--success-light);
  color: var(--success);
}

.step-item.active {
  background: var(--primary-light);
  color: var(--primary);
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
  background: var(--text-placeholder);
  color: #fff;
  font-size: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.message-text {
  font-size: 14px;
  white-space: pre-wrap;
}

.thinking-text {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-tertiary);
}

.generating-text {
  min-height: 24px;
}

.cursor {
  display: inline-block;
  width: 2px;
  height: 16px;
  background: var(--primary);
  margin-left: 2px;
  animation: blink 1s infinite;
  vertical-align: text-bottom;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

.chat-input-area {
  padding: 16px 24px 24px;
  border-top: 1px solid var(--border-secondary);
  background: var(--bg-primary);
}

.input-container {
  max-width: 800px;
  margin: 0 auto;
}

.input-wrapper {
  background: var(--bg-secondary);
  border: 1px solid var(--border-primary);
  border-radius: 14px;
  padding: 12px 14px;
  transition: all 0.2s ease;
}

.input-wrapper:focus-within {
  border-color: var(--primary);
  box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.1);
  background: var(--bg-primary);
}

.chat-input :deep(.el-textarea__inner) {
  background: transparent;
  border: none;
  box-shadow: none !important;
  font-size: 14px;
  line-height: 1.6;
  padding: 0;
}

.chat-input :deep(.el-textarea__inner:focus) {
  box-shadow: none !important;
}

.input-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
}

.input-hints {
  display: flex;
  gap: 16px;
}

.hint-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-quaternary);
}

.send-btn {
  height: 36px;
  padding: 0 18px;
  border-radius: 9px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  border: none;
  font-weight: 500;
  transition: all 0.2s ease;
}

.send-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.35);
}

@media (max-width: 1024px) {
  .chat-sidebar {
    display: none;
  }

  .prompt-grid {
    grid-template-columns: 1fr;
  }
}

.preview-dialog :deep(.el-dialog) {
  border-radius: 16px;
  overflow: hidden;
  max-height: 90vh;
}

.preview-dialog :deep(.el-dialog__header) {
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-secondary);
  margin-right: 0;
}

.preview-dialog :deep(.el-dialog__body) {
  padding: 0;
  max-height: calc(90vh - 60px);
  overflow: hidden;
}

.preview-container {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.preview-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border-secondary);
}

.preview-url {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-tertiary);
  background: var(--bg-primary);
  padding: 6px 12px;
  border-radius: 8px;
  border: 1px solid var(--border-primary);
  max-width: 60%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-frame-wrapper {
  flex: 1;
  background: #fff;
  min-height: 60vh;
}

.preview-iframe {
  width: 100%;
  height: 60vh;
  border: none;
}
</style>
