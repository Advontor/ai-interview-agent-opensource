<template>
  <div class="prepare-page">
    <!-- 步骤条：简历解析 -> 面试配置 -->
    <nav class="step-flow" aria-label="面试准备步骤">
      <template v-for="(s, i) in steps" :key="s.value">
        <button
          class="step-item"
          :class="{
            active: step === s.value,
            done: step > s.value,
            locked: s.value > step && !canEnter(s.value)
          }"
          :disabled="s.value > step && !canEnter(s.value)"
          @click="go(s.value)"
        >
          <span class="step-num">
            <svg
              v-if="step > s.value"
              width="13" height="13" viewBox="0 0 24 24" fill="none"
              stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"
            >
              <polyline points="20 6 9 17 4 12" />
            </svg>
            <template v-else>{{ i + 1 }}</template>
          </span>
          <span class="step-label">{{ s.label }}</span>
        </button>
        <span v-if="i < steps.length - 1" class="step-line" :class="{ done: step > s.value }"></span>
      </template>
    </nav>

    <!-- 第一步：简历解析 -->
    <UploadPage v-if="step === 1" @parsed="go(2)" />
    <!-- 第二步：面试配置（可携带来自知识库的题库绑定） -->
    <InterviewConfigPage v-else :knowledge-base="kb" @back="go(1)" />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import UploadPage from './UploadPage.vue'
import InterviewConfigPage from './InterviewConfigPage.vue'

const route = useRoute()
const router = useRouter()

// 从知识库页带过来的题库绑定（?kb=后端开发面试题库）
const kb = computed(() => {
  const value = route.query.kb
  return Array.isArray(value) ? (value[0] ?? '') : (value ?? '')
})

const steps = [
  { value: 1, label: '简历解析' },
  { value: 2, label: '面试配置' },
]

// 简历解析结果存在 sessionStorage 里，第二步依赖它
const hasResume = (): boolean => {
  try {
    return Boolean(sessionStorage.getItem('resumeParseResult'))
  } catch {
    return false
  }
}

const canEnter = (target: number) => target === 1 || hasResume()

const step = ref<number>(Number(route.query.step) === 2 && hasResume() ? 2 : 1)

const go = (target: number) => {
  if (target === step.value) return
  if (!canEnter(target)) return
  step.value = target
}

// 把步骤同步到 URL，便于刷新/前进后退保持一致
watch(step, (value) => {
  router.replace({ query: { ...route.query, step: String(value) } })
})

// 外部导航（如点击侧边栏）改变 URL 时，把步骤同步回来
watch(
  () => route.query.step,
  (value) => {
    const target = Number(value) === 2 ? 2 : 1
    if (target !== step.value && canEnter(target)) step.value = target
  }
)
</script>

<style scoped>
.prepare-page {
  max-width: 760px;
  margin: 0 auto;
  padding-top: 4px;
}

/* 步骤条 */
.step-flow {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 26px;
  padding: 12px 18px;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}

.step-item {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 4px 6px;
  border: none;
  background: transparent;
  font-family: inherit;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-muted);
  cursor: pointer;
  transition: color var(--transition-fast);
}

.step-item:disabled {
  cursor: not-allowed;
}

.step-item.active {
  color: var(--accent);
}

.step-item.done {
  color: var(--text-secondary);
}

.step-num {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  font-family: var(--font-mono);
  font-size: 12px;
  font-weight: 700;
  color: var(--text-muted);
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  flex-shrink: 0;
  transition: all var(--transition-fast);
}

.step-item.active .step-num {
  color: #ffffff;
  background: var(--accent);
  border-color: var(--accent);
}

.step-item.done .step-num {
  color: #ffffff;
  background: var(--accent-light);
  border-color: var(--accent-light);
}

.step-line {
  flex: 1;
  min-width: 20px;
  height: 2px;
  border-radius: 2px;
  background: var(--border-light);
  transition: background var(--transition-normal);
}

.step-line.done {
  background: var(--accent-light);
}
</style>
