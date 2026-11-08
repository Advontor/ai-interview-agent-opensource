<template>
  <div class="knowledge-page">
    <!-- 页面标题区 -->
    <PageHeader
      tag="知识库 / RAG"
      title="面试"
      highlight="知识库"
      description="点击「刷题」可按题库浏览该岗位的面试真题与参考答案，提前知道可能会被问到什么，无需上传简历。"
    />

    <!-- 文档列表 -->
    <div class="section card animate-reveal animate-reveal-delay-1">
      <div class="section-header">
        <div class="section-title">
          <span class="title-num">01</span>
          岗位题库
        </div>
        <span class="doc-count">共 {{ docs.length }} 个题库</span>
      </div>
      <div class="doc-list">
        <div v-if="loading" class="doc-state">正在加载知识库...</div>
        <div v-else-if="loadError" class="doc-state doc-state-error">{{ loadError }}</div>
        <div v-else-if="docs.length === 0" class="doc-state">知识库暂无文档，请先导入面试问答数据集。</div>
        <template v-else>
          <div
            v-for="doc in docs"
            :key="doc.id"
            class="doc-item clickable"
            role="button"
            tabindex="0"
            @click="openDoc(doc)"
            @keydown.enter="openDoc(doc)"
          >
            <div class="doc-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                <polyline points="14 2 14 8 20 8"/>
              </svg>
            </div>
            <div class="doc-info">
              <div class="doc-name">{{ doc.name }}</div>
              <div class="doc-meta">
                <span>{{ doc.position }}</span>
                <span class="meta-dot">·</span>
                <span>面试题库</span>
              </div>
            </div>
            <button
              class="doc-interview"
              type="button"
              :title="`浏览「${doc.name}」的真题与参考答案`"
              @click.stop="openPractice(doc.name)"
            >
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/>
                <path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/>
              </svg>
              刷题
            </button>
            <span class="doc-arrow" aria-hidden="true">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="9 18 15 12 9 6"/>
              </svg>
            </span>
          </div>
        </template>
      </div>
    </div>

    <!-- 题库内容抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      :title="activeDoc?.docTitle || '题库内容'"
      size="640px"
      class="chunk-drawer"
    >
      <div class="chunk-panel">
        <div class="chunk-meta">
          <span class="chunk-badge">{{ activeDoc?.position }}</span>
          <span class="chunk-count">共 {{ chunks.length }} 道题</span>
        </div>

        <el-input
          v-model="keyword"
          placeholder="在本题库内搜索，如 Java、索引、并发"
          clearable
          class="chunk-search"
        />

        <div v-if="chunkLoading" class="chunk-state">正在加载题库...</div>
        <div v-else-if="chunkError" class="chunk-state chunk-state-error">{{ chunkError }}</div>
        <div v-else-if="filteredChunks.length === 0" class="chunk-state">没有匹配的题目。</div>
        <template v-else>
          <div class="chunk-list">
            <div v-for="item in pagedChunks" :key="item.id" class="chunk-item">
              <div class="chunk-head">
                <span class="chunk-index">#{{ item.chunkIndex + 1 }}</span>
                <span v-if="item.parsed.difficulty" class="chunk-diff">{{ difficultyLabel(item.parsed.difficulty) }}</span>
                <span v-if="item.parsed.type" class="chunk-type">{{ typeLabel(item.parsed.type) }}</span>
              </div>
              <div class="chunk-q">{{ item.parsed.question }}</div>
              <div v-if="item.parsed.answer" class="chunk-a">{{ item.parsed.answer }}</div>
              <div v-if="skillList(item.parsed.skills).length" class="chunk-tags">
                <span v-for="tag in skillList(item.parsed.skills)" :key="tag" class="chunk-tag">{{ tag }}</span>
              </div>
            </div>
          </div>
          <div v-if="filteredChunks.length > visibleCount" class="chunk-more">
            <button class="btn btn-secondary btn-small" @click="visibleCount += PAGE_SIZE">
              加载更多（还有 {{ filteredChunks.length - visibleCount }} 条）
            </button>
          </div>
        </template>
      </div>

      <template #footer>
        <div class="drawer-footer">
          <button
            class="btn btn-primary"
            :disabled="!activeDoc"
            @click="activeDoc && startKbInterview(activeDoc.docTitle)"
          >
            用「{{ activeDoc?.position || '该岗位' }}」题库开始 AI 面试
          </button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getChunksByDoc, getDocList, type DocInfo, type KnowledgeChunk } from '../api/rag'
