import { createRouter, createWebHistory } from 'vue-router'
// 路由级代码分割：每个页面单独打包成 chunk，按需加载。
// 首屏只需下载 vue/element-plus + 当前页面，显著降低首屏体积与白屏时间。
const PreparePage = () => import('../pages/PreparePage.vue')
const InterviewPage = () => import('../pages/InterviewPage.vue')
const ReportPage = () => import('../pages/ReportPage.vue')
const KnowledgePage = () => import('../pages/KnowledgePage.vue')
const KbPracticePage = () => import('../pages/KbPracticePage.vue')
const LearningPage = () => import('../pages/LearningPage.vue')
const GraphPage = () => import('../pages/GraphPage.vue')
const HistoryPage = () => import('../pages/HistoryPage.vue')

const routes = [
  // 简历解析 + 面试配置 合并为一个两步式页面（见 PreparePage.vue）
  { path: '/', name: 'Prepare', component: PreparePage },
  // 兼容旧链接：/config 直接进入第二步
  { path: '/config', redirect: { path: '/', query: { step: '2' } } },
  // 面试页开启沉浸模式：隐藏侧边栏与顶部装饰条（见 Layout.vue）
  { path: '/interview', name: 'Interview', component: InterviewPage, meta: { immersive: true } },
  { path: '/report', name: 'Report', component: ReportPage },
  { path: '/knowledge', name: 'Knowledge', component: KnowledgePage },
  // 岗位真题刷题页：按题库浏览题目与参考答案，无需上传简历（?doc=题库名）
  { path: '/practice', name: 'KbPractice', component: KbPracticePage },
  { path: '/learning', name: 'Learning', component: LearningPage },
  { path: '/graph', name: 'Graph', component: GraphPage },
  { path: '/history', name: 'History', component: HistoryPage },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
