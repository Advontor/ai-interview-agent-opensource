<template>
  <div class="interview-page">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>正在加载面试...</p>
    </div>

    <template v-else>
    <!-- 顶部进度区 -->
    <div class="topbar animate-reveal">
      <div class="topbar-left">
        <div class="page-tag">
          <span class="tag-dot"></span>
          模拟面试进行中
        </div>
        <h1 class="page-title">AI 模拟面试</h1>
      </div>
      <div class="topbar-right">
        <div class="progress-info">
          <span class="progress-current">{{ currentQuestionIndex }}</span>
          <span class="progress-sep">/</span>
          <span class="progress-total">{{ totalQuestions }}</span>
        </div>
        <button class="btn btn-ghost btn-small" @click="handleFinish" :disabled="isAnswering || isFinished || isGeneratingReport">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="6" y="6" width="12" height="12" rx="2"/>
          </svg>
          结束面试
        </button>
      </div>
    </div>

    <!-- 进度条 -->
    <div class="progress-bar animate-reveal animate-reveal-delay-1">
      <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
    </div>

    <!-- 面试完成状态 -->
    <transition name="fade-scale">
      <div v-if="isFinished" class="finish-card card animate-reveal">
        <div class="finish-icon">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
            <polyline points="22 4 12 14.01 9 11.01"/>
          </svg>
        </div>
        <h2 class="finish-title">面试完成！</h2>
        <p class="finish-desc">你已完成 {{ answeredCount }} 道题目{{ isGeneratingReport ? '，AI 正在生成你的面试报告...' : '，报告已生成。' }}</p>
        <button class="btn btn-primary btn-large" @click="goToReport" :disabled="isGeneratingReport">
          {{ isGeneratingReport ? '生成报告中...' : '查看面试报告' }}
          <svg v-if="!isGeneratingReport" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M5 12h14"/>
            <path d="M12 5l7 7-7 7"/>
          </svg>
        </button>
      </div>
    </transition>

    <!-- 问题卡片 -->
    <transition name="fade-slide" mode="out-in">
      <div v-if="!isFinished" :key="currentQuestionIndex" class="question-card card animate-reveal animate-reveal-delay-1">
        <div class="question-header">
          <div class="question-type">
            <span class="type-icon" v-html="questionTypeIcon"></span>
            {{ questionTypeMap[currentQuestion?.questionType || ''] || '面试题' }}
          </div>
          <div class="question-difficulty" :class="'diff-' + (currentQuestion?.difficulty || '').toLowerCase()">
            {{ difficultyMap[currentQuestion?.difficulty || ''] || '未知' }}
          </div>
        </div>
        <div class="question-content">
          {{ currentQuestion?.question || '加载题目中...' }}<span v-if="isStreaming" class="stream-cursor"></span>
        </div>
        <div class="question-footer">
          <div class="question-hint">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"/>
              <path d="M12 16v-4"/>
              <path d="M12 8h.01"/>
            </svg>
            建议使用 STAR 法则回答：情境、任务、行动、结果
          </div>
        </div>
      </div>
    </transition>

    <!-- 回答输入区 -->
    <div v-if="!isFinished" class="answer-card card animate-reveal animate-reveal-delay-2">
      <div class="answer-header">
        <div class="answer-label">你的回答</div>
        <div class="answer-count">{{ answer.length }} 字</div>
      </div>
      <textarea
        v-model="answer"
        class="answer-textarea"
        rows="5"
        placeholder="请输入你的回答... 建议结构化表达，突出重点和成果。"
        :disabled="isAnswering"
      ></textarea>

      <!-- 语音输入 / 朗读工具栏 -->
      <div class="voice-toolbar">
        <button
          class="btn btn-ghost btn-small"
          :class="{ 'btn-recording': isRecording }"
          :disabled="isAnswering || isFinished"
          @click="toggleRecording"
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/>
            <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
            <line x1="12" y1="19" x2="12" y2="23"/>
            <line x1="8" y1="23" x2="16" y2="23"/>
          </svg>
          {{ isRecording ? '停止语音输入' : '语音输入' }}
        </button>
        <button
          class="btn btn-ghost btn-small"
          :disabled="!currentQuestion || isFinished"
          @click="speakCurrentQuestion"
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/>
            <path d="M15.54 8.46a5 5 0 0 1 0 7.07"/>
          </svg>
          {{ isSpeaking ? '停止朗读' : '朗读问题' }}
        </button>
        <span class="voice-status">{{ voiceStatus || '点击「语音输入」，说完再次点击停止，文字会保留在回答框。' }}</span>
      </div>
      <div v-if="interimTranscript" class="interim-transcript">正在识别：{{ interimTranscript }}</div>

      <div class="answer-actions">
        <button class="btn btn-ghost btn-small" @click="handleSkip" :disabled="isAnswering">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="13 17 18 12 13 7"/>
            <polyline points="6 17 11 12 6 7"/>
          </svg>
          跳过本题
        </button>
        <button
          class="btn btn-primary"
          :class="{ disabled: !answer.trim() || isAnswering }"
          :disabled="!answer.trim() || isAnswering"
          @click="handleSubmitAnswer"
        >
          <span v-if="isAnswering" class="loading-spinner"></span>
          <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="22" y1="2" x2="11" y2="13"/>
            <polygon points="22 2 15 22 11 13 2 9 22 2"/>
          </svg>
          {{ isAnswering ? 'AI 分析中...' : '提交回答' }}
        </button>
      </div>
    </div>

    <!-- AI 分析状态（题目开始逐字输出后隐藏，改为题目卡片上的光标） -->
    <transition name="fade-slide">
      <div v-if="isAnswering && !isStreaming" class="ai-thinking card">
        <div class="thinking-dots">
          <span></span>
          <span></span>
          <span></span>
        </div>
        <span>AI 正在分析你的回答，生成下一题...</span>
      </div>
    </transition>

    <!-- 状态提示 -->
    <transition name="fade-slide">
      <div v-if="statusMessage && !isAnswering" class="status-alert" :class="'status-' + statusType">
        <span>{{ statusMessage }}</span>
      </div>
    </transition>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { submitAnswer, submitAnswerStream, finishInterview, getSession, type InterviewSessionResponse } from '../api/interview'
