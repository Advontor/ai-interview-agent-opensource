<template>
  <main class="graph-page">
    <div class="head"><div><p class="eyebrow">INTERVIEW KNOWLEDGE GRAPH</p><h1>面试知识图谱</h1><p>把简历技能、面试覆盖点和待加强主题连成一张可复盘的图。</p></div><el-button @click="router.back()">返回</el-button></div>
    <el-alert v-if="error" :title="error" type="error" show-icon />
    <el-card v-if="loading" class="state">正在加载知识图谱...</el-card>
    <template v-if="!loading && !error"><el-card shadow="never"><div ref="chartRef" class="chart"></div></el-card><el-card shadow="never" class="coverage"><template #header><b>覆盖和待加强主题</b></template><el-tag v-for="(count, topic) in graph?.coverage" :key="topic" :type="count > 0 ? 'success' : 'warning'" effect="plain">{{ topic }} · {{ count > 0 ? '已覆盖' : '待加强' }}</el-tag><el-empty v-if="!Object.keys(graph?.coverage || {}).length" description="暂无主题数据" /></el-card></template>
  </main>
</template>
<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { getKnowledgeGraph, type KnowledgeGraphResult } from '../api/knowledgeGraph'
import { formatApiError } from '../utils/errorMessage'
const route = useRoute(); const router = useRouter(); const loading = ref(true); const error = ref(''); const graph = ref<KnowledgeGraphResult>(); const chartRef = ref<HTMLElement>(); let chart: echarts.ECharts | undefined
async function load() { const id = Number(route.query.sessionId || sessionStorage.getItem('interviewSessionId')); if (!id) { error.value = '缺少面试会话信息'; loading.value = false; return } try { graph.value = await getKnowledgeGraph(id); await nextTick(); if (chartRef.value) { chart = echarts.init(chartRef.value); chart.setOption({ tooltip: {}, legend: [{ data: [...new Set((graph.value.nodes || []).map(n => n.category))] }], series: [{ type: 'graph', layout: 'force', roam: true, draggable: true, data: graph.value.nodes, links: graph.value.edges, categories: [{ name: 'role' }, { name: 'skill' }, { name: 'project' }, { name: 'topic' }, { name: 'weak' }, { name: 'question' }], label: { show: true, position: 'right' }, force: { repulsion: 260, edgeLength: 100 } }] }) } } catch (e) { error.value = formatApiError(e, '知识图谱加载失败') } finally { loading.value = false } }
onMounted(load); onBeforeUnmount(() => chart?.dispose())
</script>
<style scoped>.graph-page{max-width:1180px;margin:0 auto;padding:35px 24px}.head{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:20px}.eyebrow{color:#6374f1;font-size:12px;font-weight:700;letter-spacing:.14em}.head h1{margin:8px 0}.head p{color:#718096}.chart{height:600px}.coverage{margin-top:18px}.coverage .el-tag{margin:0 8px 8px 0}.state{text-align:center;padding:40px}</style>