import PageHeader from '../components/PageHeader.vue'

const loading = ref(true)
const loadError = ref('')

const docs = reactive<Array<{
  id: number
  name: string
  position: string
}>>([])

// 加载真实文档列表（后端 /rag/doc/list 返回不重复的文档标题）
const loadDocs = async () => {
  try {
    loading.value = true
    loadError.value = ''
    const list = await getDocList()
    docs.splice(0, docs.length)
    list.forEach((doc: DocInfo, idx: number) => {
      docs.push({
        id: idx + 1,
        name: doc.docTitle,
        position: doc.position,
      })
    })
  } catch (err: any) {
    console.error('加载知识库文档失败:', err)
    loadError.value = err?.displayMessage || err?.message || '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

// ---------- 题库内容（点击文档后打开） ----------
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

const drawerVisible = ref(false)
const activeDoc = ref<{ docTitle: string; position: string } | null>(null)
const chunks = ref<Array<KnowledgeChunk & { parsed: ParsedChunk }>>([])
const chunkLoading = ref(false)
const chunkError = ref('')
const keyword = ref('')
const visibleCount = ref(PAGE_SIZE)

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

watch(keyword, () => { visibleCount.value = PAGE_SIZE })

const openDoc = async (doc: { name: string; position: string }) => {
  activeDoc.value = { docTitle: doc.name, position: doc.position }
  drawerVisible.value = true
  keyword.value = ''
  visibleCount.value = PAGE_SIZE
  chunkError.value = ''
  chunks.value = []
  chunkLoading.value = true
  try {
    const list = await getChunksByDoc(doc.name)
    chunks.value = (list || []).map((c) => ({ ...c, parsed: parseChunk(c.content) }))
  } catch (err: any) {
    console.error('加载题库内容失败:', err)
    chunkError.value = err?.displayMessage || err?.message || '加载题库内容失败'
  } finally {
    chunkLoading.value = false
  }
}

onMounted(() => {
  loadDocs()
})

const router = useRouter()

// ---------- 「刷题」：打开独立的岗位真题刷题页（无需上传简历） ----------
const openPractice = (docTitle: string) => {
  if (!docTitle) return
  drawerVisible.value = false
  router.push({ path: '/practice', query: { doc: docTitle } })
}

// ---------- 「模拟面试」：以某个题库开一场面试 ----------
const startKbInterview = (docTitle: string) => {
  if (!docTitle) return
  drawerVisible.value = false
  // 绑定题库后进入「简历与面试配置」第二步；没有简历时 PreparePage 会先停在第一步
  router.push({ path: '/', query: { step: '2', kb: docTitle } })
}
</script>

<style scoped>
.knowledge-page {
  max-width: 820px;
  margin: 0 auto;
  padding-top: 8px;
}

.btn-small {
  padding: 10px 24px;
  font-size: 13px;
}

/* 区块 */
.section {
  padding: 28px 32px;
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 17px;
  font-weight: 700;
  color: var(--text-primary);
  font-family: var(--font-heading);
}

.title-num {
  font-family: var(--font-mono);
  font-size: 12px;
  font-weight: 600;
  color: var(--accent);
  background: var(--accent-bg);
  padding: 4px 10px;
  border-radius: 6px;
}

.doc-count {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--text-muted);
}

/* 文档列表 */
.doc-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.doc-state {
  padding: 28px 20px;
  text-align: center;
  font-size: 14px;
  color: var(--text-muted);
  background: var(--bg-elevated);
  border: 1px dashed var(--border);
  border-radius: var(--radius-md);
}

.doc-state-error {
  color: #dc2626;
  background: rgba(239, 68, 68, 0.06);
  border-color: rgba(239, 68, 68, 0.3);
}

.doc-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 18px;
  background: var(--bg-elevated);
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
}