import { getMessageList } from '../api/report'

const router = useRouter()

// ---------- 会话信息 ----------
const sessionId = ref<number | null>(null)
const sessionInfo = ref<InterviewSessionResponse | null>(null)

// ---------- 状态 ----------
const currentQuestionIndex = ref(1)
const currentQuestion = ref<{ topic: string; question: string; difficulty: string; questionType: string } | null>(null)
const answer = ref('')
const isAnswering = ref(false)
/** 正在逐字接收 AI 生成的下一题（此时隐藏「AI 思考中」，改为题目边生成边显示） */
const isStreaming = ref(false)
const isFinished = ref(false)
const isGeneratingReport = ref(false)
const statusMessage = ref('')
const statusType = ref<'success' | 'warning' | 'error' | 'info'>('info')
const answersHistory = reactive<Array<{ question: string, answer: string }>>([])
const answeredCount = ref(0) // 已答题数（从后端消息列表计算）
const loading = ref(true)

// ---------- 语音识别（浏览器 Web Speech API）----------
const isRecording = ref(false)
const isSpeaking = ref(false)
const voiceStatus = ref('')
const interimTranscript = ref('')
const speechSupported = ref(true)

type RecognitionResultLike = { isFinal: boolean; [index: number]: { transcript: string } }
type RecognitionEventLike = Event & { resultIndex: number; results: { length: number; [index: number]: RecognitionResultLike } }
type RecognitionLike = {
  lang: string
  continuous: boolean
  interimResults: boolean
  start: () => void
  stop: () => void
  onresult: ((event: RecognitionEventLike) => void) | null
  onerror: ((event: Event & { error?: string }) => void) | null
  onend: (() => void) | null
}
let recognition: RecognitionLike | null = null
let shouldKeepRecognition = false
let recognitionRestartTimer: number | null = null

