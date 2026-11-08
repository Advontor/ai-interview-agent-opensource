<template>
  <header class="page-hero animate-reveal" :style="{ '--hero-accent': accent }">
    <div class="hero-main">
      <div v-if="tag" class="hero-eyebrow">
        <span class="hero-dot"></span>
        <span class="hero-tag">{{ tag }}</span>
        <span class="hero-rule"></span>
      </div>
      <h1 class="hero-title">
        {{ title }}
        <em v-if="highlight" class="hero-accent">{{ highlight }}</em>
        {{ suffix }}
      </h1>
      <p v-if="description" class="hero-desc">{{ description }}</p>
    </div>
    <div v-if="index" class="hero-index" aria-hidden="true">{{ index }}</div>
  </header>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    /** 左上角小标签，如「第一步 · 简历解析」 */
    tag?: string
    /** 标题前段 */
    title?: string
    /** 标题中需要强调（变色+高亮底）的词 */
    highlight?: string
    /** 标题后段 */
    suffix?: string
    /** 描述文字 */
    description?: string
    /** 主题强调色，默认森林绿 */
    accent?: string
    /** 右上角的大号序号（仅流程类页面用），如「01」 */
    index?: string
  }>(),
  {
    tag: '',
    title: '',
    highlight: '',
    suffix: '',
    description: '',
    accent: '#2d5a3d',
    index: '',
  }
)
</script>

<style scoped>
.page-hero {
  --hero-accent: #2d5a3d;
  position: relative;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 32px;
  padding: 6px 0 22px;
  margin-bottom: 30px;
  border-bottom: 1px solid var(--border-light);
}

/* 底部渐隐强调线 */
.page-hero::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1px;
  height: 1px;
  background: linear-gradient(90deg, var(--hero-accent) 0%, transparent 52%);
  opacity: 0.55;
  pointer-events: none;
}

.hero-main {
  flex: 1;
  min-width: 0;
}

/* 顶部小标签 + 延伸细线 */
.hero-eyebrow {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.hero-dot {
  position: relative;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--hero-accent);
  flex-shrink: 0;
}

.hero-dot::after {
  content: '';
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  background: var(--hero-accent);
  opacity: 0.16;
}

.hero-tag {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.09em;
  color: var(--hero-accent);
  white-space: nowrap;
}

.hero-rule {
  flex: 1;
  height: 1px;
  min-width: 24px;
  background: linear-gradient(90deg, var(--border) 0%, transparent 100%);
}

.hero-title {
  font-family: var(--font-heading);
  font-size: 40px;
  font-weight: 700;
  line-height: 1.18;
  letter-spacing: -0.02em;
  color: var(--text-primary);
  margin: 0 0 12px;
}

.hero-accent {
  position: relative;
  font-style: italic;
  color: var(--hero-accent);
  padding: 0 1px;
}

.hero-accent::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 5px;
  height: 9px;
  background: var(--hero-accent);
  opacity: 0.14;
  border-radius: 4px;
  z-index: -1;
}

.hero-desc {
  font-size: 15px;
  line-height: 1.7;
  color: var(--text-secondary);
  max-width: 560px;
  margin: 0;
}

/* 右上角大号序号 */
.hero-index {
  flex-shrink: 0;
  font-family: var(--font-heading);
  font-size: 84px;
  font-weight: 700;
  line-height: 0.9;
  letter-spacing: -0.05em;
  color: var(--hero-accent);
  opacity: 0.12;
  user-select: none;
  margin-top: -4px;
}

@media (max-width: 720px) {
  .hero-title {
    font-size: 28px;
  }
  .hero-index {
    display: none;
  }
  .hero-desc {
    max-width: none;
  }
}
</style>
