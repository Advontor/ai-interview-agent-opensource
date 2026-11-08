<template>
  <div class="config-page">
    <!-- 页面标题区（面试准备 · 第二步） -->
    <PageHeader
      tag="面试配置"
      title="配置你的"
      highlight="专属面试"
      description="确认岗位、考察重点和面试风格，AI 将根据你的简历生成专属面试流程。"
      accent="#b45309"
    />

    <!-- 简历完整度卡片（基于真实解析结果计算） -->
    <div v-if="resumeLoaded" class="resume-score-card card animate-reveal animate-reveal-delay-1">
      <div class="score-top">
        <div class="score-left">
          <div class="score-label">简历完整度</div>
          <div class="score-number">
            <span class="score-value">{{ completeness.score }}</span>
            <span class="score-max">/ 100</span>
          </div>
          <div class="score-desc">根据已解析简历的结构完整程度评估</div>
        </div>
        <div class="score-right">
          <div class="score-ring">
            <svg width="100" height="100" viewBox="0 0 100 100">
              <circle cx="50" cy="50" r="42" fill="none" stroke="var(--border)" stroke-width="7"/>
              <circle cx="50" cy="50" r="42" fill="none" stroke="url(#resumeGradient)" stroke-width="7"
                stroke-linecap="round" :stroke-dasharray="264" :stroke-dashoffset="264 * (1 - completeness.score / 100)"
                transform="rotate(-90 50 50)"/>
              <defs>
                <linearGradient id="resumeGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stop-color="#2d5a3d"/>
                  <stop offset="100%" stop-color="#a8d5ba"/>
                </linearGradient>
              </defs>
            </svg>
            <div class="ring-text">{{ completeness.score }}%</div>
          </div>
        </div>
      </div>
      <div class="score-breakdown">
        <span
          v-for="item in completeness.items"
          :key="item.label"
          class="score-chip"
          :class="{ filled: item.ok }"
        >
          <svg v-if="item.ok" width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="20 6 9 17 4 12"/>
          </svg>
          <svg v-else width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
            <line x1="18" y1="6" x2="6" y2="18"/>
            <line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
          {{ item.label }}
          <em>{{ item.hint }}</em>
        </span>
      </div>
    </div>

    <!-- 尚未上传简历 -->
    <div v-else class="resume-empty card animate-reveal animate-reveal-delay-1">
      <div class="resume-empty-icon">
        <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
          <polyline points="14 2 14 8 20 8"/>
        </svg>
      </div>
      <div class="resume-empty-text">
        <div class="resume-empty-title">还没有上传简历</div>
        <div class="resume-empty-desc">先完成简历解析，才能根据你的经历生成专属面试问题。</div>
      </div>
      <button class="btn btn-primary btn-small" @click="emit('back')">去上传简历</button>
    </div>

    <!-- 面试模式选择 -->
    <div class="section animate-reveal animate-reveal-delay-2">
      <div class="section-label">
        <span class="label-num">01</span>
        面试模式
      </div>
      <div class="mode-grid">
        <div
          class="mode-card"
          :class="{ active: selectedMode === 'text' }"
          @click="selectedMode = 'text'"
        >
          <div class="mode-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
            </svg>
          </div>
          <div class="mode-info">
            <div class="mode-name">文字面试</div>
            <div class="mode-desc">打字回答，更稳定，适合系统化刷题</div>
          </div>
          <div class="mode-badge recommended">推荐</div>
          <div class="mode-check">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="20 6 9 17 4 12"/>
            </svg>
          </div>
        </div>
        <div
          class="mode-card"
          :class="{ active: selectedMode === 'voice' }"
          @click="selectedMode = 'voice'"
        >
          <div class="mode-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/>
              <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
              <line x1="12" y1="19" x2="12" y2="23"/>
              <line x1="8" y1="23" x2="16" y2="23"/>
            </svg>
          </div>
          <div class="mode-info">
            <div class="mode-name">语音面试</div>
            <div class="mode-desc">实时语音对话，更贴近真实面试场景</div>
          </div>
          <div class="mode-check">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="20 6 9 17 4 12"/>
            </svg>
          </div>
        </div>
      </div>
    </div>

    <!-- 岗位与职级 -->
    <div class="section animate-reveal animate-reveal-delay-3">
      <div class="section-label">
        <span class="label-num">02</span>
        岗位信息
      </div>
      <div class="form-grid">
        <div class="form-item">
          <label class="form-label">面试岗位</label>
          <select v-model="selectedRole" class="form-select">
            <option value="JAVA_BACKEND">Java 后端开发</option>
            <option value="FRONTEND">前端开发</option>
            <option value="FULL_STACK">全栈开发</option>
            <option value="PRODUCT">产品经理</option>
          </select>
        </div>
        <div class="form-item">
          <label class="form-label">目标职级</label>
          <select v-model="selectedLevel" class="form-select">
            <option value="FRESH">应届生</option>
            <option value="JUNIOR">初级（1-3年）</option>
            <option value="MID">中级（3-5年）</option>
            <option value="SENIOR">高级（5年以上）</option>
          </select>
        </div>
      </div>
      <div class="form-item full-width">
        <label class="form-label">
          岗位描述
          <span class="label-optional">（可选）</span>
        </label>
        <textarea
          v-model="jobDescription"
          class="form-textarea"
          placeholder="补充岗位的特殊要求或技术栈偏好，AI 会据此调整面试问题..."
          maxlength="2000"
          rows="3"
        ></textarea>
        <div class="char-count">{{ jobDescription.length }} / 2000</div>
      </div>
    </div>

    <!-- 难度与题量 -->
    <div class="section animate-reveal animate-reveal-delay-4">
      <div class="section-label">
        <span class="label-num">03</span>
        面试参数
      </div>
      <div class="form-grid">
        <div class="form-item">
          <label class="form-label">面试难度</label>
          <div class="difficulty-options">
            <div
              v-for="d in difficulties"
              :key="d.value"
              class="difficulty-option"
              :class="{ active: selectedDifficulty === d.value }"
              @click="selectedDifficulty = d.value"
            >
              <div class="diff-level">{{ d.label }}</div>
              <div class="diff-desc">{{ d.desc }}</div>
            </div>
          </div>
        </div>
      </div>
      <div class="form-grid two-col">
        <div class="form-item">
          <label class="form-label">题目数量</label>
          <div class="number-input">
            <button class="num-btn" @click="questionCount = Math.max(5, questionCount - 1)">−</button>
            <span class="num-value">{{ questionCount }}</span>
            <button class="num-btn" @click="questionCount = Math.min(20, questionCount + 1)">+</button>
          </div>
        </div>
        <div class="form-item">
          <label class="form-label">预计时长</label>
          <div class="duration-display">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"/>
              <polyline points="12 6 12 12 16 14"/>
            </svg>
            <span>约 {{ estimatedDuration }} 分钟</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 重点考察方向 -->
    <div class="section animate-reveal animate-reveal-delay-5">
      <div class="section-label">
        <span class="label-num">04</span>
        重点考察方向
        <span class="label-hint">（可多选）</span>
      </div>
      <div class="tag-group">
        <div
          v-for="tag in focusAreas"
          :key="tag.value"
          class="tag-item"
          :class="{ active: selectedFocus.includes(tag.value) }"
          @click="toggleFocus(tag.value)"
        >
          {{ tag.label }}
        </div>
      </div>
    </div>

    <!-- 面试风格 -->
    <div class="section animate-reveal animate-reveal-delay-6">
      <div class="section-label">
        <span class="label-num">05</span>
        面试风格
        <span class="label-hint">（可多选）</span>
      </div>
      <div class="style-grid">
        <div
          v-for="style in interviewStyles"
          :key="style.value"
          class="style-card"
          :class="{ active: selectedStyles.includes(style.value) }"
          @click="toggleStyle(style.value)"
        >
          <div class="style-icon" v-html="style.icon"></div>
          <div class="style-name">{{ style.label }}</div>
          <div class="style-desc">{{ style.desc }}</div>
          <div class="style-check">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="20 6 9 17 4 12"/>
            </svg>
          </div>
        </div>
      </div>
    </div>

    <!-- 知识库强化（可选） -->
    <div class="section animate-reveal animate-reveal-delay-6">
      <div class="section-label">
        <span class="label-num">06</span>
        知识库强化
        <span class="label-hint">（可选）</span>
      </div>
      <p class="section-desc">
        绑定岗位题库后，AI 会检索题库中的真题作为出题参考，让问题更贴近该岗位的考察范围。
      </p>
      <div class="kb-row">
        <select v-model="kb" class="form-select">
          <option value="">不使用知识库（标准面试）</option>
          <option v-for="title in docTitles" :key="title" :value="title">{{ title }}</option>
        </select>
        <span v-if="kb" class="kb-chip">
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="20 6 9 17 4 12"/>
          </svg>
          已绑定
        </span>
      </div>
    </div>

    <!-- 操作区 -->
    <div class="actions animate-reveal animate-reveal-delay-7">
      <button class="btn btn-secondary btn-large" @click="goBack">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="19" y1="12" x2="5" y2="12"/>
          <polyline points="12 19 5 12 12 5"/>
        </svg>
        返回修改简历
      </button>
      <button class="btn btn-primary btn-large" @click="handleStartInterview" :disabled="loading">
        <span v-if="loading" class="loading-spinner"></span>
        <span v-else>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polygon points="5 3 19 12 5 21 5 3"/>
          </svg>
        </span>
        {{ loading ? '创建中...' : '创建面试' }}
      </button>
    </div>

    <!-- 错误提示 -->
    <div v-if="errorMsg" class="error-alert">
      <el-alert :title="errorMsg" type="error" show-icon closable @close="errorMsg = ''" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createSession } from '../api/interview'
