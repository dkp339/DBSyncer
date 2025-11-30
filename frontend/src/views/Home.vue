<template>
  <div style="padding: 20px;">
    <h1>欢迎进入系统</h1>
    <el-button type="danger" @click="logout">退出登录</el-button>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const logout = () => {
  // 1. 清除 Token
  localStorage.removeItem('jwt_token')
  // 2. 跳转回登录页
  router.push('/login')
}
</script>