<template>
  <div class="app-container" style="padding: 20px;">
    <el-card shadow="never" style="max-width: 800px; margin: 0 auto;">
      <template #header>
        <span style="font-weight: bold;">系统参数配置</span>
      </template>

      <el-form label-position="top">
        <el-alert
            title="关于周期同步"
            type="info"
            description="除了实时同步外，系统会根据此处的配置定期扫描并重试失败的任务，以确保最终一致性。"
            show-icon
            :closable="false"
            style="margin-bottom: 20px;"
        />

        <el-form-item label="周期同步策略 (Cron表达式)">
          <div style="display: flex; width: 100%; gap: 10px;">
            <el-input v-model="cron" placeholder="例如: 0 0 1 * * ?" />
            <el-button type="primary" :loading="saving" @click="saveCron">保存配置</el-button>
          </div>
          <div style="margin-top: 10px;">
            <el-button size="small" @click="cron = '0 0/1 * * * ?'">每分钟 (测试用)</el-button>
            <el-button size="small" @click="cron = '0 0 * * * ?'">每小时</el-button>
            <el-button size="small" @click="cron = '0 0 1 * * ?'">每天凌晨1点</el-button>
          </div>
        </el-form-item>

        <el-divider />

        <el-form-item label="通知邮箱设置">
          <el-input disabled model-value="通过后端 application.yml 配置" />
          <div class="form-tip">冲突报警邮件将发送至后端配置的管理员邮箱。</div>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request.js'
import { ElMessage } from 'element-plus'

const cron = ref('')
const saving = ref(false)

const loadConfig = async () => {
  try {
    const res = await request.get('/admin/config/cron')
    cron.value = res
  } catch(e) {}
}

const saveCron = async () => {
  if (!cron.value) return
  saving.value = true
  try {
    await request.post('/admin/config/cron', { cron: cron.value })
    ElMessage.success('配置已更新，下次调度生效')
  } catch(e) {
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadConfig()
})
</script>

<style scoped>
.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}
</style>