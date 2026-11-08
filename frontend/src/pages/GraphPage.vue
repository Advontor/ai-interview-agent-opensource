<template>
  <div class="graph-page">
    <PageHeader
      tag="学习提升 · 知识图谱"
      title="岗位与能力的"
      highlight="知识图谱"
      description="岗位 — 能力维度 — 考察场景之间的关系网络，由 2 万道面试题的标签统计生成。"
    />

    <!-- 加载状态 -->
    <div v-if="loading" class="graph-state">
      <div class="graph-spinner"></div>
      <p>正在加载知识图谱...</p>
    </div>

    <!-- 错误/空状态 -->
    <div v-else-if="loadError" class="graph-state graph-state-error">
      <p>{{ loadError }}</p>
    </div>

    <!-- 知识图谱容器 -->
    <div v-else class="graph-card card">
      <div ref="graphRef" class="graph-canvas"></div>
    </div>

    <!-- 图谱统计 -->
    <div v-if="!loading && !loadError" class="graph-stats">
      <div v-for="item in statCards" :key="item.label" class="graph-stat card">
        <div class="graph-stat-label">{{ item.label }}</div>
        <div class="graph-stat-value">{{ item.value }}</div>
      </div>
    </div>

    <div v-if="!loading && !loadError" class="graph-actions">
      <el-button @click="goBack">返回报告</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { getAllNodes, getEdgesForNode, type GraphNode } from '../api/graph'
import PageHeader from '../components/PageHeader.vue'

const router = useRouter()
const graphRef = ref<HTMLElement | null>(null)
let chart: echarts.ECharts | null = null
const loading = ref(true)
const loadError = ref('')

// 节点类型映射
const typeCategoryMap: Record<string, number> = {
  JOB: 0,
  SKILL: 1,
  KNOWLEDGE: 2,
  QUESTION: 3
}
const typeSizeMap: Record<string, number> = {
  JOB: 60,
  SKILL: 40,
  KNOWLEDGE: 25,
  QUESTION: 20
}

// 图谱数据（从后端获取）
const graphData = reactive({
  nodes: [] as Array<{ id: string; name: string; category: number; symbolSize: number }>,
  edges: [] as Array<{ source: string; target: string; weight: number }>,
  legend: ['岗位', '能力维度', '考察场景']
})

// 图谱规模统计（真实节点/关系数，替代原先写死的 0%）
const graphStats = reactive({ jobs: 0, skills: 0, scenes: 0, edges: 0 })
const statCards = computed(() => [
  { label: '岗位节点', value: graphStats.jobs },
  { label: '能力维度', value: graphStats.skills },
  { label: '考察场景', value: graphStats.scenes },
  { label: '关联关系', value: graphStats.edges }
])

// 加载真实图谱数据
const loadGraphData = async () => {
  try {
    loading.value = true
    loadError.value = ''

    // 获取所有节点
    const { jobs, skills, knowledges, questions } = await getAllNodes()
    const allNodes: GraphNode[] = [...jobs, ...skills, ...knowledges, ...questions]

    if (allNodes.length === 0) {
      loadError.value = '暂无知识图谱数据'
      return
    }

    // 转换节点格式
    const maxCategory = graphData.legend.length - 1
    graphData.nodes.splice(0, graphData.nodes.length)
    allNodes.forEach(node => {
      graphData.nodes.push({
        id: String(node.id),
        name: node.name,
        category: Math.min(typeCategoryMap[node.nodeType] ?? 2, maxCategory),
        symbolSize: typeSizeMap[node.nodeType] ?? 25
      })
    })

    graphStats.jobs = jobs.length
    graphStats.skills = skills.length
    graphStats.scenes = knowledges.length

    // 获取所有边（遍历每个节点获取边，去重）
    const edgeSet = new Set<string>()
    graphData.edges.splice(0, graphData.edges.length)

    for (const node of allNodes) {
      const edges = await getEdgesForNode(node.id)
      edges.forEach(edge => {
        const key = `${edge.sourceNodeId}-${edge.targetNodeId}`
        if (!edgeSet.has(key)) {
          edgeSet.add(key)
          graphData.edges.push({
            source: String(edge.sourceNodeId),
            target: String(edge.targetNodeId),
            weight: edge.weight ?? 0
          })
        }
      })
    }
    graphStats.edges = graphData.edges.length

    // 渲染图谱
    setTimeout(() => renderGraph(), 100)
  } catch (err: any) {
    console.error('加载知识图谱失败:', err)
    loadError.value = err?.displayMessage || err?.message || '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

const renderGraph = () => {
  if (!graphRef.value) return

  chart = echarts.init(graphRef.value)

  const maxWeight = Math.max(1, ...graphData.edges.map((e) => e.weight))

  const option = {
    tooltip: {},
    legend: {
      data: graphData.legend,
      top: 10
    },
    series: [
      {
        type: 'graph',
        layout: 'force',
        data: graphData.nodes.map((node) => ({
          ...node,
          itemStyle: {
            color: ['#2d5a3d', '#a8d5ba', '#e6a23c'][node.category] ?? '#a8d5ba'
          }
        })),
        // 边的粗细反映权重（该「岗位—能力」在多少道题里出现）
        links: graphData.edges.map((edge) => ({
          source: edge.source,
          target: edge.target,
          value: edge.weight,
          lineStyle: { width: 0.8 + 3.2 * (edge.weight / maxWeight) }
        })),
        categories: graphData.legend.map((name) => ({ name })),
        roam: true,
        label: {
          show: true,
          position: 'right',
          fontSize: 12,
          color: '#333',
          // 加描边，节点密集时标签依然可读
          textBorderColor: '#faf8f5',
          textBorderWidth: 3
        },
        force: {
          repulsion: 1500,
          edgeLength: 170,
          gravity: 0.05,
          friction: 0.12
        },
        lineStyle: {
          color: '#c9c2b4',
          width: 1,
          curveness: 0.12
        },
        emphasis: {
          focus: 'adjacency',
          lineStyle: { width: 4 }
        }
      }
    ]
  }

  chart.setOption(option)
}

const handleResize = () => {
  if (chart) chart.resize()
}

const goBack = () => router.push('/report')

onMounted(() => {
  loadGraphData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (chart) chart.dispose()
})
</script>

<style>
.graph-page {
  max-width: 1100px;
  margin: 0 auto;
  padding-top: 8px;
}

/* 加载 / 错误状态 */
.graph-state {
  text-align: center;
  padding: 70px 20px;
  color: var(--text-muted);
  background: var(--bg-card);
  border: 1px dashed var(--border);
  border-radius: var(--radius-lg);
}

.graph-state-error {
  color: #b91c1c;
  background: rgba(239, 68, 68, 0.06);
  border-color: rgba(239, 68, 68, 0.3);
}

.graph-spinner {
  width: 40px;
  height: 40px;
  margin: 0 auto 16px;
  border: 3px solid var(--border);
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

/* 图谱画布 */
.graph-card {
  padding: 16px;
}

.graph-canvas {
  width: 100%;
  height: 600px;
}

/* 规模统计 */
.graph-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-top: 18px;
}

.graph-stat {
  padding: 16px 18px;
  text-align: center;
}

.graph-stat-label {
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 6px;
}

.graph-stat-value {
  font-family: var(--font-heading);
  font-size: 26px;
  font-weight: 700;
  color: var(--accent);
  line-height: 1;
}

.graph-actions {
  text-align: center;
  margin-top: 26px;
}

@media (max-width: 720px) {
  .graph-stats {
    grid-template-columns: repeat(2, 1fr);
  }
  .graph-canvas {
    height: 420px;
  }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
