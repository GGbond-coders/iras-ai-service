<!--
  @file Login.vue
  @description 登录/注册页面组件。
               提供用户登录和注册功能，使用 Element Plus 的 Tab 切换两种模式。
               登录支持用户名或邮箱，注册需要填写用户名、密码和可选邮箱。
               登录/注册成功后自动保存用户信息并跳转到首页。
  @author IRAS Team
  @since 1.0
-->
<template>
  <div class="login-container">
    <div class="login-card">
      <!-- 页面标题区域 -->
      <div class="login-header">
        <h1>智能简历诊断系统</h1>
        <p>Intelligent Resume Analysis System</p>
      </div>

      <!-- 登录/注册 Tab 切换 -->
      <el-tabs v-model="activeTab" class="login-tabs" stretch>
        <!-- 登录表单 -->
        <el-tab-pane label="登录" name="login">
          <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" @keyup.enter="handleLogin">
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" placeholder="用户名或邮箱" size="large" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="loginForm.password" type="password" placeholder="密码" size="large" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="large" :loading="loading" @click="handleLogin" style="width: 100%">
                登 录
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 注册表单 -->
        <el-tab-pane label="注册" name="register">
          <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" @keyup.enter="handleRegister">
            <el-form-item prop="username">
              <el-input v-model="registerForm.username" placeholder="用户名" size="large" />
            </el-form-item>
            <el-form-item prop="email">
              <el-input v-model="registerForm.email" placeholder="邮箱（选填）" size="large" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="registerForm.password" type="password" placeholder="密码" size="large" show-password />
            </el-form-item>
            <el-form-item prop="confirmPassword">
              <el-input v-model="registerForm.confirmPassword" type="password" placeholder="确认密码" size="large" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="large" :loading="loading" @click="handleRegister" style="width: 100%">
                注 册
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
/**
 * 登录/注册页面逻辑。
 * <p>
 * 核心功能：
 * <ul>
 *   <li>登录：支持用户名或邮箱登录，验证通过后保存 Token 并跳转首页</li>
 *   <li>注册：填写用户名、密码、可选邮箱，注册成功后自动登录</li>
 *   <li>表单校验：使用 Element Plus 的表单验证规则</li>
 * </ul>
 * </p>
 */
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '../api'
import { useUserStore } from '../store/user'

const router = useRouter()
const userStore = useUserStore()

/** 当前激活的 Tab 名称（'login' 或 'register'） */
const activeTab = ref('login')

/** 按钮加载状态 */
const loading = ref(false)

/** 表单引用（用于触发表单验证） */
const loginFormRef = ref(null)
const registerFormRef = ref(null)

/** 登录表单数据 */
const loginForm = reactive({ username: '', password: '' })

/** 注册表单数据 */
const registerForm = reactive({ username: '', email: '', password: '', confirmPassword: '' })

/**
 * 登录表单验证规则。
 * <p>
 * 规则：
 * <ul>
 *   <li>username - 必填</li>
 *   <li>password - 必填</li>
 * </ul>
 * </p>
 */
const loginRules = {
  username: [{ required: true, message: '请输入用户名或邮箱', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

/**
 * 注册表单验证规则。
 * <p>
 * 规则：
 * <ul>
 *   <li>username - 必填，2-20 个字符</li>
 *   <li>password - 必填，不少于 6 位</li>
 *   <li>confirmPassword - 必填，需与密码一致</li>
 * </ul>
 * </p>
 */
const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度 2-20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不少于 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      /** 自定义校验器：确认密码必须与密码一致 */
      validator: (rule, value, callback) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

/**
 * 处理登录操作。
 * <p>
 * 流程：表单验证 -> 调用登录 API -> 保存用户信息 -> 跳转首页
 * </p>
 */
async function handleLogin() {
  // 触发表单验证
  try {
    await loginFormRef.value.validate()
  } catch { return }  // 验证不通过则终止

  loading.value = true
  try {
    // 调用登录 API
    const res = await authApi.login(loginForm)
    // 保存用户信息到 Pinia Store 和 localStorage
    userStore.setUser(res.data)
    ElMessage.success('登录成功')
    // 跳转到首页
    router.push('/')
  } catch (e) {
    // 错误已在 axios 响应拦截器中处理
  } finally {
    loading.value = false
  }
}

/**
 * 处理注册操作。
 * <p>
 * 流程：表单验证 -> 调用注册 API -> 保存用户信息 -> 跳转首页
 * </p>
 */
async function handleRegister() {
  // 触发表单验证
  try {
    await registerFormRef.value.validate()
  } catch { return }  // 验证不通过则终止

  loading.value = true
  try {
    // 调用注册 API（邮箱为可选，未填写时传 undefined）
    const res = await authApi.register({
      username: registerForm.username,
      password: registerForm.password,
      email: registerForm.email || undefined
    })
    // 注册成功后自动保存用户信息（自动登录）
    userStore.setUser(res.data)
    ElMessage.success('注册成功')
    // 跳转到首页
    router.push('/')
  } catch (e) {
    // 错误已在 axios 响应拦截器中处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* 登录页容器 - 全屏居中 + 渐变背景 */
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

/* 登录卡片 */
.login-card {
  width: 420px;
  padding: 40px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

/* 标题区域 */
.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.logo-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.login-header h1 {
  font-size: 24px;
  color: #303133;
  margin-bottom: 4px;
}

.login-header p {
  font-size: 13px;
  color: #909399;
}

/* Tab 标签字体大小 */
.login-tabs :deep(.el-tabs__item) {
  font-size: 16px;
}
</style>
