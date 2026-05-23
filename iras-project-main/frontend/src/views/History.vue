<!--
  @file History.vue
  @description 诊断历史记录页面组件。
               展示当前用户的所有简历诊断历史，支持分页浏览、
               查看详情和删除记录。
  @author IRAS Team
  @since 1.0
-->
<template>
  <div class="history-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>诊断历史</span>
        </div>
      </template>

      <!-- 历史记录表格 -->
      <el-table :data="records" stripe style="width: 100%" v-loading="loading" empty-text="暂无诊断记录">
        <el-table-column prop="resumeFilename" label="简历文件" min-width="200" />
        <el-table-column prop="matchCount" label="匹配岗位数" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.matchCount > 0 ? 'success' : 'info'" size="small">
              {{ row.matchCount }} 个
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="诊断时间" width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewDetail(row)">查看详情</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="fetchHistory"
          @size-change="fetchHistory"
        />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="诊断详情" width="900px" top="5vh">
      <div v-if="currentRecord" class="detail-content">
        <el-descriptions :column="2" border style="margin-bottom: 20px">
          <el-descriptions-item label="简历文件">{{ currentRecord.resumeFilename }}</el-descriptions-item>
          <el-descriptions-item label="诊断时间">{{ currentRecord.createTime }}</el-descriptions-item>
          <el-descriptions-item label="匹配岗位数">{{ currentRecord.matchCount }} 个</el-descriptions-item>
        </el-descriptions>

        <!-- 推理过程 -->
        <div v-if="currentRecord.thinkContent" class="think-section">
          <el-button text type="info" @click="showThink = !showThink">
            {{ showThink ? '隐藏推理过程' : '展示推理过程' }}
          </el-button>
          <el-collapse-transition>
            <div v-show="showThink" class="think-content">
              <pre>{{ currentRecord.thinkContent }}</pre>
            </div>
          </el-collapse-transition>
        </div>

        <!-- 诊断结果 -->
        <div v-if="diagnosisData.length > 0">
          <h3 style="margin-bottom: 16px">匹配结果</h3>
          <div v-for="(match, index) in diagnosisData" :key="index" class="match-card">
            <el-card shadow="hover">
              <div class="match-header">
                <h3>{{ match.matched_job }}</h3>
                <span class="score" :style="{ color: getScoreColor(match.matched_score) }">
                  {{ match.matched_score }}%
                </span>
              </div>
              <p v-if="match.matched_reason" class="reason">{{ match.matched_reason }}</p>

              <div v-if="match.gap_points && match.gap_points.length" class="gap-section">
                <h4>差距分析</h4>
                <ul>
                  <li v-for="(gap, i) in match.gap_points" :key="i">{{ gap }}</li>
                </ul>
              </div>

              <div v-if="match.interview_advice && match.interview_advice.length" class="interview-section">
                <h4>面试建议</h4>
                <ul>
                  <li v-for="(advice, i) in match.interview_advice" :key="i">{{  advice }}</li>
                </ul>
              </div>

            </el-card>
          </div>
        </div>

        <!-- 纯文本结果降级展示 -->
        <div v-else-if="rawResult" class="raw-result">
          <div class="markdown-body" v-html="renderedResult"></div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onActivated } from 'vue'
import { diagnosisApi } from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { marked } from 'marked'

const loading = ref(false)
const records = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)

const detailVisible = ref(false)
const currentRecord = ref(null)
const diagnosisData = ref([])
const rawResult = ref('')
const showThink = ref(false)

const renderedResult = computed(() => rawResult.value ? marked(rawResult.value) : '')

function getScoreColor(score) {
  if (score >= 80) return '#67c23a'
  if (score >= 60) return '#e6a23c'
  return '#f56c6c'
}

async function fetchHistory() {
  loading.value = true
  try {
    const res = await diagnosisApi.getHistory({ page: page.value, size: size.value })
    records.value = res.data.records
    total.value = res.data.total
  } catch (e) {
    // handled
  } finally {
    loading.value = false
  }
}

async function viewDetail(row) {
  try {
    const res = await diagnosisApi.getDetail(row.id)
    currentRecord.value = res.data
    parseResult(res.data.diagnosisResult)
    detailVisible.value = true
  } catch (e) {
    ElMessage.error('获取详情失败')
  }
}

function parseResult(data) {
  diagnosisData.value = []
  rawResult.value = ''
  if (!data) return

  try {
    const parsed = JSON.parse(data)
    if (Array.isArray(parsed)) {
      diagnosisData.value = parsed
    } else if (parsed.matches) {
      diagnosisData.value = parsed.matches
    } else if (parsed.result) {
      // {"result": "[{...}]"} 包装格式，解包内层 JSON
      const inner = parsed.result
      const cleanJson = inner.replace(/<think>[\s\S]*?<\/think>/, '').trim()
      try {
        const innerParsed = JSON.parse(cleanJson)
        diagnosisData.value = Array.isArray(innerParsed) ? innerParsed : [innerParsed]
      } catch {
        rawResult.value = cleanJson
      }
    } else {
      diagnosisData.value = [parsed]
    }
  } catch {
    rawResult.value = data
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定删除该诊断记录？', '提示', { type: 'warning' })
    await diagnosisApi.deleteRecord(row.id)
    ElMessage.success('删除成功')
    fetchHistory()
  } catch (e) {
    // cancelled or error
  }
}

onActivated(() => {
  fetchHistory()
})
</script>

<style scoped>
.history-page {
  max-width: 1200px;
}

.card-header {
  font-size: 16px;
  font-weight: 600;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.detail-content {
  max-height: 70vh;
  overflow-y: auto;
}

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

.match-card {
  margin-bottom: 16px;
}

.match-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.match-header h3 {
  font-size: 18px;
  color: #303133;
  margin: 0;
}

.score {
  font-size: 24px;
  font-weight: 700;
}

.reason {
  font-size: 14px;
  color: #606266;
  line-height: 1.8;
  margin-bottom: 12px;
}

.gap-section h4 {
  font-size: 14px;
  color: #303133;
  margin-bottom: 8px;
}

.gap-section ul {
  padding-left: 20px;
  margin: 0;
}

.gap-section li {
  font-size: 14px;
  color: #606266;
  line-height: 1.8;
}

.interview-section h4 {
  font-size: 14px;
  color: #303133;
  margin-bottom: 8px;
}

.interview-section ul {
  padding-left: 20px;
  margin: 0;
}

.interview-section li {
  font-size: 14px;
  color: #606266;
  line-height: 1.8;
}

.raw-result {
  margin-top: 16px;
}

.markdown-body {
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  line-height: 1.8;
}
</style>
