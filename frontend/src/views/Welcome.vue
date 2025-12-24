<template>
  <div class="dashboard-container" style="padding: 20px;">

    <el-row :gutter="20" style="margin-bottom: 20px;">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">待处理事件</div>
          <div class="stat-value warning">{{ stats.pendingCount || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">同步成功</div>
          <div class="stat-value success">{{ stats.successCount || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">同步失败/冲突</div>
          <div class="stat-value danger">{{ stats.failedCount || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">总事件数</div>
          <div class="stat-value info">{{ stats.totalCount || 0 }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-bottom: 20px;">
      <el-col :xs="24" :md="16">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>同步状态分布</span>
              <el-tag>实时数据</el-tag>
            </div>
          </template>
          <div id="chart-container" style="height: 300px;"></div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="8">
        <el-card shadow="never" style="height: 100%;">
          <template #header>
            <span>运维操作</span>
          </template>
          <div style="display: flex; flex-direction: column; gap: 15px;">
            <el-select v-model="currentSourceId" placeholder="选择查看的数据源" @change="handleSourceChange">
              <el-option
                  v-for="item in sourceList"
                  :key="item.sourceId"
                  :label="item.sourceName"
                  :value="item.sourceId"
              />
            </el-select>

            <el-alert title="数据每 5 秒自动刷新" type="info" :closable="false" show-icon />

            <el-button type="primary" plain icon="Refresh" @click="loadData">手动刷新</el-button>

            <el-button type="danger" icon="Odometer" @click="handleSimulation">开始流量模拟 (压测)</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>同步日志监控</span>
          <el-radio-group v-model="filterStatus" size="small" @change="loadLogs">
            <el-radio-button :label="null">全部</el-radio-button>
            <el-radio-button :label="2">仅失败</el-radio-button>
            <el-radio-button :label="0">待处理</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <el-table :data="logList" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="tableName" label="表名" width="120" />
        <el-table-column prop="opType" label="操作" width="100">
          <template #default="{ row }">
            <el-tag :type="getOpTag(row.opType)">{{ row.opType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="pkValue" label="主键值" width="100" />

        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTag(row.status)">
              {{ getStatusName(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="errorMsg" label="信息/报错" min-width="200" show-overflow-tooltip />
        <el-table-column prop="opTime" label="发生时间" width="170" />

        <el-table-column label="管理操作" width="180" fixed="right">
          <template #default="{ row }">
            <div v-if="row.status === 2">
              <el-tooltip content="重置状态，让引擎重新尝试同步" placement="top">
                <el-button type="primary" link size="small" @click="handleRetry(row)">重试</el-button>
              </el-tooltip>

              <el-divider direction="vertical" />

              <el-tooltip content="无法修复时，强制标记为成功，跳过此错误" placement="top">
                <el-popconfirm title="确定要强制跳过该错误吗？" @confirm="handleSkip(row)">
                  <template #reference>
                    <el-button type="danger" link size="small">强制通过</el-button>
                  </template>
                </el-popconfirm>
              </el-tooltip>
            </div>
            <div v-else>
              <span style="color: #909399; font-size: 12px;">无操作</span>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top: 15px; display: flex; justify-content: flex-end;">
        <el-pagination
            background
            layout="prev, pager, next"
            :total="1000"
            :page-size="20"
            @current-change="handlePageChange"
        />
      </div>
    </el-card>

  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import request from '@/utils/request.js'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { Refresh, Odometer } from '@element-plus/icons-vue'

// --- 状态数据 ---
const stats = ref({})
const logList = ref([])
const sourceList = ref([])
const currentSourceId = ref(null)
const loading = ref(false)
const filterStatus = ref(null) // 默认查全部
const currentPage = ref(1)
let timer = null
let myChart = null

// --- 初始化与加载 ---
onMounted(async () => {
  await loadSources()
  // 默认选中第一个数据源
  if (sourceList.value.length > 0) {
    currentSourceId.value = sourceList.value[0].sourceId
    loadData()
  }

  // 启动定时轮询 (每5秒刷新一次)
  timer = setInterval(() => {
    if (currentSourceId.value) {
      loadStats()
      loadLogs(true) // 静默刷新，不显示 loading
    }
  }, 5000)

  // 监听窗口大小变化调整图表
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  if (myChart) myChart.dispose()
  window.removeEventListener('resize', handleResize)
})

// --- API 请求 ---

const loadSources = async () => {
  const res = await request.get('/datasource/list')
  sourceList.value = res
}

const loadData = () => {
  loadStats()
  loadLogs()
}

const loadStats = async () => {
  if (!currentSourceId.value) return
  const res = await request.get('/admin/sync/stats', {
    params: { sourceId: currentSourceId.value }
  })
  stats.value = res
  renderChart(res)
}

const loadLogs = async (silent = false) => {
  if (!currentSourceId.value) return
  if (!silent) loading.value = true

  try {
    const res = await request.get('/admin/sync/logs', {
      params: {
        sourceId: currentSourceId.value,
        status: filterStatus.value,
        page: currentPage.value,
        size: 20
      }
    })
    logList.value = res
  } finally {
    loading.value = false
  }
}

// --- 修复操作 ---

const handleRetry = async (row) => {
  try {
    await request.post(`/admin/sync/retry/${row.id}`, null, {
      params: { sourceId: currentSourceId.value }
    })
    ElMessage.success('已提交重试请求')
    loadLogs() // 立即刷新
  } catch(e) {}
}

const handleSkip = async (row) => {
  try {
    await request.post(`/admin/sync/skip/${row.id}`, null, {
      params: { sourceId: currentSourceId.value }
    })
    ElMessage.success('已强制标记为成功')
    loadLogs()
  } catch(e) {}
}

const handleSourceChange = () => {
  currentPage.value = 1
  loadData()
}

const handlePageChange = (page) => {
  currentPage.value = page
  loadLogs()
}

// --- 图表渲染 ---
const renderChart = (data) => {
  const chartDom = document.getElementById('chart-container')
  if (!chartDom) return

  if (!myChart) {
    myChart = echarts.init(chartDom)
  }

  const option = {
    tooltip: { trigger: 'item' },
    legend: { top: '5%', left: 'center' },
    series: [
      {
        name: '同步状态',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: { show: false, position: 'center' },
        emphasis: {
          label: { show: true, fontSize: 20, fontWeight: 'bold' }
        },
        data: [
          { value: data.successCount, name: '成功', itemStyle: { color: '#67C23A' } },
          { value: data.failedCount, name: '失败/冲突', itemStyle: { color: '#F56C6C' } },
          { value: data.pendingCount, name: '待处理', itemStyle: { color: '#E6A23C' } }
        ]
      }
    ]
  }
  myChart.setOption(option)
}

const handleResize = () => {
  if (myChart) myChart.resize()
}

// 模拟压测 (可选)
const handleSimulation = async () => {
  if (!currentSourceId.value) return
  try {
    // 假设你后面会写这个接口
    // await request.post('/simulation/start', null, { params: { sourceId: currentSourceId.value } })
    ElMessage.info('压测指令已发送(需后端实现 SimulationController)')
  } catch(e) {}
}

// --- 辅助函数 ---
const getOpTag = (type) => {
  const map = { 'INSERT': 'success', 'UPDATE': 'warning', 'DELETE': 'danger' }
  return map[type]
}

const getStatusTag = (status) => {
  if (status === 0) return 'info'
  if (status === 1) return 'success'
  if (status === 2) return 'danger'
}

const getStatusName = (status) => {
  if (status === 0) return '待处理'
  if (status === 1) return '成功'
  if (status === 2) return '失败'
}
</script>

<style scoped>
.stat-card {
  text-align: center;
  margin-bottom: 10px;
}
.stat-title {
  color: #909399;
  font-size: 14px;
}
.stat-value {
  font-size: 24px;
  font-weight: bold;
  margin-top: 10px;
}
.stat-value.warning { color: #E6A23C; }
.stat-value.success { color: #67C23A; }
.stat-value.danger { color: #F56C6C; }
.stat-value.info { color: #409EFF; }

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>