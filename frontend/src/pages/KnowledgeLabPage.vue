<template>
  <main class="knowledge-page">
    <header class="hero">
      <div>
        <p class="eyebrow">KNOWLEDGE HUB / RAG</p>
        <h1>知识库中心</h1>
        <p class="hero-copy">
          查看面试题库和知识点检索结果，确认哪些资料被召回，以及它们如何作为参考上下文进入面试 Agent。
        </p>
      </div>
      <div class="hero-actions">
        <el-button @click="loadStats" :loading="statsLoading">刷新统计</el-button>
        <el-button type="primary" @click="router.push('/config')">开始一次面试</el-button>
      </div>
    </header>

    <el-alert v-if="statsError" :title="statsError" type="warning" show-icon class="mb" />

    <section class="stats-grid">
      <el-card class="stat-card" shadow="never">
        <span class="stat-label">面试题库</span>
        <strong>{{ questionStats?.entryCount ?? '—' }}</strong>
        <small>题 · {{ questionStats?.version || 'interview-rag-v1' }}</small>
      </el-card>
      <el-card class="stat-card" shadow="never">
        <span class="stat-label">知识点库</span>
        <strong>{{ conceptStats?.chunkCount ?? '—' }}</strong>
        <small>知识块 · {{ conceptStats?.version || 'week2-v1' }}</small>
      </el-card>
      <el-card class="stat-card" shadow="never">
        <span class="stat-label">当前检索模式</span>
        <strong class="stat-mode">本地 RAG</strong>
        <small>可复现词项检索 · TopK ≤ 5</small>
      </el-card>
      <el-card class="stat-card" shadow="never">
        <span class="stat-label">面试流程接入</span>
        <strong class="stat-mode">已接入</strong>
        <small>出题、追问、报告都会参考题库</small>
      </el-card>
    </section>

    <section class="rag-layout">
      <el-card class="search-card" shadow="never">
        <template #header>
          <div class="card-title">
            <div>
              <span class="section-kicker">RETRIEVAL TEST</span>
              <h2>检索测试</h2>
            </div>
            <el-tag type="success" effect="plain">后端接口已连接</el-tag>
          </div>
        </template>

        <el-tabs v-model="sourceType" @tab-change="resetResults">
          <el-tab-pane label="面试题库" name="question" />
          <el-tab-pane label="知识点库" name="concept" />
        </el-tabs>

        <el-form label-position="top" @submit.prevent="search">
          <div class="form-row">
            <el-form-item label="岗位" class="role-field">
              <el-select v-model="role" style="width: 100%">
                <template v-if="sourceType === 'question'">
                  <el-option label="后端开发" value="后端开发" />
                  <el-option label="前端开发" value="前端开发" />
                  <el-option label="产品经理" value="产品经理" />
                  <el-option label="通用面试" value="通用面试" />
                  <el-option label="其他岗位" value="其他岗位" />
                </template>
                <template v-else>
                  <el-option label="Java 后端" value="JAVA_BACKEND" />
                  <el-option label="前端开发" value="FRONTEND" />
                  <el-option label="全栈开发" value="FULL_STACK" />
                </template>
              </el-select>
            </el-form-item>
            <el-form-item label="难度" class="difficulty-field">
              <el-select v-model="difficulty" style="width: 100%">
                <el-option label="不限制" value="" />
                <el-option label="简单" value="EASY" />
                <el-option label="中等" value="MEDIUM" />
                <el-option label="困难" value="HARD" />
              </el-select>
            </el-form-item>
          </div>
          <el-form-item label="问题或知识点">
            <el-input v-model="query" maxlength="500" show-word-limit placeholder="例如：Redis 缓存一致性、SSE 断线重连" @keyup.enter="search" />
          </el-form-item>
          <div class="search-actions">
            <span class="search-tip">召回内容只作为参考资料，不会替代简历和真实回答。</span>
            <el-button type="primary" :loading="loading" @click="search">检索上下文</el-button>
          </div>
        </el-form>

        <el-alert v-if="error" :title="error" type="error" show-icon class="mb" />
        <el-empty v-if="searched && !loading && !hits.length" description="没有达到相关性阈值的结果" />

        <div v-if="hits.length" class="result-list">
          <div class="result-summary">召回 {{ hits.length }} 条 · 已按相关性排序</div>
          <el-card v-for="hit in hits" :key="hit.key" class="result-card" shadow="never">
            <div class="result-heading">
              <h3>{{ hit.title }}</h3>
              <el-tag size="small" effect="plain">相关性 {{ hit.score }}</el-tag>
            </div>
            <div class="reference-label">知识库参考内容（不是候选人回答，也不是标准答案）</div>
            <p>{{ hit.content }}</p>
            <div class="result-meta">
              <span>{{ hit.id }}</span>
              <span>{{ hit.role }}</span>
              <span>{{ hit.difficulty || '未标注难度' }}</span>
              <span v-if="hit.tags">{{ hit.tags.join(' · ') }}</span>
            </div>
            <a v-if="hit.source" :href="hit.source" target="_blank" rel="noreferrer">查看来源 ↗</a>
          </el-card>
        </div>
      </el-card>

      <aside class="side-column">
        <el-card class="context-card" shadow="never">
          <template #header>
            <div class="card-title">
              <div>
                <span class="section-kicker">CONTEXT PREVIEW</span>
                <h2>送入 Agent 的上下文</h2>
              </div>
            </div>
          </template>
          <el-empty v-if="!context" description="完成一次检索后，这里会显示拼接后的上下文" :image-size="70" />
          <pre v-else>{{ context }}</pre>
        </el-card>

        <el-card class="flow-card" shadow="never">
          <template #header><h2>RAG 在面试中的位置</h2></template>
          <div class="flow-step"><b>1</b><div><strong>检索</strong><p>根据岗位、难度和当前问题召回相关题目或知识块。</p></div></div>
          <div class="flow-step"><b>2</b><div><strong>拼接</strong><p>将命中的内容、来源和面试历史放入模型上下文。</p></div></div>
          <div class="flow-step"><b>3</b><div><strong>生成</strong><p>AI 生成下一题、追问或评价，题库只作为参考资料。</p></div></div>
          <el-alert title="当前实现使用本地词项检索，暂未接入向量数据库。" type="info" :closable="false" show-icon />
        </el-card>
      </aside>
    </section>
  </main>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'

