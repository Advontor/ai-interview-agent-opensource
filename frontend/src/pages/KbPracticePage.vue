<template>
  <div class="practice-page">
    <PageHeader
      tag="知识库 / 刷题"
      title="岗位"
      highlight="真题刷题"
      description="按题库浏览该岗位的面试真题与参考答案，提前知道可能会被问到什么。无需上传简历，随时可看。"
      accent="#0d9488"
    />

    <!-- 题库切换 / 搜索 / 视图控制 -->
    <div class="toolbar card">
      <div class="toolbar-row">
        <div class="toolbar-item toolbar-doc">
          <span class="toolbar-label">题库</span>
          <el-select
            v-model="activeDoc"
            placeholder="选择题库"
            size="large"
            filterable
            @change="onDocChange"
          >
            <el-option
              v-for="doc in docs"
              :key="doc.docTitle"
              :label="doc.docTitle"
              :value="doc.docTitle"
            >
              <span>{{ doc.docTitle }}</span>
              <span class="option-position">{{ doc.position }}</span>
            </el-option>
          </el-select>
        </div>

        <button class="btn btn-ghost btn-small" @click="goKnowledge">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M19 12H5"/><polyline points="12 19 5 12 12 5"/>
          </svg>
          返回知识库
        </button>
      </div>

      <div class="toolbar-row">
        <el-input
          v-model="keyword"
          placeholder="在本题库内搜索，如 Java、索引、并发、优先级"
          clearable
          size="large"
          class="toolbar-search"
        />
        <button
          class="btn btn-secondary btn-small answer-toggle"
          @click="toggleAllAnswers"
          :disabled="chunkLoading || filteredChunks.length === 0"
        >
          {{ allRevealed ? '隐藏全部答案' : '显示全部答案' }}
        </button>
      </div>

      <div class="toolbar-meta">
        <span v-if="activeDoc" class="meta-badge">{{ activePosition }}</span>
        <span class="meta-text">
          共 <strong>{{ filteredChunks.length }}</strong> 道题{{ keyword.trim() ? '（已按关键词筛选）' : '' }}
        </span>
        <button
          class="btn btn-primary btn-small start-interview"
          :disabled="!activeDoc || chunkLoading || practiceQuestions.length === 0"
          @click="startPractice"
        >
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polygon points="5 3 19 12 5 21 5 3"/>
          </svg>
          开始刷题练习
        </button>
      </div>
    </div>

    <!-- ============ 刷题练习模式：出题 → 作答 → 评分，无需简历 ============ -->
    <template v-if="practiceMode">
      <div class="practice-panel card">
        <div class="pp-head">
          <div class="pp-title">
            <span class="pp-badge">刷题练习</span>
            <span class="pp-doc">{{ activeDoc }}</span>
          </div>
          <button class="btn btn-ghost btn-small" @click="exitPractice">退出练习</button>
        </div>

        <div class="pp-progress">
          <span>已练 <strong>{{ practiceDone }}</strong> 题</span>
          <span v-if="practiceChunk" class="pp-cur">
            本题难度：{{ difficultyLabel(practiceChunk.parsed.difficulty) || '未标注' }}
          </span>
        </div>

        <div v-if="!practiceChunk" class="pp-empty">本题库暂无可练习的题目。</div>

        <template v-else>
          <div class="pp-question">
            <div class="pp-q-label">题目</div>
            <div class="pp-q-text">{{ practiceChunk.parsed.question }}</div>
            <div v-if="skillList(practiceChunk.parsed.skills).length" class="q-tags pp-q-tags">
              <span v-for="tag in skillList(practiceChunk.parsed.skills)" :key="tag" class="q-skill">{{ tag }}</span>
            </div>
          </div>

          <div class="pp-answer-area">
            <div class="pp-a-label">
              你的作答
              <span class="pp-a-hint">按自己的理解写，提交后 AI 会评分并给出点评</span>
            </div>
            <el-input
              v-model="practiceAnswer"
              type="textarea"
              :rows="6"
              resize="vertical"
              :disabled="grading || !!gradeResult"
              placeholder="在这里写下你的回答……"
            />
          </div>

          <!-- 评分结果 -->
          <div v-if="gradeResult" class="pp-result">
            <div class="pp-score-row">
              <div class="pp-score" :class="getScoreClass(gradeResult.score)">
                <span class="pp-score-num">{{ gradeResult.score }}</span>
                <span class="pp-score-max">/100</span>
              </div>
              <div class="pp-score-desc">{{ scoreComment(gradeResult.score) }}</div>
            </div>

            <div class="pp-comment">
              <div class="pp-comment-label">AI 点评</div>
              <div class="pp-comment-text">{{ gradeResult.comment }}</div>
            </div>

            <div v-if="gradeResult.referenceAnswer" class="pp-reference">
              <div class="pp-ref-label">参考答案</div>
              <div class="pp-ref-text">{{ gradeResult.referenceAnswer }}</div>
            </div>
          </div>

          <div v-if="practiceError" class="pp-error">{{ practiceError }}</div>

          <div class="pp-actions">
            <template v-if="!gradeResult">
              <button
                class="btn btn-primary"
                :disabled="grading || !practiceAnswer.trim()"
                @click="submitPractice"
              >
                {{ grading ? '正在评分...' : '提交评分' }}
              </button>
              <button class="btn btn-ghost btn-small" :disabled="grading" @click="nextPractice">
                换一道题
              </button>
            </template>
            <template v-else>
              <button class="btn btn-primary" @click="nextPractice">下一题</button>
              <button class="btn btn-ghost btn-small" @click="exitPractice">退出练习</button>
            </template>
          </div>
        </template>
      </div>
    </template>

    <!-- ============ 浏览模式：题目列表 ============ -->
    <template v-else>

    <!-- 状态 -->
    <div v-if="chunkLoading" class="state card">
      <div class="loading-spinner"></div>
      <p>正在加载题库内容...</p>
    </div>
    <div v-else-if="chunkError" class="state card state-error">
      <p>{{ chunkError }}</p>
      <button class="btn btn-primary btn-small" @click="loadChunks(activeDoc)">重新加载</button>
    </div>
    <div v-else-if="!activeDoc" class="state card">
      <p>请先在上方选择一个题库。</p>
    </div>
    <div v-else-if="filteredChunks.length === 0" class="state card">
      <p>没有匹配的题目{{ keyword.trim() ? '，换个关键词试试' : '' }}。</p>
    </div>

    <!-- 题目列表 -->
    <template v-else>
      <div
        v-for="(item, idx) in pagedChunks"
        :key="item.id"
        class="question-card card"
      >
        <div class="q-head">
          <span class="q-index">第 {{ idx + 1 }} 题</span>
          <span v-if="item.parsed.difficulty" class="q-tag q-diff">
            {{ difficultyLabel(item.parsed.difficulty) }}
          </span>
          <span v-if="item.parsed.type" class="q-tag q-type">
            {{ typeLabel(item.parsed.type) }}
          </span>
        </div>

        <div class="q-text">{{ item.parsed.question }}</div>

        <div v-if="item.parsed.answer" class="q-answer-area">
          <button
            v-if="!revealed.has(item.id)"
            class="btn btn-secondary btn-small"
            @click="reveal(item.id)"
          >
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
              <circle cx="12" cy="12" r="3"/>
            </svg>
            查看参考答案
          </button>
          <div v-else class="q-answer">
            <div class="q-answer-label">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M9 11l3 3L22 4"/>
                <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/>
              </svg>
              参考答案
              <button class="link-btn" @click="hide(item.id)">收起</button>
            </div>
            <div class="q-answer-text">{{ item.parsed.answer }}</div>
          </div>
        </div>
        <div v-else class="q-answer-empty">该题暂无参考答案</div>

        <div v-if="skillList(item.parsed.skills).length" class="q-tags">
          <span v-for="tag in skillList(item.parsed.skills)" :key="tag" class="q-skill">{{ tag }}</span>
        </div>
      </div>

      <div v-if="filteredChunks.length > visibleCount" class="load-more">
        <button class="btn btn-primary" @click="visibleCount += PAGE_SIZE">
          加载更多（还有 {{ filteredChunks.length - visibleCount }} 道）
        </button>
      </div>
      <div v-else class="load-more">
        <span class="all-loaded">已显示全部 {{ filteredChunks.length }} 道题</span>
      </div>
    </template>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getChunksByDoc, getDocList, type DocInfo, type KnowledgeChunk } from '../api/rag'
