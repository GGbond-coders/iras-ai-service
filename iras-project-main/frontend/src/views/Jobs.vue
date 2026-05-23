<!--
  @file Jobs.vue
  @description 职位检索页面组件。
               提供职位的多条件搜索功能（职位名称、城市、薪资范围），
               搜索结果以表格形式展示，支持分页浏览。
               点击"查看详情"可弹窗查看职位的完整描述信息。
  @author IRAS Team
  @since 1.0
-->
<template>
  <div class="jobs-page">
    <!-- 搜索条件区域 -->
    <el-card class="search-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>职位搜索</span>
        </div>
      </template>

      <!-- 搜索表单 - 内联布局 -->
      <el-form :model="searchForm" inline>
        <el-form-item label="职位名称">
          <el-input v-model="searchForm.jobName" placeholder="请输入职位名称" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="工作地点">
          <el-input v-model="searchForm.city" placeholder="请输入城市" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="薪资范围">
          <el-input v-model="searchForm.salaryMin" placeholder="最低" clearable style="width: 100px" />
          <span style="margin: 0 8px; color: #909399">—</span>
          <el-input v-model="searchForm.salaryMax" placeholder="最高" clearable style="width: 100px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 搜索结果列表区域 -->
    <el-card class="result-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>搜索结果（共 {{ total }} 条）</span>
        </div>
      </template>

      <!-- 职位数据表格 -->
      <el-table :data="jobList" stripe style="width: 100%" v-loading="loading" empty-text="暂无职位数据">
        <el-table-column prop="jobName" label="职位名称" min-width="150" />
        <el-table-column prop="companyName" label="公司名称" min-width="150" />
        <el-table-column prop="city" label="城市" width="100" />
        <el-table-column prop="salary" label="薪资" width="120">
          <!-- 薪资列自定义渲染：红色加粗显示 -->
          <template #default="{ row }">
            <span style="color: #f56c6c; font-weight: 600">{{ row.salary }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="100">
          <!-- 类型列自定义渲染：标签样式显示 -->
          <template #default="{ row }">
            <el-tag size="small">{{ row.type || '全职' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <!-- 操作列：查看详情按钮 -->
          <template #default="{ row }">
            <el-button type="primary" link @click="viewDetail(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页组件 -->
      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination
          v-model:current-page="searchForm.page"
          v-model:page-size="searchForm.size"
          :total="total"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="fetchJobs"
          @size-change="fetchJobs"
        />
      </div>
    </el-card>

    <!-- 职位详情弹窗 -->
    <el-dialog v-model="detailVisible" title="职位详情" width="700px">
      <div v-if="currentJob" class="job-detail">
        <h2>{{ currentJob.jobName }}</h2>
        <!-- 职位基本信息描述列表 -->
        <el-descriptions :column="2" border style="margin-top: 16px">
          <el-descriptions-item label="公司名称">{{ currentJob.companyName }}</el-descriptions-item>
          <el-descriptions-item label="工作城市">{{ currentJob.city }}</el-descriptions-item>
          <el-descriptions-item label="薪资范围">{{ currentJob.salary }}</el-descriptions-item>
          <el-descriptions-item label="职位类型">{{ currentJob.type || '全职' }}</el-descriptions-item>
        </el-descriptions>
        <h4 style="margin-top: 20px; color: #303133">职位描述</h4>
        <div class="jd-content">{{ currentJob.jdText }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 职位检索页面逻辑。
 * <p>
 * 功能：
 * <ul>
 *   <li>多条件搜索（职位名称、城市、薪资范围）</li>
 *   <li>分页浏览搜索结果</li>
 *   <li>查看职位详情弹窗</li>
 * </ul>
 * </p>
 */
import { ref, reactive, onActivated } from 'vue'
import { jobApi } from '../api'

/** 表格加载状态 */
const loading = ref(false)

/** 职位列表数据 */
const jobList = ref([])

/** 搜索结果总记录数 */
const total = ref(0)

/** 详情弹窗是否可见 */
const detailVisible = ref(false)

/** 当前查看的职位对象 */
const currentJob = ref(null)

/**
 * 搜索表单数据。
 * <p>
 * 包含搜索条件和分页参数，使用 reactive 实现双向绑定。
 * </p>
 */
const searchForm = reactive({
  jobName: '',      // 职位名称
  city: '',         // 城市
  salaryMin: '',    // 最低薪资
  salaryMax: '',    // 最高薪资
  page: 1,          // 当前页码
  size: 20          // 每页记录数
})

/**
 * 获取职位列表数据。
 * <p>
 * 调用职位搜索 API，更新列表数据和总记录数。
 * </p>
 */
async function fetchJobs() {
  loading.value = true
  try {
    const res = await jobApi.search(searchForm)
    jobList.value = res.data.records   // 更新列表
    total.value = res.data.total       // 更新总数
  } catch (e) {
    // 错误已在 axios 响应拦截器中处理
  } finally {
    loading.value = false
  }
}

/**
 * 执行搜索（重置到第一页）。
 */
function handleSearch() {
  searchForm.page = 1  // 搜索时重置到第一页
  fetchJobs()
}

/**
 * 重置搜索条件并重新搜索。
 */
function handleReset() {
  searchForm.jobName = ''
  searchForm.city = ''
  searchForm.salaryMin = ''
  searchForm.salaryMax = ''
  searchForm.page = 1
  fetchJobs()
}

/**
 * 查看职位详情（打开弹窗）。
 * @param {Object} job - 职位对象
 */
function viewDetail(job) {
  currentJob.value = job
  detailVisible.value = true
}

/**
 * 组件挂载后自动加载职位列表。
 */
onActivated(() => {
  fetchJobs()
})
</script>

<style scoped>
.jobs-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-header {
  font-size: 16px;
  font-weight: 600;
}

/* 分页组件容器 - 右对齐 */
.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* 职位详情标题 */
.job-detail h2 {
  color: #303133;
  font-size: 22px;
}

/* JD 内容区域 */
.jd-content {
  margin-top: 10px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  line-height: 1.8;
  color: #606266;
  white-space: pre-wrap;  /* 保留换行符 */
}
</style>
