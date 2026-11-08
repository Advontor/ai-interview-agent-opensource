<template>
  <div class="upload-page">
    <!-- 页面标题区（面试准备 · 第一步） -->
    <PageHeader
      tag="简历解析"
      title="先让 AI"
      highlight="读懂"
      suffix="你的简历"
      description="上传简历后，我们会按教育、实习、项目、技能和岗位匹配度拆解，并把结果带入下一步面试配置。"
    />

    <!-- 上传卡片 -->
    <div class="upload-card card animate-reveal animate-reveal-delay-1">
      <el-upload
        class="upload-area"
        drag
        action=""
        :auto-upload="false"
        :on-change="handleFileChange"
        :limit="1"
        :on-exceed="handleExceed"
        accept=".pdf,.docx"
        :show-file-list="false"
      >
        <div class="upload-inner">
          <div class="upload-icon">
            <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
              <polyline points="17 8 12 3 7 8"/>
              <line x1="12" y1="3" x2="12" y2="15"/>
            </svg>
          </div>
          <div class="upload-title">点击或拖拽上传简历</div>
          <div class="upload-formats">
            <span class="format-badge">PDF</span>
            <span class="format-badge">DOCX</span>
            <span class="upload-hint">最大 10MB</span>
          </div>
        </div>
      </el-upload>

      <!-- 已选文件 -->
      <transition name="fade-slide">
        <div v-if="selectedFile" class="file-info">
          <div class="file-icon">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
              <polyline points="14 2 14 8 20 8"/>
            </svg>
          </div>
          <div class="file-meta">
            <div class="file-name">{{ selectedFile.name }}</div>
            <div class="file-size">{{ formatFileSize(selectedFile.size) }}</div>
          </div>
          <button class="file-remove" @click="clearFile" title="移除">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
      </transition>

      <!-- 上传按钮 -->
      <div class="upload-actions">
        <button
          class="btn btn-primary btn-large"
          :class="{ disabled: !selectedFile || uploading }"
          :disabled="!selectedFile || uploading"
          @click="handleUpload"
        >
          <svg v-if="!uploading" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M5 12h14"/>
            <path d="M12 5l7 7-7 7"/>
          </svg>
          <span v-if="uploading" class="loading-spinner"></span>
          {{ uploading ? '正在解析简历...' : '开始解析简历' }}
        </button>
      </div>
    </div>

    <!-- 特性说明 -->
    <div class="features animate-reveal animate-reveal-delay-2">
      <div class="feature-item">
        <div class="feature-num">01</div>
        <div class="feature-text">
          <div class="feature-title">智能解析</div>
          <div class="feature-desc">AI 自动提取教育、实习、项目经历</div>
        </div>
      </div>
      <div class="feature-item">
        <div class="feature-num">02</div>
        <div class="feature-text">
          <div class="feature-title">岗位匹配</div>
          <div class="feature-desc">根据目标岗位评估简历匹配度</div>
        </div>
      </div>
      <div class="feature-item">
        <div class="feature-num">03</div>
        <div class="feature-text">
          <div class="feature-title">个性化面试</div>
          <div class="feature-desc">基于简历生成专属面试问题</div>
        </div>
      </div>
    </div>

    <!-- 结果提示 -->
    <transition name="fade-slide">
      <div v-if="uploadResult" class="result-alert" :class="{ success: uploadSuccess, error: !uploadSuccess }">
        <div class="result-icon">
          <svg v-if="uploadSuccess" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
            <polyline points="22 4 12 14.01 9 11.01"/>
          </svg>
          <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"/>
            <line x1="15" y1="9" x2="9" y2="15"/>
            <line x1="9" y1="9" x2="15" y2="15"/>
          </svg>
        </div>
        <span>{{ uploadResult }}</span>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadResume } from '../api/resume'
import type { UploadFile } from 'element-plus'
import PageHeader from '../components/PageHeader.vue'

// 本组件是「面试准备」流程的第一步（简历解析），
// 解析成功后通过 parsed 事件交给 PreparePage 切到第二步。
const emit = defineEmits<{ (e: 'parsed'): void }>()

const selectedFile = ref<File | null>(null)
const uploading = ref(false)
const uploadResult = ref('')
const uploadSuccess = ref(false)

const handleFileChange = (file: UploadFile) => {
  if (file.size && file.size > 10 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过 10MB')
    return
  }
  selectedFile.value = file.raw as File
  uploadResult.value = ''
}

const handleExceed = () => {
  ElMessage.warning('只能上传一个文件，请先移除当前文件')
}

