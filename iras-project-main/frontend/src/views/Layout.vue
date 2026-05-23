<!--
  @file Layout.vue
  @description 主布局组件。
               采用 Element Plus 的 Container 布局，包含左侧边栏和右侧内容区。
               侧边栏包含导航菜单，管理员用户额外显示管理后台菜单。
               顶部栏包含面包屑导航和用户下拉菜单。
  @author IRAS Team
  @since 1.0
-->
<template>
  <el-container class="layout-container">
    <!-- 左侧边栏 - 导航菜单 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="layout-aside">
      <div class="aside-header" @click="isCollapse = !isCollapse">
        <span class="aside-title">IRAS</span>
      </div>

      <el-menu
        :default-active="$route.path"
        router
        :collapse="isCollapse"
        background-color="#1e293b"
        text-color="#94a3b8"
        active-text-color="#60a5fa"
        class="aside-menu"
      >
        <!-- 普通用户菜单 -->
        <el-menu-item index="/jobs">
          <template #title>职位检索</template>
        </el-menu-item>
        <el-menu-item index="/job-profile">
          <template #title>职能画像</template>
        </el-menu-item>
        <el-menu-item index="/diagnosis">
          <template #title>智能诊断</template>
        </el-menu-item>
        <el-menu-item index="/history">
          <template #title>诊断历史</template>
        </el-menu-item>

        <!-- 管理员专属菜单 -->
        <template v-if="userStore.isAdmin">
          <el-divider style="border-color: #334155; margin: 12px 16px" />
          <el-menu-item index="/admin/dashboard">
            <template #title>系统概览</template>
          </el-menu-item>
          <el-menu-item index="/admin/users">
            <template #title>用户管理</template>
          </el-menu-item>
          <el-menu-item index="/admin/jobs">
            <template #title>职位管理</template>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <!-- 右侧主内容区 -->
    <el-container>
      <el-header class="layout-header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ $route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-tag v-if="userStore.isAdmin" size="small" type="danger" style="margin-right: 8px">管理员</el-tag>
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              {{ userStore.username }}
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="layout-main">
        <router-view v-slot="{ Component }">
          <keep-alive>
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'

const router = useRouter()
const userStore = useUserStore()
const isCollapse = ref(false)

function handleCommand(command) {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.layout-aside {
  background-color: #1e293b;
  transition: width 0.3s;
  overflow: hidden;
}

.aside-header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  gap: 10px;
  border-bottom: 1px solid #334155;
}

.aside-logo {
  font-size: 28px;
}

.aside-title {
  color: #f1f5f9;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 2px;
}

.aside-menu {
  border-right: none;
}

.layout-header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  padding: 0 20px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #606266;
  font-size: 14px;
}

.layout-main {
  background-color: #f5f7fa;
  padding: 20px;
  overflow-y: auto;
}
</style>
