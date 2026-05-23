/**
 * @file main.js
 * @description IRAS 前端应用入口文件。
 *              负责初始化 Vue 3 应用，注册全局插件（Pinia 状态管理、Vue Router 路由、
 *              Element Plus UI 组件库）和全局图标组件，最后将应用挂载到 DOM。
 *
 * @author IRAS Team
 * @since 1.0
 */

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'

// 创建 Vue 3 应用实例
const app = createApp(App)

// 注册所有 Element Plus 图标为全局组件
// 遍历图标库中的每个图标，以组件名注册到应用中
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 注册 Pinia 状态管理插件（用于管理全局状态，如用户登录信息）
app.use(createPinia())
// 注册 Vue Router 路由插件（用于页面导航和路由守卫）
app.use(router)
// 注册 Element Plus UI 组件库，设置中文语言包
app.use(ElementPlus, { locale: zhCn })
// 将应用挂载到 HTML 中 id 为 app 的 DOM 元素上
app.mount('#app')