const clearFile = () => {
  selectedFile.value = null
  uploadResult.value = ''
}

const formatFileSize = (bytes: number): string => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

const handleUpload = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }

  uploading.value = true
  uploadResult.value = ''

  try {
    const resumeData = await uploadResume(selectedFile.value)
    // 保存解析结果到 sessionStorage，供第二步（面试配置）读取
    sessionStorage.setItem('resumeParseResult', JSON.stringify(resumeData))
    uploadSuccess.value = true
    uploadResult.value = '简历解析成功！正在进入下一步...'

    setTimeout(() => {
      emit('parsed')
    }, 900)
  } catch (error: any) {
    uploadSuccess.value = false
    uploadResult.value = error?.message || '上传失败，请稍后重试'
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.upload-page {
  max-width: 720px;
  margin: 0 auto;
  padding-top: 8px;
}

/* 上传卡片 */
.upload-card {
  padding: 36px;
  margin-bottom: 28px;
}

.upload-area {
  margin-bottom: 0;
}

.upload-area :deep(.el-upload-dragger) {
  width: 100%;
  padding: 0;
  border: 2px dashed var(--accent-border);
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, var(--accent-bg) 0%, var(--bg-elevated) 100%);
  transition: all var(--transition-normal);
  position: relative;
  overflow: hidden;
}

.upload-area :deep(.el-upload-dragger)::before {
  content: '';
  position: absolute;
  top: -40%;
  right: -10%;
  width: 180px;
  height: 180px;
  background: radial-gradient(circle, rgba(168, 213, 186, 0.25) 0%, transparent 70%);
  border-radius: 50%;
}

.upload-area :deep(.el-upload-dragger:hover) {
  border-color: var(--accent);
  background: linear-gradient(135deg, var(--accent-bg) 0%, var(--bg-card) 100%);
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.upload-inner {
  padding: 48px 24px;
  text-align: center;
}

.upload-icon {
  width: 72px;
  height: 72px;
  margin: 0 auto 20px;
  border-radius: 50%;
  background: var(--accent-bg);
  color: var(--accent);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-normal);
}

.upload-area :deep(.el-upload-dragger:hover) .upload-icon {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(45, 90, 61, 0.15);
}

.upload-title {
  font-size: 17px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 10px;
}

.upload-formats {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.format-badge {
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 6px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  color: var(--text-secondary);
}

.upload-hint {
  font-size: 12px;
  color: var(--text-muted);
}

/* 文件信息 */
.file-info {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  background: var(--bg-elevated);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  margin-top: 20px;
}

.file-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-sm);
  background: var(--accent-bg);
  color: var(--accent);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.file-meta {
  flex: 1;
  min-width: 0;
}

.file-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-size {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 2px;
}

.file-remove {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  color: var(--text-muted);
  border-radius: var(--radius-sm);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
  flex-shrink: 0;
}

.file-remove:hover {
  background: rgba(220, 53, 69, 0.1);
  color: #dc3545;
}

/* 上传按钮 */
.upload-actions {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

.btn-large {
  padding: 14px 40px;
  font-size: 15px;
  border-radius: var(--radius-md);
}

.btn.disabled {
  opacity: 0.4;
  cursor: not-allowed;
  transform: none !important;
  box-shadow: none !important;
}

.loading-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 特性说明 */
.features {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.feature-item {
  display: flex;
  gap: 14px;
  padding: 20px;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  transition: all var(--transition-normal);
}

.feature-item:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
  border-color: var(--accent-border);
}

.feature-num {
  font-family: var(--font-heading);
  font-size: 24px;
  font-weight: 700;
  color: var(--accent-light);
  line-height: 1;
  flex-shrink: 0;
}

.feature-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.feature-desc {
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.5;
}

/* 结果提示 */
.result-alert {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  border-radius: var(--radius-md);
  margin-top: 20px;
  font-size: 14px;
  font-weight: 500;
}

.result-alert.success {
  background: rgba(45, 90, 61, 0.08);
  color: var(--accent);
  border: 1px solid var(--accent-border);
}

.result-alert.error {
  background: rgba(220, 53, 69, 0.06);
  color: #dc3545;
  border: 1px solid rgba(220, 53, 69, 0.2);
}

/* 动画 */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.3s ease;
}

.fade-slide-enter-from,
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* 响应式 */
@media (max-width: 640px) {
  .features {
    grid-template-columns: 1fr;
  }
  .upload-card {
    padding: 24px;
  }
}
</style>
