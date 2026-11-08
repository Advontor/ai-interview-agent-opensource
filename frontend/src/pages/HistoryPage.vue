<template>
  <div class="history-page">
    <!-- 页面标题区 -->
    <PageHeader
      tag="历史面试"
      title="你的"
      highlight="面试记录"
      description="查看所有已保存的面试会话，可回顾完整问答、继续未结束的面试或打开评估报告。"
      accent="#0d9488"
    />

    <!-- 筛选栏 -->
    <div class="filter-bar animate-reveal animate-reveal-delay-1">
      <div class="filter-tabs">
        <button
          v-for="tab in statusTabs"
          :key="tab.value"
          class="filter-tab"
          :class="{ active: activeStatus === tab.value }"
          @click="activeStatus = tab.value"
        >
          {{ tab.label }}
          <span class="tab-count">{{ getStatusCount(tab.value) }}</span>
        </button>
      </div>
      <div class="filter-search">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="11" cy="11" r="8"/>
          <line x1="21" y1="21" x2="16.65" y2="16.65"/>
        </svg>
        <input v-model="searchKeyword" type="text" placeholder="搜索岗位..." class="search-input" />
      </div>
    </div>

    <!-- 面试列表 -->
    <div v-if="filteredSessions.length > 0" class="session-list">
      <div
        v-for="(session, index) in filteredSessions"
        :key="session.id"
        class="session-card card animate-reveal"
        :style="{ animationDelay: (index % 3) * 0.1 + 0.2 + 's' }"
      >
        <div class="session-header">
          <div class="session-title">
            <!-- 候选人基础信息：不点进去也能快速辨认这一条记录 -->
            <div class="session-candidate">
              <span class="candidate-name" :class="{ empty: !session.candidateName }">
                {{ session.candidateName || '未填写姓名' }}
              </span>
              <span v-if="session.candidateAge" class="candidate-chip">{{ session.candidateAge }}岁</span>
              <span v-if="session.candidateCity" class="candidate-chip">{{ session.candidateCity }}</span>
              <span v-if="session.candidateEducation" class="candidate-chip">{{ session.candidateEducation }}</span>
            </div>
            <div class="session-role">
              <span>{{ session.role }}</span>
              <span
                v-if="session.knowledgeBase"
                class="role-kb"
                :title="`出题参考题库：${session.knowledgeBase}`"
              >
                <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/>
                  <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>
                </svg>
                {{ session.knowledgeBase }}
              </span>
            </div>
            <div class="session-meta">
              <span class="meta-item">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="12" r="10"/>
                  <polyline points="12 6 12 12 16 14"/>
                </svg>
                {{ session.date }}
              </span>
              <span class="meta-dot">·</span>
              <span class="meta-item">{{ session.questionCount }} 道题</span>
              <span class="meta-dot">·</span>
              <span class="meta-item">{{ session.duration }}</span>
            </div>
          </div>
          <div class="session-status" :class="'status-' + session.status">
            {{ getStatusLabel(session.status) }}
          </div>
        </div>

        <div class="session-body">
          <div class="session-info">
            <div class="info-item">
              <span class="info-label">难度</span>
              <span class="info-value">{{ session.difficulty }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">模式</span>
              <span class="info-value">{{ session.mode }}</span>
            </div>
            <div v-if="session.score !== null" class="info-item score-item">
              <span class="info-label">得分</span>
              <span class="info-value score-value">{{ session.score }}/100</span>
            </div>
          </div>
        </div>

        <div class="session-actions">
          <button class="btn btn-ghost btn-small" @click="viewQA(session)">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
              <circle cx="12" cy="12" r="3"/>
            </svg>
            查看问答
          </button>
          <button v-if="session.status === 'ongoing'" class="btn btn-secondary btn-small" @click="continueInterview(session)">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polygon points="5 3 19 12 5 21 5 3"/>
            </svg>
            继续面试
          </button>
          <button v-if="session.status === 'completed'" class="btn btn-primary btn-small" @click="viewReport(session)">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
              <polyline points="14 2 14 8 20 8"/>
              <line x1="16" y1="13" x2="8" y2="13"/>
              <line x1="16" y1="17" x2="8" y2="17"/>
            </svg>
            查看报告
          </button>
          <button
            class="btn btn-ghost btn-small session-delete"
            :disabled="deletingId === session.id"
            @click="removeSession(session)"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="3 6 5 6 21 6"/>
              <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
            </svg>
            {{ deletingId === session.id ? '删除中...' : '删除' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-state animate-reveal animate-reveal-delay-2">
      <div class="empty-icon">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
          <polyline points="14 2 14 8 20 8"/>
          <line x1="16" y1="13" x2="8" y2="13"/>
          <line x1="16" y1="17" x2="8" y2="17"/>
        </svg>
      </div>
      <div class="empty-title">暂无历史面试</div>
      <div class="empty-desc">开始一场面试后会自动保存到这里</div>
      <button class="btn btn-primary" @click="startNewInterview">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="12" y1="5" x2="12" y2="19"/>
          <line x1="5" y1="12" x2="19" y2="12"/>
        </svg>
        开始新面试
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getSessionList, deleteSession, type InterviewSessionItem } from '../api/interview'
import PageHeader from '../components/PageHeader.vue'

const router = useRouter()
const loading = ref(true)
// 正在删除的会话 id（用于禁用按钮、避免重复点击）
const deletingId = ref<number | null>(null)

// 状态筛选
const activeStatus = ref('all')
const searchKeyword = ref('')

const statusTabs = [
  { value: 'all', label: '全部' },
  { value: 'completed', label: '已完成' },
  { value: 'ongoing', label: '进行中' }
]

// 面试会话数据（从后端获取；候选人信息与得分由后端从简历/报告中解析）
const sessions = reactive<Array<{
  id: number
  role: string
  date: string
  questionCount: number
  duration: string
  difficulty: string
  mode: string
  status: string
  score: number | null
  candidateName: string
  candidateAge: string
  candidateCity: string
  candidateEducation: string
  knowledgeBase: string
}>>([])

// 难度映射
const difficultyMap: Record<string, string> = {
  EASY: '简单',
  MEDIUM: '中等',
  HARD: '困难'
}

// 加载真实会话列表
const loadSessions = async () => {
  try {
    loading.value = true
    // 默认用户ID=1（项目暂未做登录系统）
    const list = await getSessionList(1)

    sessions.splice(0, sessions.length)
    list.forEach((item: InterviewSessionItem) => {
      // 计算时长
      let duration = '未知'
      if (item.endTime && item.createTime) {
        const start = new Date(item.createTime).getTime()
        const end = new Date(item.endTime).getTime()
        const mins = Math.round((end - start) / 60000)
        duration = mins > 0 ? `${mins}分钟` : '不足1分钟'
      }
      // 状态映射：后端status可能是 ONGOING/FINISHED
      const status = item.status === 'FINISHED' ? 'completed' : 'ongoing'

      sessions.push({
        id: item.id,
        role: item.jobTitle,
        date: item.createTime ? new Date(item.createTime).toLocaleString('zh-CN') : '',
        questionCount: item.questionCount || 0,
        duration,
        difficulty: difficultyMap[item.difficulty] || item.difficulty,
        mode: '文字',
        status,
        score: item.score ?? null,
        candidateName: item.candidateName || '',
        candidateAge: item.candidateAge || '',
        candidateCity: item.candidateCity || '',
        candidateEducation: item.candidateEducation || '',
        knowledgeBase: item.knowledgeBase || ''
      })
    })
  } catch (err) {
    console.error('加载面试记录失败:', err)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadSessions()
})

// 筛选后的会话
const filteredSessions = computed(() => {
  let result = sessions
  if (activeStatus.value !== 'all') {
    result = result.filter(s => s.status === activeStatus.value)
  }
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(s => s.role.toLowerCase().includes(keyword))
  }
  return result
})

// 状态计数
const getStatusCount = (status: string) => {
  if (status === 'all') return sessions.length
  return sessions.filter(s => s.status === status).length
}

// 状态标签
const getStatusLabel = (status: string) => {
  if (status === 'completed') return '已完成'
  if (status === 'ongoing') return '进行中'
  return status
}

// 操作：用 URL ?sessionId= 传值，避免之前依赖 sessionStorage 导致"点张三跳到付晨"的串号 bug
const viewQA = (session: any) => {
  router.push({ path: '/interview', query: { sessionId: String(session.id) } })
}

const continueInterview = (session: any) => {
  router.push({ path: '/interview', query: { sessionId: String(session.id) } })
}

const viewReport = (session: any) => {
  router.push({ path: '/report', query: { sessionId: String(session.id) } })
}

const startNewInterview = () => {
  router.push('/')
}

// 删除单条面试记录（会一并删除该会话的问答与报告，不可恢复，所以先二次确认）
const removeSession = async (session: { id: number; role: string; status: string }) => {
  try {
    await ElMessageBox.confirm(
      `确定删除「${session.role}」这条面试记录吗？该场面试的问答与评估报告会一并删除，且无法恢复。`,
      '删除面试记录',
      {
        type: 'warning',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--danger',
      }
    )
  } catch {
    // 用户取消
    return
  }

  try {
    deletingId.value = session.id
    await deleteSession(session.id)
    const index = sessions.findIndex((s) => s.id === session.id)
    if (index > -1) sessions.splice(index, 1)
    ElMessage.success('面试记录已删除')
  } catch (err: any) {
    ElMessage.error(err?.displayMessage || err?.message || '删除失败，请稍后重试')
  } finally {
    deletingId.value = null
  }
}
</script>

<style scoped>
.history-page {
  max-width: 860px;
  margin: 0 auto;
  padding-top: 8px;
}

/* 筛选栏 */
.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  gap: 16px;
  flex-wrap: wrap;
}

