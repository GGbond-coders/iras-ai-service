/**
 * @file index.js
 * @description Vue Router 路由配置文件。
 *              定义应用的路由规则、路由守卫（权限控制）。
 *              新增：诊断历史、管理员后台（仪表盘/用户管理/职位管理）。
 *
 * @author IRAS Team
 * @since 1.0
 */

import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../store/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    redirect: '/jobs',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'jobs',
        name: 'Jobs',
        component: () => import('../views/Jobs.vue'),
        meta: { title: '职位检索' }
      },
      {
        path: 'job-profile',
        name: 'JobProfile',
        component: () => import('../views/JobProfile.vue'),
        meta: { title: '职能画像' }
      },
      {
        path: 'diagnosis',
        name: 'Diagnosis',
        component: () => import('../views/Diagnosis.vue'),
        meta: { title: '智能诊断' }
      },
      {
        path: 'history',
        name: 'History',
        component: () => import('../views/History.vue'),
        meta: { title: '诊断历史' }
      },
      // 管理员路由
      {
        path: 'admin/dashboard',
        name: 'AdminDashboard',
        component: () => import('../views/AdminDashboard.vue'),
        meta: { title: '系统概览', adminOnly: true }
      },
      {
        path: 'admin/users',
        name: 'AdminUsers',
        component: () => import('../views/AdminUsers.vue'),
        meta: { title: '用户管理', adminOnly: true }
      },
      {
        path: 'admin/jobs',
        name: 'AdminJobs',
        component: () => import('../views/AdminJobs.vue'),
        meta: { title: '职位管理', adminOnly: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  if (to.meta.requiresAuth !== false && !userStore.token) {
    next('/login')
  } else if (to.path === '/login' && userStore.token) {
    next('/')
  } else if (to.meta.adminOnly && !userStore.isAdmin) {
    // 非管理员访问管理员页面 -> 重定向到首页
    next('/')
  } else {
    next()
  }
})

export default router
