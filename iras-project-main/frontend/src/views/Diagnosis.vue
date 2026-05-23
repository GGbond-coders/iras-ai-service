<!--
  @file Diagnosis.vue
  @description 智能简历诊断页面组件。
               用户上传简历文件后，调用 Dify AI 接口进行深度分析，
               匹配最适合的岗位并生成详细的诊断报告。
               报告包含匹配岗位列表、匹配分数、匹配原因、差距分析和面试建议。
               支持展示 AI 推理过程（think 标签内容）。
  @author IRAS Team
  @since 1.0
-->
<template>
  <div class="diagnosis-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>智能简历诊断</span>
        </div>
      </template>

      <!-- 文件上传区域 -->
      <div class="upload-section">
        <!-- 未选择文件时：显示拖拽上传区域 -->
        <el-upload
          v-if="!selectedFile"
          ref="uploadRef"
          class="resume-upload"
          drag
          :auto-upload="false"
          :limit="1"
          :on-change="handleFileChange"
          :on-exceed="handleExceed"
          accept=".txt,.text,.pdf,.doc,.docx"
        >
          <div class="el-upload__text">
            将简历文件拖到此处，或 <em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              支持 .txt / .text / .pdf / .doc / .docx 格式，文件大小不超过 10MB
            </div>
          </template>
        </el-upload>

        <!-- 已选择文件时：显示文件信息和重新上传按钮 -->
        <div v-else class="file-selected">
          <div class="file-info">
            <span class="file-name">{{ selectedFile.name }}</span>
            <span class="file-size">({{ (selectedFile.size / 1024).toFixed(1) }} KB)</span>
          </div>
          <el-button size="small" @click="reUpload" :disabled="loading">重新上传</el-button>
        </div>

        <!-- 操作按钮区域 -->
        <div class="action-bar">
          <el-button type="primary" size="large" :loading="loading" :disabled="!selectedFile" @click="diagnose">
            {{ loading ? 'AI 诊断中...' : '开始诊断' }}
          </el-button>
          <el-button size="large" @click="clearAll" :disabled="loading">清空</el-button>
        </div>
        <p class="input-tip">上传简历文件后，AI 将为您匹配最适合的岗位并生成详细的诊断报告（约需 3 分钟）</p>
      </div>

      <!-- AI 分析中加载提示 -->
      <div v-if="loading" class="loading-section">
        <p class="loading-text">AI 正在深度分析您的简历并与岗位库进行匹配，请耐心等待...</p>
      </div>

      <!-- 诊断结果展示区域 -->
      <div v-if="diagnosisResult && !loading" class="result-section">
        <el-divider />

        <!-- AI 推理过程（默认隐藏，可展开查看） -->
        <div v-if="thinkContent" class="think-section">
          <el-button text type="info" @click="showThink = !showThink">
            {{ showThink ? '隐藏推理过程' : '展示推理过程' }}
          </el-button>
          <el-collapse-transition>
            <div v-show="showThink" class="think-content">
              <pre>{{ thinkContent }}</pre>
            </div>
          </el-collapse-transition>
        </div>

        <!-- 匹配结果列表（JSON 格式） -->
        <div v-if="matches.length > 0">
          <h3 class="result-title">诊断报告 — 共匹配到 {{ matches.length }} 个岗位</h3>

          <!-- 遍历每个匹配岗位 -->
          <div v-for="(match, index) in matches" :key="index" class="match-card">
            <el-card shadow="hover">
              <!-- 匹配头部：岗位名称 + 匹配分数 -->
              <div class="match-header">
                <div class="match-info">
                  <h3>{{ match.matched_job }}</h3>
                  <div class="match-meta">
                    <el-tag type="primary" effect="plain">{{ match.type || '全职' }}</el-tag>
                    <el-tag type="warning" effect="plain" v-if="match.company">{{ match.company }}</el-tag>
                    <el-tag effect="plain" v-if="match.city">{{ match.city }}</el-tag>
                  </div>
                </div>
                <!-- 匹配分数（颜色根据分数变化） -->
                <div class="match-score">
                  <span class="score-value" :style="{ color: getScoreColor(match.matched_score) }">{{ match.matched_score }}%</span>
                  <span class="score-label">匹配分</span>
                </div>
              </div>

              <!-- 薪资信息 -->
              <div class="salary-row" v-if="match.salary">
                <span class="salary-label">薪资：</span>
                <span class="salary-value">{{ match.salary }}</span>
              </div>

              <!-- 匹配原因 -->
              <div class="section-block">
                <h4>匹配原因</h4>
                <p>{{ match.matched_reason }}</p>
              </div>

              <!-- 差距分析 -->
              <div class="section-block" v-if="match.gap_points && match.gap_points.length">
                <h4>差距分析</h4>
                <ul class="gap-list">
                  <li v-for="(gap, i) in match.gap_points" :key="i">{{ gap }}</li>
                </ul>
              </div>

              <!-- 面试建议 -->
              <div class="section-block" v-if="match.interview_advice && match.interview_advice.length">
                <h4>面试建议</h4>
                <ul class="advice-list">
                  <li v-for="(advice, i) in match.interview_advice" :key="i">{{ advice }}</li>
                </ul>
              </div>
            </el-card>
          </div>
        </div>

        <!-- 纯文本结果（非 JSON 格式时降级展示） -->
        <div v-else-if="rawResult" class="raw-result">
          <h3>诊断报告</h3>
          <!-- 使用 marked 库将 Markdown 渲染为 HTML -->
          <div class="markdown-body" v-html="renderedResult"></div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