.filter-tabs {
  display: flex;
  gap: 8px;
}

.filter-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 100px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.filter-tab:hover {
  border-color: var(--accent-border);
  color: var(--accent);
}

.filter-tab.active {
  background: var(--accent);
  color: white;
  border-color: var(--accent);
}

.tab-count {
  font-family: var(--font-mono);
  font-size: 11px;
  padding: 2px 6px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 4px;
}

.filter-tab:not(.active) .tab-count {
  background: var(--bg-elevated);
  color: var(--text-muted);
}

.filter-search {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  color: var(--text-muted);
  transition: all var(--transition-fast);
}

.filter-search:focus-within {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-bg);
}

.search-input {
  border: none;
  outline: none;
  background: transparent;
  font-size: 13px;
  color: var(--text-primary);
  width: 140px;
  font-family: inherit;
}

.search-input::placeholder {
  color: var(--text-muted);
}

/* 会话列表 */
.session-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.session-card {
  padding: 24px;
  transition: all var(--transition-normal);
}

.session-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-hover);
  border-color: var(--accent-border);
}

.session-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.session-title {
  flex: 1;
}

/* 候选人基础信息（姓名/年龄/城市/学历），列表上快速辨认 */
.session-candidate {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 6px;
}

.candidate-name {
  font-size: 17px;
  font-weight: 700;
  color: var(--text-primary);
  font-family: var(--font-heading);
}