function speechRecognitionConstructor(): (new () => RecognitionLike) | undefined {
  const browser = window as unknown as {
    SpeechRecognition?: new () => RecognitionLike
    webkitSpeechRecognition?: new () => RecognitionLike
  }
  return browser.SpeechRecognition || browser.webkitSpeechRecognition
}

/**
 * 浏览器只在「安全上下文」下允许调用麦克风：
 * https、http://localhost、http://127.0.0.1 算安全；
 * http + 公网 IP/域名（例如 http://47.111.224.185）不算，
 * 此时 SpeechRecognition 对象存在但一启动就报 not-allowed。
 */
function isSecureContextForMic(): boolean {
  if (window.isSecureContext) return true
  const host = window.location.hostname
  return host === 'localhost' || host === '127.0.0.1' || host === '[::1]'
}

function speechErrorMessage(error?: string) {
  switch (error) {
    case 'not-allowed':
    case 'service-not-allowed':
      // 非安全上下文（http + 非 localhost）下，浏览器会直接拒绝麦克风，
      // 报的还是 not-allowed，容易被误判成「用户没给权限」，这里分开提示。
      if (!isSecureContextForMic()) {
        return '语音识别需要 HTTPS 或 localhost 环境：当前页面是 http 且非本机地址，浏览器禁止调用麦克风。请改用 https 访问，或在本机 localhost 下使用。'
      }
      return '麦克风或语音服务未获授权，请在 Chrome/Edge 中允许麦克风权限。'
    case 'audio-capture':
      return '没有检测到可用麦克风，请检查系统输入设备。'
    case 'no-speech':
      return '没有听到清晰语音，请靠近麦克风后重试。'
    case 'network':
      return '浏览器语音服务网络不可用，可先用文字输入。'
    case 'language-not-supported':
      return '当前语音服务不支持中文识别，请改用 Chrome/Edge。'
    case 'aborted':
      return '语音识别被中断，可重新点击语音输入。'
    default:
      return '语音识别服务不可用，请改用 Chrome/Edge 或直接输入文字。'
  }
}

// 把识别到的文字追加进回答框
function appendTranscript(text: string) {
  const normalized = text.replace(/\s+/g, ' ').trim()
  if (!normalized || isFinished.value) return
  answer.value = `${answer.value.trim()}${answer.value.trim() ? ' ' : ''}${normalized}`
  voiceStatus.value = '已识别到语音，可继续补充或提交回答。'
}

