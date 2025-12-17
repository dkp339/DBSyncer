<template>
  <el-container class="layout-container">
    <el-aside width="200px" class="aside">
      <div class="logo">DBSyncer 系统</div>

      <el-menu
          active-text-color="#ffd04b"
          background-color="#545c64"
          class="el-menu-vertical-demo"
          default-active="/welcome"
          text-color="#fff"
          router
          style="height: 100%; border-right: 0;"
      >
        <el-menu-item index="/welcome">
          <el-icon><HomeFilled /></el-icon>
          <span>系统首页</span>
        </el-menu-item>

        <el-menu-item index="/datasource">
          <el-icon><DataLine /></el-icon>
          <span>数据源配置</span>
        </el-menu-item>

      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-content">
          <span>管理员</span>
          <el-button type="danger" link size="small" @click="logout">退出登录</el-button>
        </div>
      </el-header>

      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { HomeFilled, DataLine } from '@element-plus/icons-vue'

const router = useRouter()

const logout = () => {
  localStorage.removeItem('jwt_token')
  router.push('/login')
}
</script>

<style scoped>
.layout-container {
  height: 100vh; /* 全屏高度 */
}

.aside {
  background-color: #545c64;
  color: white;
}

.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  font-weight: bold;
  font-size: 20px;
  background-color: #434a50;
}

.header {
  background-color: #fff;
  border-bottom: 1px solid #dcdfe6;
  display: flex;
  align-items: center;
  justify-content: flex-end; /* 内容靠右 */
}

.header-content {
  font-size: 14px;
}
.header-content span {
  margin-right: 15px;
}
</style>