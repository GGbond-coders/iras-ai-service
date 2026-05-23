<!--
  @file AdminDashboard.vue
  @description 管理员仪表盘页面组件。
               使用 ECharts 渲染系统统计数据：用户数、职位数、诊断数卡片，
               近 7 天注册/诊断趋势折线图，职位城市分布饼图。
  @author IRAS Team
  @since 1.0
-->
<template>
  <div class="dashboard-page">
    <!-- 核心数据卡片 -->
    <el-row :gutter="20" style="margin-bottom: 20px">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card stat-card--blue">
          <div class="stat-icon"></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.userCount || 0 }}</div>
            <div class="stat-label">注册用户数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card stat-card--green">
          <div class="stat-icon"></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.jobCount || 0 }}</div>
            <div class="stat-label">职位总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card stat-card--orange">
          <div class="stat-icon"></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.diagnosisCount || 0 }}</div>
            <div class="stat-label">诊断总次数</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 趋势图表 -->
    <el-row :gutter="20" style="margin-bottom: 20px">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">近 7 天用户注册趋势</div>
          </template>
          <div ref="userTrendChart" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">近 7 天诊断趋势</div>
          </template>
          <div ref="diagnosisTrendChart" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 城市分布 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">职位城市分布 Top 10</div>
      </template>
      <div ref="cityChart" class="chart-container chart-container--large"></div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onActivated, onBeforeUnmount, nextTick, watch } from 'vue'
import * as echarts from 'echarts'
import { adminApi } from '../api'
import { ElMessage } from 'element-plus'

const stats = ref({})

const userTrendChart = ref(null)
const diagnosisTrendChart = ref(null)
const cityChart = ref(null)

let userChartInstance = null
let diagnosisChartInstance = null
let cityChartInstance = null
let resizeHandler = null

function initCharts() {
  nextTick(() => {
    // 用户注册趋势 - 折线面积图
    if (userTrendChart.value) {
      if (userChartInstance) userChartInstance.dispose()
      userChartInstance = echarts.init(userTrendChart.value)
      const trend = stats.value.userTrend || []
      userChartInstance.setOption({
        tooltip: { trigger: 'axis', formatter: '{b}<br/>注册数: {c}' },
        grid: { top: 20, right: 20, bottom: 30, left: 50 },
        xAxis: {
          type: 'category',
          data: trend.map(i => i.date),
          axisLabel: { color: '#909399', fontSize: 12 },
          axisLine: { lineStyle: { color: '#e4e7ed' } }
        },
        yAxis: {
          type: 'value',
          minInterval: 1,
          axisLabel: { color: '#909399', fontSize: 12 },
          splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } }
        },
        series: [{
          data: trend.map(i => i.count),
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 8,
          lineStyle: { width: 3, color: '#409eff' },
          itemStyle: { color: '#409eff', borderWidth: 2, borderColor: '#fff' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(64,158,255,0.3)' },
              { offset: 1, color: 'rgba(64,158,255,0.02)' }
            ])
          }
        }]
      })
    }

    // 诊断趋势 - 折线面积图
    if (diagnosisTrendChart.value) {
      if (diagnosisChartInstance) diagnosisChartInstance.dispose()
      diagnosisChartInstance = echarts.init(diagnosisTrendChart.value)
      const trend = stats.value.diagnosisTrend || []
      diagnosisChartInstance.setOption({
        tooltip: { trigger: 'axis', formatter: '{b}<br/>诊断数: {c}' },
        grid: { top: 20, right: 20, bottom: 30, left: 50 },
        xAxis: {
          type: 'category',
          data: trend.map(i => i.date),
          axisLabel: { color: '#909399', fontSize: 12 },
          axisLine: { lineStyle: { color: '#e4e7ed' } }
        },
        yAxis: {
          type: 'value',
          minInterval: 1,
          axisLabel: { color: '#909399', fontSize: 12 },
          splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } }
        },
        series: [{
          data: trend.map(i => i.count),
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 8,
          lineStyle: { width: 3, color: '#e6a23c' },
          itemStyle: { color: '#e6a23c', borderWidth: 2, borderColor: '#fff' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(230,162,60,0.3)' },
              { offset: 1, color: 'rgba(230,162,60,0.02)' }
            ])
          }
        }]
      })
    }

    // 城市分布 - 环形饼图
    if (cityChart.value) {
      if (cityChartInstance) cityChartInstance.dispose()
      cityChartInstance = echarts.init(cityChart.value)
      const cities = stats.value.cityDistribution || []
      const colors = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399',
        '#b37feb', '#36cfc9', '#ff85c0', '#ffc53d', '#73d13d']
      cityChartInstance.setOption({
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c} 个 ({d}%)'
        },
        legend: {
          orient: 'vertical',
          right: 20,
          top: 'center',
          textStyle: { color: '#606266', fontSize: 13 }
        },
        series: [{
          type: 'pie',
          radius: ['40%', '70%'],
          center: ['40%', '50%'],
          avoidLabelOverlap: true,
          itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
          label: {
            show: true,
            formatter: '{b}\n{d}%',
            fontSize: 12,
            color: '#606266'
          },
          emphasis: {
            label: { show: true, fontSize: 14, fontWeight: 'bold' },
            itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.2)' }
          },
          data: cities.map((item, i) => ({
            name: item.city,
            value: item.count,
            itemStyle: { color: colors[i % colors.length] }
          }))
        }]
      })
    }
  })
}

function handleResize() {
  userChartInstance?.resize()
  diagnosisChartInstance?.resize()
  cityChartInstance?.resize()
}

async function fetchStats() {
  try {
    const res = await adminApi.getStatistics()
    stats.value = res.data
    initCharts()
  } catch (e) {
    ElMessage.error('获取统计数据失败')
  }
}

onActivated(() => {
  fetchStats()
  if (!resizeHandler) {
    resizeHandler = () => handleResize()
    window.addEventListener('resize', resizeHandler)
  }
})

onBeforeUnmount(() => {
  userChartInstance?.dispose()
  diagnosisChartInstance?.dispose()
  cityChartInstance?.dispose()
  if (resizeHandler) {
    window.removeEventListener('resize', resizeHandler)
    resizeHandler = null
  }
})
</script>

<style scoped>
.dashboard-page {
  max-width: 1200px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 20px 24px;
  gap: 16px;
}

.stat-card--blue {
  border-left: 4px solid #409eff;
}

.stat-card--green {
  border-left: 4px solid #67c23a;
}

.stat-card--orange {
  border-left: 4px solid #e6a23c;
}

.stat-icon {
  font-size: 36px;
  line-height: 1;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.card-header {
  font-size: 16px;
  font-weight: 600;
}

.chart-container {
  width: 100%;
  height: 280px;
}

.chart-container--large {
  height: 360px;
}
</style>