/**
 * 智能简历诊断页面逻辑。
 * <p>
 * 功能：
 * <ul>
 *   <li>简历文件上传（支持拖拽和点击上传）</li>
 *   <li>调用 Dify AI 接口进行简历诊断</li>
 *   <li>解析 AI 返回的 JSON 结构化数据或 Markdown 文本</li>
 *   <li>支持展示 AI 推理过程（think 标签）</li>
 * </ul>
 * </p>
 */
import { ref, computed } from 'vue'
import { difyApi } from '../api'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'

/** 上传组件引用 */
const uploadRef = ref(null)

/** 已选择的文件对象 */
const selectedFile = ref(null)

/** AI 诊断加载状态 */
const loading = ref(false)

/** 诊断结果是否已返回 */
const diagnosisResult = ref(false)

/** 结构化匹配结果列表 */
const matches = ref([])

/** 非结构化纯文本结果（降级方案） */
const rawResult = ref('')

/** AI 推理过程内容（think 标签） */
const thinkContent = ref('')

/** 是否展示推理过程 */
const showThink = ref(false)

/**
 * 将 Markdown 文本渲染为 HTML。
 * @returns {string} 渲染后的 HTML 字符串
 */
const renderedResult = computed(() => {
  return rawResult.value ? marked(rawResult.value) : ''
})

/**
 * 根据匹配分数返回对应颜色。
 * <p>
 * 颜色规则：
 * <ul>
 *   <li>≥ 80 - 绿色（高匹配）</li>
 *   <li>≥ 60 - 橙色（中等匹配）</li>
 *   <li>&lt; 60 - 红色（低匹配）</li>
 * </ul>
 * </p>
 *
 * @param {number} score - 匹配分数（0-100）
 * @returns {string} CSS 颜色值
 */
function getScoreColor(score) {
  if (score >= 80) return '#67c23a'
  if (score >= 60) return '#e6a23c'
  return '#f56c6c'
}

/**
 * 处理文件选择事件。
 * @param {Object} file - Element Plus 上传组件的文件对象
 */
function handleFileChange(file) {
  selectedFile.value = file.raw
}

/**
 * 处理超出文件数量限制事件。
 */
function handleExceed() {
  ElMessage.warning('只能上传一个文件，请先移除已选文件')
}

/**
 * 重新上传（清空已选文件）。
 */
function reUpload() {
  selectedFile.value = null
  uploadRef.value?.clearFiles()
}

/**
 * 执行 AI 简历诊断。
 * <p>
 * 流程：
 * 1. 调用 Dify 诊断 API（上传文件）
 * 2. 提取 <think> 标签中的推理过程
 * 3. 尝试解析 JSON 结构化数据
 * 4. 如果非 JSON，降级为纯文本 Markdown 展示
 * </p>
 */