import { getDocList } from '../api/rag'
import type { ResumeDTO } from '../api/resume'
import PageHeader from '../components/PageHeader.vue'

// 本组件是「面试准备」流程的第二步（面试配置）；
// 点「返回修改简历」或缺少简历时，通过 back 事件让 PreparePage 回到第一步。
// knowledgeBase 由知识库页（?kb=）经 PreparePage 传入，表示「基于知识库面试」。
const props = withDefaults(defineProps<{ knowledgeBase?: string }>(), { knowledgeBase: '' })
const emit = defineEmits<{ (e: 'back'): void }>()

const router = useRouter()

// 基础配置
const selectedRole = ref('JAVA_BACKEND')
const selectedLevel = ref('FRESH')
const selectedDifficulty = ref('MEDIUM')
const selectedMode = ref('text')
const questionCount = ref(10)
const jobDescription = ref('')
const loading = ref(false)
const errorMsg = ref('')

// 知识库强化：绑定的岗位题库（空 = 标准面试，不检索题库）
const kb = ref(props.knowledgeBase || '')
const docTitles = ref<string[]>([])

// 从知识库页跳转进来时同步绑定
watch(
  () => props.knowledgeBase,
  (value) => {
    kb.value = value || ''
  }
)

const loadDocTitles = async () => {
  try {
    const list = await getDocList()
    docTitles.value = list.map((d) => d.docTitle)
  } catch {
    docTitles.value = []
  }
  // 已绑定的题库若不在返回列表中（如接口异常），仍然展示出来，避免选择框显示为空
  if (kb.value && !docTitles.value.includes(kb.value)) {
    docTitles.value = [kb.value, ...docTitles.value]
  }
}

