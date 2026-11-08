<template>
  <div class="report-page">
    <!-- 页面标题区 -->
    <PageHeader
      tag="面试完成 · 报告生成"
      title="你的"
      highlight="面试报告"
      description="AI 基于你的回答生成了综合评估，包含能力分析、亮点薄弱点和改进建议。"
      accent="#7f1d1d"
    />

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>正在生成面试报告...</p>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="loadError" class="error-state">
      <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="12" cy="12" r="10"/>
        <line x1="12" y1="8" x2="12" y2="12"/>
        <line x1="12" y1="16" x2="12.01" y2="16"/>
      </svg>
      <p>{{ loadError }}</p>
      <button class="btn btn-primary" @click="loadReportData">重新加载</button>
    </div>

    <!-- 报告内容 -->
    <template v-else>

    <!-- 本场面试信息：不同岗位的报告在此处可见区分 -->
    <div v-if="reportData.jobTitle" class="session-meta card animate-reveal">
      <span class="meta-label">应聘岗位</span>
      <span class="meta-value">{{ reportData.jobTitle }}</span>
      <span v-if="reportData.difficulty" class="meta-tag">{{ difficultyText }}</span>
    </div>

    <!-- 总分卡片 -->
    <div class="score-card card animate-reveal animate-reveal-delay-1">
      <div class="score-left">
        <div class="score-label">综合得分</div>
        <div class="score-number">
          <span class="score-value">{{ reportData.totalScore }}</span>
          <span class="score-max">/ 100</span>
        </div>
        <div class="score-level">
          <span class="level-badge">{{ scoreLevel }}</span>
          <span class="level-desc">{{ scoreDesc }}</span>
        </div>
      </div>
      <div class="score-right">
        <div class="score-ring">
          <svg width="140" height="140" viewBox="0 0 140 140">
            <circle cx="70" cy="70" r="60" fill="none" stroke="var(--border)" stroke-width="10"/>
            <circle cx="70" cy="70" r="60" fill="none" stroke="url(#scoreGradient)" stroke-width="10"
              stroke-linecap="round" :stroke-dasharray="circumference" :stroke-dashoffset="dashOffset"
              transform="rotate(-90 70 70)"/>
            <defs>
              <linearGradient id="scoreGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stop-color="#2d5a3d"/>
                <stop offset="100%" stop-color="#a8d5ba"/>
              </linearGradient>
            </defs>
          </svg>
          <div class="ring-text">{{ reportData.totalScore }}%</div>
        </div>
      </div>
    </div>

    <!-- 能力评估 -->
    <div class="section card animate-reveal animate-reveal-delay-2">
      <div class="section-header">
        <div class="section-title">
          <span class="title-num">01</span>
          五维能力评估
        </div>
      </div>
      <div class="ability-content">
        <div class="chart-container" ref="chartRef"></div>
        <div class="ability-list">
          <div v-for="(dim, index) in reportData.dimensions" :key="dim" class="ability-item">
            <div class="ability-name">{{ dim }}</div>
            <div class="ability-bar">
              <div class="ability-fill" :style="{ width: reportData.scores[index] + '%' }"></div>
            </div>
            <div class="ability-score">{{ reportData.scores[index] }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 亮点与薄弱点 -->
    <div class="two-col animate-reveal animate-reveal-delay-3">
      <div class="insight-card card strengths">
        <div class="insight-header">
          <div class="insight-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
            </svg>
          </div>
          <div class="insight-title">面试亮点</div>
        </div>
        <ul class="insight-list">
          <li v-for="(item, i) in reportData.strengths" :key="i">
            <span class="list-num">{{ String(i + 1).padStart(2, '0') }}</span>
            {{ item }}
          </li>
        </ul>
      </div>

      <div class="insight-card card weaknesses">
        <div class="insight-header">
          <div class="insight-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"/>
              <line x1="12" y1="8" x2="12" y2="12"/>
              <line x1="12" y1="16" x2="12.01" y2="16"/>
            </svg>
          </div>
          <div class="insight-title">待提升点</div>
        </div>
        <ul class="insight-list">
          <li v-for="(item, i) in reportData.weaknesses" :key="i">
            <span class="list-num">{{ String(i + 1).padStart(2, '0') }}</span>
            {{ item }}
          </li>
        </ul>
      </div>
    </div>

    <!-- 面试详情：每道题问答记录 -->
    <div class="section card animate-reveal animate-reveal-delay-3">
      <div class="section-header">
        <div class="section-title">
          <span class="title-num">02</span>
          面试详情
        </div>
        <span class="doc-count">共 {{ questionDetails.length }} 道题</span>
      </div>
      <div class="question-list">
        <div v-for="(q, index) in questionDetails" :key="index" class="question-item">
          <div class="question-header">
            <div class="question-num">第 {{ index + 1 }} 题</div>
            <div v-if="q.score !== null" class="question-score" :class="getScoreClass(q.score)">
              <span class="score-num">{{ q.score }}</span>
              <span class="score-max">/100</span>
            </div>
            <div v-else class="question-score score-unknown">
              <span class="score-num">—</span>
            </div>
          </div>
          <div class="question-type-tag">{{ q.type }}</div>
          <div class="question-text">{{ q.question }}</div>
          <div class="answer-block">
            <div class="answer-label">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                <circle cx="12" cy="7" r="4"/>
              </svg>
              你的回答
            </div>
            <div class="answer-text">{{ q.answer }}</div>
          </div>
          <div v-if="q.referenceAnswer" class="reference-block">
            <div class="reference-label">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M9 11l3 3L22 4"/>
                <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/>
              </svg>
              参考回复
            </div>
            <div class="reference-text">{{ q.referenceAnswer }}</div>
          </div>
          <div class="feedback-block">
            <div class="feedback-label">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M12 2L2 7l10 5 10-5-10-5z"/>
                <path d="M2 17l10 5 10-5"/>
                <path d="M2 12l10 5 10-5"/>
              </svg>
              AI 点评
            </div>
            <div class="feedback-text">{{ q.feedback || '暂无逐题点评' }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 改进建议 -->
    <div class="section card animate-reveal animate-reveal-delay-4">
      <div class="section-header">
        <div class="section-title">
          <span class="title-num">03</span>
          改进建议
        </div>
      </div>
      <div class="suggestions-list">
        <div v-for="(item, i) in reportData.suggestions" :key="i" class="suggestion-item">
          <div class="suggestion-num">{{ String(i + 1).padStart(2, '0') }}</div>
          <div class="suggestion-content">{{ item }}</div>
        </div>
      </div>
    </div>

    <!-- 操作区 -->
    <div class="actions animate-reveal animate-reveal-delay-5">
      <button class="btn btn-secondary btn-large" @click="goHome">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
          <polyline points="9 22 9 12 15 12 15 22"/>
        </svg>
        返回首页
      </button>
      <button class="btn btn-primary btn-large" @click="restartInterview">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="23 4 23 10 17 10"/>
          <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/>
        </svg>
        再来一次
      </button>
    </div>

    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { getReportBySessionId, getMessageList } from '../api/report'
import PageHeader from '../components/PageHeader.vue'

const router = useRouter()
const chartRef = ref<HTMLElement | null>(null)
let chartInstance: echarts.ECharts | null = null
const loading = ref(true)
const loadError = ref('')

// ---------- 报告数据（初始为空，API返回后填充） ----------
const reportData = reactive({
  totalScore: 0,
  dimensions: ['专业能力', '逻辑结构', '表达沟通', '岗位匹配度', '知识覆盖度'],
  scores: [0, 0, 0, 0, 0],
  summary: '',
  strengths: [] as string[],
  weaknesses: [] as string[],
  suggestions: [] as string[],
  // 本场面试的岗位与难度（来自报告接口，用于区分不同岗位的报告）
  jobTitle: '',
  difficulty: ''
})

// ---------- 每道题问答记录（初始为空，API返回后填充） ----------
const questionDetails = reactive<Array<{
  type: string
  question: string
  answer: string
  referenceAnswer: string
  // null 表示后端未提供该题评分（旧报告），与真实 0 分区分开
  score: number | null
  feedback: string
}>>([])

// 难度中文映射
const difficultyText = computed(() => {
  const map: Record<string, string> = { EASY: '简单', MEDIUM: '中等', HARD: '困难' }
  return map[reportData.difficulty] || reportData.difficulty
})

const getScoreClass = (score: number) => {
  if (score >= 85) return 'score-high'
  if (score >= 70) return 'score-medium'
  return 'score-low'
}

/**
 * 判断是否为开场自我介绍。后端把它作为预热消息入库，不计入有效题目，
 * 因此报告页的「共 N 道题」也不应该把它算进去。
 */
const isSelfIntro = (content?: string) =>
  !!content && content.includes('自我介绍')

// 计算属性
const circumference = 2 * Math.PI * 60
const dashOffset = computed(() => circumference * (1 - reportData.totalScore / 100))

const scoreLevel = computed(() => {
  const s = reportData.totalScore
  if (s >= 90) return '优秀'
  if (s >= 80) return '良好'
  if (s >= 70) return '中等'
  if (s >= 60) return '及格'
  return '待提升'
})

const scoreDesc = computed(() => {
  const s = reportData.totalScore
  if (s >= 90) return '表现出色，继续保持'
  if (s >= 80) return '整体不错，有提升空间'
  if (s >= 70) return '中规中矩，建议加强练习'
  if (s >= 60) return '基础薄弱，需要系统提升'
  return '建议从基础开始系统学习'
})

// ---------- 渲染雷达图 ----------
const renderChart = () => {
  if (!chartRef.value) return

  chartInstance = echarts.init(chartRef.value)
  const option = {
    radar: {
      indicator: reportData.dimensions.map((name) => ({ name, max: 100 })),
      shape: 'circle',
      center: ['50%', '50%'],
      radius: '65%',
      splitNumber: 5,
      axisName: {
        color: '#1a2e1f',
        fontSize: 13,
        fontWeight: 600,
        fontFamily: 'Inter, sans-serif',
        padding: [8, 8]
      },
      splitLine: {
        lineStyle: {
          color: ['#e5e0d8', '#d4cfc5', '#c3beb2', '#b2ada0', '#a19c8d'],
          width: 1
        }
      },
      splitArea: {
        areaStyle: {
          color: [
            'rgba(168, 213, 186, 0.08)',
            'rgba(168, 213, 186, 0.12)',
            'rgba(168, 213, 186, 0.16)',
            'rgba(168, 213, 186, 0.20)',
            'rgba(168, 213, 186, 0.24)'
          ]
        }
      },
      axisLine: {
        lineStyle: {
          color: '#b8b3a8',
          width: 1.5
        }
      }
    },
    series: [
      {
        type: 'radar',
        symbolSize: 10,
        data: [
          {
            value: reportData.scores,
            name: '你的表现',
            areaStyle: {
              color: new echarts.graphic.RadialGradient(0.5, 0.5, 1, [
                { offset: 0, color: 'rgba(45, 90, 61, 0.5)' },
                { offset: 0.7, color: 'rgba(45, 90, 61, 0.3)' },
                { offset: 1, color: 'rgba(168, 213, 186, 0.2)' }
              ])
            },
            lineStyle: {
              color: '#2d5a3d',
              width: 3,
              shadowColor: 'rgba(45, 90, 61, 0.4)',
              shadowBlur: 10
            },
            itemStyle: {
              color: '#2d5a3d',
              borderColor: '#fff',
              borderWidth: 3,
              shadowColor: 'rgba(45, 90, 61, 0.5)',
              shadowBlur: 8
            },
            label: {
              show: true,
              formatter: '{c}',
              color: '#2d5a3d',
              fontSize: 12,
              fontWeight: 700,
              fontFamily: 'JetBrains Mono, monospace',
              backgroundColor: 'rgba(255, 255, 255, 0.9)',
              padding: [3, 7],
              borderRadius: 4,
              borderColor: '#2d5a3d',
              borderWidth: 1
            }
          }
        ],
        animationDuration: 1500,
        animationEasing: 'cubicOut'
      }
    ]
  }

  chartInstance.setOption(option)
}

const handleResize = () => {
  chartInstance?.resize()
}

// ---------- 导航 ----------
const goHome = () => {
  router.push('/')
}

const restartInterview = () => {
  // 复用已解析的简历，直接进入「面试准备」的第二步（面试配置）
  router.push({ path: '/', query: { step: '2' } })
}

// ---------- 加载真实报告数据 ----------
const loadReportData = async () => {
  try {
    loading.value = true
    loadError.value = ''

    // 优先从 URL ?sessionId= 读（HistoryPage「查看报告/查看问答」跳转传的），再 fallback sessionStorage
    let sessionId: number | null = null
    const urlParams = new URLSearchParams(window.location.search)
    const fromQuery = urlParams.get('sessionId')
    if (fromQuery) {
      sessionId = Number(fromQuery)
    } else {
      const sessionStr = sessionStorage.getItem('interviewSession')
      if (sessionStr) {
        const session = JSON.parse(sessionStr)
        sessionId = session.sessionId || session.id
      }
    }
    if (!sessionId) {
      loadError.value = '未找到面试会话，请先完成面试'
      return
    }

    // 并行获取报告数据和消息列表
    const [report, messages] = await Promise.all([
      getReportBySessionId(sessionId),
      getMessageList(sessionId)
    ])

    // 填充报告数据
    reportData.totalScore = report.score || 0
    reportData.summary = report.summary || ''
    reportData.jobTitle = report.jobTitle || ''
    reportData.difficulty = report.difficulty || ''
    reportData.scores = [
      report.professionalAbility || 0,
      report.logicalThinking || 0,
      report.communication || 0,
      report.jobMatch || 0,
      report.knowledgeCoverage || 0
    ]
    // 亮点和薄弱点按换行分割成数组
    reportData.strengths = report.strengths ? report.strengths.split(/\n|；|;/).filter(s => s.trim()) : []
    reportData.weaknesses = report.weaknesses ? report.weaknesses.split(/\n|；|;/).filter(s => s.trim()) : []
    reportData.suggestions = report.suggestions || []

    // 填充问答记录
    // 优先用后端下发的逐题评价（questionReviews）：它已按真实技术题对齐，
    // 天然不含开场自我介绍，且带每题得分与 AI 点评。
    const reviews = report.questionReviews || []
    questionDetails.splice(0, questionDetails.length)
    if (reviews.length > 0) {
      // 后端逐题评价的顺序即真实题目顺序，index 从 1 开始
      reviews
        .slice()
        .sort((a, b) => (a.index || 0) - (b.index || 0))
        .forEach((r, idx) => {
          questionDetails.push({
            type: `第${idx + 1}题`,
            question: r.question || '',
            answer: r.answer || '（未回答）',
            referenceAnswer: r.referenceAnswer || '',
            score: typeof r.score === 'number' ? r.score : null,
            feedback: r.comment || ''
          })
        })
    } else {
      // 兼容旧报告（生成于本功能上线前）：退化为按消息列表拼装，
      // 跳过开场自我介绍，且不显示伪造的 0 分。
      const questionMsgs = messages.filter(
        (m: any) => (m.sender === 'interviewer' || m.sender === 'ai') && !isSelfIntro(m.content)
      )
      const answerMsgs = messages.filter((m: any) => m.sender === 'candidate' || m.sender === 'user')
      questionMsgs.forEach((q: any, idx: number) => {
        const answer = answerMsgs[idx]
        questionDetails.push({
          type: `第${idx + 1}题`,
          question: q.content,
          answer: answer?.content || '（未回答）',
          referenceAnswer: q.referenceAnswer || '',
          score: null,
          feedback: ''
        })
      })
    }

    // 数据加载完成后渲染雷达图
    setTimeout(() => renderChart(), 100)
  } catch (err: any) {
    console.error('加载报告失败:', err)
    loadError.value = err?.displayMessage || err?.message || '加载报告失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

// ---------- 生命周期 ----------
onMounted(() => {
  loadReportData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
})
</script>

<style scoped>
/* 加载状态 */
.loading-state {
  text-align: center;
  padding: 80px 20px;
  color: var(--text-muted);
}
.loading-spinner {
  width: 40px;
  height: 40px;
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

/* 错误状态 */
.error-state {
  text-align: center;
  padding: 60px 20px;
  color: #b91c1c;
}
.error-state svg {
  margin-bottom: 16px;
}
.error-state p {
  font-size: 15px;
  margin-bottom: 20px;
  color: var(--text-secondary);
}

.report-page {
  max-width: 860px;
  margin: 0 auto;
  padding-top: 8px;
}

/* 本场面试信息（岗位/难度） */
.session-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 22px;
  margin-bottom: 20px;
}

.session-meta .meta-label {
  font-size: 13px;
  color: var(--text-secondary);
}

.session-meta .meta-value {
  font-size: 15px;
  font-weight: 700;
  color: var(--accent);
}

.session-meta .meta-tag {
  margin-left: auto;
  padding: 3px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: var(--accent);
  background: rgba(45, 90, 61, 0.1);
}

/* 总分卡片 */
.score-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 36px 40px;
  margin-bottom: 24px;
  background: linear-gradient(135deg, #1a2e1f 0%, #2d5a3d 100%);
  border: none;
  color: white;
  position: relative;
  overflow: hidden;
}

.score-card::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -10%;
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(168, 213, 186, 0.15) 0%, transparent 70%);
  border-radius: 50%;
}

.score-left {
  position: relative;
  z-index: 1;
}

.score-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 8px;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

.score-number {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 12px;
}

.score-value {
  font-family: var(--font-heading);
  font-size: 64px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: -0.03em;
}

.score-max {
  font-size: 20px;
  color: rgba(255, 255, 255, 0.5);
  font-weight: 500;
}

.score-level {
  display: flex;
  align-items: center;
  gap: 12px;
}

.level-badge {
  font-size: 13px;
  font-weight: 600;
  padding: 5px 14px;
  background: rgba(168, 213, 186, 0.2);
  color: #a8d5ba;
  border-radius: 100px;
}

.level-desc {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
}

.score-right {
  position: relative;
  z-index: 1;
}

.score-ring {
  position: relative;
  width: 140px;
  height: 140px;
}

.ring-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-family: var(--font-heading);
  font-size: 24px;
  font-weight: 700;
  color: white;
}

/* 区块通用 */
.section {
  padding: 28px 32px;
  margin-bottom: 24px;
}

.section-header {
  margin-bottom: 24px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  font-family: var(--font-heading);
}

.title-num {
  font-family: var(--font-mono);
  font-size: 13px;
  font-weight: 600;
  color: var(--accent);
  background: var(--accent-bg);
  padding: 4px 10px;
  border-radius: 6px;
}

/* 能力评估 */
.ability-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 32px;
  align-items: center;
}

