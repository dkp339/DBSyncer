<template>
  <el-container class="layout-container">
    <el-aside width="200px" class="aside">
      <div class="logo">DBSyncer 系统</div>

      <el-menu
          active-text-color="#ffd04b"
          background-color="#545c64"
          class="el-menu-vertical-demo"
          :default-active="activeMenu"
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

        <el-menu-item index="/query">
          <el-icon><Search /></el-icon> <span>数据查询台</span>
        </el-menu-item>

        <el-menu-item index="/sys/config" v-if="isAdmin">
          <el-icon><Setting /></el-icon>
          <span>系统配置</span>
        </el-menu-item>

      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-content">
          <div class="user-info">
            <el-avatar :size="30" icon="UserFilled" style="margin-right: 10px; background-color: #409EFF;" />

            <span class="username">{{ userName }}</span>

            <el-tag
                size="small"
                :type="isAdmin ? 'danger' : 'info'"
                effect="dark"
                style="margin-left: 8px; margin-right: 15px;"
            >
              {{ roleName }}
            </el-tag>
          </div>

          <el-divider direction="vertical" />

          <el-button type="danger" link size="small" @click="logout" style="margin-left: 15px;">
            <el-icon style="margin-right: 3px"><SwitchButton /></el-icon> 退出登录
          </el-button>
        </div>
      </el-header>

      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRouter, useRoute } from 'vue-router'
import {
  HomeFilled, DataLine, Search, Setting,
  UserFilled, SwitchButton
} from '@element-plus/icons-vue' // 引入新图标
import { ref, onMounted, computed } from 'vue'

const router = useRouter()
const route = useRoute()

// 状态变量
const isAdmin = ref(false)
const userName = ref('')
const userRole = ref('')

const activeMenu = computed(() => route.path)

// 计算角色显示名称
const roleName = computed(() => {
  return isAdmin.value ? '管理员' : '普通用户'
})

onMounted(() => {
  // 1. 从 LocalStorage 获取信息
  const role = localStorage.getItem('user_role')
  const name = localStorage.getItem('user_name')

  // 2. 赋值
  userRole.value = role || 'USER'
  userName.value = name || '未登录用户'

  // 3. 判断权限
  isAdmin.value = (role === 'ADMIN')
})

const logout = () => {
  // 清除所有缓存
  localStorage.removeItem('jwt_token')
  localStorage.removeItem('user_role')
  localStorage.removeItem('user_name')

  // 跳转登录
  router.push('/login')
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
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
  color: #fff;
}

.header {
  background-color: #fff;
  border-bottom: 1px solid #dcdfe6;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0 20px;
  box-shadow: 0 1px 4px rgba(0,21,41,.08); /* 加一点阴影更有层次感 */
}

.header-content {
  display: flex;
  align-items: center;
  font-size: 14px;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: default;
}

.username {
  font-weight: 500;
  color: #303133;
}
</style>