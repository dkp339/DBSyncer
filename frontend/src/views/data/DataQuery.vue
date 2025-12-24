<template>
  <div class="app-container" style="display: flex; height: calc(100vh - 84px); padding: 20px;">

    <el-card shadow="never" style="width: 260px; margin-right: 15px; display: flex; flex-direction: column;">
      <template #header>
        <div class="card-header">
          <span>数据库导航</span>
          <el-tag type="info" size="small" v-if="tableList.length">{{ tableList.length }} 张表</el-tag>
        </div>
      </template>

      <el-select
          v-model="currentSourceId"
          placeholder="请选择数据源"
          @change="handleSourceChange"
          style="width: 100%; margin-bottom: 15px;"
          filterable
      >
        <el-option
            v-for="item in sourceList"
            :key="item.sourceId"
            :label="item.sourceName"
            :value="item.sourceId"
        >
          <span style="float: left">{{ item.sourceName }}</span>
          <span style="float: right; color: #8492a6; font-size: 12px">{{ item.dbType }}</span>
        </el-option>
      </el-select>

      <el-input
          v-model="searchTableText"
          placeholder="搜索表名..."
          prefix-icon="Search"
          clearable
          style="margin-bottom: 10px;"
      />

      <el-scrollbar style="flex: 1; border: 1px solid #ebeef5; border-radius: 4px;">
        <div v-loading="treeLoading" style="min-height: 100px;">
          <div v-if="filteredTableList.length === 0" style="text-align: center; color: #909399; margin-top: 20px;">
            <small>暂无数据</small>
          </div>
          <div
              v-for="table in filteredTableList"
              :key="table"
              class="table-item"
              :class="{ 'active': currentTable === table }"
              @click="handleTableClick(table)"
              title="点击生成查询语句"
          >
            <el-icon><Grid /></el-icon>
            <span class="table-name">{{ table }}</span>
          </div>
        </div>
      </el-scrollbar>
    </el-card>

    <el-card shadow="never" style="flex: 1; display: flex; flex-direction: column; overflow: hidden;" body-style="height: 100%; display: flex; flex-direction: column;">

      <div style="margin-bottom: 15px;">
        <div style="margin-bottom: 10px; display: flex; justify-content: space-between;">
          <div style="font-weight: bold; color: #606266;">SQL 编辑器</div>
          <div>
            <el-button type="info" link @click="clearSql">清空</el-button>
            <el-button type="primary" icon="VideoPlay" :loading="executing" @click="handleExecute">运行 (Run)</el-button>
            <el-button type="success" icon="DataAnalysis" :loading="analyzing" @click="handleExplain">分析 (Explain)</el-button>
          </div>
        </div>

        <el-input
            v-model="sqlContent"
            type="textarea"
            :rows="5"
            placeholder="请输入 SQL 语句，例如: SELECT * FROM student LIMIT 10"
            resize="none"
            style="font-family: Consolas, Monaco, monospace;"
        />
      </div>

      <div v-if="lastExecTime !== null" class="status-bar">
        <el-alert
            :title="`执行成功 | 耗时: ${lastExecTime}ms | 返回行数: ${queryResult.length} 行 (限制最大1000行)`"
            type="success"
            :closable="false"
            show-icon
        />
      </div>

      <div style="flex: 1; overflow: hidden; margin-top: 10px;">
        <el-table
            :data="queryResult"
            border
            stripe
            style="width: 100%; height: 100%;"
            v-loading="executing"
            element-loading-text="正在查询数据库..."
        >
          <template v-if="queryResult.length > 0">
            <el-table-column
                v-for="key in Object.keys(queryResult[0])"
                :key="key"
                :prop="key"
                :label="key"
                min-width="150"
                show-overflow-tooltip
                sortable
            />
          </template>
          <template #empty>
            <el-empty description="暂无结果，请执行 SQL 查询" />
          </template>
        </el-table>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import request from '@/utils/request.js'
import { ElMessage } from 'element-plus'
// 引入图标
import { Grid, VideoPlay, Search, DataAnalysis } from '@element-plus/icons-vue' // 加上 DataAnalysis

