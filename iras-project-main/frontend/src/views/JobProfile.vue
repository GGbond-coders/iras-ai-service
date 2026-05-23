<!--
  @file JobProfile.vue
  @description 职能画像分析页面组件。
               用户输入职位名称后，调用 Dify AI 接口生成该职位的完整画像，
               包括硬技能、软技能、常用工具、经验与学历要求等维度的分析结果。
               分析结果以卡片网格形式展示，约需 2 分钟完成。
  @author IRAS Team
  @since 1.0
-->
<template>
  <div class="profile-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>职能画像分析</span>
        </div>
      </template>

      <!-- 输入区域 -->
      <div class="input-section">
        <el-input
          v-model="jobName"
          placeholder="请输入职位名称，如：软件工程师、产品经理、数据分析师..."
          size="large"
          clearable
          @keyup.enter="analyze"
        >
          <template #prepend>职位名称</template>
          <template #append>
            <el-button type="primary" :loading="loading" @click="analyze">
              {{ loading ? 'AI 分析中...' : '开始分析' }}
            </el-button>
          </template>
        </el-input>
        <p class="input-tip">AI 将为您生成该职位的技能要求、工具清单、经验要求等完整画像（约需 2 分钟）</p>
      </div>

      <!-- AI 分析中加载提示 -->
      <div v-if="loading" class="loading-section">
        <p class="loading-text">AI 正在深度分析职位画像，请耐心等待...</p>
      </div>

      <!-- 分析结果展示区域 -->
      <div v-if="profileData && !loading" class="result-section">
        <el-divider />

        <!-- 画像标题 -->
        <div class="profile-header">
          <h2>{{ profileData.job_title }}</h2>
          <el-tag type="success" size="large">分析完成</el-tag>
        </div>

        <!-- 第一行：硬技能 + 软技能 -->
        <el-row :gutter="20" style="margin-top: 24px">
          <!-- 硬技能卡片 -->
          <el-col :span="12">
            <el-card class="skill-card" shadow="hover">
              <template #header>
                <div class="skill-header">
                  <span>硬技能要求</span>
                </div>
              </template>
              <ul class="skill-list">
                <li v-for="(skill, i) in profileData.hard_skills" :key="i">
                  {{ skill }}
                </li>
              </ul>
            </el-card>
          </el-col>

          <!-- 软技能卡片 -->
          <el-col :span="12">
            <el-card class="skill-card" shadow="hover">
              <template #header>
                <div class="skill-header">
                  <span>软技能要求</span>
                </div>
              </template>
              <ul class="skill-list">
                <li v-for="(skill, i) in profileData.soft_skills" :key="i">
                  {{ skill }}
                </li>
              </ul>
            </el-card>
          </el-col>
        </el-row>

        <!-- 第二行：工具 + 经验学历 -->
        <el-row :gutter="20" style="margin-top: 20px">
          <!-- 常用工具卡片 -->
          <el-col :span="12">
            <el-card class="skill-card" shadow="hover">
              <template #header>
                <div class="skill-header">
                  <span>常用工具</span>
                </div>
              </template>
              <div class="tool-tags">
                <el-tag v-for="(tool, i) in profileData.tools" :key="i" type="info" effect="plain" class="tool-tag">
                  {{ tool }}
                </el-tag>
              </div>
            </el-card>
          </el-col>

          <!-- 经验与学历要求卡片 -->
          <el-col :span="12">
            <el-card class="skill-card" shadow="hover">
              <template #header>
                <div class="skill-header">
                  <span>经验与学历</span>
                </div>
              </template>
              <div class="exp-section">
                <h4>工作经验</h4>
                <p>{{ profileData.experience }}</p>
              </div>
              <div class="exp-section" style="margin-top: 16px">
                <h4>学历要求</h4>
                <p>{{ profileData.education }}</p>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </el-card>
  </div>
</template>

<script setup>
/**
 * 职能画像分析页面逻辑。
 * <p>
 * 功能：
 * <ul>
 *   <li>接收用户输入的职位名称</li>
 *   <li>调用 Dify AI 接口进行职能画像分析</li>
 *   <li>解析 AI 返回的 JSON 数据并展示</li>
 * </ul>
 * </p>
 */
import { ref } from 'vue'
import { difyApi } from '../api'
import { ElMessage } from 'element-plus'

/** 用户输入的职位名称 */
const jobName = ref('')

/** AI 分析加载状态 */
const loading = ref(false)

/**
 * AI 返回的职能画像数据。
 * <p>
 * 结构示例：
 * <pre>
 * {
 *   job_title: "软件工程师",
 *   hard_skills: ["Java", "Spring", ...],
 *   soft_skills: ["沟通能力", "团队协作", ...],
 *   tools: ["IDEA", "Git", ...],
 *   experience: "3-5年",
 *   education: "本科及以上"
 * }
 * </pre>
 * </p>
 */
const profileData = ref(null)

/**
 * 执行 AI 职能画像分析。
 * <p>
 * 流程：校验输入 -> 调用 API -> 解析 JSON 结果 -> 展示数据
 * </p>
 */
async function analyze() {
  // 校验输入是否为空
  if (!jobName.value.trim()) {
    ElMessage.warning('请输入职位名称')
    return
  }

  loading.value = true
  profileData.value = null

  try {
    // 调用 Dify 职能画像 API
    const res = await difyApi.getJobProfile(jobName.value.trim())
    // 解析返回的 JSON 字符串（AI 返回的可能是字符串格式的 JSON）
    const data = typeof res.data === 'string' ? JSON.parse(res.data) : res.data
    profileData.value = data
    ElMessage.success('分析完成！')
  } catch (e) {
    ElMessage.error('分析失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.profile-page {
  max-width: 1200px;
}

.card-header {
  font-size: 16px;
  font-weight: 600;
}

/* 输入区域居中 */
.input-section {
  max-width: 800px;
  margin: 0 auto;
}

.input-tip {
  margin-top: 10px;
  color: #909399;
  font-size: 13px;
  text-align: center;
}

/* 加载提示区域 */
.loading-section {
  margin-top: 40px;
  text-align: center;
}

.loading-text {
  margin-top: 16px;
  color: #909399;
  font-size: 14px;
}

/* 画像标题区域 */
.profile-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.profile-header h2 {
  font-size: 24px;
  color: #303133;
}

/* 技能卡片样式 */
.skill-card {
  height: 100%;
}

.skill-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 15px;
}

/* 技能列表样式 */
.skill-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.skill-list li {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 14px;
  color: #606266;
  line-height: 1.5;
}

.skill-list li:last-child {
  border-bottom: none;
}

/* 工具标签容器 */
.tool-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tool-tag {
  font-size: 13px;
}

/* 经验与学历区块 */
.exp-section h4 {
  font-size: 14px;
  color: #303133;
  margin-bottom: 8px;
}

.exp-section p {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}
</style>
