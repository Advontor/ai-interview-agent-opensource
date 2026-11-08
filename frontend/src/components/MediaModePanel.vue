<template>
  <el-card class="media-panel" shadow="never">
    <div class="media-head">
      <div>
        <strong>{{ mode === 'VIDEO' ? '视频面试预览' : '语音面试准备' }}</strong>
        <p>{{ connected ? '媒体通道已连接' : '当前使用浏览器采集，可随时切回文字输入。' }}</p>
      </div>
      <el-tag :type="connected ? 'success' : 'info'">{{ connected ? '已连接' : '浏览器降级' }}</el-tag>
    </div>
    <video v-if="mode === 'VIDEO'" ref="videoRef" class="preview" autoplay muted playsinline />
    <div class="media-actions">
      <el-button type="primary" plain :loading="starting" @click="toggleCapture">
        {{ capturing ? '停止采集' : (mode === 'VIDEO' ? '开启摄像头和麦克风' : '开启麦克风') }}
      </el-button>
      <el-button v-if="capturing" plain @click="toggleRecording">{{ recording ? '停止本地录制' : '本地录制' }}</el-button>
    </div>
    <el-alert v-if="notice" :title="notice" type="info" show-icon :closable="false" />
  </el-card>
</template>

<script setup lang="ts">
import { onUnmounted, ref } from 'vue'
import { mediaWebSocketUrl } from '../api/media'

const props = defineProps<{ mode: 'VOICE' | 'VIDEO'; sessionId?: number }>()
const emit = defineEmits<{
  (event: 'transcript', text: string): void
  (event: 'status', message: string): void
}>()
const videoRef = ref<HTMLVideoElement | null>(null)
const stream = ref<MediaStream | null>(null)
const socket = ref<WebSocket | null>(null)
const recorder = ref<MediaRecorder | null>(null)
const capturing = ref(false)
const recording = ref(false)
const connected = ref(false)
const starting = ref(false)
const notice = ref('')
let reconnectTimer: number | null = null
let reconnectAttempts = 0
let shouldReconnect = false

function socketUrl() {
  const base = mediaWebSocketUrl()
  return props.sessionId ? `${base}?sessionId=${encodeURIComponent(String(props.sessionId))}` : base
}

function clearReconnectTimer() {
  if (reconnectTimer !== null) window.clearTimeout(reconnectTimer)
  reconnectTimer = null
}

function connectSocket() {
  if (!shouldReconnect || socket.value || typeof WebSocket === 'undefined') return
  try {
    const ws = new WebSocket(socketUrl())
    socket.value = ws
    ws.onopen = () => {
      reconnectAttempts = 0
      connected.value = true
      // Keep the media channel scoped to the interview session. The server can
      // use this handshake when ASR/TTS providers are enabled later.
      ws.send(JSON.stringify({ type: 'session.started', sessionId: props.sessionId || 0 }))
    }
    ws.onmessage = (event) => {
      try {
        const value: unknown = JSON.parse(String(event.data || ''))
        if (!value || typeof value !== 'object') return
        const message = value as { type?: unknown; payload?: unknown }
        const payload = message.payload && typeof message.payload === 'object'
          ? message.payload as { text?: unknown }
          : null
        if (message.type === 'transcript.partial' || message.type === 'transcript.completed') {
          if (typeof payload?.text === 'string' && payload.text.trim()) emit('transcript', payload.text)
          return
        }
        if (message.type === 'stream.error') {
          notice.value = '实时媒体通道返回错误，已保留本地采集。'
          emit('status', notice.value)
        }
      } catch { /* server acknowledgements are optional */ }
    }
    ws.onclose = () => {
      if (socket.value === ws) socket.value = null
      connected.value = false
      if (!shouldReconnect || reconnectTimer !== null) return
      const delay = Math.min(8000, 500 * (2 ** Math.min(reconnectAttempts, 4)))
      reconnectAttempts += 1
      reconnectTimer = window.setTimeout(() => {
        reconnectTimer = null
        connectSocket()
      }, delay)
    }
    ws.onerror = () => {
      connected.value = false
      notice.value = '实时媒体通道不可用，仍可使用浏览器采集和文字提交。'
    }
  } catch { notice.value = '实时媒体通道不可用，已保留本地采集。' }
}

async function toggleCapture() {
  if (capturing.value) { stopCapture(); return }
  starting.value = true
  notice.value = ''
  try {
    stream.value = await navigator.mediaDevices.getUserMedia({ audio: true, video: props.mode === 'VIDEO' })
    if (videoRef.value && stream.value) videoRef.value.srcObject = stream.value
    shouldReconnect = true
    reconnectAttempts = 0
    connectSocket()
    capturing.value = true
  } catch (error) {
    notice.value = error instanceof DOMException && error.name === 'NotAllowedError'
      ? '麦克风或摄像头权限被拒绝，请允许权限后重试。'
      : '无法开启媒体设备，请检查浏览器和设备设置。'
  } finally { starting.value = false }
}

function toggleRecording() {
  if (!stream.value) return
  if (recording.value) { recorder.value?.stop(); recording.value = false; return }
  if (typeof MediaRecorder === 'undefined') { notice.value = '当前浏览器不支持本地录制。'; return }
  const value = new MediaRecorder(stream.value)
  value.ondataavailable = (event) => {
    if (event.data.size > 0 && socket.value?.readyState === WebSocket.OPEN) socket.value.send(event.data)
  }
  value.start(1000)
  recorder.value = value
  recording.value = true
}

function stopCapture() {
  shouldReconnect = false
  clearReconnectTimer()
  if (recording.value) { recorder.value?.stop(); recording.value = false }
  stream.value?.getTracks().forEach(track => track.stop())
  stream.value = null
  if (videoRef.value) videoRef.value.srcObject = null
  socket.value?.close()
  socket.value = null
  connected.value = false
  capturing.value = false
}

onUnmounted(stopCapture)
</script>

<style scoped>
.media-panel { margin-bottom: 18px; }
.media-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; }
.media-head p { margin: 6px 0 0; color: #7a8499; font-size: 13px; }
.preview { width: 100%; max-height: 260px; margin-top: 14px; border-radius: 10px; background: #121826; object-fit: cover; }
.media-actions { display: flex; gap: 10px; margin-top: 14px; flex-wrap: wrap; }
.media-panel .el-alert { margin-top: 12px; }
</style>