.chart-container {
  width: 100%;
  height: 320px;
}

.ability-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ability-item {
  display: flex;
  align-items: center;
  gap: 14px;
}

.ability-name {
  width: 80px;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  flex-shrink: 0;
}

.ability-bar {
  flex: 1;
  height: 8px;
  background: var(--bg-elevated);
  border-radius: 4px;
  overflow: hidden;
}

.ability-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--accent), var(--accent-light));
  border-radius: 4px;
  transition: width 1s cubic-bezier(0.16, 1, 0.3, 1);
}

.ability-score {
  width: 32px;
  font-family: var(--font-mono);
  font-size: 14px;
  font-weight: 700;
  color: var(--accent);
  text-align: right;
  flex-shrink: 0;
}

/* 两列布局 */
.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 24px;
}

.insight-card {
  padding: 24px;
}

.insight-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-light);
}

.insight-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
}

.strengths .insight-icon {
  background: var(--accent-bg);
  color: var(--accent);
}

.weaknesses .insight-icon {
  background: rgba(245, 158, 11, 0.1);
  color: #d97706;
}

.insight-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  font-family: var(--font-heading);
}

.insight-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.insight-list li {
  display: flex;
  gap: 12px;
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-secondary);
}

.list-num {
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 600;
  color: var(--text-muted);
  flex-shrink: 0;
  margin-top: 2px;
}

