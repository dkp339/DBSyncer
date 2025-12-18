<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <div class="card-header">
          <span>数据库同步管理系统</span>
        </div>
      </template>

      <el-form :model="loginForm" :rules="rules" ref="loginFormRef">
        <el-form-item prop="username">
          <el-input v-model="loginForm.username" placeholder="请输入用户名" prefix-icon="User" />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              prefix-icon="Lock"
              show-password
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleLogin" style="width: 100%">
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const router = useRouter()
const loginFormRef = ref(null)
const loading = ref(false)

// 定义表单数据
const loginForm = reactive({
  username: '',
  password: ''
})

// 定义校验规则
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// 处理登录逻辑
const handleLogin = () => {
  loginFormRef.value.validate((valid) => {
    if (valid) {
      loading.value = true

      // 发送请求给后端
      request.post('/auth/login', {
        username: loginForm.username,
        password: loginForm.password
      }).then(res => {

        const token = res.token

        if (token) {
          localStorage.setItem('jwt_token', token)
          ElMessage.success('登录成功')
          router.push('/')
        } else {
          throw new Error('未获取到 Token')
        }

      }).catch(err => {
        console.error('登录流程中断', err)

      }).finally(() => {
        loading.value = false
      })
    }
  })
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f0f2f5;
}
.login-card {
  width: 400px;
}
.card-header {
  text-align: center;
  font-weight: bold;
  font-size: 18px;
}
</style>