function toggleRecording() {
  if (isRecording.value) {
    shouldKeepRecognition = false
    if (recognitionRestartTimer !== null) {
      window.clearTimeout(recognitionRestartTimer)
      recognitionRestartTimer = null
    }
    recognition?.stop()
    isRecording.value = false
    interimTranscript.value = ''
    voiceStatus.value = answer.value.trim() ? '语音输入已停止，可检查文字后提交。' : '没有识别到文字，请重试或直接输入。'
    return
  }
  const Constructor = speechRecognitionConstructor()
  if (!Constructor) {
    speechSupported.value = false
    voiceStatus.value = '当前浏览器不支持网页语音识别，请用 Chrome/Edge 打开并允许麦克风权限。'
    return
  }
  // 非安全上下文下直接给出明确原因，避免用户反复去找「麦克风权限」而找不到问题
  if (!isSecureContextForMic()) {
    voiceStatus.value = '语音识别需要 HTTPS 或 localhost：当前是 http + 非本机地址，浏览器禁止调用麦克风。请用 https 访问，或在本机 localhost 下使用（文字输入不受影响）。'
    return
  }
  recognition = new Constructor()
  recognition.lang = 'zh-CN'
  recognition.continuous = true
  recognition.interimResults = true
  shouldKeepRecognition = true
  interimTranscript.value = ''
  recognition.onresult = (event) => {
    let finalText = ''
    let interim = ''
    for (let i = event.resultIndex; i < event.results.length; i += 1) {
      const text = event.results[i][0]?.transcript || ''
      if (event.results[i].isFinal) finalText += text
      else interim += text
    }
    if (finalText) appendTranscript(finalText)
    interimTranscript.value = interim
    voiceStatus.value = interim ? '正在识别，请继续说话...' : '正在听取语音...'
  }
  recognition.onerror = (event) => {
    shouldKeepRecognition = false
    isRecording.value = false
    interimTranscript.value = ''
    voiceStatus.value = speechErrorMessage(event.error)
  }
  recognition.onend = () => {
    if (shouldKeepRecognition && isRecording.value) {
      recognitionRestartTimer = window.setTimeout(() => {
        recognitionRestartTimer = null
        if (!shouldKeepRecognition || !isRecording.value) return
        try { recognition?.start() } catch { /* 已在启动中 */ }
      }, 180)
      return
    }
    isRecording.value = false
    interimTranscript.value = ''
    if (!voiceStatus.value || voiceStatus.value === '正在听取语音...') voiceStatus.value = '语音输入已停止，可检查文字后提交。'
  }
  try {
    recognition.start()
    isRecording.value = true
    voiceStatus.value = '正在听取语音...'
  } catch {
    shouldKeepRecognition = false
    isRecording.value = false
    voiceStatus.value = '无法启动麦克风，请改用文字输入。'
  }
}

// 朗读当前题目（TTS）
function speakCurrentQuestion() {
  const text = currentQuestion.value?.question || ''
  if (!text || !('speechSynthesis' in window)) {
    voiceStatus.value = '当前浏览器不支持朗读，请直接阅读文字。'
    return
  }
  if (isSpeaking.value) {
    window.speechSynthesis.cancel()
    isSpeaking.value = false
    return
  }
  if (isRecording.value) toggleRecording()
  window.speechSynthesis.cancel()
  const utterance = new SpeechSynthesisUtterance(text)
  utterance.lang = 'zh-CN'
  utterance.onstart = () => { isSpeaking.value = true; voiceStatus.value = '正在朗读问题...' }
  utterance.onend = () => { isSpeaking.value = false; voiceStatus.value = '朗读完成。' }
  utterance.onerror = () => { isSpeaking.value = false; voiceStatus.value = '朗读失败，请直接阅读文字。' }
  window.speechSynthesis.speak(utterance)
}

// 用户配置的题目总数：优先从后端响应里读，没有就 fallback 到默认 10
const totalQuestions = computed(() => sessionInfo.value?.totalQuestions || 10)
const progressPercent = computed(() => Math.min((currentQuestionIndex.value / totalQuestions.value) * 100, 100))

// 映射表
const questionTypeMap: Record<string, string> = {
  'RESUME_PROJECT': '项目经历',
  'TECHNICAL': '技术问题',
  'BEHAVIORAL': '行为面试',
  'PROJECT': '项目经历',
}

const questionTypeIcon = computed(() => {
  const type = currentQuestion.value?.questionType
  if (type === 'TECHNICAL') {
    return '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="16 18 22 12 16 6"/><polyline points="8 6 2 12 8 18"/></svg>'
  }
  if (type === 'BEHAVIORAL') {
    return '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>'
  }
  return '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>'
})

const difficultyMap: Record<string, string> = {
  'EASY': '简单',
  'MEDIUM': '中等',
  'HARD': '困难',
}

// ---------- 从后端响应更新当前题目 ----------
const updateCurrentQuestion = (resp: InterviewSessionResponse) => {
  sessionInfo.value = resp
  if (resp.currentQuestion) {
    currentQuestion.value = {
      topic: resp.currentQuestion,
      question: resp.currentQuestion,
      difficulty: resp.difficulty || 'MEDIUM',
      questionType: 'TECHNICAL'
    }
  }
  currentQuestionIndex.value = resp.currentQuestionIndex || currentQuestionIndex.value
}

