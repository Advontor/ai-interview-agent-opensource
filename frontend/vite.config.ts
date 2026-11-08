import vue from '@vitejs/plugin-vue'
import { defineConfig, loadEnv } from 'vite'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  // 联调：把 /api 代理到后端
  // 默认本机 8080，也可以通过 .env 中的 VITE_API_TARGET 覆盖
  const apiTarget = env.VITE_API_TARGET || 'http://localhost:8080'

  return {
    plugins: [vue()],

    build: {
      // 单个 chunk 超过 800KB 时提示（默认 500KB 对本项目过于严格）
      chunkSizeWarningLimit: 800,
      rollupOptions: {
        output: {
          // 把体积大、改动少的第三方依赖拆成独立 chunk：
          // 首屏只需要 vue/element-plus + 当前页面 chunk，其余页面按需加载，
          // 且依赖 chunk 可长期命中浏览器缓存。
          manualChunks(id) {
            if (!id.includes('node_modules')) return
            if (id.includes('element-plus')) return 'element-plus'
            if (id.includes('echarts') || id.includes('zrender')) return 'echarts'
            if (id.includes('@vue') || id.includes('/vue/') || id.includes('pinia') || id.includes('vue-router')) {
              return 'vue'
            }
            return 'vendor'
          },
        },
      },
    },

    server: {
      proxy: {
        '/api': {
          target: apiTarget,
          changeOrigin: true,
          ws: true,
        },
      },
    },
  }
})
