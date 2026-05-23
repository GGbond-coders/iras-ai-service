/**
 * @file index.js
 * @description 前端 API 模块。
 *              基于 axios 封装 HTTP 请求客户端，提供统一的请求/响应拦截、
 *              Token 自动注入、错误处理等功能。
 *              导出 API 模块：authApi、jobApi、difyApi、diagnosisApi、adminApi。
 *
 * @author IRAS Team
 * @since 1.0
 */

import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'
import { useUserStore } from '../store/user'

const api = axios.create({
  baseURL: '/iras',
  timeout: 300000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器 - 自动添加 Token
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  console.log('[API Request]', config.method?.toUpperCase(), config.url, token ? 'Token: ' + token.substring(0, 20) + '...' : 'Token: NULL')
  return config
})

// 响应拦截器
api.interceptors.response.use(
  response => {
    const { data } = response
    if (data.code && data.code !== 200) {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(data)
    }
    return data
  },
  error => {
    console.error('[API Error]', error.response?.status, error.config?.url, error.response?.data)
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      localStorage.removeItem('email')
      localStorage.removeItem('role')
      // 同步清除 Pinia store
      try {
        useUserStore().logout()
      } catch (_) { /* ignore */ }
      router.push('/login')
      ElMessage.error('登录已过期，请重新登录')
    } else {
      ElMessage.error(error.response?.data?.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

// ============ 认证 API ============
export const authApi = {
  register(data) {
    return api.post('/api/auth/register', data)
  },
  login(data) {
    return api.post('/api/auth/login', data)
  }
}

// ============ 职位 API ============
export const jobApi = {
  search(params) {
    return api.get('/api/jobs/search', { params })
  },
  getById(id) {
    return api.get(`/api/jobs/${id}`)
  }
}

// ============ Dify API ============
export const difyApi = {
  getJobProfile(jobName) {
    return api.post('/api/dify/job-profile', { job_name: jobName })
  },
  diagnoseResume(file) {
    const formData = new FormData()
    formData.append('file', file)
    return api.post('/api/dify/diagnose', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}

// ============ 诊断历史 API ============
export const diagnosisApi = {
  /** 获取诊断历史列表（分页） */
  getHistory(params) {
    return api.get('/api/diagnosis/history', { params })
  },
  /** 获取诊断记录详情 */
  getDetail(id) {
    return api.get(`/api/diagnosis/detail/${id}`)
  },
  /** 删除诊断记录 */
  deleteRecord(id) {
    return api.delete(`/api/diagnosis/${id}`)
  }
}

// ============ 管理员 API ============
export const adminApi = {
  // --- 用户管理 ---
  getUsers(params) {
    return api.get('/api/admin/users', { params })
  },
  updateUserRole(id, role) {
    return api.put(`/api/admin/users/${id}/role`, { role })
  },
  deleteUser(id) {
    return api.delete(`/api/admin/users/${id}`)
  },
  // --- 职位管理 ---
  getJobs(params) {
    return api.get('/api/admin/jobs', { params })
  },
  addJob(data) {
    return api.post('/api/admin/jobs', data)
  },
  updateJob(id, data) {
    return api.put(`/api/admin/jobs/${id}`, data)
  },
  deleteJob(id) {
    return api.delete(`/api/admin/jobs/${id}`)
  },
  // --- 系统统计 ---
  getStatistics() {
    return api.get('/api/admin/statistics')
  },
  // --- AI 服务管理 ---
  syncChromaDb() {
    return api.post('/api/admin/chromadb/sync')
  }
}

export default api
