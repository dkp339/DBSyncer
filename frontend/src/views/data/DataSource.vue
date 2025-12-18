<template>
  <div class="app-container" style="padding: 20px;">
    <el-card shadow="never" class="mb-20">
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <span style="font-weight: bold; font-size: 16px;">数据源管理</span>
        <el-button type="primary" icon="Plus" @click="handleAdd">新建数据源</el-button>
      </div>
    </el-card>

    <el-card shadow="never" style="margin-top: 20px;">
      <el-table :data="tableData" v-loading="loading" style="width: 100%" stripe>
        <el-table-column prop="sourceName" label="名称" min-width="120" />

        <el-table-column prop="dbType" label="类型" width="140">
          <template #default="scope">
            <el-tag :type="getDbTypeTag(scope.row.dbType)">
              {{ getDbTypeName(scope.row.dbType) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="host" label="主机地址" min-width="150" />
        <el-table-column prop="port" label="端口" width="80" />
        <el-table-column prop="dbName" label="数据库名" min-width="120" />

        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-switch
                v-model="scope.row.status"
                :active-value="1"
                :inactive-value="0"
                @change="handleStatusChange(scope.row)"
            />
          </template>
        </el-table-column>

        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button type="primary" link icon="Edit" @click="handleEdit(scope.row)">
              编辑
            </el-button>
            <el-divider direction="vertical" />
            <el-popconfirm title="确定要删除该数据源吗？" @confirm="handleDelete(scope.row.sourceId)">
              <template #reference>
                <el-button type="danger" link icon="Delete">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
        v-model="dialogVisible"
        :title="isEditMode ? '编辑数据源' : '新建数据源'"
        width="500px"
        @close="resetForm"
    >
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
        <el-form-item label="连接名称" prop="sourceName">
          <el-input v-model="formData.sourceName" placeholder="例如：主生产库" />
        </el-form-item>

        <el-form-item label="数据库类型" prop="dbType">
          <el-select v-model="formData.dbType" placeholder="请选择" style="width: 100%" @change="handleTypeChange">
            <el-option label="MySQL" value="MYSQL" />
            <el-option label="Oracle" value="ORACLE" />
            <el-option label="SQL Server" value="SQL_SERVER" />
            <el-option label="PostgreSQL" value="POSTGRESQL" />
          </el-select>
        </el-form-item>

        <el-form-item label="主机地址" prop="host">
          <el-input v-model="formData.host" placeholder="例如：127.0.0.1" />
        </el-form-item>

        <el-form-item label="端口" prop="port">
          <el-input v-model="formData.port" />
        </el-form-item>

        <el-form-item label="数据库名" prop="dbName">
          <el-input v-model="formData.dbName" placeholder="库名 或 ServiceName" />
        </el-form-item>

        <el-form-item label="用户名" prop="username">
          <el-input v-model="formData.username" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
              v-model="formData.password"
              type="password"
              show-password
              :placeholder="isEditMode ? '不修改请留空' : '请输入密码'"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div style="display: flex; justify-content: space-between;">
          <el-button type="warning" :loading="testing" @click="handleTestConnection">
            测试连接
          </el-button>

          <div>
            <el-button @click="dialogVisible = false">取消</el-button>
            <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import request from '@/utils/request.js'
import { ElMessage } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'

// --- 状态变量 ---
const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const submitting = ref(false)
const testing = ref(false)
const formRef = ref(null)
const isEditMode = ref(false) // 标记当前模式

// 表单数据
const formData = reactive({
  sourceId: null, // 自增字段
  sourceName: '',
  dbType: '',
  host: '',
  port: '',
  dbName: '',
  username: '',
  password: ''
})

// --- 动态校验规则 ---
const rules = computed(() => {
  return {
    sourceName: [{ required: true, message: '请输入连接名称', trigger: 'blur' }],
    dbType: [{ required: true, message: '请选择数据库类型', trigger: 'change' }],
    host: [{ required: true, message: '请输入主机地址', trigger: 'blur' }],
    port: [{ required: true, message: '请输入端口', trigger: 'blur' }],
    dbName: [{ required: true, message: '请输入数据库名', trigger: 'blur' }],
    username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
    // 密码部分置空
    password: [{ required: !isEditMode.value, message: '请输入密码', trigger: 'blur' }]
  }
})

// --- 方法 ---

const fetchList = async () => {
  loading.value = true
  try {
    const res = await request.get('/datasource/list')
    tableData.value = res
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 点击新增
const handleAdd = () => {
  isEditMode.value = false
  resetForm()
  dialogVisible.value = true
}

// 点击编辑
const handleEdit = (row) => {
  isEditMode.value = true
  // 数据回显
  // Object.assign(目标, 源)
  Object.assign(formData, row)

  // 密码置空，避免显示 "******" 让用户困惑
  formData.password = ''

  dialogVisible.value = true
}

const handleTypeChange = (val) => {
  const portMap = { 'MYSQL': '3306', 'ORACLE': '1521', 'SQL_SERVER': '1433', 'POSTGRESQL': '5432' }
  if(portMap[val]) formData.port = portMap[val]
}

// 提交保存 (区分新增/更新)
const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        if (isEditMode.value) {
          // 编辑接口
          await request.put('/datasource/update', formData)
          ElMessage.success('更新成功')
        } else {
          // 新增接口
          await request.post('/datasource/add', formData)
          ElMessage.success('添加成功')
        }
        dialogVisible.value = false
        await fetchList()
      } catch (error) {
        // request.js 处理错误
      } finally {
        submitting.value = false
      }
    }
  })
}

// 状态切换
const handleStatusChange = async (row) => {
  try {
    // 参数结构对应 StatusParam
    await request.put('/datasource/status', {
      id: row.sourceId,
      status: row.status
    })
    ElMessage.success('状态已更新')
  } catch (error) {
    // 失败回滚开关状态
    row.status = row.status === 1 ? 0 : 1
  }
}

// 测试连接
const handleTestConnection = async () => {
  // 简单校验
  if (!formData.dbType || !formData.host) {
    ElMessage.warning('请至少填写类型和主机')
    return
  }

  if (!formData.password && !isEditMode.value) {
    ElMessage.warning('请输入密码进行测试')
    return
  }

  if (isEditMode.value && !formData.password) {
    ElMessage.warning('为了安全，测试连接时请手动输入密码')
    return
  }

  testing.value = true
  try {
    await request.post('/datasource/test', formData)
    ElMessage.success('连接测试成功！')
  } finally {
    testing.value = false
  }
}

const handleDelete = async (id) => {
  try {
    await request.delete(`/datasource/${id}`)
    ElMessage.success('删除成功')
    await fetchList()
  } catch (error) {}
}

const resetForm = () => {
  if (formRef.value) formRef.value.resetFields()
  formData.sourceId = null
  Object.keys(formData).forEach(key => {
    if (key !== 'sourceId') formData[key] = ''
  })
}

// 显示辅助函数
const getDbTypeTag = (type) => {
  const map = { 'MYSQL': 'success', 'ORACLE': 'danger', 'SQL_SERVER': 'info', 'POSTGRESQL': 'primary' }
  return map[type] || 'info'
}

const getDbTypeName = (type) => {
  const map = {
    'MYSQL': 'MySQL数据库',
    'ORACLE': 'Oracle数据库',
    'SQL_SERVER': 'SQL Server数据库',
    'POSTGRESQL': 'PostgreSQL数据库'
  }
  return map[type] || type
}

onMounted(() => {
  fetchList()
})
</script>