// 简历完整度：由第一步解析出的简历实时计算（后端不提供评分，这里按结构完整度评估）
const resumeLoaded = ref(false)
const resume = ref<ResumeDTO | null>(null)

const completeness = computed(() => {
  const r = resume.value
  const items: Array<{ label: string; ok: boolean; hint: string }> = []
  if (!r) return { score: 0, items }

  let score = 0

  // 基本信息 20 分
  const bi = (r.basicInfo || {}) as Record<string, unknown>
  const biKeys = ['name', 'phone', 'email', 'gender', 'age', 'city']
  const biFilled = biKeys.filter((k) => String(bi[k] ?? '').trim()).length
  const basicOk = Boolean(
    String(bi.name ?? '').trim() &&
    (String(bi.phone ?? '').trim() || String(bi.email ?? '').trim())
  )
  score += Math.round((20 * biFilled) / biKeys.length)
  items.push({ label: '基本信息', ok: basicOk, hint: `${biFilled}/6` })

  // 教育经历 15 分
  const edu = r.educationList || []
  const eduOk = edu.some((e) => Boolean(e.school && e.major && e.degree))
  score += edu.length === 0 ? 0 : eduOk ? 15 : 9
  items.push({ label: '教育经历', ok: eduOk, hint: edu.length ? `${edu.length} 段` : '缺失' })

  // 实习经历 15 分
  const intern = r.internshipList || []
  const internOk = intern.some((i) => Boolean(i.company && i.description))
  score += intern.length === 0 ? 0 : internOk ? 15 : 8
  items.push({ label: '实习经历', ok: internOk, hint: intern.length ? `${intern.length} 段` : '缺失' })

  // 项目经历 30 分（面试最看重）
  const proj = r.projectList || []
  score += proj.length === 0 ? 0 : proj.length === 1 ? 18 : 30
  items.push({ label: '项目经历', ok: proj.length >= 2, hint: proj.length ? `${proj.length} 个` : '缺失' })

  // 技能标签 15 分
  const skills = (r.skills || []).filter(Boolean)
  score += Math.min(15, skills.length * 2)
  items.push({ label: '技能标签', ok: skills.length >= 5, hint: `${skills.length} 项` })

  // 证书 5 分
  const certs = (r.certificates || []).filter(Boolean)
  score += certs.length ? 5 : 0
  items.push({ label: '证书', ok: certs.length > 0, hint: certs.length ? `${certs.length} 项` : '缺失' })

  return { score: Math.min(100, score), items }
})