import { gradePractice, type PracticeGradeResult } from '../api/practice'
import PageHeader from '../components/PageHeader.vue'

const PAGE_SIZE = 20

interface ParsedChunk {
  tags: string
  skills: string
  scenes: string
  type: string
  difficulty: string
  question: string
  answer: string
}

const route = useRoute()
const router = useRouter()

const docs = ref<DocInfo[]>([])
const activeDoc = ref<string>('')
const keyword = ref('')
const visibleCount = ref(PAGE_SIZE)
const chunkLoading = ref(false)
const chunkError = ref('')
const chunks = ref<Array<KnowledgeChunk & { parsed: ParsedChunk }>>([])
// 已展开答案的题目 id 集合（默认全部收起，方便自我检测）
const revealed = ref<Set<number>>(new Set())

const activePosition = computed(() => {
  const hit = docs.value.find((d) => d.docTitle === activeDoc.value)
  return hit?.position || activeDoc.value.replace(/面试题库$/, '')
})

// content 是结构化检索块，按标签拆成可读字段（参考回答可能多行）
const parseChunk = (content: string): ParsedChunk => {
  const parsed: ParsedChunk = { tags: '', skills: '', scenes: '', type: '', difficulty: '', question: '', answer: '' }
  const labelMap: Record<string, keyof ParsedChunk> = {
    '岗位标签': 'tags',
    '技能标签': 'skills',
    '场景标签': 'scenes',
    '题型': 'type',
    '难度': 'difficulty',
    '问题': 'question',
    '参考回答': 'answer',
  }
  let current: keyof ParsedChunk | null = null
  for (const line of content.split('\n')) {
    const sep = line.indexOf(': ')
    const label = sep > 0 ? line.slice(0, sep) : ''
    if (label && labelMap[label]) {
      current = labelMap[label]
      parsed[current] = line.slice(sep + 2)
    } else if (current) {
      parsed[current] += '\n' + line
    }
  }
  if (!parsed.question) parsed.question = content
  return parsed
}