// ---------- 流式提交（打字机平滑渲染） ----------
// 注意：模型出题速度极快（整题常在 0.2~0.3s 内全部推完），直接逐帧渲染肉眼看不出过程。
// 因此前端做平滑渲染：先入缓冲区，再按可控节奏放出，保证约 1~2 秒的打字机观感。
// 流式链路失败时自动回退同步接口，面试不中断。提交回答与跳过本题共用此入口。
const streamSubmitWithFallback = async (submittedAnswer: string): Promise<InterviewSessionResponse> => {
  try {
    let streamBuf = ''
    let typedLen = 0
    let streamEnded = false
    let typingTimer: ReturnType<typeof setInterval> | null = null

    const stopTyping = () => {
      if (typingTimer) {
        clearInterval(typingTimer)
        typingTimer = null
      }
    }
    const applyTyped = () => {
      if (currentQuestion.value) {
        const text = streamBuf.slice(0, typedLen)
        currentQuestion.value.question = text
        currentQuestion.value.topic = text
      }
    }
    const startTyping = () => {
      if (typingTimer) return
      typingTimer = setInterval(() => {
        const remaining = streamBuf.length - typedLen
        if (remaining <= 0) {
          if (streamEnded) stopTyping()
          return
        }
        // 剩余越多走得越快（约 1.5s 时间常数追平），至少每帧 1 字
        typedLen += Math.max(1, Math.ceil(remaining / 50))
        if (typedLen > streamBuf.length) typedLen = streamBuf.length
        applyTyped()
      }, 30)
    }
    const waitTypingDrained = () =>
      new Promise<void>((resolve) => {
        const check = () => {
          if (typedLen >= streamBuf.length) {
            resolve()
            return
          }
          setTimeout(check, 30)
        }
        check()
      })

    const fallbackDifficulty = currentQuestion.value?.difficulty
      || sessionInfo.value?.difficulty
      || 'MEDIUM'

    // 用对象持有回调结果：直接声明 let 变量会被 TS 控制流分析收窄成 never
    const streamResult: { resp: InterviewSessionResponse | null; error: string | null } = {
      resp: null,
      error: null
    }

    try {
      await submitAnswerStream(sessionId.value!, submittedAnswer, {
        onToken: (text) => {
          streamBuf += text
          if (!isStreaming.value) {
            // 收到第一个字才切到新题卡片，此前保持「AI 正在分析」状态
            isStreaming.value = true
            currentQuestion.value = {
              topic: '',
              question: '',
              difficulty: fallbackDifficulty,
              questionType: 'TECHNICAL'
            }
          }
          startTyping()
        },
        onDone: (resp) => {
          streamEnded = true
          streamResult.resp = resp
        },
        onError: (msg) => { streamResult.error = msg || '流式输出失败' },
      })

      // 等打字机把已接收的文本放完再返回，避免最终校正打断动画
      await waitTypingDrained()
    } finally {
      stopTyping()
    }

    if (streamResult.error) throw new Error(streamResult.error)
    const finalResp = streamResult.resp
    if (!finalResp) throw new Error('未收到本轮面试结果')
    return finalResp
  } catch (error: any) {
    console.error('流式提交失败，回退同步接口:', error)
    // 流式链路不可用时退回同步接口，保证面试不中断
    return await submitAnswer(sessionId.value!, submittedAnswer)
  }
}