// 读取第一步存入的简历解析结果
onMounted(() => {
  try {
    const raw = sessionStorage.getItem('resumeParseResult')
    if (raw) {
      const parsed = JSON.parse(raw) as ResumeDTO | null
      if (parsed && typeof parsed === 'object') {
        resume.value = parsed
        resumeLoaded.value = true
      }
    }
  } catch {
    resumeLoaded.value = false
  }
  loadDocTitles()
})

// 难度选项
const difficulties = [
  { value: 'EASY', label: '简单', desc: '基础概念为主' },
  { value: 'MEDIUM', label: '中等', desc: '基础+项目实践' },
  { value: 'HARD', label: '困难', desc: '深度原理+系统设计' }
]

// 预计时长
const estimatedDuration = computed(() => questionCount.value * 3)

// 重点考察方向
const focusAreas = [
  { value: 'java', label: 'Java' },
  { value: 'spring', label: 'Spring' },
  { value: 'database', label: '数据库' },
  { value: 'redis', label: 'Redis' },
  { value: 'concurrency', label: '并发' },
  { value: 'system', label: '系统设计' },
  { value: 'algorithm', label: '算法' },
  { value: 'project', label: '项目实践' },
  { value: 'behavior', label: '行为面试' }
]
const selectedFocus = ref<string[]>(['java', 'spring', 'database', 'project'])

const toggleFocus = (value: string) => {
  const index = selectedFocus.value.indexOf(value)
  if (index > -1) {
    selectedFocus.value.splice(index, 1)
  } else {
    selectedFocus.value.push(value)
  }
}

// 面试风格
const interviewStyles = [
  {
    value: 'deep',
    label: '项目深挖',
    desc: '围绕项目经历逐层追问',
    icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>'
  },
  {
    value: 'knowledge',
    label: '知识追问',
    desc: '技术知识点深度考察',
    icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>'
  },
  {
    value: 'behavior',
    label: '行为面试',
    desc: 'STAR法则考察软技能',
    icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>'
  },
  {
    value: 'system',
    label: '系统设计',
    desc: '架构设计与权衡能力',
    icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="2" width="20" height="8" rx="2" ry="2"/><rect x="2" y="14" width="20" height="8" rx="2" ry="2"/><line x1="6" y1="6" x2="6.01" y2="6"/><line x1="6" y1="18" x2="6.01" y2="18"/></svg>'
  }
]
const selectedStyles = ref<string[]>(['deep', 'knowledge'])