const skillList = (skills: string) =>
  skills.split(',').map((s) => s.trim()).filter(Boolean)

const difficultyLabel = (value: string) =>
  ({ basic: '基础', intermediate: '进阶', advanced: '高级' } as Record<string, string>)[value] || value

const typeLabel = (value: string) =>
  ({
    how: '原理题',
    general: '综合题',
    why: '原因题',
    behavior_case: '行为案例',
    situational: '情景题',
    comparison: '对比题',
    opinion: '观点题',
  } as Record<string, string>)[value] || value

const filteredChunks = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return chunks.value
  return chunks.value.filter(
    (c) => c.content.toLowerCase().includes(kw) || c.parsed.question.toLowerCase().includes(kw)
  )
})

const pagedChunks = computed(() => filteredChunks.value.slice(0, visibleCount.value))

const allRevealed = computed(
  () => filteredChunks.value.length > 0 && filteredChunks.value.every((c) => revealed.value.has(c.id))
)

const reveal = (id: number) => {
  // Set 是引用类型，需要换新实例才能触发视图更新
  const next = new Set(revealed.value)
  next.add(id)
  revealed.value = next
}

const hide = (id: number) => {
  const next = new Set(revealed.value)
  next.delete(id)
  revealed.value = next
}

const toggleAllAnswers = () => {
  if (allRevealed.value) {
    revealed.value = new Set()
    return
  }
  revealed.value = new Set(filteredChunks.value.map((c) => c.id))
}

watch(keyword, () => { visibleCount.value = PAGE_SIZE })

const loadChunks = async (docTitle: string) => {
  if (!docTitle) return
  chunkLoading.value = true
  chunkError.value = ''
  chunks.value = []
  revealed.value = new Set()
  visibleCount.value = PAGE_SIZE
  // 题库换了，练习中的题目已失效，直接退出练习模式
  exitPractice()
  try {
    const list = await getChunksByDoc(docTitle)
    chunks.value = (list || []).map((c) => ({ ...c, parsed: parseChunk(c.content) }))
  } catch (err: any) {
    console.error('加载题库内容失败:', err)
    chunkError.value = err?.displayMessage || err?.message || '加载题库内容失败'
  } finally {
    chunkLoading.value = false
  }
}

const onDocChange = (docTitle: string) => {
  keyword.value = ''
  // 同步到 URL，便于刷新/分享
  router.replace({ path: '/practice', query: { doc: docTitle } })
  loadChunks(docTitle)
}

const goKnowledge = () => router.push('/knowledge')

// ==================== 刷题练习模式 ====================
// 出题 → 作答 → AI 评分，纯前端流程，不需要简历、不创建面试会话
const practiceMode = ref(false)
const practiceChunk = ref<(KnowledgeChunk & { parsed: ParsedChunk }) | null>(null)
const practiceAnswer = ref('')
const grading = ref(false)
const gradeResult = ref<PracticeGradeResult | null>(null)
const practiceError = ref('')
const practiceDone = ref(0)
// 已出过的题，尽量不重复
const askedIds = ref<Set<number>>(new Set())

