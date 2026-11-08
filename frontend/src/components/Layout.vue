<template>
  <div class="layout" :class="{ immersive }">
    <!-- 侧边栏（沉浸模式下隐藏） -->
    <aside v-if="!immersive" class="sidebar animate-reveal">
      <!-- Logo -->
      <div class="sidebar-logo">
        <div class="logo-icon">
          <img src="/logo.png" alt="面向未来" />
        </div>
        <div class="logo-text">
          <div class="logo-title">面向未来</div>
          <div class="logo-subtitle">基于 AI Agent 的<br />智能模拟面试系统</div>
        </div>
      </div>

      <!-- 导航菜单 -->
      <nav class="sidebar-nav">
        <div v-for="group in navGroups" :key="group.title" class="nav-group">
          <div class="nav-section">{{ group.title }}</div>
          <router-link
            v-for="item in group.items"
            :key="item.path"
            :to="item.path"
            class="nav-item"
            :class="{ active: isActive(item.path) }"
          >
            <span class="nav-icon" v-html="item.icon"></span>
            <span class="nav-label">{{ item.label }}</span>
            <span class="nav-indicator"></span>
          </router-link>
        </div>
      </nav>

      <!-- 底部信息 -->
      <div class="sidebar-footer">
        <div class="footer-version">v1.0.0</div>
        <div class="footer-tag">Powered by AI</div>
      </div>
    </aside>

    <!-- 主内容区 -->
    <main class="main">
      <!-- 顶部装饰条（沉浸模式下隐藏） -->
      <div v-if="!immersive" class="main-topbar">
        <div class="topbar-dot"></div>
        <div class="topbar-dot"></div>
        <div class="topbar-dot"></div>
      </div>

      <!-- 页面内容 -->
      <div class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="page" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

// 沉浸模式：面试进行中隐藏侧边栏与顶部装饰条，让用户专注作答
const immersive = computed(() => Boolean(route.meta?.immersive))

const navGroups = [
  {
    title: '面试流程',
    items: [
      {
        path: '/',
        label: '简历与面试配置',
        icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>'
      },
      {
        path: '/interview',
        label: '当前面试',
        icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>'
      },
      {
        path: '/report',
        label: '面试报告',
        icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/></svg>'
      },
      {
        path: '/history',
        label: '历史面试',
        icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 3v5h5"/><path d="M3.05 13A9 9 0 1 0 6 5.3L3 8"/><path d="M12 7v5l4 2"/></svg>'
      }
    ]
  },
  {
    title: '学习提升',
    items: [
      {
        path: '/learning',
        label: '记录与学习',
        icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/></svg>'
      },
      {
        path: '/knowledge',
        label: '知识库',
        icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>'
      },
      {
        path: '/graph',
        label: '知识图谱',
        icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="5" r="3"/><circle cx="5" cy="19" r="3"/><circle cx="19" cy="19" r="3"/><line x1="12" y1="8" x2="5" y2="16"/><line x1="12" y1="8" x2="19" y2="16"/></svg>'
      }
    ]
  }
]

const isActive = (path: string) => {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}
</script>

<style scoped>
.layout {
  display: flex;
  min-height: 100vh;
}

/* 沉浸模式：面试进行中，隐藏导航，全宽专注作答 */
.layout.immersive {
  background:
    radial-gradient(ellipse 900px 500px at 50% -10%, rgba(168, 213, 186, 0.22), transparent),
    var(--bg-page);
}
.layout.immersive .main-content {
  max-width: 880px;
  padding: 44px 40px 72px;
}

/* ===== 侧边栏 ===== */
.sidebar {
  width: 280px;
  min-width: 280px;
  background: linear-gradient(180deg, #f5f2ed 0%, #faf8f5 100%);
  border-right: 2px solid var(--border);
  box-shadow: 4px 0 24px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  padding: 28px 20px;
  position: sticky;
  top: 0;
  height: 100vh;
  z-index: 10;
}

/* Logo */
.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 12px 24px;
  border-bottom: 1px solid var(--border-light);
  margin-bottom: 24px;
}

.logo-text {
  flex: 1;
  min-width: 0;
}

.logo-icon {
  width: 46px;
  height: 46px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.logo-icon img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}

.logo-title {
  font-family: var(--font-heading);
  font-size: 17px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.01em;
  line-height: 1.2;
}

.logo-subtitle {
  font-size: 11.5px;
  line-height: 1.45;
  color: var(--text-muted);
  margin-top: 3px;
}

/* 导航 */
.sidebar-nav {
  flex: 1;
}

.nav-group {
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-light);
}

.nav-group:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.nav-section {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--accent);
  padding: 0 12px;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.nav-section::before {
  content: '';
  width: 3px;
  height: 12px;
  background: var(--accent);
  border-radius: 2px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 11px 12px;
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  position: relative;
  transition: all var(--transition-fast);
  margin-bottom: 4px;
  outline: none;
  border: 2px solid transparent;
}

.nav-item:focus {
  outline: none;
  border-color: transparent;
}

.nav-item:hover {
  background: var(--accent-bg);
  color: var(--accent);
}

.nav-item.active {
  background: rgba(45, 90, 61, 0.08);
  color: var(--accent);
  font-weight: 600;
  border-color: transparent;
}

.nav-item.active .nav-icon {
  color: var(--accent);
}

.nav-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 60%;
  background: var(--accent);
  border-radius: 0 4px 4px 0;
}

.nav-item.active .nav-indicator {
  opacity: 1;
  transform: scaleY(1);
}

.nav-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
}

.nav-label {
  flex: 1;
}

.nav-indicator {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%) scaleY(0);
  width: 3px;
  height: 20px;
  background: var(--accent);
  border-radius: 0 3px 3px 0;
  opacity: 0;
  transition: all var(--transition-normal);
}

/* 底部 */
.sidebar-footer {
  padding-top: 20px;
  border-top: 1px solid var(--border-light);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 12px 0;
}

.footer-version {
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--text-muted);
}

.footer-tag {
  font-size: 11px;
  color: var(--accent);
  font-weight: 500;
  padding: 3px 10px;
  background: var(--accent-bg);
  border-radius: 100px;
}

/* ===== 主内容区 ===== */
.main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.main-topbar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 20px 40px;
}

.topbar-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--border);
}

.topbar-dot:first-child {
  background: #f5a623;
}
.topbar-dot:nth-child(2) {
  background: #f8e71c;
}
.topbar-dot:nth-child(3) {
  background: var(--accent-light);
}

.main-content {
  flex: 1;
  padding: 0 48px 48px;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
}

/* 页面切换动画 */
.page-enter-active,
.page-leave-active {
  transition: all 0.3s ease;
}

.page-enter-from {
  opacity: 0;
  transform: translateY(12px);
}

.page-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* 响应式 */
@media (max-width: 900px) {
  .sidebar {
    display: none;
  }
  .main-content {
    padding: 0 24px 32px;
  }
  .main-topbar {
    padding: 16px 24px;
  }
}
</style>