const router = useRouter()
const sourceType = ref<'question' | 'concept'>('question')
const role = ref('后端开发')
const difficulty = ref('')
const query = ref('Redis 缓存一致性')
const loading = ref(false)
const statsLoading = ref(false)
const searched = ref(false)
const error = ref('')
const statsError = ref('')
const context = ref('')

interface QuestionStats { entryCount: number; version: string }
interface ConceptStats { chunkCount: number; version: string }
interface RawHit {
  rowId?: string; knowledgeId?: string; question?: string; title?: string; answer?: string; content?: string
  role?: string; targetRole?: string; difficulty?: string; tags?: string[]; score?: number; source?: string
}
interface SearchResult { hits?: RawHit[]; context?: string }
interface DisplayHit { key: string; id: string; title: string; content: string; role: string; difficulty: string; tags: string[]; score: string; source?: string }
const questionStats = ref<QuestionStats | null>(null)
const conceptStats = ref<ConceptStats | null>(null)
const hits = ref<DisplayHit[]>([])

async function loadStats() {
  statsLoading.value = true
  statsError.value = ''
  const [questionResult, conceptResult] = await Promise.allSettled([
    request.get('/knowledge/interview-rag/stats') as Promise<QuestionStats>,
    request.get('/knowledge/stats') as Promise<ConceptStats>,
  ])
  if (questionResult.status === 'fulfilled') questionStats.value = questionResult.value
  if (conceptResult.status === 'fulfilled') conceptStats.value = conceptResult.value
  if (questionResult.status === 'rejected' || conceptResult.status === 'rejected') statsError.value = '部分知识库统计暂时不可用，检索接口仍可单独测试。'
  statsLoading.value = false
}

function resetResults() {
  hits.value = []
  context.value = ''
  searched.value = false
  error.value = ''
  if (sourceType.value === 'concept' && role.value === '后端开发') role.value = 'JAVA_BACKEND'
  if (sourceType.value === 'question' && role.value === 'JAVA_BACKEND') role.value = '后端开发'
}

function displayHits(result: SearchResult): DisplayHit[] {
  return (result.hits || []).map((hit, index) => ({
    key: hit.rowId || hit.knowledgeId || String(index),
    id: hit.rowId || hit.knowledgeId || '未命名记录',
    title: hit.question || hit.title || '知识块',
    content: hit.answer || hit.content || '',
    role: hit.role || hit.targetRole || '未标注岗位',
    difficulty: hit.difficulty || '',
    tags: hit.tags || [],
    score: typeof hit.score === 'number' ? hit.score.toFixed(2) : '—',
    source: hit.source,
  }))
}

async function search() {
  if (!query.value.trim()) { error.value = '请输入要检索的问题或知识点'; return }
  loading.value = true
  searched.value = true
  error.value = ''
  try {
    const path = sourceType.value === 'question' ? '/knowledge/interview-rag/search' : '/knowledge/search'
    const result = await request.get(path, { params: { targetRole: role.value, difficulty: difficulty.value, query: query.value.trim(), topK: 5 } }) as SearchResult
    hits.value = displayHits(result)
    context.value = result.context || ''
  } catch (e) {
    error.value = (e as Error).message || '知识库检索失败'
    hits.value = []
    context.value = ''
  } finally { loading.value = false }
}

