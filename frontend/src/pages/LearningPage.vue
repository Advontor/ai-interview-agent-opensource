<template>
  <div class="learning-page">
    <!-- 页面标题区 -->
    <PageHeader
      tag="记录与学习"
      title="面试"
      highlight="学习资源"
      description="根据你的面试表现，AI 推荐了针对性的学习资源，帮你查漏补缺、持续提升。"
      accent="#b45309"
    />

    <!-- 筛选标签 -->
    <div class="filter-tabs animate-reveal animate-reveal-delay-1">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        class="filter-tab"
        :class="{ active: activeTab === tab.value }"
        @click="activeTab = tab.value"
      >
        {{ tab.label }}
      </button>
    </div>

    <!-- 资源列表 -->
    <div v-if="!loading" class="resource-grid">
      <div
        v-for="(resource, index) in filteredResources"
        :key="resource.id"
        class="resource-card card animate-reveal"
        :style="{ animationDelay: (index % 3) * 0.1 + 0.2 + 's' }"
      >
        <div class="resource-top">
          <div class="resource-category" :class="'cat-' + getCategoryClass(resource.category)">
            {{ resource.category }}
          </div>
          <div class="resource-skill">{{ resource.relatedSkill }}</div>
        </div>
        <h3 class="resource-title">{{ resource.title }}</h3>
        <p class="resource-desc">{{ resource.description }}</p>
        <div class="resource-footer">
          <div class="resource-meta">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
              <line x1="16" y1="2" x2="16" y2="6"/>
              <line x1="8" y1="2" x2="8" y2="6"/>
              <line x1="3" y1="10" x2="21" y2="10"/>
            </svg>
            {{ resource.createdAt?.split('T')[0] || '近期' }}
          </div>
          <button class="btn btn-ghost btn-xsmall" @click="startLearning(resource)">
            去 B 站学习
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/>
              <polyline points="15 3 21 3 21 9"/>
              <line x1="10" y1="14" x2="21" y2="3"/>
            </svg>
          </button>
        </div>
      </div>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <div class="loading-text">正在加载学习资源...</div>
    </div>

    <!-- 空状态 -->
    <div v-if="filteredResources.length === 0" class="empty-state">
      <div class="empty-icon">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/>
          <path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/>
        </svg>
      </div>
      <div class="empty-text">暂无该分类的学习资源</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getLearningList, type LearningResource } from '../api/learning'
import PageHeader from '../components/PageHeader.vue'

const activeTab = ref('all')
const loading = ref(false)

const tabs = [
  { value: 'all', label: '全部' },
  { value: 'Java', label: 'Java 后端' },
  { value: '前端', label: '前端' },
  { value: '算法', label: '算法' },
  { value: '行为面试', label: '行为面试' }
]

// 学习资源列表（从后端获取）
const resources = ref<LearningResource[]>([])
const loadError = ref('')

// 从后端加载学习资源
const loadResources = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const data = await getLearningList()
    resources.value = data || []
  } catch (error: any) {
    console.error('加载学习资源失败:', error)
    loadError.value = error?.displayMessage || error?.message || '加载学习资源失败，请稍后重试'
    resources.value = []
  } finally {
    loading.value = false
  }
}

// 筛选后的资源
const filteredResources = computed(() => {
  if (activeTab.value === 'all') return resources.value
  return resources.value.filter(r => r.category.includes(activeTab.value))
})

// 开始学习（跳转到资源链接）
const startLearning = (resource: LearningResource) => {
  if (resource.url) {
    window.open(resource.url, '_blank')
  }
}

// 分类颜色映射
const getCategoryClass = (category: string) => {
  if (category.includes('Java')) return 'java'
  if (category.includes('前端')) return 'frontend'
  if (category.includes('算法')) return 'algorithm'
  if (category.includes('行为')) return 'behavior'
  return 'default'
}

onMounted(() => {
  loadResources()
})
</script>

<style scoped>
.learning-page {
  max-width: 960px;
  margin: 0 auto;
  padding-top: 8px;
}

/* 筛选标签 */
.filter-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 28px;
  flex-wrap: wrap;
}

.filter-tabs {
  display: flex;
  gap: 32px;
  margin-bottom: 32px;
  border-bottom: 1px solid var(--border-light);
  padding-bottom: 0;
}

.filter-tab {
  padding: 12px 4px;
  font-size: 15px;
  font-weight: 500;
  color: var(--text-muted);
  background: transparent;
  border: none;
  border-bottom: 2px solid transparent;
  border-radius: 0;
  cursor: pointer;
  transition: all var(--transition-fast);
  margin-bottom: -1px;
  position: relative;
}

.filter-tab:hover {
  color: var(--accent);
}

.filter-tab.active {
  color: var(--accent);
  font-weight: 600;
  border-bottom-color: var(--accent);
}

/* 资源卡片网格 */
.resource-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.resource-card {
  padding: 24px;
  display: flex;
  flex-direction: column;
  transition: all var(--transition-normal);
}

.resource-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-hover);
  border-color: var(--accent-border);
}

.resource-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.resource-category {
  font-size: 11px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 6px;
}

.cat-java {
  background: rgba(245, 158, 11, 0.1);
  color: #d97706;
}

.cat-frontend {
  background: rgba(59, 130, 246, 0.1);
  color: #2563eb;
}

.cat-algorithm {
  background: rgba(168, 85, 247, 0.1);
  color: #9333ea;
}

.cat-behavior {
  background: var(--accent-bg);
  color: var(--accent);
}

.resource-skill {
  font-size: 11px;
  font-weight: 500;
  color: var(--text-muted);
  background: var(--bg-elevated);
  padding: 3px 8px;
  border-radius: 4px;
}

.resource-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.4;
  margin-bottom: 10px;
  font-family: var(--font-heading);
}

.resource-desc {
  font-size: 13px;
  line-height: 1.6;
  color: var(--text-secondary);
  flex: 1;
  margin-bottom: 16px;
}

.resource-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 14px;
  border-top: 1px solid var(--border-light);
}

.resource-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-muted);
}

.btn-xsmall {
  padding: 6px 14px;
  font-size: 12px;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 60px 20px;
}

.empty-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 16px;
  border-radius: 50%;
  background: var(--bg-elevated);
  color: var(--text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-text {
  font-size: 14px;
  color: var(--text-muted);
}

/* 加载状态 */
.loading-state {
  text-align: center;
  padding: 60px 20px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  margin: 0 auto 16px;
  border: 3px solid var(--border);
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.loading-text {
  font-size: 14px;
  color: var(--text-muted);
}

/* 响应式 */
@media (max-width: 900px) {
  .resource-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .resource-grid {
    grid-template-columns: 1fr;
  }
}
</style>