async function diagnose() {
  if (!selectedFile.value) {
    ElMessage.warning('请先上传简历文件')
    return
  }

  // 重置状态
  loading.value = true
  diagnosisResult.value = false
  matches.value = []
  rawResult.value = ''
  thinkContent.value = ''

  try {
    // 调用 Dify 诊断 API
    const res = await difyApi.diagnoseResume(selectedFile.value)
    let data = res.data

    // 提取 <think> 标签中的 AI 推理过程内容
    const thinkMatch = data.match(/<think>([\s\S]*?)<\/think>/)
    if (thinkMatch) {
      thinkContent.value = thinkMatch[1].trim()
      // 移除 think 标签，保留有效内容
      data = data.replace(/<think>[\s\S]*?<\/think>/, '').trim()
    }

    // 尝试解析为 JSON 结构化数据
    try {
      const parsed = JSON.parse(data)
      if (Array.isArray(parsed)) {
        // 直接是数组格式（匹配结果列表）
        matches.value = parsed
      } else if (parsed.matches) {
        // 包含 matches 字段的对象
        matches.value = parsed.matches
      } else if (parsed.result) {
        // 包含 result 字段（可能嵌套 think 标签和 JSON）
        const inner = parsed.result
        const innerThink = inner.match(/<think>([\s\S]*?)<\/think>/)
        if (innerThink) {
          thinkContent.value = innerThink[1].trim()
        }
        const cleanJson = inner.replace(/<think>[\s\S]*?<\/think>/, '').trim()
        try {
          const innerParsed = JSON.parse(cleanJson)
          matches.value = Array.isArray(innerParsed) ? innerParsed : [innerParsed]
        } catch {
          // 内层非 JSON，降级为纯文本
          rawResult.value = cleanJson
        }
      } else {
        // 单个对象，包装为数组
        matches.value = [parsed]
      }
    } catch {
      // 非 JSON 格式，降级为纯文本展示
      rawResult.value = data
    }

    diagnosisResult.value = true
    ElMessage.success('诊断完成！')
  } catch (e) {
    ElMessage.error('诊断失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

/**
 * 清空所有状态（重置页面）。
 */
function clearAll() {
  selectedFile.value = null
  uploadRef.value?.clearFiles()
  diagnosisResult.value = false
  matches.value = []
  rawResult.value = ''
  thinkContent.value = ''
}
</script>

<style scoped>
.diagnosis-page {
  max-width: 1000px;
}

.card-header {
  font-size: 16px;
  font-weight: 600;
}

/* 上传区域居中 */
.upload-section {
  max-width: 600px;
  margin: 0 auto;
}

.resume-upload {
  width: 100%;
}

.resume-upload :deep(.el-upload-dragger) {
  padding: 40px 20px;
}

/* 已选文件信息区域 */
.file-selected {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: #f5f7fa;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.file-size {
  font-size: 13px;
  color: #909399;
}

/* 操作按钮区域 */
.action-bar {
  margin-top: 20px;
  display: flex;
  gap: 12px;
  justify-content: center;
}

.input-tip {
  margin-top: 10px;
  color: #909399;
  font-size: 13px;
  text-align: center;
}

/* 加载提示 */
.loading-section {
  margin-top: 40px;
  text-align: center;
}

.loading-text {
  margin-top: 16px;
  color: #909399;
}

/* 结果标题 */
.result-title {
  font-size: 20px;
  color: #303133;
  margin-bottom: 20px;
}

.match-card {
  margin-bottom: 20px;
}

/* 匹配头部布局 */
.match-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.match-info h3 {
  font-size: 20px;
  color: #303133;
  margin-bottom: 8px;
}

.match-meta {
  display: flex;
  gap: 8px;
}

/* 匹配分数样式 */
.match-score {
  text-align: center;
}

.score-value {
  font-size: 28px;
  font-weight: 700;
}

.score-label {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

/* 薪资行样式 */
.salary-row {
  padding: 12px 16px;
  background: #fdf6ec;
  border-radius: 8px;
  margin-bottom: 20px;
}

.salary-label {
  color: #909399;
}

.salary-value {
  font-size: 18px;
  font-weight: 700;
  color: #f56c6c;
}

/* 内容区块 */
.section-block {
  margin-top: 20px;
}

.section-block h4 {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  color: #303133;
  margin-bottom: 12px;
}

.section-block > p {
  font-size: 14px;
  color: #606266;
  line-height: 1.8;
  padding-left: 24px;
}

/* 列表样式（差距分析、面试建议） */
.gap-list, .advice-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.gap-list li, .advice-list li {
  position: relative;
  padding: 8px 0 8px 24px;
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  border-bottom: 1px solid #f5f5f5;
}

.gap-list li::before {
  content: '';
  position: absolute;
  left: 0;
}

.advice-list li::before {
  content: '';
  position: absolute;
  left: 0;
}

/* 推理过程区域 */
.think-section {
  margin-bottom: 20px;
}

.think-content {
  margin-top: 12px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  border: 1px dashed #dcdfe6;
}

.think-content pre {
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 13px;
  color: #909399;
  line-height: 1.6;
  margin: 0;
}

/* 纯文本结果区域 */
.raw-result {
  margin-top: 20px;
}

.raw-result h3 {
  margin-bottom: 16px;
}

.markdown-body {
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  line-height: 1.8;
}
</style>