const toggleStyle = (value: string) => {
  const index = selectedStyles.value.indexOf(value)
  if (index > -1) {
    selectedStyles.value.splice(index, 1)
  } else {
    selectedStyles.value.push(value)
  }
}

// 岗位对应的职位信息
const roleOptions: Record<string, { jobTitle: string; jobDescription: string }> = {
  JAVA_BACKEND: { jobTitle: 'Java后端开发', jobDescription: '熟悉 Spring Boot、MySQL、Redis 等后端技术栈' },
  FRONTEND: { jobTitle: '前端开发', jobDescription: '熟悉 Vue、TypeScript 等前端技术栈' },
  FULL_STACK: { jobTitle: '全栈开发', jobDescription: '熟悉前后端全链路开发' },
  PRODUCT: { jobTitle: '产品经理', jobDescription: '负责产品规划与需求分析' },
}

// 返回上一步（简历解析）
const goBack = () => {
  emit('back')
}

// 创建面试
const handleStartInterview = async () => {
  if (!selectedRole.value) {
    ElMessage.warning('请选择目标岗位')
    return
  }

  const resumeRaw = sessionStorage.getItem('resumeParseResult')
  if (!resumeRaw) {
    ElMessage.error('请先上传简历')
    emit('back')
    return
  }

  loading.value = true
  errorMsg.value = ''

  try {
    const role = roleOptions[selectedRole.value]
    const resume = JSON.parse(resumeRaw) as ResumeDTO

    const session = await createSession({
      jobTitle: role.jobTitle,
      jobDescription: jobDescription.value || role.jobDescription,
      difficulty: selectedDifficulty.value,
      resume,
      // 用户选择的题目总数：不传后端会兜底为 10，导致「选 5 题却显示 /10」
      questionCount: questionCount.value,
      // 绑定题库即「基于知识库面试」，后端会检索该题库辅助出题
      knowledgeBase: kb.value || undefined,
    })

    sessionStorage.setItem('interviewSession', JSON.stringify(session))
    sessionStorage.setItem('interviewConfig', JSON.stringify({
      mode: selectedMode.value,
      level: selectedLevel.value,
      questionCount: questionCount.value,
      focusAreas: selectedFocus.value,
      styles: selectedStyles.value,
      knowledgeBase: kb.value || undefined,
    }))

    router.push('/interview')
  } catch (error: any) {
    errorMsg.value = error?.displayMessage || error?.message || '创建面试失败，请稍后重试'
    console.error(error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.config-page {
  max-width: 760px;
  margin: 0 auto;
  padding-top: 8px;
}

/* 简历评分卡片 */
.resume-score-card {
  padding: 24px 28px;
  margin-bottom: 24px;
  background: linear-gradient(135deg, var(--accent-bg) 0%, var(--bg-card) 100%);
  border: 1px solid var(--accent-border);
}

.score-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

/* 完整度分项 */
.score-breakdown {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px dashed var(--accent-border);
}

.score-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 10px;
  font-size: 12px;
  color: var(--text-muted);
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 100px;
}

.score-chip em {
  font-style: normal;
  font-family: var(--font-mono);
  font-size: 11px;
  opacity: 0.8;
}

.score-chip.filled {
  color: var(--accent);
  border-color: var(--accent-border);
  background: var(--accent-bg);
}

/* 未上传简历时的空状态 */
.resume-empty {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 22px 26px;
  margin-bottom: 24px;
  background: var(--bg-card);
  border: 1px dashed var(--border);
}

.resume-empty-icon {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background: var(--bg-elevated);
  color: var(--text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.resume-empty-text {
  flex: 1;
  min-width: 0;
}

.resume-empty-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 3px;
}

.resume-empty-desc {
  font-size: 13px;
  color: var(--text-muted);
  line-height: 1.5;
}

.score-left {
  flex: 1;
}

.score-label {
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 6px;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

.score-number {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-bottom: 6px;
}

.score-value {
  font-family: var(--font-heading);
  font-size: 42px;
  font-weight: 700;
  color: var(--accent);
  line-height: 1;
}

.score-max {
  font-size: 16px;
  color: var(--text-muted);
  font-weight: 500;
}

.score-desc {
  font-size: 12px;
  color: var(--text-muted);
}

.score-ring {
  position: relative;
  width: 100px;
  height: 100px;
}

.ring-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-family: var(--font-heading);
  font-size: 18px;
  font-weight: 700;
  color: var(--accent);
}

/* 区块通用 */
.section {
  margin-bottom: 24px;
}

.section-label {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 16px;
  font-family: var(--font-heading);
}

.label-num {
  font-family: var(--font-mono);
  font-size: 12px;
  font-weight: 600;
  color: var(--accent);
  background: var(--accent-bg);
  padding: 4px 10px;
  border-radius: 6px;
}

.label-hint {
  font-size: 12px;
  font-weight: 400;
  color: var(--text-muted);
}

.label-optional {
  font-size: 12px;
  font-weight: 400;
  color: var(--text-muted);
}

/* 知识库强化 */
.section-desc {
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-muted);
  margin-bottom: 14px;
}

.kb-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.kb-row .form-select {
  flex: 1;
}

.kb-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  flex-shrink: 0;
  padding: 7px 12px;
  font-size: 12px;
  font-weight: 600;
  color: var(--accent);
  background: var(--accent-bg);
  border: 1px solid var(--accent-border);
  border-radius: 100px;
}

/* 面试模式卡片 */
.mode-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}