// ---------- 提交答案 ----------
const handleSubmitAnswer = async () => {
  if (!answer.value.trim()) {
    ElMessage.warning('请输入回答内容')
    return
  }
  // 提交前先停止录音，避免把后续语音继续写进回答框
  if (isRecording.value) toggleRecording()
  if (!sessionId.value) {
    statusMessage.value = '会话ID无效，请重新开始面试'
    statusType.value = 'error'
    return
  }

  isAnswering.value = true
  isStreaming.value = false
  statusMessage.value = ''

  // 保存回答记录
  answersHistory.push({
    question: currentQuestion.value?.question || '',
    answer: answer.value
  })
  answeredCount.value++

  const submittedAnswer = answer.value

  try {
    const finalResp = await streamSubmitWithFallback(submittedAnswer)

    // 以后端返回为准做最终校正（题目文本、题号、是否结束）
    updateCurrentQuestion(finalResp)

    // 如果面试已结束（finished=true），自动结束
    if (finalResp.finished) {
      await finishAndGenerateReport()
      return
    }

    answer.value = ''
    statusMessage.value = '回答已提交，准备下一题...'
    statusType.value = 'success'
  } catch (error: any) {
    console.error('提交答案失败:', error)
    statusMessage.value = error?.displayMessage || error?.message || '提交失败，请重试'
    statusType.value = 'error'
  } finally {
    isStreaming.value = false
    isAnswering.value = false
  }
}

// ---------- 跳过本题 ----------
const handleSkip = async () => {
  if (isFinished.value) return
  if (!sessionId.value) return
  if (isAnswering.value) return
  isAnswering.value = true
  statusMessage.value = ''
  try {
    // 用固定标记让后端 localPrecheck 直接判 SKIP → 切下一题。
    // 之前是调 handleSubmitAnswer，但空答案会被前端「请输入回答内容」拦截，导致"已跳过本题"toast 之后题没切。
    // 现在走流式入口：跳过后 AI 出的新题同样逐字推送。
    const resp = await streamSubmitWithFallback('（用户跳过本题）')
    updateCurrentQuestion(resp)
    if (resp.finished) {
      await finishAndGenerateReport()
      return
    }
    answer.value = ''
    statusMessage.value = '已跳过本题，进入下一题'
    statusType.value = 'info'
  } catch (error: any) {
    console.error('跳过本题失败:', error)
    statusMessage.value = error?.displayMessage || error?.message || '跳过失败，请重试'
    statusType.value = 'error'
  } finally {
    isAnswering.value = false
  }
}

// ---------- 结束面试并生成报告 ----------
const finishAndGenerateReport = async () => {
  if (!sessionId.value) return
  try {
    isGeneratingReport.value = true
    isFinished.value = true
    // 调用后端结束面试，生成报告
    await finishInterview(sessionId.value)
    statusMessage.value = '报告生成成功！'
    statusType.value = 'success'
  } catch (error: any) {
    console.error('生成报告失败:', error)
    statusMessage.value = error?.displayMessage || error?.message || '报告生成失败，但可以查看已有数据'
    statusType.value = 'warning'
  } finally {
    isGeneratingReport.value = false
  }
}

// ---------- 手动结束面试 ----------
const handleFinish = () => {
  if (isFinished.value) return
  if (answersHistory.length === 0) {
    ElMessage.warning('至少回答一题才能结束面试')
    return
  }
  finishAndGenerateReport()
}

// ---------- 跳转到报告页 ----------
const goToReport = () => {
  router.push('/report')
}

// ---------- 页面加载时恢复会话 ----------
onMounted(async () => {
  // 检测浏览器是否支持网页语音识别
  speechSupported.value = Boolean(speechRecognitionConstructor())
  if (!speechSupported.value) {
    voiceStatus.value = '当前浏览器未提供网页语音识别，文字输入仍可正常使用。'
  }
  try {
    loading.value = true
    // 优先从 URL ?sessionId= 读（HistoryPage「查看问答/继续面试」跳转传的），再 fallback sessionStorage
    let resolvedSessionId: number | null = null
    const urlParams = new URLSearchParams(window.location.search)
    const fromQuery = urlParams.get('sessionId')
    if (fromQuery) {
      resolvedSessionId = Number(fromQuery)
    } else {
      const sessionStr = sessionStorage.getItem('interviewSession')
      if (sessionStr) {
        const session = JSON.parse(sessionStr)
        resolvedSessionId = session.sessionId || session.id
      }
    }
    sessionId.value = resolvedSessionId

    if (!sessionId.value) {
      statusMessage.value = '会话ID无效，请先创建面试'
      statusType.value = 'error'
      loading.value = false
      return
    }

    // 从后端获取会话最新状态（包括当前题目）
    const resp = await getSession(sessionId.value)
    updateCurrentQuestion(resp)

    // 从后端获取消息列表，计算已答题数（candidate 的消息数）
    try {
      const messages = await getMessageList(sessionId.value)
      answeredCount.value = messages.filter(m => m.sender === 'candidate' || m.sender === 'user').length
    } catch (e) {
      console.error('获取消息列表失败:', e)
    }

    // 如果会话已结束，直接显示完成状态
    if (resp.finished) {
      isFinished.value = true
    }
  } catch (error: any) {
    console.error('加载面试会话失败:', error)
    statusMessage.value = error?.displayMessage || error?.message || '加载面试失败，请重试'
    statusType.value = 'error'
  } finally {
    loading.value = false
  }
})