// --- 状态数据 ---
const sourceList = ref([])
const currentSourceId = ref(null)
const tableList = ref([])
const searchTableText = ref('') // 搜索关键词
const currentTable = ref('')    // 当前选中的表

const sqlContent = ref('')
const queryResult = ref([])
const lastExecTime = ref(null)  // 耗时统计

const treeLoading = ref(false)
const executing = ref(false)
const analyzing = ref(false)

// --- 计算属性：表名过滤 ---
const filteredTableList = computed(() => {
  if (!searchTableText.value) return tableList.value
  return tableList.value.filter(t => t.toLowerCase().includes(searchTableText.value.toLowerCase()))
})

// --- 方法 ---

// 1. 加载所有数据源
const loadSources = async () => {
  try {
    const res = await request.get('/datasource/list')
    sourceList.value = res
  } catch(e) {}
}

// 2. 切换数据源
const handleSourceChange = async (val) => {
  if (!val) return
  treeLoading.value = true
  tableList.value = []
  queryResult.value = []
  currentTable.value = ''
  sqlContent.value = ''
  lastExecTime.value = null

  try {
    const res = await request.get('/meta/tables', { params: { sourceId: val } })
    tableList.value = res
  } catch(e) {
    // 错误处理已在拦截器中
  } finally {
    treeLoading.value = false
  }
}

// 3. 点击表名：生成 SQL
const handleTableClick = (tableName) => {
  currentTable.value = tableName
  sqlContent.value = `SELECT * FROM ${tableName} LIMIT 50`
}

// 4. 执行查询
const handleExecute = async () => {
  if (!currentSourceId.value) {
    ElMessage.warning('请先选择数据源')
    return
  }
  if (!sqlContent.value.trim()) {
    ElMessage.warning('请输入 SQL 语句')
    return
  }

  executing.value = true
  lastExecTime.value = null
  queryResult.value = []

  const startTime = Date.now()

  try {
    const res = await request.post('/meta/query', {
      sourceId: currentSourceId.value,
      sql: sqlContent.value,
      limit: 1000 // 显式传参，虽然DTO有默认值
    })

    queryResult.value = res
    lastExecTime.value = Date.now() - startTime

  } catch (error) {
    // request.js 会弹红框，这里不用重复弹
  } finally {
    executing.value = false
  }
}

const handleExplain = async () => {
  if (!currentSourceId.value) {
    ElMessage.warning('请先选择数据源')
    return
  }
  if (!sqlContent.value.trim()) {
    ElMessage.warning('请输入 SQL 语句')
    return
  }

  const upperSql = sqlContent.value.trim().toUpperCase()
  if (!upperSql.startsWith('SELECT')) {
    ElMessage.warning('只有 SELECT 查询语句支持优化分析')
    return
  }

  // 清空之前的执行耗时，因为 EXPLAIN 通常瞬间完成，不需要统计耗时
  analyzing.value = true
  lastExecTime.value = null
  queryResult.value = []

  try {
    const res = await request.post('/meta/explain', {
      sourceId: currentSourceId.value,
      sql: sqlContent.value
      // explain 不需要 limit 和 timeout，传默认的即可
    })

    queryResult.value = res
    ElMessage.success('SQL 分析完成，请查看下方执行计划')
  } catch (error) {
    // request.js 处理报错
  } finally {
    analyzing.value = false
  }
}

const clearSql = () => {
  sqlContent.value = ''
  queryResult.value = []
  lastExecTime.value = null
}

onMounted(() => {
  loadSources()
})
</script>

<style scoped>
/* 样式美化 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.table-item {
  padding: 8px 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  font-size: 14px;
  color: #606266;
  transition: all 0.2s;
  border-bottom: 1px solid #f5f7fa;
}

.table-item:hover {
  background-color: #ecf5ff;
  color: #409EFF;
}

.table-item.active {
  background-color: #ecf5ff;
  color: #409EFF;
  font-weight: bold;
  border-right: 3px solid #409EFF;
}

.table-name {
  margin-left: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-bar {
  margin-bottom: 10px;
}

/* 强制让表格撑满容器 */
:deep(.el-table__inner-wrapper) {
  height: 100% !important;
}
</style>