.candidate-name.empty {
  font-size: 15px;
  font-weight: 500;
  color: var(--text-muted);
  font-family: var(--font-body);
}

.candidate-chip {
  padding: 2px 9px;
  font-size: 12px;
  color: var(--text-secondary);
  background: var(--bg-elevated);
  border: 1px solid var(--border-light);
  border-radius: 100px;
}

.session-role {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

/* 绑定了知识库题库时的小标记 */
.role-kb {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 9px;
  font-size: 12px;
  font-weight: 500;
  color: var(--accent);
  background: var(--accent-bg);
  border: 1px solid var(--accent-border);
  border-radius: 100px;
}

.session-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--text-muted);
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.meta-dot {
  color: var(--border);
}

.session-status {
  font-size: 12px;
  font-weight: 600;
  padding: 5px 12px;
  border-radius: 100px;
  flex-shrink: 0;
}

.status-completed {
  background: var(--accent-bg);
  color: var(--accent);
}

.status-ongoing {
  background: rgba(245, 158, 11, 0.1);
  color: #d97706;
}

.session-body {
  padding: 14px 0;
  border-top: 1px solid var(--border-light);
  border-bottom: 1px solid var(--border-light);
  margin-bottom: 16px;
}

.session-info {
  display: flex;
  gap: 32px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 11px;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.info-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.score-value {
  color: var(--accent);
  font-family: var(--font-heading);
  font-size: 18px;
}

.session-actions {
  display: flex;
  gap: 10px;
}

/* 删除按钮：靠右放置，默认低调，悬停才显红 */
.session-delete {
  margin-left: auto;
  color: var(--text-muted);
}

.session-delete:hover:not(:disabled) {
  color: #b91c1c;
  border-color: rgba(185, 28, 28, 0.35);
  background: rgba(185, 28, 28, 0.06);
}

.session-delete:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 60px 20px;
}

.empty-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 20px;
  border-radius: 50%;
  background: var(--accent-bg);
  color: var(--accent);
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
  font-family: var(--font-heading);
}

.empty-desc {
  font-size: 14px;
  color: var(--text-muted);
  margin-bottom: 24px;
}

/* 响应式 */
@media (max-width: 640px) {
  .filter-bar {
    flex-direction: column;
    align-items: stretch;
  }
  .session-info {
    flex-wrap: wrap;
    gap: 16px;
  }
  .session-actions {
    flex-wrap: wrap;
  }
}
</style>
