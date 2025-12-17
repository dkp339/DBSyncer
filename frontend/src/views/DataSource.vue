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
        <el-table-column prop="name" label="名称" min-width="120" />
        <el-table-column prop="dbType" label="类型" width="120">
          <template #default="scope">
            <el-tag :type="getDbTypeTag(scope.row.dbType)">{{ scope.row.dbType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="host" label="主机地址" min-width="150" />
        <el-table-column prop="port" label="端口" width="80" />
        <el-table-column prop="dbName" label="数据库/服务名" min-width="120" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="scope">
            <el-popconfirm title="确定要删除该数据源吗？" @confirm="handleDelete(scope.row.id)">
              <template #reference>
                <el-button type="danger" link size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="新建数据源" width="500px" @close="resetForm">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
        <el-form-item label="连接名称" prop="name">
          <el-input v-model="formData.name" placeholder="例如：主生产库" />
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
          <el-input v-model="formData.port" placeholder="例如：3306" />
        </el-form-item>

        <el-form-item label="数据库名" prop="dbName">
          <el-input v-model="formData.dbName" placeholder="库名 或 ServiceName" />
        </el-form-item>

        <el-form-item label="用户名" prop="username">
          <el-input v-model="formData.username" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input v-model="formData.password" type="password" show-password />
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
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request.js'
import { ElMessage } from 'element-plus'

// --- 状态变量 ---
const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const submitting = ref(false)
const testing = ref(false)
const formRef = ref(null)

// 表单数据
const formData = reactive({
  name: '',
  dbType: '',
  host: '',
  port: '',
  dbName: '',
  username: '',
  password: ''
})

// 表单校验规则
const rules = {
  name: [{ required: true, message: '请输入连接名称', trigger: 'blur' }],
  dbType: [{ required: true, message: '请选择数据库类型', trigger: 'change' }],
  host: [{ required: true, message: '请输入主机地址', trigger: 'blur' }],
  port: [{ required: true, message: '请输入端口', trigger: 'blur' }],
  dbName: [{ required: true, message: '请输入数据库名', trigger: 'blur' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// --- 方法 ---

// 1. 获取列表
const fetchList = async () => {
  loading.value = true
  try {
    // 你的 request.js 已经配置了 baseURL='/api'
    // 且响应拦截器返回了 response.data，所以这里 res 直接就是 List<DataSourceConfig>
    const res = await request.get('/datasource/list')
    tableData.value = res
  } catch (error) {

    console.error(error)
  } finally {
    loading.value = false
  }
}

// 2. 点击新增
const handleAdd = () => {
  dialogVisible.value = true
}

// 3. 处理数据库类型改变
const handleTypeChange = (val) => {
  if (val === 'MYSQL') formData.port = '3306'
  else if (val === 'ORACLE') formData.port = '1521'
  else if (val === 'SQL_SERVER') formData.port = '1433'
  else if (val === 'POSTGRESQL') formData.port = '5432'
}

// 4. 提交保存
const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        await request.post('/datasource/add', formData)
        ElMessage.success('添加成功')
        dialogVisible.value = false
        await fetchList() // 刷新列表
      } catch (error) {
        // request.js 会处理错误弹窗
      } finally {
        submitting.value = false
      }
    }
  })
}

// 5. 测试连接
const handleTestConnection = async () => {
  if (!formData.dbType || !formData.host || !formData.port || !formData.username || !formData.password) {
    ElMessage.warning('请先填写完整的数据库连接信息')
    return
  }

  testing.value = true
  try {
    // 后端成功(200)才会走这里，失败(500)会进 catch
    // request.js 拦截器保证了只有成功才会到这里
    const res = await request.post('/datasource/test', formData)

    // 如果后端返回普通字符串 "连接成功"，res 就是字符串
    ElMessage.success(res || '连接测试成功！')
  } catch (error) {
    // 失败逻辑已经被 request.js 里的 ElMessage.error(msg) 处理了
  } finally {
    testing.value = false
  }
}

// 6. 删除
const handleDelete = async (id) => {
  try {
    await request.delete(`/datasource/${id}`)
    ElMessage.success('删除成功')
    await fetchList()
  } catch (error) {
    // 错误处理
  }
}

// 7. 重置表单
const resetForm = () => {
  if (formRef.value) formRef.value.resetFields()
  Object.keys(formData).forEach(key => formData[key] = '')
}

// 8. 标签颜色
const getDbTypeTag = (type) => {
  const map = {
    'MYSQL': 'success',
    'ORACLE': 'danger',
    'SQL_SERVER': 'info',
    'POSTGRESQL': 'primary'
  }
  return map[type] || 'info'
}

onMounted(() => {
  fetchList()
})
</script>