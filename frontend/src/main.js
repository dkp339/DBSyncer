import { createApp } from 'vue'
import './style.css'
import App from './App.vue'

// 1. 导入 Element Plus 及其样式
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
// 导入所有图标
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// 2. 导入路由
import router from './router'

const app = createApp(App)

// 3. 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}

// 4. 使用插件
app.use(router)
app.use(ElementPlus)

app.mount('#app')