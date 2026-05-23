<!--
  @file AdminUsers.vue
  @description 管理员用户管理页面组件。
               展示系统所有用户列表，支持修改用户角色和删除用户。
  @author IRAS Team
  @since 1.0
-->
<template>
  <div class="admin-users-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
        </div>
      </template>

      <el-table :data="users" stripe style="width: 100%" v-loading="loading" empty-text="暂无用户数据">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="150" />
        <el-table-column prop="email" label="邮箱" min-width="200">
          <template #default="{ row }">
            {{ row.email || '—' }}
          </template>
        </el-table-column>
        <el-table-column prop="role" label="角色" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.role === 'admin' ? 'danger' : 'info'" size="small">
              {{ row.role === 'admin' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="toggleRole(row)">
              {{ row.role === 'admin' ? '设为普通用户' : '设为管理员' }}
            </el-button>
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
          @current-change="fetchUsers"
          @size-change="fetchUsers"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onActivated } from 'vue'
import { adminApi } from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const users = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

async function fetchUsers() {
  loading.value = true
  try {
    const res = await adminApi.getUsers({ page: page.value, size: size.value })
    users.value = res.data.records
    total.value = res.data.total
  } catch (e) {
    // handled
  } finally {
    loading.value = false
  }
}

async function toggleRole(user) {
  const newRole = user.role === 'admin' ? 'user' : 'admin'
  const label = newRole === 'admin' ? '管理员' : '普通用户'
  try {
    await ElMessageBox.confirm(`确定将用户 "${user.username}" 设为${label}？`, '提示', { type: 'warning' })
    await adminApi.updateUserRole(user.id, newRole)
    ElMessage.success('修改成功')
    fetchUsers()
  } catch (e) {
    // cancelled or error
  }
}

async function handleDelete(user) {
  try {
    await ElMessageBox.confirm(`确定删除用户 "${user.username}"？此操作不可恢复！`, '警告', { type: 'error' })
    await adminApi.deleteUser(user.id)
    ElMessage.success('删除成功')
    fetchUsers()
  } catch (e) {
    // cancelled or error
  }
}

onActivated(() => {
  fetchUsers()
})
</script>

<style scoped>
.admin-users-page {
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
</style>