.doc-item:hover {
  background: var(--accent-bg);
  transform: translateX(4px);
}

.doc-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-sm);
  background: var(--bg-card);
  color: var(--accent);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.doc-info {
  flex: 1;
  min-width: 0;
}

.doc-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.doc-meta {
  font-size: 12px;
  color: var(--text-muted);
  display: flex;
  align-items: center;
  gap: 8px;
}

.meta-dot {
  color: var(--border);
}

/* 抽屉底部操作 */
.drawer-footer {
  display: flex;
  justify-content: flex-end;
}

.drawer-footer .btn {
  width: 100%;
}

/* 可点击的文档条目 */
.doc-item.clickable {
  cursor: pointer;
  transition: all var(--transition-fast);
}

.doc-item.clickable:hover {
  border-color: var(--choice-border-hover);
  background: var(--accent-bg);
  transform: translateX(2px);
}

.doc-item.clickable:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}

.doc-arrow {
  display: flex;
  align-items: center;
  color: var(--text-muted);
  margin-left: 4px;
  transition: all var(--transition-fast);
}

.doc-item.clickable:hover .doc-arrow {
  color: var(--accent);
  transform: translateX(2px);
}

/* 每个题库的「模拟面试」入口 */
.doc-interview {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  padding: 7px 14px;
  font-family: inherit;
  font-size: 13px;
  font-weight: 600;
  color: var(--accent);
  background: var(--accent-bg);
  border: 1px solid var(--accent-border);
  border-radius: 100px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.doc-interview:hover {
  color: #ffffff;
  background: var(--accent);
  border-color: var(--accent);
}

.doc-interview:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}

/* ===== 题库抽屉 ===== */
.chunk-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.chunk-meta {
  display: flex;
  align-items: center;
  gap: 10px;
}

.chunk-badge {
  font-size: 12px;
  font-weight: 600;
  color: var(--accent);
  background: var(--accent-bg);
  padding: 4px 12px;
  border-radius: 100px;
}

.chunk-count {
  font-size: 12px;
  color: var(--text-muted);
}

.chunk-search {
  margin-bottom: 2px;
}

.chunk-state {
  padding: 28px 16px;
  text-align: center;
  font-size: 13px;
  color: var(--text-muted);
  background: var(--bg-elevated);
  border: 1px dashed var(--border);
  border-radius: var(--radius-md);
}

.chunk-state-error {
  color: #dc2626;
  background: rgba(239, 68, 68, 0.06);
  border-color: rgba(239, 68, 68, 0.3);
}

.chunk-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.chunk-item {
  padding: 14px 16px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
}

.chunk-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.chunk-index {
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--text-muted);
}

.chunk-diff,
.chunk-type {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 100px;
}

.chunk-diff {
  color: #ca8a04;
  background: rgba(234, 179, 8, 0.12);
}

.chunk-type {
  color: var(--accent);
  background: var(--accent-bg);
}

.chunk-q {
  font-size: 14px;
  font-weight: 600;
  line-height: 1.6;
  color: var(--text-primary);
  margin-bottom: 8px;
  white-space: pre-wrap;
}

.chunk-a {
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-secondary);
  white-space: pre-wrap;
  padding-left: 10px;
  border-left: 2px solid var(--border);
}

.chunk-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}

.chunk-tag {
  font-size: 11px;
  color: var(--text-muted);
  background: var(--bg-elevated);
  padding: 2px 8px;
  border-radius: 100px;
}

.chunk-more {
  text-align: center;
  padding: 6px 0 12px;
}
</style>