// 离开页面时停止录音与朗读，释放麦克风
onUnmounted(() => {
  shouldKeepRecognition = false
  if (recognitionRestartTimer !== null) window.clearTimeout(recognitionRestartTimer)
  recognition?.stop()
  window.speechSynthesis?.cancel()
})
</script>

<style scoped>
.interview-page {
  max-width: 760px;
  margin: 0 auto;
  padding-top: 8px;
}

/* 加载状态 */
.loading-state {
  text-align: center;
  padding: 80px 20px;
  color: var(--text-muted);
}
.loading-spinner {
  width: 36px;
  height: 36px;
  border: 3px solid var(--border);
  border-top-color: var(--accent);
  border-radius: 50%;
  margin: 0 auto 16px;
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
.loading-state p {
  font-size: 15px;
}

/* 顶部 */
.topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.topbar-left {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.page-tag {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--accent);
  font-weight: 600;
  letter-spacing: 0.5px;
}
.tag-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--accent);
  animation: pulse 2s infinite;
}
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}
.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
}
.topbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.progress-info {
  font-family: var(--font-mono);
  font-size: 14px;
  color: var(--text-secondary);
}
.progress-current {
  font-size: 20px;
  font-weight: 700;
  color: var(--accent);
}
.progress-sep {
  margin: 0 2px;
}

/* 进度条 */
.progress-bar {
  height: 4px;
  background: var(--border-light);
  border-radius: 2px;
  overflow: hidden;
  margin-bottom: 24px;
}
.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--accent), var(--accent-light));
  border-radius: 2px;
  transition: width 0.5s ease;
}

/* 完成卡片 */
.finish-card {
  text-align: center;
  padding: 48px 32px;
  margin-bottom: 24px;
}
.finish-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 20px;
  border-radius: 50%;
  background: var(--accent-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--accent);
}
.finish-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 8px;
}
.finish-desc {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0 0 24px;
}

/* 问题卡片 */
.question-card {
  padding: 28px;
  margin-bottom: 20px;
}
.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.question-type {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--accent);
}
.type-icon {
  display: inline-flex;
}
.question-difficulty {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 100px;
  font-weight: 600;
}
.diff-easy {
  background: rgba(34, 197, 94, 0.1);
  color: #16a34a;
}
.diff-medium {
  background: rgba(234, 179, 8, 0.1);
  color: #ca8a04;
}
.diff-hard {
  background: rgba(239, 68, 68, 0.1);
  color: #dc2626;
}
.question-content {
  font-size: 18px;
  line-height: 1.7;
  color: var(--text-primary);
  font-weight: 500;
  margin-bottom: 20px;
  /* 流式输出时逐字插入，避免整段重排导致的抖动 */
  white-space: pre-wrap;
  word-break: break-word;
}
/* 流式输出光标 */
.stream-cursor {
  display: inline-block;
  width: 2px;
  height: 1em;
  margin-left: 2px;
  vertical-align: text-bottom;
  background: currentColor;
  animation: stream-blink 1s steps(2, start) infinite;
}
@keyframes stream-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}
.question-footer {
  display: flex;
  align-items: center;
}
.question-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-muted);
}