/* 面试详情：每道题 */
.question-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.question-item {
  padding: 22px;
  background: var(--bg-elevated);
  border-radius: var(--radius-md);
  border-left: 3px solid var(--accent);
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.question-num {
  font-family: var(--font-heading);
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
}

.question-score {
  display: flex;
  align-items: baseline;
  gap: 2px;
  padding: 6px 14px;
  border-radius: 100px;
  font-weight: 700;
}

.question-score.score-high {
  background: var(--accent-bg);
  color: var(--accent);
}

.question-score.score-medium {
  background: rgba(245, 158, 11, 0.1);
  color: #d97706;
}

.question-score.score-low {
  background: rgba(220, 53, 69, 0.1);
  color: #dc3545;
}

/* 后端未提供该题评分（旧报告）时的占位样式，避免看着像「真的 0 分」 */
.question-score.score-unknown {
  background: rgba(0, 0, 0, 0.05);
  color: var(--text-muted, #9aa0a6);
}

.question-score .score-num {
  font-family: var(--font-mono);
  font-size: 18px;
}

.question-score .score-max {
  font-size: 11px;
  opacity: 0.7;
}

.question-type-tag {
  display: inline-block;
  font-size: 11px;
  font-weight: 600;
  color: var(--accent);
  background: var(--accent-bg);
  padding: 3px 10px;
  border-radius: 4px;
  margin-bottom: 10px;
}

.question-text {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.6;
  margin-bottom: 16px;
}

.answer-block,
.feedback-block,
.reference-block {
  margin-bottom: 14px;
}

.answer-block:last-child,
.feedback-block:last-child,
.reference-block:last-child {
  margin-bottom: 0;
}

.answer-label,
.feedback-label,
.reference-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.feedback-label {
  color: var(--accent);
}

.reference-label {
  color: #0d9488;
}

.answer-text {
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-secondary);
  background: var(--bg-card);
  padding: 14px 16px;
  border-radius: var(--radius-sm);
}

.feedback-text {
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-primary);
  background: var(--accent-bg);
  padding: 14px 16px;
  border-radius: var(--radius-sm);
  border-left: 2px solid var(--accent);
}