onMounted(loadStats)
</script>

<style scoped>
.knowledge-page { max-width: 1200px; margin: 0 auto; padding: 38px 24px 60px; color: #273248; }
.hero { display: flex; justify-content: space-between; align-items: flex-end; gap: 24px; margin-bottom: 26px; }
.eyebrow, .section-kicker { color: #6374f1; font-size: 12px; font-weight: 700; letter-spacing: .14em; margin: 0 0 8px; }
.hero h1 { color: #18233a; font-size: 38px; margin: 0 0 10px; font-weight: 700; letter-spacing: -.03em; }
.hero-copy { max-width: 660px; color: #6d7890; line-height: 1.7; }
.hero-actions { display: flex; gap: 10px; white-space: nowrap; }
.mb { margin-bottom: 18px; }
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 20px; }
.stat-card { border: 1px solid #e4e8f3; background: linear-gradient(145deg, #fff, #f8f9ff); }
.stat-card :deep(.el-card__body) { padding: 18px; }
.stat-label { display: block; color: #7b8496; font-size: 13px; margin-bottom: 12px; }
.stat-card strong { display: block; color: #23305a; font-size: 26px; line-height: 1.2; }
.stat-card .stat-mode { font-size: 21px; }
.stat-card small { display: block; color: #929bad; margin-top: 8px; line-height: 1.4; }
.rag-layout { display: grid; grid-template-columns: minmax(0, 1.55fr) minmax(300px, .85fr); gap: 20px; align-items: start; }
.search-card, .context-card, .flow-card { border: 1px solid #e4e8f3; border-radius: 12px; }
.card-title { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; }
.card-title h2, .flow-card h2 { margin: 0; color: #202b44; font-size: 18px; }
.form-row { display: flex; gap: 14px; }
.role-field { flex: 1; }.difficulty-field { width: 160px; }
.search-actions { display: flex; align-items: center; justify-content: space-between; gap: 15px; margin-top: -4px; }
.search-tip { color: #8b94a5; font-size: 12px; line-height: 1.5; }
.result-list { margin-top: 24px; }.result-summary { color: #69758d; font-size: 13px; margin-bottom: 10px; }
.result-card { margin-bottom: 10px; background: #fbfcff; border-color: #e7ebf5; }
.result-card :deep(.el-card__body) { padding: 16px; }.result-heading { display: flex; justify-content: space-between; gap: 12px; align-items: flex-start; }
.result-heading h3 { margin: 0 0 8px; color: #27345a; font-size: 16px; line-height: 1.5; }
.reference-label { margin: 0 0 6px; color: #6374f1; font-size: 12px; font-weight: 700; }
.result-card p { margin: 0; color: #56627a; line-height: 1.7; white-space: pre-wrap; }
.result-meta { display: flex; flex-wrap: wrap; gap: 8px 14px; margin-top: 12px; color: #8a94a7; font-size: 12px; }
.result-card a { display: inline-block; margin-top: 9px; color: #6374f1; font-size: 12px; text-decoration: none; }
.side-column { display: grid; gap: 20px; }.context-card pre { max-height: 430px; overflow: auto; white-space: pre-wrap; word-break: break-word; background: #20283b; color: #dce5ff; padding: 14px; border-radius: 8px; font: 12px/1.7 ui-monospace, Consolas, monospace; }
.flow-step { display: flex; gap: 12px; padding: 12px 0; border-bottom: 1px solid #edf0f6; }.flow-step:last-of-type { border-bottom: 0; }
.flow-step b { width: 24px; height: 24px; border-radius: 50%; background: #eef0ff; color: #6374f1; text-align: center; line-height: 24px; flex: 0 0 auto; }.flow-step strong { color: #32405e; font-size: 14px; }.flow-step p { color: #7a8498; font-size: 13px; line-height: 1.6; margin: 4px 0 0; }
.flow-card :deep(.el-alert) { margin-top: 14px; }
@media (max-width: 900px) { .stats-grid { grid-template-columns: repeat(2, 1fr); }.rag-layout { grid-template-columns: 1fr; } }
@media (max-width: 640px) { .knowledge-page { padding: 24px 14px 40px; }.hero { align-items: flex-start; flex-direction: column; }.hero h1 { font-size: 30px; }.hero-actions { width: 100%; }.hero-actions .el-button { flex: 1; }.form-row { flex-direction: column; gap: 0; }.difficulty-field { width: 100%; }.search-actions { align-items: flex-start; flex-direction: column; }.stats-grid { gap: 8px; }.stat-card strong { font-size: 21px; } }
</style>