.mode-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px;
  background: var(--bg-card);
  border: 2px solid var(--choice-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-normal);
  position: relative;
  box-shadow: var(--shadow-sm);
}

.mode-card:hover {
  border-color: var(--choice-border-hover);
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.mode-card.active {
  border-color: var(--accent);
  background: var(--accent-bg);
  box-shadow: 0 4px 16px rgba(45, 90, 61, 0.14);
}

.mode-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  background: var(--bg-elevated);
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all var(--transition-normal);
}

.mode-card.active .mode-icon {
  background: var(--accent);
  color: white;
}

.mode-info {
  flex: 1;
  min-width: 0;
}

.mode-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 3px;
}

.mode-desc {
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.4;
}

.mode-badge {
  font-size: 10px;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 4px;
  position: absolute;
  top: 12px;
  right: 40px;
}

.mode-badge.recommended {
  background: var(--accent-bg);
  color: var(--accent);
}

.mode-check {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--border);
  color: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all var(--transition-normal);
}

.mode-card.active .mode-check {
  background: var(--accent);
  color: white;
}

/* 表单 */
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}

.form-grid.two-col {
  margin-bottom: 0;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-item.full-width {
  grid-column: 1 / -1;
}

.form-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
}

.form-select {
  width: 100%;
  padding: 12px 14px;
  font-size: 14px;
  color: var(--text-primary);
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--transition-fast);
  font-family: inherit;
}

.form-select:focus {
  outline: none;
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-bg);
}

.form-textarea {
  width: 100%;
  padding: 12px 14px;
  font-size: 14px;
  color: var(--text-primary);
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  resize: vertical;
  transition: all var(--transition-fast);
  font-family: inherit;
  line-height: 1.6;
}

.form-textarea:focus {
  outline: none;
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-bg);
}

.form-textarea::placeholder {
  color: var(--text-muted);
}

.char-count {
  font-size: 11px;
  color: var(--text-muted);
  text-align: right;
  font-family: var(--font-mono);
}

/* 难度选项 */
.difficulty-options {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.difficulty-option {
  position: relative;
  padding: 14px;
  text-align: center;
  background: var(--bg-card);
  border: 2px solid var(--choice-border);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--transition-normal);
  box-shadow: var(--shadow-sm);
}

