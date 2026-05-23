<!--
  @file AdminJobs.vue
  @description 管理员职位管理页面组件。
               展示系统所有职位列表，支持新增、编辑和删除职位。
  @author IRAS Team
  @since 1.0
-->
<template>
  <div class="admin-jobs-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>职位管理</span>
          <div class="header-actions">
            <el-button type="warning" size="small" :loading="syncing" @click="handleSyncChromaDb">
              {{ syncing ? '同步中...' : '同步向量库' }}
            </el-button>
            <el-button type="primary" size="small" @click="openDialog(null)">新增职位</el-button>
          </div>
        </div>
      </template>

      <el-table :data="jobs" stripe style="width: 100%" v-loading="loading" empty-text="暂无职位数据">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="jobName" label="职位名称" min-width="150" />
        <el-table-column prop="companyName" label="公司名称" min-width="150" />
        <el-table-column prop="city" label="城市" width="100" />
        <el-table-column prop="salary" label="薪资" width="100">
          <template #default="{ row }">
            <span style="color: #f56c6c; font-weight: 600">{{ row.salary }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.type || '全职' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDialog(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @current-change="fetchJobs"
          @size-change="fetchJobs"
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="editingJob ? '编辑职位' : '新增职位'" width="600px">
      <el-form ref="formRef" :model="jobForm" :rules="formRules" label-width="80px">
        <el-form-item label="职位名称" prop="jobName">
          <el-input v-model="jobForm.jobName" placeholder="如：Java后台开发工程师" />
        </el-form-item>
        <el-form-item label="公司名称" prop="companyName">
          <el-input v-model="jobForm.companyName" placeholder="如：阿里巴巴" />
        </el-form-item>
        <el-form-item label="城市" prop="city">
          <el-input v-model="jobForm.city" placeholder="如：杭州" />
        </el-form-item>
        <el-form-item label="薪资" prop="salary">
          <el-input v-model="jobForm.salary" placeholder="如：20000" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="jobForm.type" placeholder="请选择">
            <el-option label="全职" value="全职" />
            <el-option label="实习" value="实习" />
            <el-option label="校招" value="校招" />
          </el-select>
        </el-form-item>
        <el-form-item label="职位描述" prop="jdText">
          <el-input v-model="jobForm.jdText" type="textarea" :rows="6" placeholder="请输入职位描述（JD）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onActivated } from 'vue'
import { adminApi } from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const syncing = ref(false)
const jobs = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

const dialogVisible = ref(false)
const editingJob = ref(null)
const submitting = ref(false)
const formRef = ref(null)

const jobForm = reactive({
  jobName: '',
  companyName: '',
  city: '',
  salary: '',
  type: '全职',
  jdText: ''
})

const formRules = {
  jobName: [{ required: true, message: '请输入职位名称', trigger: 'blur' }],
  companyName: [{ required: true, message: '请输入公司名称', trigger: 'blur' }],
  jdText: [{ required: true, message: '请输入职位描述', trigger: 'blur' }]
}

async function fetchJobs() {
  loading.value = true
  try {
    const res = await adminApi.getJobs({ page: page.value, size: size.value })
    jobs.value = res.data.records
    total.value = res.data.total
  } catch (e) {
    // handled
  } finally {
    loading.value = false
  }
}

function openDialog(job) {
  editingJob.value = job
  if (job) {
    jobForm.jobName = job.jobName
    jobForm.companyName = job.companyName
    jobForm.city = job.city || ''
    jobForm.salary = job.salary || ''
    jobForm.type = job.type || '全职'
    jobForm.jdText = job.jdText
  } else {
    jobForm.jobName = ''
    jobForm.companyName = ''
    jobForm.city = ''
    jobForm.salary = ''
    jobForm.type = '全职'
    jobForm.jdText = ''
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch { return }

  submitting.value = true
  try {
    if (editingJob.value) {
      await adminApi.updateJob(editingJob.value.id, { ...jobForm })
      ElMessage.success('更新成功')
    } else {
      await adminApi.addJob({ ...jobForm })
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    fetchJobs()
  } catch (e) {
    // handled
  } finally {
    submitting.value = false
  }
}

async function handleSyncChromaDb() {
  try {
    await ElMessageBox.confirm(
      '将增量同步有变化的岗位到向量库，确定继续？',
      '同步向量库',
      { type: 'info' }
    )
  } catch { return }

  syncing.value = true
  try {
    const res = await adminApi.syncChromaDb()
    ElMessage.success(res.message || '向量库同步完成')
  } catch (e) {
    if (e?.message) {
      ElMessage.error(e.message)
    }
  } finally {
    syncing.value = false
  }
}

async function handleDelete(job) {
  try {
    await ElMessageBox.confirm(`确定删除职位 "${job.jobName}"？`, '警告', { type: 'error' })
    await adminApi.deleteJob(job.id)
    ElMessage.success('删除成功')
    fetchJobs()
  } catch (e) {
    // cancelled or error
  }
}

onActivated(() => {
  fetchJobs()
})
</script>

<style scoped>
.admin-jobs-page {
  max-width: 1200px;
}

.card-header {
  font-size: 16px;
  font-weight: 600;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