.reference-text {
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-primary);
  background: rgba(13, 148, 136, 0.08);
  padding: 14px 16px;
  border-radius: var(--radius-sm);
  border-left: 2px solid #0d9488;
  white-space: pre-wrap;
}

/* 改进建议 */
.suggestions-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.suggestion-item {
  display: flex;
  gap: 18px;
  padding: 18px 20px;
  background: var(--bg-elevated);
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
}

.suggestion-item:hover {
  background: var(--accent-bg);
  transform: translateX(4px);
}

.suggestion-num {
  font-family: var(--font-heading);
  font-size: 24px;
  font-weight: 700;
  color: var(--accent-light);
  line-height: 1;
  flex-shrink: 0;
}

.suggestion-content {
  font-size: 14px;
  line-height: 1.7;
  color: var(--text-primary);
  padding-top: 2px;
}

/* 操作区 */
.actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 36px;
  margin-bottom: 24px;
}

/* 响应式 */
@media (max-width: 768px) {
  .score-card {
    flex-direction: column;
    text-align: center;
    gap: 24px;
  }
  .score-level {
    justify-content: center;
  }
  .ability-content {
    grid-template-columns: 1fr;
  }
  .two-col {
    grid-template-columns: 1fr;
  }
  .score-value {
    font-size: 48px;
  }
  .actions {
    flex-direction: column;
  }
}
</style>