.difficulty-option:hover {
  border-color: var(--choice-border-hover);
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.difficulty-option.active {
  border-color: var(--accent);
  background: var(--accent-bg);
  box-shadow: 0 4px 14px rgba(45, 90, 61, 0.14);
}

.difficulty-option.active::after {
  content: '';
  position: absolute;
  top: 8px;
  right: 8px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: var(--accent);
}

.difficulty-option.active::before {
  content: '';
  position: absolute;
  top: 12px;
  right: 12.5px;
  width: 5px;
  height: 8px;
  border: solid #ffffff;
  border-width: 0 1.8px 1.8px 0;
  transform: rotate(45deg);
  z-index: 1;
}

.difficulty-option.active .diff-level {
  color: var(--accent);
  font-weight: 700;
  text-decoration: underline;
  text-underline-offset: 4px;
  text-decoration-thickness: 2px;
}

.diff-level {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.diff-desc {
  font-size: 11px;
  color: var(--text-muted);
}

/* 数字输入 */
.number-input {
  display: flex;
  align-items: center;
  gap: 16px;
}

.num-btn {
  width: 36px;
  height: 36px;
  border: 1px solid var(--border);
  background: var(--bg-card);
  color: var(--text-primary);
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: 18px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
}

.num-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
  background: var(--accent-bg);
}

.num-value {
  font-family: var(--font-heading);
  font-size: 24px;
  font-weight: 700;
  color: var(--accent);
  min-width: 40px;
  text-align: center;
}

/* 预计时长 */
.duration-display {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: var(--accent-bg);
  border-radius: var(--radius-sm);
  color: var(--accent);
  font-size: 14px;
  font-weight: 600;
}

/* 标签组 */
.tag-group {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.tag-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 16px;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  background: var(--bg-card);
  border: 1.5px solid var(--choice-border);
  border-radius: 100px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.tag-item:hover {
  border-color: var(--choice-border-hover);
  color: var(--accent);
}

.tag-item.active {
  background: var(--accent-bg);
  color: var(--accent);
  border-color: var(--accent);
  font-weight: 600;
}

.tag-item.active::before {
  content: '';
  width: 5px;
  height: 9px;
  border: solid var(--accent);
  border-width: 0 1.8px 1.8px 0;
  transform: rotate(45deg) translateY(-1px);
}

/* 面试风格卡片 */
.style-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.style-card {
  padding: 18px 14px;
  text-align: center;
  background: var(--bg-card);
  border: 2px solid var(--choice-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-normal);
  position: relative;
  box-shadow: var(--shadow-sm);
}

.style-card:hover {
  border-color: var(--choice-border-hover);
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.style-card.active {
  border-color: var(--accent);
  background: rgba(45, 90, 61, 0.06);
  box-shadow: 0 4px 12px rgba(45, 90, 61, 0.15);
}

.style-card.active .style-name {
  color: var(--accent);
}

.style-icon {
  width: 40px;
  height: 40px;
  margin: 0 auto 10px;
  border-radius: var(--radius-sm);
  background: var(--bg-elevated);
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-normal);
}

.style-card.active .style-icon {
  background: var(--accent);
  color: white;
}

.style-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.style-desc {
  font-size: 11px;
  color: var(--text-muted);
  line-height: 1.4;
}

.style-check {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--border);
  color: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-normal);
}

.style-card.active .style-check {
  background: var(--accent);
  color: white;
}

/* 操作区 */
.actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 32px;
  margin-bottom: 24px;
}

/* 错误提示 */
.error-alert {
  margin-top: 20px;
}

/* 响应式 */
@media (max-width: 640px) {
  .mode-grid {
    grid-template-columns: 1fr;
  }
  .form-grid {
    grid-template-columns: 1fr;
  }
  .difficulty-options {
    grid-template-columns: 1fr;
  }
  .style-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .actions {
    flex-direction: column;
  }
  .resume-score-card .score-top {
    flex-direction: column;
    gap: 16px;
    text-align: center;
  }
}
</style>
