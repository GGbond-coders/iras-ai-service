/**
 * @file user.js
 * @description 用户状态管理模块（Pinia Store）。
 *              使用 Pinia 管理全局用户状态，包括 Token、用户名、邮箱和角色。
 *              状态数据持久化到 localStorage，确保页面刷新后登录状态不丢失。
 *
 * @author IRAS Team
 * @since 1.0
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

/**
 * 用户状态 Store。
 * <p>
 * 使用 Pinia 的组合式 API（Composition API）风格定义。
 * 管理以下状态：
 * <ul>
 *   <li>token - JWT 认证令牌</li>
 *   <li>username - 用户名</li>
 *   <li>email - 邮箱</li>
 *   <li>role - 用户角色（user / admin）</li>
 * </ul>
 * </p>
 */
export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const username = ref(localStorage.getItem('username') || '')
  const email = ref(localStorage.getItem('email') || '')
  const role = ref(localStorage.getItem('role') || 'user')

  /** 是否为管理员 */
  const isAdmin = computed(() => role.value === 'admin')

  /**
   * 设置用户信息（登录/注册成功后调用）。
   *
   * @param {Object} userData - 用户数据对象
   */
  function setUser(userData) {
    token.value = userData.token
    username.value = userData.username
    email.value = userData.email || ''
    role.value = userData.role || 'user'
    localStorage.setItem('token', userData.token)
    localStorage.setItem('username', userData.username)
    localStorage.setItem('email', userData.email || '')
    localStorage.setItem('role', userData.role || 'user')
  }

  /**
   * 退出登录，清除所有用户信息。
   */
  function logout() {
    token.value = ''
    username.value = ''
    email.value = ''
    role.value = 'user'
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('email')
    localStorage.removeItem('role')
  }

  return { token, username, email, role, isAdmin, setUser, logout }
})