/* 回答卡片 */
.answer-card {
  padding: 24px;
}
.answer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.answer-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}
.answer-count {
  font-size: 12px;
  color: var(--text-muted);
  font-family: var(--font-mono);
}
.answer-textarea {
  width: 100%;
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-primary);
  background: var(--bg-card);
  resize: vertical;
  font-family: inherit;
  transition: border-color var(--transition-fast);
  box-sizing: border-box;
}
.answer-textarea:focus {
  outline: none;
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-bg);
}
.answer-textarea:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.answer-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
}

/* 语音输入 / 朗读 */
.voice-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 14px;
}
.voice-status {
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.5;
}
.btn-recording {
  background: rgba(239, 68, 68, 0.08) !important;
  border-color: #dc2626 !important;
  color: #dc2626 !important;
  animation: pulse 1.6s ease-in-out infinite;
}
.interim-transcript {
  margin-top: 10px;
  padding: 8px 12px;
  border-left: 3px solid var(--accent);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
  background: var(--accent-bg);
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.5;
}

/* 按钮 */
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 18px;
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);
  border: none;
  font-family: inherit;
  line-height: 1.5;
}
.btn-primary {
  background: #2d5a3d !important;
  color: #ffffff !important;
}
.btn-primary:hover:not(:disabled) {
  background: #1f4230 !important;
  color: #ffffff !important;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(45, 90, 61, 0.3);
}
.btn-primary:disabled,
.btn-primary.disabled {
  background: #9ca3af !important;
  color: #ffffff !important;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
  opacity: 1 !important;
}
.btn-primary svg {
  stroke: currentColor !important;
  fill: none !important;
}
.btn-ghost {
  background: transparent;
  color: #4b5563;
  border: 1px solid #d1d5db;
}
.btn-ghost:hover:not(:disabled) {
  border-color: #2d5a3d;
  color: #2d5a3d;
  background: rgba(45, 90, 61, 0.08);
}
.btn-small {
  padding: 6px 12px;
  font-size: 13px;
}
.btn-large {
  padding: 14px 28px;
  font-size: 16px;
}

/* AI思考 */
.ai-thinking {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 16px;
  margin-top: 16px;
  color: var(--text-secondary);
  font-size: 14px;
}
.thinking-dots {
  display: flex;
  gap: 4px;
}
.thinking-dots span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--accent);
  animation: bounce 1.4s infinite ease-in-out both;
}
.thinking-dots span:nth-child(1) { animation-delay: -0.32s; }
.thinking-dots span:nth-child(2) { animation-delay: -0.16s; }
@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

/* 状态提示 */
.status-alert {
  padding: 12px 16px;
  border-radius: var(--radius-sm);
  font-size: 14px;
  margin-top: 16px;
}
.status-success {
  background: rgba(34, 197, 94, 0.1);
  color: #16a34a;
}
.status-warning {
  background: rgba(234, 179, 8, 0.1);
  color: #ca8a04;
}
.status-error {
  background: rgba(239, 68, 68, 0.1);
  color: #dc2626;
}
.status-info {
  background: var(--accent-bg);
  color: var(--accent);
}

/* 动画 */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.3s ease;
}
.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(10px);
}
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
.fade-scale-enter-active,
.fade-scale-leave-active {
  transition: all 0.4s ease;
}
.fade-scale-enter-from,
.fade-scale-leave-to {
  opacity: 0;
  transform: scale(0.9);
}
.animate-reveal {
  opacity: 0;
  transform: translateY(20px);
  animation: reveal 0.6s ease forwards;
}
.animate-reveal-delay-1 { animation-delay: 0.1s; }
.animate-reveal-delay-2 { animation-delay: 0.2s; }
@keyframes reveal {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式 */
@media (max-width: 640px) {
  .topbar {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  .question-card,
  .answer-card {
    padding: 20px;
  }
  .question-content {
    font-size: 16px;
  }
}
</style>