// 可练习的题：优先带参考答案的
const practiceQuestions = computed(() => {
  const withAnswer = chunks.value.filter((c) => c.parsed.answer)
  return withAnswer.length > 0 ? withAnswer : chunks.value
})

const startPractice = () => {
  if (practiceQuestions.value.length === 0) return
  practiceMode.value = true
  practiceDone.value = 0
  askedIds.value = new Set()
  nextPractice()
}

const exitPractice = () => {
  practiceMode.value = false
  practiceChunk.value = null
  practiceAnswer.value = ''
  gradeResult.value = null
  practiceError.value = ''
}

const nextPractice = () => {
  const pool = practiceQuestions.value
  if (pool.length === 0) {
    practiceChunk.value = null
    return
  }
  // 优先挑没出过的；全出过则重置
  let candidates = pool.filter((c) => !askedIds.value.has(c.id))
  if (candidates.length === 0) {
    askedIds.value = new Set()
    candidates = pool
  }
  const picked = candidates[Math.floor(Math.random() * candidates.length)]
  practiceChunk.value = picked
  const nextAsked = new Set(askedIds.value)
  nextAsked.add(picked.id)
  askedIds.value = nextAsked
  practiceAnswer.value = ''
  gradeResult.value = null
  practiceError.value = ''
}

const submitPractice = async () => {
  const chunk = practiceChunk.value
  if (!chunk || grading.value) return
  practiceError.value = ''
  grading.value = true
  try {
    const res = await gradePractice(chunk.id, practiceAnswer.value)
    gradeResult.value = res
    practiceDone.value += 1
  } catch (err: any) {
    console.error('刷题评分失败:', err)
    practiceError.value = err?.displayMessage || err?.message || '评分失败，请稍后重试'
  } finally {
    grading.value = false
  }
}

const getScoreClass = (score: number) => {
  if (score >= 85) return 'score-high'
  if (score >= 60) return 'score-medium'
  return 'score-low'
}

const scoreComment = (score: number) => {
  if (score >= 90) return '回答得很完整，继续保持'
  if (score >= 75) return '基本答到要点，细节可再补'
  if (score >= 60) return '方向对，但内容偏浅'
  if (score >= 40) return '沾边但不完整，建议系统复习'
  return '离参考答案差距较大，重点补课'
}

onMounted(async () => {
  try {
    docs.value = await getDocList()
  } catch (err) {
    console.error('加载题库列表失败:', err)
  }
  const fromQuery = (route.query.doc as string) || ''
  const target = fromQuery || docs.value[0]?.docTitle || ''
  activeDoc.value = target
  if (target) await loadChunks(target)
})
</script>

<style scoped>
.practice-page {
  max-width: 860px;
  margin: 0 auto;
  padding-top: 8px;
}

/* 工具条 */
.toolbar {
  padding: 20px 22px;
  margin-bottom: 20px;
}

.toolbar-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toolbar-row + .toolbar-row {
  margin-top: 12px;
}

.toolbar-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.toolbar-doc {
  flex: 1;
  min-width: 0;
}

.toolbar-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  flex-shrink: 0;
}

.toolbar-search {
  flex: 1;
}

.answer-toggle {
  flex-shrink: 0;
}

.toolbar-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid var(--border);
}

.meta-badge {
  padding: 3px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: #0d9488;
  background: rgba(13, 148, 136, 0.1);
  flex-shrink: 0;
}

.meta-text {
  font-size: 13px;
  color: var(--text-secondary);
}

.meta-text strong {
  color: var(--accent);
  font-weight: 700;
}

.start-interview {
  margin-left: auto;
}

