/**
 * @file vite.config.js
 * @description Vite 构建工具配置文件。
 *              配置 Vue 插件、开发服务器端口和 API 代理规则。
 *              代理配置将 /iras 前缀的请求转发到后端 Spring Boot 服务。
 *
 * @author IRAS Team
 * @since 1.0
 */

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  // 注册 Vue 3 单文件组件（SFC）插件
  plugins: [vue()],

  // 开发服务器配置
  server: {
    port: 5173,  // 开发服务器端口

    // API 代理配置（解决开发环境跨域问题）
    proxy: {
      // 将 /iras 前缀的请求代理到后端服务
      '/iras': {
        target: 'http://localhost:8080',  // 后端服务地址
        changeOrigin: true                 // 修改请求头中的 Origin 为目标地址
      }
    }
  }
})
