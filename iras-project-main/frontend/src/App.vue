<!--
  @file App.vue
  @description 应用根组件。
               作为整个应用的顶层容器，只负责渲染当前路由对应的页面组件。
               同时定义了全局基础样式（重置 margin/padding、设置字体、背景色等）。
  @author IRAS Team
  @since 1.0
-->
<template>
  <!-- 应用根容器，router-view 会根据当前路由渲染对应的页面组件 -->
  <div id="app">
    <router-view />
  </div>
</template>

<script setup>
/**
 * 根组件脚本。
 * 使用 Vue 3 的 <script setup> 语法糖，无需显式导出组件对象。
 * 当前组件无额外逻辑，仅作为路由视图的容器。
 */
</script>

<style>
/* 全局基础样式重置 */
* {
  margin: 0;          /* 清除默认外边距 */
  padding: 0;         /* 清除默认内边距 */
  box-sizing: border-box;  /* 使用 border-box 盒模型 */
}

/* 页面 body 样式 */
body {
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;  /* 设置中英文字体 */
  background-color: #f5f7fa;  /* 设置页面背景色 */
}

/* 应用根容器 */
#app {
  min-height: 100vh;  /* 最小高度为视口高度 */
}
</style>