.option-position {
  float: right;
  color: var(--text-muted, #9aa0a6);
  font-size: 12px;
}

/* 状态 */
.state {
  padding: 56px 24px;
  text-align: center;
  color: var(--text-secondary);
  font-size: 14px;
}

.state-error {
  color: #b91c1c;
}

.state .btn {
  margin-top: 14px;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  margin: 0 auto 16px;
  border: 3px solid var(--border);
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: spin 0.9s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 题目卡片 */
.question-card {
  padding: 22px 24px;
  margin-bottom: 16px;
}

.q-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.q-index {
  font-size: 13px;
  font-weight: 700;
  color: var(--accent);
}

.q-tag {
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.q-diff {
  background: rgba(180, 83, 9, 0.1);
  color: #b45309;
}

.q-type {
  background: rgba(45, 90, 61, 0.08);
  color: var(--text-secondary);
}

.q-text {
  font-size: 15px;
  line-height: 1.75;
  color: var(--text-primary);
  font-weight: 500;
  margin-bottom: 14px;
}

.q-answer-area {
  margin-bottom: 12px;
}

.q-answer {
  background: var(--accent-bg);
  border-left: 2px solid var(--accent);
  border-radius: var(--radius-sm);
  padding: 14px 16px;
}

.q-answer-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 700;
  color: var(--accent);
  margin-bottom: 8px;
}

.link-btn {
  margin-left: auto;
  border: none;
  background: none;
  padding: 0;
  font-size: 12px;
  color: var(--text-secondary);
  cursor: pointer;
  text-decoration: underline;
}

.link-btn:hover {
  color: var(--accent);
}

.q-answer-text {
  font-size: 13.5px;
  line-height: 1.8;
  color: var(--text-secondary);
  white-space: pre-wrap;
}

.q-answer-empty {
  font-size: 13px;
  color: var(--text-muted, #9aa0a6);
  margin-bottom: 12px;
}

.q-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.q-skill {
  padding: 3px 10px;
  border-radius: 6px;
  font-size: 12px;
  color: var(--text-secondary);
  background: rgba(45, 90, 61, 0.06);
}

/* ==================== 刷题练习面板 ==================== */
.practice-panel {
  padding: 24px 26px 26px;
  margin-bottom: 20px;
}

.pp-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.pp-title {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.pp-badge {
  padding: 3px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  color: white;
  background: #0d9488;
  flex-shrink: 0;
}

.pp-doc {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pp-progress {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 14px;
  margin-bottom: 18px;
  border-radius: var(--radius-sm);
  background: rgba(13, 148, 136, 0.06);
  font-size: 13px;
  color: var(--text-secondary);
}

.pp-progress strong {
  color: #0d9488;
  font-weight: 700;
}

.pp-cur {
  margin-left: auto;
}

.pp-empty {
  padding: 40px 0;
  text-align: center;
  color: var(--text-secondary);
  font-size: 14px;
}

.pp-question {
  margin-bottom: 18px;
}

.pp-q-label,
.pp-a-label,
.pp-comment-label,
.pp-ref-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  font-weight: 700;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.pp-q-text {
  font-size: 16px;
  line-height: 1.75;
  font-weight: 500;
  color: var(--text-primary);
  padding: 16px 18px;
  border-radius: var(--radius-sm);
  background: var(--accent-bg);
  border-left: 3px solid #0d9488;
}

.pp-q-tags {
  margin-top: 12px;
}

.pp-answer-area {
  margin-bottom: 18px;
}

.pp-a-hint {
  font-size: 12px;
  font-weight: 400;
  color: var(--text-muted, #9aa0a6);
}

.pp-result {
  margin-bottom: 20px;
  animation: reveal 0.35s ease;
}

@keyframes reveal {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: none; }
}

.pp-score-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  margin-bottom: 16px;
  border-radius: var(--radius-sm);
  background: rgba(13, 148, 136, 0.06);
}

.pp-score {
  display: flex;
  align-items: baseline;
  gap: 2px;
  padding: 6px 16px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.05);
  flex-shrink: 0;
}

.pp-score.score-high {
  background: rgba(45, 90, 61, 0.12);
  color: #2d5a3d;
}

.pp-score.score-medium {
  background: rgba(180, 83, 9, 0.12);
  color: #b45309;
}

.pp-score.score-low {
  background: rgba(220, 53, 69, 0.1);
  color: #dc3545;
}

.pp-score-num {
  font-size: 26px;
  font-weight: 700;
  line-height: 1;
}

.pp-score-max {
  font-size: 12px;
  opacity: 0.75;
}

.pp-score-desc {
  font-size: 13px;
  color: var(--text-secondary);
}

.pp-comment,
.pp-reference {
  padding: 14px 16px;
  border-radius: var(--radius-sm);
  margin-bottom: 12px;
}

.pp-comment {
  background: rgba(13, 148, 136, 0.06);
  border-left: 2px solid #0d9488;
}

.pp-reference {
  background: rgba(45, 90, 61, 0.06);
  border-left: 2px solid var(--accent);
}

.pp-comment-text,
.pp-ref-text {
  font-size: 13.5px;
  line-height: 1.8;
  color: var(--text-secondary);
  white-space: pre-wrap;
}

.pp-error {
  padding: 10px 14px;
  margin-bottom: 14px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  color: #b91c1c;
  background: rgba(220, 53, 69, 0.08);
}

.pp-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid var(--border);
}

.load-more {
  text-align: center;
  padding: 12px 0 40px;
}

.all-loaded {
  font-size: 13px;
  color: var(--text-muted, #9aa0a6);
}
</style>
